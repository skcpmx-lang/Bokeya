package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.ui.CalendarViewModel
import com.shohankhan.bokeya.ui.DayMarkers
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onAccountClick: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = Clocks.today()

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
        item("header") {
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

        item("grid") {
            BokeyaCard(contentPadding = 12.dp) {
                MonthGrid(
                    month = state.month,
                    selected = state.selectedDate,
                    today = today,
                    markers = state.markers,
                    onSelect = { viewModel.select(it) },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot("আয়", MaterialTheme.bokeya.moneyIn)
                    LegendDot("খরচ", MaterialTheme.bokeya.moneyOut)
                    LegendDot("পরিশোধ", MaterialTheme.colorScheme.primary)
                    LegendDot("কিস্তি", MaterialTheme.bokeya.warning)
                }
            }
        }

        item("day_summary") {
            BokeyaCard {
                Text(
                    BanglaDate.full(state.selectedDate) + " · " + BanglaDate.weekdayName(state.selectedDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayTile("আয়", state.dayIncome, MaterialTheme.bokeya.moneyIn, Modifier.weight(1f))
                    DayTile("খরচ", state.dayExpense, MaterialTheme.bokeya.moneyOut, Modifier.weight(1f))
                    DayTile("দিতে হবে", state.dayDueTotal, MaterialTheme.bokeya.warning, Modifier.weight(1f))
                }
            }
        }

        if (state.dayDues.isNotEmpty()) {
            item("dues_h") { SectionHeader("এই দিনের payment") }
            items(state.dayDues, key = { "d_${it.accountId}_${it.installmentId ?: 0}" }) { due ->
                UpcomingRow(due) { onAccountClick(due.accountId) }
            }
        }

        if (state.dayTransactions.isNotEmpty()) {
            item("tx_h") { SectionHeader("এই দিনের লেনদেন") }
            item("tx") {
                BokeyaCard(contentPadding = 8.dp) {
                    state.dayTransactions.forEach { tx ->
                        TransactionRow(
                            title = tx.title,
                            subtitle = tx.type.label,
                            amount = Money(tx.amount),
                            type = tx.type,
                            date = LocalDate.ofEpochDay(tx.date),
                        )
                    }
                }
            }
        }

        if (state.dayDues.isEmpty() && state.dayTransactions.isEmpty()) {
            item("empty") {
                EmptyState(
                    icon = Icons.Filled.EventBusy,
                    title = "এই দিনে কিছু নেই",
                    message = "এই তারিখে কোনো লেনদেন বা payment নেই।",
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    markers: Map<LocalDate, DayMarkers>,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = month.withDayOfMonth(1)
    // Bangla week starts on Saturday.
    val shift = (firstDay.dayOfWeek.value + 1) % 7
    val daysInMonth = month.lengthOfMonth()
    val weekdayLabels = listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র")

    Column {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.bokeya.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        var day = 1
        val totalCells = shift + daysInMonth
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val cellIndex = row * 7 + column
                    if (cellIndex < shift || day > daysInMonth) {
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    } else {
                        val date = month.withDayOfMonth(day)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            markers = markers[date],
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onSelect(date) },
                        )
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    markers: DayMarkers?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val extras = MaterialTheme.bokeya
    Box(
        modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                BanglaNumbers.toBanglaDigits(date.dayOfMonth.toString()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (markers?.hasIncome == true) Dot(extras.moneyIn, isSelected)
                if (markers?.hasExpense == true) Dot(extras.moneyOut, isSelected)
                if (markers?.hasPayment == true) Dot(MaterialTheme.colorScheme.primary, isSelected)
                if (markers?.hasDue == true) Dot(extras.warning, isSelected)
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color, onPrimary: Boolean) {
    Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(if (onPrimary) androidx.compose.ui.graphics.Color.White else color),
    )
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
    }
}

@Composable
private fun DayTile(
    label: String,
    amount: Money,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
            Spacer(Modifier.height(2.dp))
            MoneyText(amount, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}
