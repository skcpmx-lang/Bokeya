package com.shohankhan.bokeya.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.backup.ReportKind
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.ui.RangePreset
import com.shohankhan.bokeya.ui.ReportsViewModel
import com.shohankhan.bokeya.ui.SettingsViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.ChipSelector
import com.shohankhan.bokeya.ui.components.DateSelector
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.SecondaryButton
import com.shohankhan.bokeya.ui.components.SelectorOption
import com.shohankhan.bokeya.ui.theme.bokeya
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.components.RankedBar
import com.shohankhan.bokeya.ui.components.Eyebrow
import com.shohankhan.bokeya.ui.components.ComparisonBars
import com.shohankhan.bokeya.ui.components.BokeyaSection
import com.shohankhan.bokeya.ui.components.BokeyaGroup

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            settingsViewModel.exportCsv(it, kind, range.first, range.second) { ok ->
                if (ok) shareFile(context, it, "text/csv")
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        uri?.let {
            settingsViewModel.exportPdf(it, kind, range.first, range.second) { ok ->
                if (ok) shareFile(context, it, "application/pdf")
            }
        }
    }

    ScreenScaffold(
        title = "Reports",
        subtitle = "${BanglaDate.dayMonth(range.first)} — ${BanglaDate.dayMonth(range.second)}",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("presets") {
                ChipSelector(
                    options = RangePreset.entries.map { SelectorOption(it.name, it.label) },
                    selectedId = null,
                    onSelect = { viewModel.setPreset(RangePreset.valueOf(it)) },
                    label = "সময়কাল",
                    wrap = false,
                )
            }

            item("range") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        DateSelector(
                            range.first,
                            { viewModel.setRange(it, range.second) },
                            label = "শুরু",
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        DateSelector(
                            range.second,
                            { viewModel.setRange(range.first, it) },
                            label = "শেষ",
                        )
                    }
                }
            }

            item("kind") {
                ChipSelector(
                    options = ReportKind.entries.map { SelectorOption(it.name, it.label) },
                    selectedId = kind.name,
                    onSelect = { viewModel.setKind(ReportKind.valueOf(it)) },
                    label = "Report-এর ধরন",
                )
            }

            val data = report ?: return@LazyColumn

            item("summary") {
                val net = data.totalIncome - data.totalExpense
                BokeyaSection(title = "আয় ও খরচ") {
                    ComparisonBars(
                        leftLabel = "আয়",
                        leftValue = data.totalIncome.poisha,
                        leftColor = MaterialTheme.bokeya.moneyIn,
                        leftText = CurrencyFormatter.format(data.totalIncome),
                        rightLabel = "খরচ",
                        rightValue = data.totalExpense.poisha,
                        rightColor = MaterialTheme.bokeya.moneyOut,
                        rightText = CurrencyFormatter.format(data.totalExpense),
                    )
                    Spacer(Modifier.height(Space.md))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            if (net.isNegative) "ঘাটতি" else "উদ্বৃত্ত",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.bokeya.muted,
                        )
                        MoneyText(
                            net,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (net.isNegative) {
                                MaterialTheme.bokeya.moneyOut
                            } else {
                                MaterialTheme.bokeya.moneyIn
                            },
                        )
                    }
                }
            }

            item("debt") {
                BokeyaSection(title = "বকেয়ার অবস্থা") {
                    if (data.totalDebt.isPositive) {
                        val done = data.totalPaid.poisha.toFloat() / data.totalDebt.poisha.toFloat()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Eyebrow("এখনো বাকি")
                                MoneyText(data.remaining, style = MaterialTheme.typography.headlineSmall)
                            }
                            Text(
                                BanglaNumbers.toBanglaDigits((done * 100).toInt().toString()) + "% শোধ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.bokeya.muted,
                            )
                        }
                        Spacer(Modifier.height(Space.sm))
                        BokeyaProgress(done, height = 6.dp)
                        Spacer(Modifier.height(Space.md))
                    }
                    BokeyaGroup(contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.xs)) {
                        InfoRow("মোট বকেয়া", CurrencyFormatter.format(data.totalDebt))
                        InfoRow("পরিশোধ হয়েছে", CurrencyFormatter.format(data.totalPaid))
                        InfoRow("আগামী ৩০ দিনে", CurrencyFormatter.format(data.upcoming))
                    }
                }
            }

            if (data.categoryRows.isNotEmpty()) {
                item("cat") {
                    val max = data.categoryRows.maxOf { it.second.poisha }.coerceAtLeast(1L)
                    val total = data.categoryRows.sumOf { it.second.poisha }.coerceAtLeast(1L)
                    BokeyaSection(title = "Category অনুযায়ী খরচ") {
                        data.categoryRows.forEachIndexed { index, (name, amount) ->
                            RankedBar(
                                label = name,
                                valueText = CurrencyFormatter.format(amount),
                                fraction = amount.poisha.toFloat() / max.toFloat(),
                                color = MaterialTheme.bokeya.series[index % MaterialTheme.bokeya.series.size],
                                caption = BanglaNumbers.toBanglaDigits(
                                    (amount.poisha * 100 / total).toString(),
                                ) + "%",
                            )
                        }
                    }
                }
            }

            if (data.accountRows.isNotEmpty()) {
                item("acc") {
                    BokeyaSection(title = "হিসাবসমূহ") {
                        data.accountRows.forEach { row ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${row.type} · ${row.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.bokeya.muted,
                                    )
                                }
                                MoneyText(row.remaining, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            item("export") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "PDF",
                        icon = Icons.Filled.PictureAsPdf,
                        onClick = { pdfLauncher.launch("bokeya-${kind.name.lowercase()}-${range.second}.pdf") },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "CSV",
                        icon = Icons.Filled.TableChart,
                        onClick = { csvLauncher.launch("bokeya-${kind.name.lowercase()}-${range.second}.csv") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}


internal fun shareFile(context: android.content.Context, uri: android.net.Uri, mime: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Report share করুন"))
    }
}
