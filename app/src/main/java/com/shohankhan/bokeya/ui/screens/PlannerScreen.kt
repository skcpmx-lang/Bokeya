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
import androidx.compose.foundation.lazy.item
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
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.theme.bokeya

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
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.suggested.isNotEmpty()) {
                item("suggest_h") { SectionHeader("আগে এগুলো দেখুন") }
                item("suggest") {
                    BokeyaCard {
                        Text(
                            "তারিখ আর পরিমাণ দেখে সাজানো হয়েছে। সিদ্ধান্ত আপনার।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.bokeya.muted,
                        )
                        Spacer(Modifier.height(10.dp))
                        state.suggested.forEachIndexed { index, payment ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    BanglaNumbers.toBanglaDigits((index + 1).toString()) + ".",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    payment.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                MoneyText(payment.amount, style = MaterialTheme.typography.bodyLarge)
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
    item("h_${bucket.label}") {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                bucket.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (danger) MaterialTheme.bokeya.danger else MaterialTheme.colorScheme.onSurface,
            )
            MoneyText(
                bucket.total,
                style = MaterialTheme.typography.titleSmall,
                color = if (danger) MaterialTheme.bokeya.danger else MaterialTheme.colorScheme.primary,
            )
        }
    }
    items(bucket.payments, key = { "${bucket.label}_${it.accountId}_${it.installmentId ?: 0}" }) { payment ->
        UpcomingRow(payment) { onPay(payment.accountId) }
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("total") {
                    BokeyaCard {
                        Text(
                            "মোট বাকি পড়ে আছে",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.bokeya.muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        MoneyText(
                            state.overdue.total,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.bokeya.danger,
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
