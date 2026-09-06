package com.shohankhan.bokeya.backup

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.BokeyaDatabase
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.TxType
import kotlinx.coroutines.flow.first
import java.time.LocalDate

enum class ReportKind(val label: String) {
    FULL("সম্পূর্ণ report"),
    DEBT("বকেয়া report"),
    LOAN("Loan report"),
    EMI("EMI report"),
    INCOME("আয় report"),
    EXPENSE("খরচ report"),
}

data class ReportData(
    val kind: ReportKind,
    val from: LocalDate,
    val to: LocalDate,
    val totalIncome: Money,
    val totalExpense: Money,
    val totalDebt: Money,
    val totalPaid: Money,
    val remaining: Money,
    val upcoming: Money,
    val categoryRows: List<Pair<String, Money>>,
    val accountRows: List<AccountRow>,
    val transactionRows: List<TransactionRow>,
) {
    data class AccountRow(
        val title: String,
        val type: String,
        val direction: String,
        val total: Money,
        val paid: Money,
        val remaining: Money,
        val status: String,
    )

    data class TransactionRow(
        val date: LocalDate,
        val title: String,
        val type: String,
        val category: String,
        val amount: Money,
    )
}

class ReportBuilder(
    private val repository: BokeyaRepository,
    private val db: BokeyaDatabase,
) {
    suspend fun build(kind: ReportKind, from: LocalDate, to: LocalDate): ReportData {
        val summaries = repository.summaries.first()
        val transactions = repository.transactionsBetweenOnce(from, to)
        val categories = db.categoryDao().allOnce().associateBy { it.id }

        val filteredAccounts = when (kind) {
            ReportKind.LOAN -> summaries.filter { it.type == AccountType.LOAN }
            ReportKind.EMI -> summaries.filter { it.type == AccountType.EMI }
            ReportKind.DEBT -> summaries.filter { it.direction == Direction.I_OWE }
            ReportKind.INCOME, ReportKind.EXPENSE -> emptyList()
            ReportKind.FULL -> summaries
        }

        val filteredTx = when (kind) {
            ReportKind.INCOME -> transactions.filter { it.type == TxType.INCOME }
            ReportKind.EXPENSE -> transactions.filter { it.type == TxType.EXPENSE }
            ReportKind.LOAN -> transactions.filter { it.type == TxType.LOAN_PAYMENT || it.type == TxType.LOAN }
            ReportKind.EMI -> transactions.filter { it.type == TxType.EMI_PAYMENT || it.type == TxType.EMI }
            ReportKind.DEBT -> transactions.filter {
                it.type == TxType.PAYMENT || it.type == TxType.BORROWED || it.type == TxType.SHOP_PURCHASE
            }
            ReportKind.FULL -> transactions
        }

        val categoryTotals = repository.categoryTotals(TxType.EXPENSE, from, to)
            .map { (it.name ?: "অন্যান্য") to Money(it.total) }

        return ReportData(
            kind = kind,
            from = from,
            to = to,
            totalIncome = Money(transactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }),
            totalExpense = Money(transactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }),
            totalDebt = Money(summaries.filter { it.direction == Direction.I_OWE }.sumOf { it.total.poisha }),
            totalPaid = Money(summaries.filter { it.direction == Direction.I_OWE }.sumOf { it.paid.poisha }),
            remaining = Money(summaries.filter { it.direction == Direction.I_OWE }.sumOf { it.remaining.poisha }),
            upcoming = Money(
                repository.upcomingPayments(LocalDate.now(), LocalDate.now().plusDays(30))
                    .sumOf { it.amount.poisha },
            ),
            categoryRows = categoryTotals,
            accountRows = filteredAccounts.map {
                ReportData.AccountRow(
                    it.title, it.type.label,
                    it.direction.label, it.total, it.paid, it.remaining, it.status.label,
                )
            },
            transactionRows = filteredTx.map {
                ReportData.TransactionRow(
                    date = LocalDate.ofEpochDay(it.date),
                    title = it.title,
                    type = it.type.label,
                    category = it.categoryId?.let { id -> categories[id]?.name } ?: "—",
                    amount = Money(it.amount),
                )
            },
        )
    }
}

object CsvExporter {

    fun toCsv(report: ReportData): String = buildString {
        appendLine("Bokeya Report,${report.kind.label}")
        appendLine("From,${report.from},To,${report.to}")
        appendLine()
        appendLine("Summary")
        appendLine("Total Income,${plain(report.totalIncome)}")
        appendLine("Total Expense,${plain(report.totalExpense)}")
        appendLine("Total Debt,${plain(report.totalDebt)}")
        appendLine("Total Paid,${plain(report.totalPaid)}")
        appendLine("Remaining,${plain(report.remaining)}")
        appendLine("Upcoming 30 days,${plain(report.upcoming)}")
        appendLine()
        if (report.accountRows.isNotEmpty()) {
            appendLine("Accounts")
            appendLine("Title,Type,Direction,Total,Paid,Remaining,Status")
            report.accountRows.forEach {
                appendLine(
                    listOf(
                        esc(it.title), esc(it.type), esc(it.direction),
                        plain(it.total), plain(it.paid), plain(it.remaining), esc(it.status),
                    ).joinToString(","),
                )
            }
            appendLine()
        }
        if (report.categoryRows.isNotEmpty()) {
            appendLine("Expense by Category")
            appendLine("Category,Amount")
            report.categoryRows.forEach { appendLine("${esc(it.first)},${plain(it.second)}") }
            appendLine()
        }
        if (report.transactionRows.isNotEmpty()) {
            appendLine("Transactions")
            appendLine("Date,Title,Type,Category,Amount")
            report.transactionRows.forEach {
                appendLine(
                    listOf(
                        it.date.toString(), esc(it.title), esc(it.type), esc(it.category), plain(it.amount),
                    ).joinToString(","),
                )
            }
        }
    }

    fun write(context: Context, uri: Uri, csv: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } != null
    }.getOrDefault(false)

    /** Parses a Bokeya transaction CSV block for import preview. */
    fun parseTransactions(text: String): List<ImportRow> {
        val lines = text.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("Date,Title,Type,Category,Amount") }
        if (headerIndex < 0) return emptyList()
        return lines.drop(headerIndex + 1)
            .takeWhile { it.isNotBlank() }
            .mapNotNull { line ->
                val cols = splitCsv(line)
                if (cols.size < 5) return@mapNotNull null
                val date = runCatching { LocalDate.parse(cols[0]) }.getOrNull() ?: return@mapNotNull null
                val amount = Money.parseOrNull(cols[4]) ?: return@mapNotNull null
                ImportRow(date, cols[1], cols[2], cols[3], amount)
            }
    }

    private fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun plain(money: Money) = money.toString()
    private fun esc(value: String) = if (value.contains(',') || value.contains('"')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

data class ImportRow(
    val date: LocalDate,
    val title: String,
    val type: String,
    val category: String,
    val amount: Money,
)

/**
 * PDF is drawn with the platform PdfDocument so no third-party dependency (and no network
 * font fetch) is needed. Layout mirrors the in-app report card styling.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44f

    fun write(context: Context, uri: Uri, report: ReportData): Boolean = runCatching {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        val title = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 22f
            color = 0xFF17394C.toInt()
            isAntiAlias = true
        }
        val heading = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 13f
            color = 0xFF1F4B63.toInt()
            isAntiAlias = true
        }
        val body = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = 11f
            color = 0xFF1A1A1A.toInt()
            isAntiAlias = true
        }
        val muted = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = 10f
            color = 0xFF6B7280.toInt()
            isAntiAlias = true
        }
        val rule = Paint().apply { color = 0xFFE0E4E9.toInt(); strokeWidth = 1f }

        fun newPageIfNeeded(needed: Float) {
            if (y + needed < PAGE_HEIGHT - MARGIN) return
            document.finishPage(page)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            canvas = page.canvas
            y = MARGIN
        }

        fun line(text: String, paint: Paint, gap: Float = 16f, x: Float = MARGIN) {
            newPageIfNeeded(gap)
            canvas.drawText(text, x, y, paint)
            y += gap
        }

        fun row(left: String, right: String, paint: Paint = body) {
            newPageIfNeeded(16f)
            canvas.drawText(left, MARGIN, y, paint)
            val width = paint.measureText(right)
            canvas.drawText(right, PAGE_WIDTH - MARGIN - width, y, paint)
            y += 16f
        }

        fun divider() {
            newPageIfNeeded(12f)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule)
            y += 14f
        }

        line("Bokeya", title, 26f)
        line(report.kind.label, muted, 14f)
        line("${report.from} — ${report.to}", muted, 20f)
        divider()

        line("সারসংক্ষেপ / Summary", heading, 20f)
        row("Total Income", CurrencyFormatter.format(report.totalIncome, banglaDigits = false))
        row("Total Expense", CurrencyFormatter.format(report.totalExpense, banglaDigits = false))
        row("Net", CurrencyFormatter.format(report.totalIncome - report.totalExpense, banglaDigits = false))
        row("Total Debt", CurrencyFormatter.format(report.totalDebt, banglaDigits = false))
        row("Total Paid", CurrencyFormatter.format(report.totalPaid, banglaDigits = false))
        row("Remaining", CurrencyFormatter.format(report.remaining, banglaDigits = false))
        row("Upcoming (30d)", CurrencyFormatter.format(report.upcoming, banglaDigits = false))
        y += 6f
        divider()

        if (report.accountRows.isNotEmpty()) {
            line("Accounts", heading, 20f)
            report.accountRows.forEach {
                newPageIfNeeded(30f)
                canvas.drawText("${it.title}  (${it.type})", MARGIN, y, body)
                val amount = CurrencyFormatter.format(it.remaining, banglaDigits = false)
                canvas.drawText(amount, PAGE_WIDTH - MARGIN - body.measureText(amount), y, body)
                y += 13f
                canvas.drawText(
                    "${it.direction} · ${it.status} · paid ${CurrencyFormatter.format(it.paid, banglaDigits = false)}",
                    MARGIN, y, muted,
                )
                y += 17f
            }
            divider()
        }

        if (report.categoryRows.isNotEmpty()) {
            line("Expense by Category", heading, 20f)
            report.categoryRows.forEach {
                row(it.first, CurrencyFormatter.format(it.second, banglaDigits = false))
            }
            divider()
        }

        if (report.transactionRows.isNotEmpty()) {
            line("Transactions", heading, 20f)
            report.transactionRows.take(400).forEach {
                newPageIfNeeded(16f)
                canvas.drawText("${it.date}  ${it.title.take(38)}", MARGIN, y, body)
                val amount = CurrencyFormatter.format(it.amount, banglaDigits = false)
                canvas.drawText(amount, PAGE_WIDTH - MARGIN - body.measureText(amount), y, body)
                y += 15f
            }
        }

        newPageIfNeeded(30f)
        y += 8f
        canvas.drawText("Generated by Bokeya · bokeya app · offline & private", MARGIN, y, muted)

        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
        document.close()
        true
    }.getOrDefault(false)
}
