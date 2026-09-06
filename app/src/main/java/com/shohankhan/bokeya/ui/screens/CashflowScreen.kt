package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.ui.CashflowViewModel
import com.shohankhan.bokeya.ui.TxFilter
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.FilterChipRow
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate

@Composable
fun CashflowScreen(
    viewModel: CashflowViewModel,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onUndo: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val selection = remember { mutableStateListOf<Long>() }
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            16.dp,
            12.dp,
            16.dp,
            contentPadding.calculateBottomPadding() + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("month") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "আগের মাস")
                }
                Text(
                    BanglaDate.monthYear(state.month),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "পরের মাস")
                }
            }
        }

        item("summary") {
            BokeyaCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile("আয়", state.income, MaterialTheme.bokeya.moneyIn, Modifier.weight(1f))
                    SummaryTile("খরচ", state.expense, MaterialTheme.bokeya.moneyOut, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                // Income vs expense as a single proportional bar — no fake chart library.
                val total = (state.income + state.expense).poisha.coerceAtLeast(1L)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                ) {
                    Box(
                        Modifier
                            .weight(state.income.poisha.toFloat().coerceAtLeast(0.001f))
                            .fillMaxSize()
                            .background(MaterialTheme.bokeya.moneyIn),
                    )
                    Box(
                        Modifier
                            .weight(state.expense.poisha.toFloat().coerceAtLeast(0.001f))
                            .fillMaxSize()
                            .background(MaterialTheme.bokeya.moneyOut),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Net cash flow", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        state.net,
                        style = MaterialTheme.typography.titleMedium,
                        signed = true,
                        color = if (state.net.isNegative) {
                            MaterialTheme.bokeya.moneyOut
                        } else {
                            MaterialTheme.bokeya.moneyIn
                        },
                    )
                }
            }
        }

        if (state.categoryBreakdown.isNotEmpty()) {
            item("cat_h") { SectionHeader("খরচের ভাগ") }
            item("cat") {
                BokeyaCard {
                    val max = state.categoryBreakdown.maxOf { it.second.poisha }.coerceAtLeast(1L)
                    state.categoryBreakdown.take(8).forEach { (name, amount) ->
                        val categoryId = state.categories.values.firstOrNull { it.name == name }?.id
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCategory(
                                        if (selectedCategory == categoryId) null else categoryId,
                                    )
                                }
                                .padding(vertical = 7.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedCategory == categoryId) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                                Text(
                                    CurrencyFormatter.format(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            BokeyaProgress(
                                amount.poisha.toFloat() / max.toFloat(),
                                height = 5.dp,
                                color = MaterialTheme.bokeya.moneyOut,
                            )
                        }
                    }
                }
            }
        }

        item("filters") {
            FilterChipRow(
                options = TxFilter.entries.map { it.label },
                selectedIndex = TxFilter.entries.indexOf(filter),
                onSelect = { viewModel.setFilter(TxFilter.entries[it]) },
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        if (selection.isNotEmpty()) {
            item("bulk") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            BanglaNumbers.toBanglaDigits(selection.size.toString()) + "টি নির্বাচিত",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { selection.clear() }) { Text("বাতিল") }
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("মুছুন", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (state.transactions.isEmpty()) {
            item("empty") {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "এই মাসে এখনো কিছু যোগ হয়নি",
                    message = "আয় বা খরচ যোগ করলে এখানে দেখতে পাবেন।",
                    ctaLabel = "খরচ যোগ করুন",
                    onCta = onAddExpense,
                )
            }
        } else {
            val grouped = state.transactions.groupBy { it.date }.toSortedMap(compareByDescending { it })
            grouped.forEach { (epochDay, list) ->
                val date = LocalDate.ofEpochDay(epochDay)
                item("h_$epochDay") {
                    Text(
                        BanglaDate.full(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.bokeya.muted,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                    )
                }
                item("g_$epochDay") {
                    BokeyaCard(contentPadding = 6.dp) {
                        list.forEach { tx ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selection.isNotEmpty()) {
                                    Checkbox(
                                        checked = selection.contains(tx.id),
                                        onCheckedChange = {
                                            if (it) selection.add(tx.id) else selection.remove(tx.id)
                                        },
                                    )
                                }
                                Box(Modifier.weight(1f)) {
                                    TransactionRow(
                                        title = tx.title,
                                        subtitle = tx.categoryId
                                            ?.let { state.categories[it]?.name }
                                            ?: tx.type.label,
                                        amount = Money(tx.amount),
                                        type = tx.type,
                                        date = LocalDate.ofEpochDay(tx.date),
                                        onClick = {
                                            if (selection.isEmpty()) {
                                                selection.add(tx.id)
                                            } else if (selection.contains(tx.id)) {
                                                selection.remove(tx.id)
                                            } else {
                                                selection.add(tx.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("মুছে ফেলবেন?") },
            text = {
                Text(
                    BanglaNumbers.toBanglaDigits(selection.size.toString()) +
                        "টি entry মুছে যাবে। এটি ফেরানো যাবে না।",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransactions(selection.toList())
                    onUndo("${BanglaNumbers.toBanglaDigits(selection.size.toString())}টি entry মুছে ফেলা হয়েছে।")
                    selection.clear()
                    confirmDelete = false
                }) { Text("মুছুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } },
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    amount: Money,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.bokeya.muted)
            Spacer(Modifier.height(3.dp))
            MoneyText(amount, style = MaterialTheme.typography.titleLarge, color = color, animate = true)
        }
    }
}

@Composable
fun TimelineScreen(viewModel: CashflowViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "সব লেনদেন",
        subtitle = BanglaDate.monthYear(state.month),
        onBack = onBack,
    ) { padding ->
        if (state.transactions.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "কোনো লেনদেন নেই",
                    message = "এই মাসে এখনো কিছু যোগ হয়নি।",
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val grouped = state.transactions.groupBy { it.date }
                    .toSortedMap(compareByDescending { it })
                grouped.forEach { (epochDay, list) ->
                    item("h_$epochDay") {
                        Text(
                            BanglaDate.full(LocalDate.ofEpochDay(epochDay)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.bokeya.muted,
                        )
                    }
                    item("g_$epochDay") {
                        BokeyaCard(contentPadding = 6.dp) {
                            list.forEach { tx ->
                                TransactionRow(
                                    title = tx.title,
                                    subtitle = tx.categoryId
                                        ?.let { state.categories[it]?.name } ?: tx.type.label,
                                    amount = Money(tx.amount),
                                    type = tx.type,
                                    date = LocalDate.ofEpochDay(tx.date),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
