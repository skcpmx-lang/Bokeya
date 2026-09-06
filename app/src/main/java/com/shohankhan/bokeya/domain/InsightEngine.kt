package com.shohankhan.bokeya.domain

import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import java.time.LocalDate

/**
 * Local, rules-based intelligence. No network, no model — deterministic statements derived
 * purely from the user's own data, phrased without false certainty.
 */
object InsightEngine {

    data class Input(
        val today: LocalDate,
        val totalIOwe: Money,
        val totalTheyOwe: Money,
        val breakdown: Map<AccountType, Money>,
        val overdueCount: Int,
        val overdueAmount: Money,
        val next7DaysAmount: Money,
        val next7DaysCount: Int,
        val monthIncome: Money,
        val monthExpense: Money,
        val previousMonthExpense: Money,
        val topExpenseCategory: String?,
        val topExpenseAmount: Money,
        val nearestDueDays: Long?,
        val totalPaidAllTime: Money,
    )

    fun generate(input: Input): List<Insight> {
        val insights = mutableListOf<Insight>()

        if (input.overdueCount > 0) {
            insights += Insight(
                id = "overdue",
                text = "${bn(input.overdueCount)}টি হিসাবের তারিখ পেরিয়ে গেছে — মোট ${money(input.overdueAmount)}।",
                tone = InsightTone.WARNING,
            )
        }

        if (input.next7DaysCount > 0) {
            insights += Insight(
                id = "next7",
                text = "আগামী ৭ দিনে আপনার প্রায় ${money(input.next7DaysAmount)} পরিশোধ করতে হবে।",
                tone = InsightTone.NEUTRAL,
            )
        }

        input.nearestDueDays?.let { days ->
            if (days in 0..2 && input.overdueCount == 0) {
                val phrase = when (days) {
                    0L -> "আজই"
                    1L -> "আগামীকাল"
                    else -> "${bn(days.toInt())} দিন পর"
                }
                insights += Insight(
                    id = "nearest",
                    text = "আপনার একটি payment-এর সময় $phrase।",
                    tone = InsightTone.WARNING,
                )
            }
        }

        if (input.previousMonthExpense.isPositive && input.monthExpense.isPositive) {
            val diff = input.monthExpense - input.previousMonthExpense
            val ratio = diff.poisha.toDouble() / input.previousMonthExpense.poisha.toDouble()
            when {
                ratio > 0.15 -> insights += Insight(
                    id = "spend_up",
                    text = "এই মাসে আপনার খরচ গত মাসের তুলনায় বেশি হচ্ছে।",
                    tone = InsightTone.WARNING,
                )
                ratio < -0.15 -> insights += Insight(
                    id = "spend_down",
                    text = "এই মাসে গত মাসের তুলনায় খরচ কম হয়েছে।",
                    tone = InsightTone.POSITIVE,
                )
            }
        }

        if (input.topExpenseCategory != null && input.topExpenseAmount.isPositive) {
            insights += Insight(
                id = "top_category",
                text = "এই মাসে ${input.topExpenseCategory} খাতে সবচেয়ে বেশি খরচ হয়েছে — ${money(input.topExpenseAmount)}।",
                tone = InsightTone.NEUTRAL,
            )
        }

        val emiShare = input.breakdown[AccountType.EMI] ?: Money.ZERO
        if (input.totalIOwe.isPositive && emiShare.poisha * 2 > input.totalIOwe.poisha) {
            insights += Insight(
                id = "emi_heavy",
                text = "আপনার মোট বকেয়ার বড় অংশ EMI হিসেবে রয়েছে।",
                tone = InsightTone.NEUTRAL,
            )
        }

        val leftover = input.monthIncome - input.monthExpense
        if (input.monthIncome.isPositive) {
            if (leftover.isPositive) {
                insights += Insight(
                    id = "leftover",
                    text = "এই মাসে আপনার আয় থেকে আনুমানিক ${money(leftover)} অবশিষ্ট আছে।",
                    tone = InsightTone.POSITIVE,
                )
            } else if (leftover.isNegative) {
                insights += Insight(
                    id = "overspend",
                    text = "এই মাসে আয়ের চেয়ে খরচ বেশি হয়েছে — খেয়াল রাখুন।",
                    tone = InsightTone.WARNING,
                )
            }
        }

        if (input.totalTheyOwe.isPositive) {
            insights += Insight(
                id = "receivable",
                text = "অন্যদের কাছে আপনার ${money(input.totalTheyOwe)} পাওনা রয়েছে।",
                tone = InsightTone.NEUTRAL,
            )
        }

        if (insights.isEmpty()) {
            insights += Insight(
                id = "all_clear",
                text = "এই মুহূর্তে জরুরি কিছু নেই। হিসাব ঠিকঠাক আছে।",
                tone = InsightTone.POSITIVE,
            )
        }

        return insights.take(5)
    }

    /**
     * A soft organization indicator — deliberately not a credit-score-like number.
     */
    fun health(
        totalIOwe: Money,
        overdueCount: Int,
        next7Days: Money,
        monthIncome: Money,
        monthExpense: Money,
    ): Pair<HealthLevel, String> {
        if (overdueCount >= 2) {
            return HealthLevel.HIGH_LOAD to "কয়েকটি হিসাবের তারিখ পেরিয়েছে, আগে সেগুলো দেখুন।"
        }
        if (overdueCount == 1) {
            return HealthLevel.ATTENTION to "একটি হিসাবের তারিখ পেরিয়েছে।"
        }
        val incomeHeavy = monthIncome.isPositive &&
            next7Days.poisha > monthIncome.poisha / 2
        if (incomeHeavy) {
            return HealthLevel.HIGH_LOAD to "সামনের ৭ দিনে payment-এর চাপ তুলনামূলক বেশি।"
        }
        if (monthIncome.isPositive && monthExpense > monthIncome) {
            return HealthLevel.ATTENTION to "এই মাসে আয়ের চেয়ে খরচ বেশি হয়েছে।"
        }
        if (totalIOwe.isZero) {
            return HealthLevel.GOOD to "আপনার কোনো বকেয়া নেই।"
        }
        return HealthLevel.GOOD to "সব হিসাব নিয়ন্ত্রণে আছে।"
    }

    fun monthlySummaryText(snapshot: MonthSnapshot, month: LocalDate): String {
        val name = BanglaDate.monthName(month.monthValue)
        return buildString {
            append("$name মাসে আপনার আয় ${money(snapshot.income)} এবং খরচ ${money(snapshot.expense)}। ")
            if (snapshot.debtPayments.isPositive) {
                append("বকেয়া পরিশোধ করেছেন ${money(snapshot.debtPayments)}। ")
            }
            snapshot.topExpenseCategory?.let { append("সবচেয়ে বেশি খরচ হয়েছে $it খাতে। ") }
            val net = snapshot.net
            if (net.isPositive) append("মাস শেষে হাতে ছিল আনুমানিক ${money(net)}।")
            else if (net.isNegative) append("এই মাসে খরচ আয়ের চেয়ে ${money(net.abs())} বেশি ছিল।")
        }
    }

    /** Suggested review order — never an instruction, only a hint. */
    fun prioritize(payments: List<UpcomingPayment>, today: LocalDate): List<UpcomingPayment> =
        payments.sortedWith(
            compareBy(
                { if (it.dueDate.isBefore(today)) 0 else if (it.dueDate == today) 1 else 2 },
                { it.dueDate },
                { -it.amount.poisha },
            ),
        )

    private fun money(value: Money) = CurrencyFormatter.format(value)
    private fun bn(value: Int) = com.shohankhan.bokeya.core.BanglaNumbers.toBanglaDigits(value.toString())
}
