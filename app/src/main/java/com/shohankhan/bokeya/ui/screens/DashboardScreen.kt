package com.shohankhan.bokeya.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EastRounded
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.HealthLevel
import com.shohankhan.bokeya.domain.InsightTone
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.domain.UpcomingPayment
import com.shohankhan.bokeya.ui.DashboardState
import com.shohankhan.bokeya.ui.components.BokeyaCanvas
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaGroup
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.BokeyaRow
import com.shohankhan.bokeya.ui.components.BokeyaSection
import com.shohankhan.bokeya.ui.components.BokeyaToneCard
import com.shohankhan.bokeya.ui.components.CanvasInset
import com.shohankhan.bokeya.ui.components.ComparisonBars
import com.shohankhan.bokeya.ui.components.Eyebrow
import com.shohankhan.bokeya.ui.components.IconBadge
import com.shohankhan.bokeya.ui.components.LegendItem
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.ProgressRing
import com.shohankhan.bokeya.ui.components.QuickAction
import com.shohankhan.bokeya.ui.components.RowDivider
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.components.Skeleton
import com.shohankhan.bokeya.ui.components.SkeletonHero
import com.shohankhan.bokeya.ui.components.Slice
import com.shohankhan.bokeya.ui.components.Sparkline
import com.shohankhan.bokeya.ui.components.StackedBar
import com.shohankhan.bokeya.ui.theme.Durations
import com.shohankhan.bokeya.ui.theme.IconSize
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Dashboard.
 *
 * Reading order is fixed by priority, not by convenience: overdue → hero (what I owe / receive,
 * with pressure + composition) → today → what's next → insight → month → activity. Only two
 * blocks on this screen are cards; everything else is a flat section or a tonal group.
 */
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
            start = Space.gutter,
            end = Space.gutter,
            top = Space.sm,
            bottom = contentPadding.calculateBottomPadding() + 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.xxl),
    ) {
        item("greeting") { GreetingRow(state.userName, onSearch) }

        if (state.loading) {
            item("skeleton") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                    SkeletonHero()
                    Skeleton(Modifier.fillMaxWidth(0.4f), 16.dp)
                    Skeleton(Modifier.fillMaxWidth(), 84.dp, Radius.lg)
                }
            }
            return@LazyColumn
        }

        if (!state.hasAnyData) {
            item("firstrun") { FirstRunPanel(onQuickAction) }
            return@LazyColumn
        }

        if (state.overdue.isNotEmpty()) {
            item("overdue") { OverdueBanner(state, onOverdueClick) }
        }

        item("hero") {
            HeroOverview(
                state = state,
                showReceivable = showReceivable,
                onToggle = { showReceivable = it },
                onNextClick = { state.nextPayment?.let { onAccountClick(it.accountId) } },
            )
        }

        item("today") { TodaySection(state) }

        if (state.upcoming.isNotEmpty()) {
            item("upcoming") {
                BokeyaSection(
                    title = "সামনে যা আছে",
                    subtitle = upcomingSubtitle(state),
                    actionLabel = "সব",
                    onAction = onSeeAllUpcoming,
                ) {
                    BokeyaGroup {
                        val shown = state.upcoming.take(4)
                        shown.forEachIndexed { index, payment ->
                            UpcomingRow(payment) { onAccountClick(payment.accountId) }
                            if (index != shown.lastIndex) RowDivider(inset = 68.dp)
                        }
                    }
                }
            }
        }

        state.insights.firstOrNull()?.let { insight ->
            item("insight") {
                BokeyaSection(title = "আপনার জন্য") {
                    InsightPanel(insight.text, insight.tone, state.insights.drop(1).map { it.text })
                }
            }
        }

        item("month") { MonthSection(state) }

        if (state.recent.isNotEmpty()) {
            item("recent") {
                BokeyaSection(
                    title = "সাম্প্রতিক",
                    actionLabel = "সব",
                    onAction = onSeeAllTransactions,
                ) {
                    BokeyaGroup {
                        state.recent.forEachIndexed { index, tx ->
                            TransactionRow(
                                title = tx.title,
                                subtitle = tx.type.label,
                                amount = Money(tx.amount),
                                type = tx.type,
                                date = LocalDate.ofEpochDay(tx.date),
                            )
                            if (index != state.recent.lastIndex) RowDivider(inset = 68.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun upcomingSubtitle(state: DashboardState): String? {
    val week = state.weekTotal
    if (!week.isPositive) return null
    return "আগামী ৭ দিনে " + CurrencyFormatter.format(week)
}

// ---------------------------------------------------------------- header

@Composable
private fun GreetingRow(userName: String, onSearch: () -> Unit) {
    val today = Clocks.today()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = BanglaDate.greeting() + if (userName.isNotBlank()) ", $userName" else "",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = BanglaDate.weekdayName(today) + " · " + BanglaDate.full(today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.bokeya.faint,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.bokeya.surface2)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "খুঁজুন",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(IconSize.md),
            )
        }
    }
}

// ---------------------------------------------------------------- hero

/**
 * Asymmetric two-column hero.
 *
 * Left column carries the headline number and the direction switch; the right column carries the
 * pressure signal (settled ring, or receivable count). Beneath them a full-width stacked bar plus
 * a legend grid uses the remaining width for the category split — this is what removes the dead
 * space the old pill row left behind.
 */
@Composable
private fun HeroOverview(
    state: DashboardState,
    showReceivable: Boolean,
    onToggle: (Boolean) -> Unit,
    onNextClick: () -> Unit,
) {
    val extras = MaterialTheme.bokeya
    val amount = if (showReceivable) state.totalTheyOwe else state.totalIOwe

    BokeyaCanvas(contentPadding = PaddingValues(Space.xl)) {
        DirectionSwitch(showReceivable, onToggle)

        Spacer(Modifier.height(Space.xl))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow(
                    if (showReceivable) "মোট পাওনা" else "মোট বকেয়া",
                    color = extras.onHeroMuted,
                )
                Spacer(Modifier.height(Space.xs))
                AnimatedContent(
                    targetState = amount,
                    transitionSpec = {
                        fadeIn(tween(Durations.standard)) togetherWith fadeOut(tween(Durations.fast))
                    },
                    label = "heroAmount",
                ) { value ->
                    MoneyText(
                        money = value,
                        style = MaterialTheme.typography.displayMedium,
                        color = extras.onHero,
                        animate = true,
                    )
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = heroCaption(state, showReceivable),
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.onHeroMuted,
                )
            }

            Spacer(Modifier.width(Space.md))

            // Right column: the pressure signal. Never empty — it always has something to say.
            if (!showReceivable && state.totalObligations.isPositive) {
                ProgressRing(
                    progress = state.debtFreeProgress,
                    size = 92.dp,
                    stroke = 9.dp,
                    color = extras.onHero,
                    trackColor = Color.White.copy(alpha = 0.16f),
                    caption = BanglaNumbers.toBanglaDigits(
                        (state.debtFreeProgress * 100).toInt().toString(),
                    ) + "%",
                    captionColor = extras.onHero,
                    subCaption = "শোধ",
                )
            } else {
                ReceivableGlyph(state.receivableCount, extras.onHero)
            }
        }

        Spacer(Modifier.height(Space.xl))

        if (!showReceivable) {
            HeroComposition(state)
        } else {
            HeroReceivableNote(state)
        }

        // Next obligation lives inside the hero: it is the single most actionable fact here.
        state.nextPayment?.takeIf { !showReceivable }?.let { next ->
            Spacer(Modifier.height(Space.md))
            NextPaymentStrip(next, onNextClick)
        }
    }
}

private fun heroCaption(state: DashboardState, showReceivable: Boolean): String {
    if (showReceivable) {
        return if (state.receivableCount == 0) {
            "কারও কাছে আপনার পাওনা নেই"
        } else {
            BanglaNumbers.toBanglaDigits(state.receivableCount.toString()) + " জনের কাছে পাওনা"
        }
    }
    val active = state.accountCounts.values.sum()
    return if (active == 0) {
        "সব হিসাব পরিশোধ হয়ে গেছে"
    } else {
        BanglaNumbers.toBanglaDigits(active.toString()) + "টি চলমান হিসাব"
    }
}

@Composable
private fun DirectionSwitch(showReceivable: Boolean, onToggle: (Boolean) -> Unit) {
    val extras = MaterialTheme.bokeya
    Surface(
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(Radius.pill),
    ) {
        Row(Modifier.padding(3.dp)) {
            listOf("আমি দেব" to false, "আমি পাব" to true).forEach { (label, value) ->
                val selected = showReceivable == value
                val alpha by animateFloatAsState(
                    if (selected) 1f else 0f,
                    tween(Durations.standard),
                    label = "switch",
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Color.White.copy(alpha = alpha * 0.95f))
                        .clickable { onToggle(value) }
                        .padding(horizontal = Space.lg, vertical = Space.sm + 1.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) extras.heroStart else extras.onHeroMuted,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Full-width stacked bar + 2×2 legend grid. Fills the hero's lower band with real information. */
@Composable
private fun HeroComposition(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    val series = listOf(
        Triple(AccountType.SHOP, "দোকান", Color(0xFF9FE3D0)),
        Triple(AccountType.LOAN, "Loan", Color(0xFF7FC9EA)),
        Triple(AccountType.EMI, "EMI", Color(0xFFF3C98B)),
        Triple(AccountType.PERSONAL, "ব্যক্তিগত", Color(0xFFC7B7EF)),
    )
    val entries = series.map { (type, label, color) ->
        Triple(label, state.breakdown[type] ?: Money.ZERO, color)
    }
    val present = entries.filter { it.second.isPositive }

    if (present.isEmpty()) return

    Column {
        StackedBar(
            slices = present.map { Slice(it.first, it.second.poisha, it.third) },
            height = 10.dp,
            trackColor = Color.White.copy(alpha = 0.14f),
        )
        Spacer(Modifier.height(Space.lg))
        // Two columns keep every legend cell wide enough for a full amount — no truncation,
        // no horizontal scroll, no leftover gutter on the right.
        present.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                row.forEach { (label, money, color) ->
                    LegendItem(
                        color = color,
                        label = label,
                        value = CurrencyFormatter.format(money),
                        labelColor = extras.onHeroMuted,
                        valueColor = extras.onHero,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            if (row != present.chunked(2).last()) Spacer(Modifier.height(Space.md))
        }
    }
}

@Composable
private fun HeroReceivableNote(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    CanvasInset(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Handshake,
                null,
                tint = extras.onHeroMuted,
                modifier = Modifier.size(IconSize.md),
            )
            Spacer(Modifier.width(Space.md))
            Text(
                if (state.receivableCount == 0) {
                    "কেউ আপনার কাছে ঋণী নন।"
                } else {
                    "টাকা ফেরত পাওয়ার তারিখ এলে মনে করিয়ে দেওয়া হবে।"
                },
                style = MaterialTheme.typography.bodySmall,
                color = extras.onHeroMuted,
            )
        }
    }
}

@Composable
private fun NextPaymentStrip(next: UpcomingPayment, onClick: () -> Unit) {
    val extras = MaterialTheme.bokeya
    val days = ChronoUnit.DAYS.between(Clocks.today(), next.dueDate)
    val when_ = when {
        days < 0L -> "তারিখ পেরিয়েছে"
        days == 0L -> "আজ"
        days == 1L -> "আগামীকাল"
        else -> BanglaNumbers.toBanglaDigits(days.toString()) + " দিন পর"
    }
    CanvasInset(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("পরবর্তী পরিশোধ", color = extras.onHeroMuted)
                Spacer(Modifier.height(3.dp))
                Text(
                    next.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.onHero,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    next.amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = extras.onHero,
                )
                Text(
                    when_,
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.onHeroMuted,
                )
            }
            Spacer(Modifier.width(Space.sm))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "বিস্তারিত",
                tint = extras.onHeroMuted,
                modifier = Modifier.size(IconSize.sm),
            )
        }
    }
}

@Composable
private fun ReceivableGlyph(count: Int, tint: Color) {
    Box(
        Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                BanglaNumbers.toBanglaDigits(count.toString()),
                style = MaterialTheme.typography.headlineMedium,
                color = tint,
            )
            Text("জন", style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.75f))
        }
    }
}

// ---------------------------------------------------------------- overdue

@Composable
private fun OverdueBanner(state: DashboardState, onClick: () -> Unit) {
    val extras = MaterialTheme.bokeya
    BokeyaToneCard(
        tone = extras.danger,
        container = extras.dangerContainer,
        onClick = onClick,
        contentPadding = Space.lg,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(extras.danger.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PriorityHigh,
                    null,
                    tint = extras.danger,
                    modifier = Modifier.size(IconSize.sm),
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    BanglaNumbers.toBanglaDigits(state.overdue.size.toString()) +
                        "টি হিসাবের তারিখ পেরিয়েছে",
                    style = MaterialTheme.typography.titleSmall,
                    color = extras.danger,
                )
                Text(
                    "আগে এগুলো দেখে নিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.danger.copy(alpha = 0.78f),
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

// ---------------------------------------------------------------- today

/**
 * Two primary money figures get real estate; income/expense sit below as secondary inline stats
 * separated by a hairline. Four identical tiles would flatten the hierarchy, so we don't.
 */
@Composable
private fun TodaySection(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    val today = state.today

    BokeyaSection(title = "আজকের হিসাব", subtitle = BanglaDate.dayMonth(Clocks.today())) {
        BokeyaCard(contentPadding = Space.lg) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                PrimaryStat(
                    label = "দিতে হবে",
                    amount = today.toPay,
                    color = if (today.toPay.isPositive) extras.moneyOut else extras.faint,
                    icon = Icons.Filled.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(extras.divider),
                )
                PrimaryStat(
                    label = "পাবেন",
                    amount = today.toReceive,
                    color = if (today.toReceive.isPositive) extras.moneyIn else extras.faint,
                    icon = Icons.Filled.ArrowDownward,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Space.lg),
                )
            }

            Spacer(Modifier.height(Space.lg))
            Box(Modifier.fillMaxWidth().height(1.dp).background(extras.divider))
            Spacer(Modifier.height(Space.md))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                InlineStat("আয়", today.income, extras.moneyIn, Modifier.weight(1f))
                InlineStat("খরচ", today.expense, extras.moneyOut, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Net")
                    MoneyText(
                        today.net,
                        style = MaterialTheme.typography.titleMedium,
                        signed = true,
                        color = when {
                            today.net.isNegative -> extras.moneyOut
                            today.net.isPositive -> extras.moneyIn
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryStat(
    label: String,
    amount: Money,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(IconSize.xs))
            Spacer(Modifier.width(Space.xs + 2.dp))
            Eyebrow(label)
        }
        Spacer(Modifier.height(Space.xs + 2.dp))
        MoneyText(amount, style = MaterialTheme.typography.headlineSmall, color = color)
    }
}

@Composable
private fun InlineStat(label: String, amount: Money, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Eyebrow(label)
        MoneyText(amount, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

// ---------------------------------------------------------------- insight

@Composable
private fun InsightPanel(text: String, tone: InsightTone, more: List<String>) {
    val extras = MaterialTheme.bokeya
    val (accent, container) = when (tone) {
        InsightTone.WARNING -> extras.warning to extras.warningContainer
        InsightTone.POSITIVE -> extras.success to extras.successContainer
        InsightTone.NEUTRAL -> extras.info to extras.infoContainer
    }
    BokeyaToneCard(tone = accent, container = container) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Insights,
                null,
                tint = accent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(IconSize.md),
            )
            Spacer(Modifier.width(Space.md))
            Column {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                more.take(2).forEach { extra ->
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.bokeya.muted,
                    )
                }
            }
        }
    }
}

/** Public: reused by the planner and overdue screens. */
@Composable
fun InsightCard(text: String, tone: InsightTone, modifier: Modifier = Modifier) {
    val extras = MaterialTheme.bokeya
    val (accent, container) = when (tone) {
        InsightTone.WARNING -> extras.warning to extras.warningContainer
        InsightTone.POSITIVE -> extras.success to extras.successContainer
        InsightTone.NEUTRAL -> extras.info to extras.infoContainer
    }
    BokeyaToneCard(tone = accent, container = container, modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Insights,
                null,
                tint = accent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(IconSize.md),
            )
            Spacer(Modifier.width(Space.md))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------- month

@Composable
private fun MonthSection(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    val month = state.month
    val today = Clocks.today()
    val hasTrend = state.netTrend.count { it != 0L } >= 2

    BokeyaSection(title = "এই মাস", subtitle = BanglaDate.monthYear(today)) {
        Column {
            ComparisonBars(
                leftLabel = "আয়",
                leftValue = month.income.poisha,
                leftColor = extras.moneyIn,
                leftText = CurrencyFormatter.format(month.income),
                rightLabel = "খরচ",
                rightValue = month.expense.poisha,
                rightColor = extras.moneyOut,
                rightText = CurrencyFormatter.format(month.expense),
            )

            Spacer(Modifier.height(Space.lg))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Net cash flow")
                    MoneyText(
                        month.net,
                        style = MaterialTheme.typography.headlineSmall,
                        signed = true,
                        color = if (month.net.isNegative) extras.moneyOut else extras.moneyIn,
                    )
                }
                if (hasTrend) {
                    Sparkline(
                        values = state.netTrend,
                        modifier = Modifier
                            .width(96.dp)
                            .height(38.dp),
                        color = if (month.net.isNegative) extras.moneyOut else extras.moneyIn,
                        contentDescription = "গত ছয় মাসের ধারা",
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            BokeyaGroup(contentPadding = PaddingValues(vertical = Space.xs)) {
                MonthRow("বকেয়া পরিশোধ", month.debtPayments, extras.moneyIn)
                RowDivider(inset = Space.md)
                MonthRow("নতুন বকেয়া", month.newDebt, extras.muted)
                month.topExpenseCategory?.let {
                    RowDivider(inset = Space.md)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.md, vertical = Space.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "সবচেয়ে বেশি খরচ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.bokeya.muted,
                        )
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(Space.lg))
            HealthRow(state)
        }
    }
}

@Composable
private fun MonthRow(label: String, amount: Money, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.md, vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.bokeya.muted)
        MoneyText(amount, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/**
 * Financial health: an organization indicator, phrased as an observation. Deliberately a quiet
 * inline row, not a scored gauge, so it can never be mistaken for a credit rating.
 */
@Composable
private fun HealthRow(state: DashboardState) {
    val extras = MaterialTheme.bokeya
    val (dot, label) = when (state.health) {
        HealthLevel.GOOD -> extras.success to "ভালো"
        HealthLevel.ATTENTION -> extras.warning to "মনোযোগ দরকার"
        HealthLevel.HIGH_LOAD -> extras.danger to "চাপ বেশি"
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            "আর্থিক অবস্থা · $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            state.healthMessage,
            style = MaterialTheme.typography.bodySmall,
            color = extras.faint,
            maxLines = 2,
        )
    }
}

// ---------------------------------------------------------------- first run

@Composable
private fun FirstRunPanel(onQuickAction: (String) -> Unit) {
    Column {
        BokeyaCanvas(contentPadding = PaddingValues(Space.xl)) {
            Text(
                "আপনার হিসাব শুরু করুন",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.bokeya.onHero,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                "যা দিতে হবে আর যা পাবেন — দুটোই এক জায়গায় থাকবে। " +
                    "প্রথম হিসাবটি যোগ করলেই এখানে সব দেখতে পাবেন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.bokeya.onHeroMuted,
            )
        }
        Spacer(Modifier.height(Space.xl))
        BokeyaSection(title = "শুরু করুন") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuickAction(Icons.Filled.Storefront, "বাকি", { onQuickAction("shop") })
                QuickAction(Icons.Filled.Handshake, "ধার", { onQuickAction("personal") })
                QuickAction(Icons.Filled.AccountBalance, "Loan", { onQuickAction("loan") })
                QuickAction(Icons.Filled.CreditCard, "EMI", { onQuickAction("emi") })
            }
        }
    }
}

// ---------------------------------------------------------------- shared rows

@Composable
fun UpcomingRow(payment: UpcomingPayment, onClick: () -> Unit) {
    val extras = MaterialTheme.bokeya
    val today = Clocks.today()
    val days = ChronoUnit.DAYS.between(today, payment.dueDate)
    val (tint, when_) = when {
        days < 0L -> extras.danger to "পেরিয়ে গেছে"
        days == 0L -> extras.warning to "আজ"
        days == 1L -> extras.warning to "আগামীকাল"
        days <= 7L -> extras.info to (BanglaNumbers.toBanglaDigits(days.toString()) + " দিন পর")
        else -> extras.muted to BanglaDate.dayMonth(payment.dueDate)
    }

    BokeyaRow(
        title = payment.title,
        subtitle = payment.type.label + " · " + when_,
        onClick = onClick,
        leading = {
            IconBadge(icon = iconFor(payment.type), tint = tint, size = 40.dp)
        },
        trailing = {
            MoneyText(payment.amount, style = MaterialTheme.typography.titleSmall)
        },
    )
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
    val incoming = type == TxType.INCOME || type == TxType.RECEIVED
    val tint = if (incoming) extras.moneyIn else extras.moneyOut

    BokeyaRow(
        title = title,
        subtitle = subtitle + " · " + BanglaDate.relative(date),
        onClick = onClick,
        leading = {
            IconBadge(
                icon = if (incoming) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                tint = tint,
                size = 40.dp,
            )
        },
        trailing = {
            Text(
                (if (incoming) "+" else "−") + CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.titleSmall,
                color = tint,
            )
        },
    )
}

fun iconFor(type: AccountType): ImageVector = when (type) {
    AccountType.SHOP -> Icons.Filled.Storefront
    AccountType.LOAN -> Icons.Filled.AccountBalance
    AccountType.EMI -> Icons.Filled.CreditCard
    AccountType.PERSONAL -> Icons.Filled.Handshake
}
