package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.ui.PlannerBucket
import com.shohankhan.bokeya.ui.PlannerViewModel
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.theme.bokeya
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.components.RowDivider
import com.shohankhan.bokeya.ui.components.Eyebrow
import com.shohankhan.bokeya.ui.components.BokeyaToneCard
import com.shohankhan.bokeya.ui.components.BokeyaSection
import com.shohankhan.bokeya.ui.components.BokeyaGroup
import com.shohankhan.bokeya.core.BanglaDate
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onBack: () -> Unit,
    onPay: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }

    ScreenScaffold(title = "Payment Planner", subtitle = "সামনে কী কী আছে", onBack = onBack) { padding ->
        val hasAny = listOf(state.overdue, state.today, state.week, state.next7, state.month)
            .any { it.payments.isNotEmpty() }

        if (!hasAny && !state.loading) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "সামনে কোনো payment নেই",
                    message = "এই মুহূর্তে দেওয়ার মতো কোনো কিস্তি বাকি নেই।",
                )
            }
            return@ScreenScaffold
        }

        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(Space.gutter, Space.sm, Space.gutter, Space.xxxl),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            if (state.suggested.isNotEmpty()) {
                item("suggest") {
                    BokeyaSection(
                        title = "আগে এগুলো দেখুন",
                        subtitle = "তারিখ আর পরিমাণ দেখে সাজানো — সিদ্ধান্ত আপনার",
                    ) {
                        BokeyaGroup(contentPadding = PaddingValues(vertical = Space.xs)) {
                            state.suggested.forEachIndexed { index, payment ->
                                if (index > 0) RowDivider(inset = 56.dp)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Space.md, vertical = Space.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            BanglaNumbers.toBanglaDigits((index + 1).toString()),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.width(Space.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            payment.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            payment.type.label + " · " +
                                                BanglaDate.relative(payment.dueDate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.bokeya.faint,
                                        )
                                    }
                                    MoneyText(
                                        payment.amount,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            bucketSection(state.overdue, onPay, danger = true)
            bucketSection(state.today, onPay)
            bucketSection(state.week, onPay)
            bucketSection(state.next7, onPay)
            bucketSection(state.month, onPay)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bucketSection(
    bucket: PlannerBucket,
    onPay: (Long) -> Unit,
    danger: Boolean = false,
) {
    if (bucket.payments.isEmpty()) return
    item("b_${bucket.label}") {
        val accent = if (danger) MaterialTheme.bokeya.danger else MaterialTheme.colorScheme.primary
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Space.xs, bottom = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        bucket.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                MoneyText(
                    bucket.total,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
            BokeyaGroup(contentPadding = PaddingValues(vertical = Space.xs)) {
                bucket.payments.forEachIndexed { index, payment ->
                    if (index > 0) RowDivider(inset = 68.dp)
                    UpcomingRow(payment) { onPay(payment.accountId) }
                }
            }
        }
    }
}

@Composable
fun OverdueScreen(
    viewModel: PlannerViewModel,
    onBack: () -> Unit,
    onPay: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }

    ScreenScaffold(title = "তারিখ পেরিয়ে গেছে", onBack = onBack) { padding ->
        if (state.overdue.payments.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "কিছুই পেরিয়ে যায়নি",
                    message = "সব হিসাব সময়মতো আছে।",
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                item("total") {
                    BokeyaToneCard(
                        tone = MaterialTheme.bokeya.danger,
                        container = MaterialTheme.bokeya.dangerContainer,
                    ) {
                        Eyebrow("মোট বাকি পড়ে আছে", color = MaterialTheme.bokeya.danger)
                        Spacer(Modifier.height(Space.xs))
                        MoneyText(
                            state.overdue.total,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.bokeya.danger,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            BanglaNumbers.toBanglaDigits(
                                state.overdue.payments.size.toString(),
                            ) + "টি পরিশোধের তারিখ পেরিয়ে গেছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.bokeya.muted,
                        )
                    }
                }
                items(state.overdue.payments, key = { "o_${it.accountId}_${it.installmentId ?: 0}" }) {
                    UpcomingRow(it) { onPay(it.accountId) }
                }
            }
        }
    }
}
