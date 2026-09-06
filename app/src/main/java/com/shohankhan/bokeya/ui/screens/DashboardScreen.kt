package com.shohankhan.bokeya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.HealthLevel
import com.shohankhan.bokeya.domain.InsightTone
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.domain.UpcomingPayment
import com.shohankhan.bokeya.ui.DashboardState
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.HeroGradient
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.QuickAction
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.components.SegmentedToggle
import com.shohankhan.bokeya.ui.components.SkeletonCard
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate

@Composable
fun DashboardScreen(
    state: DashboardState,
    onSearch: () -> Unit,
    onQuickAction: (String) -> Unit,
    onAccountClick: (Long) -> Unit,
    onSeeAllUpcoming: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOverdueClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var showReceivable by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("greeting") { GreetingRow(state.userName, onSearch) }

        if (state.loading) {
            item("skeleton") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkeletonCard()
                    SkeletonCard()
                }
            }
            return@LazyColumn
        }

        // Dashboard is dynamic: overdue first when it exists, onboarding when empty.
        if (!state.hasAnyData) {
            item("empty") {
                BokeyaCard {
                    Text(
                        "চলুন আপনার প্রথম হিসাবটি যোগ করি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "বকেয়া = আপনাকে দিতে হবে · পাওনা = আপনি পাবেন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    QuickActionsRow(onQuickAction)
                }
            }
        }

        if (state.overdue.isNotEmpty()) {
            item("overdue") { OverdueCard(state, onOverdueClick) }
        }

        item("hero") {
            HeroCard(
                state = state,
                showReceivable = showReceivable,
                onToggle = { showReceivable = it },
            )
        }

        if (state.hasAnyData) {
            item("quick") {
                BokeyaCard(contentPadding = 12.dp) { QuickActionsRow(onQuickAction) }
            }
        }

        item("today") { TodayCard(state) }

        item("health") { HealthCard(state) }

        if (state.upcoming.isNotEmpty()) {
            item("upcoming_header") {
                SectionHeader("সামনে যা আছে", actionLabel = "সব দেখুন", onAction = onSeeAllUpcoming)
            }
            items(state.upcoming.take(4), key = { "up_${it.accountId}_${it.installmentId ?: 0}" }) { payment ->
                UpcomingRow(payment, onClick = { onAccountClick(payment.accountId) })
            }
        }

        if (state.insights.isNotEmpty()) {
            item("insights_header") { SectionHeader("আপনার জন্য") }
            items(state.insights, key = { "in_${it.id}" }) { insight ->
                InsightCard(insight.text, insight.tone)
            }
        }

        item("month") { MonthCard(state) }

        if (state.totalObligations.isPositive) {
            item("debtfree") { DebtFreeCard(state) }
        }

        if (state.recent.isNotEmpty()) {
            item("recent_header") {
                SectionHeader("সাম্প্রতিক", actionLabel = "সব দেখুন", onAction = onSeeAllTransactions)
            }
            item("recent") {
                BokeyaCard(contentPadding = 8.dp) {
                    state.recent.forEach { tx ->
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
    }
}

@Composable
private fun GreetingRow(userName: String, onSearch: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = BanglaDate.greeting() + if (userName.isNotBlank()) ", $userName" else "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = BanglaDate.full(Clocks.today()) + " · " + BanglaDate.weekdayName(Clocks.today()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.bokeya.muted,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "খুঁজুন")
            }
        }
    }
}

@Composable
private fun HeroCard(
    state: DashboardState,
    showReceivable: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    HeroGradient {
        SegmentedToggleOnHero(showReceivable, onToggle)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (showReceivable) "মোট পাওনা" else "মোট বকেয়া",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(4.dp))
        MoneyText(
            money = if (showReceivable) state.totalTheyOwe else state.totalIOwe,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            animate = true,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        if (!showReceivable) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BreakdownPill("দোকান", state.breakdown[AccountType.SHOP] ?: Money.ZERO)
                BreakdownPill("Loan", state.breakdown[AccountType.LOAN] ?: Money.ZERO)
                BreakdownPill("EMI", state.breakdown[AccountType.EMI] ?: Money.ZERO)
                BreakdownPill("ব্যক্তিগত", state.breakdown[AccountType.PERSONAL] ?: Money.ZERO)
            }
        } else {
            Text(
                "অন্যদের কাছে আপনার পাওনা টাকা",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun SegmentedToggleOnHero(showReceivable: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(3.dp)) {
            listOf("আমি দেব" to false, "আমি পাব" to true).forEach { (label, value) ->
                val selected = showReceivable == value
                Box(
                    Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.92f) else Color.Transparent)
                        .clickable { onToggle(value) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.bokeya.heroStart else Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownPill(label: String, amount: Money) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            MoneyText(
                amount,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun OverdueCard(state: DashboardState, onClick: () -> Unit) {
    val extras = MaterialTheme.bokeya
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = extras.dangerContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(extras.danger.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.WarningAmber, null, tint = extras.danger, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${BanglaNumbers.toBanglaDigits(state.overdue.size.toString())}টি হিসাবের তারিখ পেরিয়েছে",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = extras.danger,
                )
                Text(
                    "দেখে নিন কোনগুলো বাকি রয়ে গেছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.danger.copy(alpha = 0.8f),
                )
            }
            MoneyText(
                state.overdueTotal,
                style = MaterialTheme.typography.titleMedium,
                color = extras.danger,
            )
        }
    }
}

@Composable
private fun TodayCard(state: DashboardState) {
    BokeyaCard {
        Text("আজকের হিসাব", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TodayTile("দিতে হবে", state.today.toPay, MaterialTheme.bokeya.moneyOut, Modifier.weight(1f))
            TodayTile("পাবেন", state.today.toReceive, MaterialTheme.bokeya.moneyIn, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TodayTile("আয়", state.today.income, MaterialTheme.bokeya.moneyIn, Modifier.weight(1f))
            TodayTile("খরচ", state.today.expense, MaterialTheme.bokeya.moneyOut, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        InfoRow(
            "আজকের Net",
            "",
            valueColor = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            MoneyText(
                state.today.net,
                style = MaterialTheme.typography.titleMedium,
                signed = true,
                color = if (state.today.net.isNegative) {
                    MaterialTheme.bokeya.moneyOut
                } else {
                    MaterialTheme.bokeya.moneyIn
                },
            )
        }
    }
}

@Composable
private fun TodayTile(label: String, amount: Money, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
            Spacer(Modifier.height(3.dp))
            MoneyText(amount, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
private fun HealthCard(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    val (color, container) = when (state.health) {
        HealthLevel.GOOD -> extras.success to extras.successContainer
        HealthLevel.ATTENTION -> extras.warning to extras.warningContainer
        HealthLevel.HIGH_LOAD -> extras.danger to extras.dangerContainer
    }
    BokeyaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "আপনার আর্থিক অবস্থা",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.bokeya.muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(state.health.label, style = MaterialTheme.typography.headlineSmall, color = color)
        Spacer(Modifier.height(4.dp))
        Text(
            state.healthMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.bokeya.muted,
        )
        if (state.weekTotal.isPositive) {
            Spacer(Modifier.height(12.dp))
            Surface(color = container, shape = RoundedCornerShape(12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "আগামী ৭ দিনে",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                    MoneyText(state.weekTotal, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}

@Composable
fun InsightCard(text: String, tone: InsightTone) {
    val extras = MaterialTheme.bokeya
    val color = when (tone) {
        InsightTone.POSITIVE -> extras.success
        InsightTone.WARNING -> extras.warning
        InsightTone.NEUTRAL -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Lightbulb,
                null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MonthCard(state: DashboardState) {
    BokeyaCard {
        Text(
            "এই মাস · " + BanglaDate.monthName(Clocks.today().monthValue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        MonthRow("আয়", state.month.income, MaterialTheme.bokeya.moneyIn, Icons.Filled.TrendingUp)
        MonthRow("খরচ", state.month.expense, MaterialTheme.bokeya.moneyOut, Icons.Filled.TrendingDown)
        MonthRow("পরিশোধ", state.month.debtPayments, MaterialTheme.colorScheme.primary, Icons.Filled.ArrowUpward)
        MonthRow("নতুন বকেয়া", state.month.newDebt, MaterialTheme.bokeya.muted, Icons.Filled.ArrowDownward)
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Net cash flow", style = MaterialTheme.typography.labelMedium)
                MoneyText(
                    state.month.net,
                    style = MaterialTheme.typography.titleSmall,
                    signed = true,
                    color = if (state.month.net.isNegative) {
                        MaterialTheme.bokeya.moneyOut
                    } else {
                        MaterialTheme.bokeya.moneyIn
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthRow(label: String, amount: Money, color: Color, icon: ImageVector) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        MoneyText(amount, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun DebtFreeCard(state: DashboardState) {
    BokeyaCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("পরিশোধের অগ্রগতি", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                BanglaNumbers.toBanglaDigits(((state.debtFreeProgress * 100).toInt()).toString()) + "%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        BokeyaProgress(state.debtFreeProgress)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("পরিশোধ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
                MoneyText(state.totalPaid, style = MaterialTheme.typography.bodyLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("মোট", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
                MoneyText(state.totalObligations, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun UpcomingRow(payment: UpcomingPayment, onClick: () -> Unit) {
    val today = Clocks.today()
    val overdue = payment.dueDate.isBefore(today)
    val extras = MaterialTheme.bokeya
    BokeyaCard(onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconFor(payment.type),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    payment.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    payment.type.label + " · " + BanglaDate.relative(payment.dueDate, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) extras.danger else MaterialTheme.bokeya.muted,
                )
            }
            MoneyText(payment.amount, style = MaterialTheme.typography.titleSmall)
        }
    }
}

fun iconFor(type: AccountType): ImageVector = when (type) {
    AccountType.SHOP -> Icons.Filled.Storefront
    AccountType.LOAN -> Icons.Filled.AccountBalance
    AccountType.EMI -> Icons.Filled.CreditCard
    AccountType.PERSONAL -> Icons.Filled.People
}

@Composable
fun TransactionRow(
    title: String,
    subtitle: String,
    amount: Money,
    type: TxType,
    date: LocalDate,
    onClick: (() -> Unit)? = null,
) {
    val extras = MaterialTheme.bokeya
    val income = type.flow == com.shohankhan.bokeya.domain.MoneyFlow.MONEY_IN
    val color = when (type.flow) {
        com.shohankhan.bokeya.domain.MoneyFlow.MONEY_IN -> extras.moneyIn
        com.shohankhan.bokeya.domain.MoneyFlow.MONEY_OUT -> extras.moneyOut
        com.shohankhan.bokeya.domain.MoneyFlow.NONE -> MaterialTheme.bokeya.muted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (income) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                "$subtitle · ${BanglaDate.dayMonth(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.bokeya.muted,
            )
        }
        Text(
            (if (income) "+" else "−") + com.shohankhan.bokeya.core.CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun QuickActionsRow(onQuickAction: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        QuickAction(Icons.Filled.Storefront, "বাকি", { onQuickAction("shop") })
        QuickAction(Icons.Filled.AccountBalance, "Loan", { onQuickAction("loan") })
        QuickAction(Icons.Filled.CreditCard, "EMI", { onQuickAction("emi") })
        QuickAction(Icons.Filled.People, "ধার", { onQuickAction("personal") })
        QuickAction(Icons.Filled.TrendingUp, "আয়", { onQuickAction("income") })
        QuickAction(Icons.Filled.TrendingDown, "খরচ", { onQuickAction("expense") })
    }
}
