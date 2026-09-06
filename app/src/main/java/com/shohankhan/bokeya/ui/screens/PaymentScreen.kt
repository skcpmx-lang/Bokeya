package com.shohankhan.bokeya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.repo.PaymentResult
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.ui.DetailViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaTextField
import com.shohankhan.bokeya.ui.components.ChipSelector
import com.shohankhan.bokeya.ui.components.DateSelector
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyInput
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.components.SecondaryButton
import com.shohankhan.bokeya.ui.components.SelectorOption
import com.shohankhan.bokeya.ui.components.SuccessCheck
import com.shohankhan.bokeya.ui.theme.bokeya
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PaymentScreen(
    viewModel: DetailViewModel,
    confettiEnabled: Boolean,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    val account = state.account

    var amountText by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.CASH.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<PaymentResult.Success?>(null) }

    val haptics = LocalHapticFeedback.current
    val parsed = Money.parseOrNull(amountText)
    val receivable = summary?.direction == Direction.THEY_OWE

    ScreenScaffold(
        title = if (receivable) "টাকা পেয়েছি" else "টাকা পরিশোধ করুন",
        subtitle = account?.title,
        onBack = onBack,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (summary != null) {
                BokeyaCard {
                    Text(
                        if (receivable) "এখনো পাওনা" else "এখনো বাকি",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Spacer(Modifier.height(4.dp))
                    MoneyText(
                        summary.remaining,
                        style = MaterialTheme.typography.headlineMedium,
                        animate = true,
                    )
                    if (summary.nextDueAmount != null && summary.nextDueAmount != summary.remaining) {
                        Spacer(Modifier.height(8.dp))
                        InfoRow("এই কিস্তি", CurrencyFormatter.format(summary.nextDueAmount))
                    }
                }
            }

            MoneyInput(
                value = amountText,
                onValueChange = { amountText = it; error = null },
                label = "কত টাকা?",
                isError = error != null,
                errorText = error,
            )

            if (summary != null && !summary.remaining.isZero) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.nextDueAmount?.takeIf { it.isPositive && it < summary.remaining }?.let {
                        SecondaryButton(
                            text = "কিস্তি " + CurrencyFormatter.format(it),
                            onClick = { amountText = it.toString() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SecondaryButton(
                        text = "পুরোটা",
                        onClick = { amountText = summary.remaining.toString() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            DateSelector(date = date, onDateChange = { date = it })

            ChipSelector(
                options = PaymentMethod.entries.map { SelectorOption(it.name, it.label) },
                selectedId = method,
                onSelect = { method = it },
                label = "Payment method",
            )

            BokeyaTextField(
                value = note,
                onValueChange = { note = it },
                label = "নোট (optional)",
                singleLine = false,
            )

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = "পরিশোধ করুন",
                onClick = {
                    when {
                        parsed == null || !parsed.isPositive -> error = "টাকার পরিমাণ সঠিকভাবে লিখুন।"
                        summary != null && summary.remaining.isZero -> error = "এই হিসাবটি আগেই পরিশোধ হয়েছে।"
                        else -> confirming = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = summary != null && !summary.remaining.isZero,
            )

            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirming && parsed != null && account != null) {
        val capped = if (summary != null && parsed > summary.remaining) summary.remaining else parsed
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("নিশ্চিত করুন") },
            text = {
                Column {
                    Text(
                        if (receivable) "আপনি পাচ্ছেন:" else "আপনি পরিশোধ করছেন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(account.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    MoneyText(capped, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("তারিখ", BanglaDate.full(date))
                    InfoRow("Method", PaymentMethod.valueOf(method).label)
                    if (parsed > capped) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "বাকির চেয়ে বেশি লিখেছেন, তাই ${CurrencyFormatter.format(capped)} ধরা হবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.bokeya.warning,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    viewModel.pay(
                        amount = parsed,
                        date = date,
                        method = PaymentMethod.valueOf(method),
                        note = note.takeIf { it.isNotBlank() },
                    ) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                success = result
                                if (hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            PaymentResult.InvalidAmount -> error = "টাকার পরিমাণ সঠিকভাবে লিখুন।"
                            PaymentResult.AlreadySettled -> error = "এই হিসাবটি আগেই পরিশোধ হয়েছে।"
                            PaymentResult.NotFound -> error = "এই হিসাবটি খুঁজে পাওয়া যায়নি।"
                        }
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("বাতিল") }
            },
        )
    }

    success?.let { result ->
        SuccessSheet(
            result = result,
            confettiEnabled = confettiEnabled,
            onDismiss = { success = null; viewModel.clearPaymentResult(); onDone() },
        )
    }
}

@Composable
private fun SuccessSheet(
    result: PaymentResult.Success,
    confettiEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ঠিক আছে") }
        },
        text = {
            Box(Modifier.fillMaxWidth()) {
                if (confettiEnabled && result.fullyPaid) {
                    SubtleConfetti(Modifier.fillMaxWidth().height(220.dp))
                }
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SuccessCheck()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (result.fullyPaid) "পুরো টাকা পরিশোধ হয়েছে" else "পরিশোধ সফল হয়েছে",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    InfoRow("Paid", CurrencyFormatter.format(result.applied))
                    InfoRow(
                        "Remaining",
                        CurrencyFormatter.format(result.remaining),
                        valueColor = if (result.remaining.isZero) {
                            MaterialTheme.bokeya.success
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (result.overpaymentTrimmed) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "বাকির চেয়ে বেশি অংশটুকু হিসাবে ধরা হয়নি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.bokeya.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )
}

/** Deliberately restrained: a handful of small particles, one short pass, no looping. */
@Composable
private fun SubtleConfetti(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400, easing = LinearEasing)) }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.bokeya.success,
        MaterialTheme.bokeya.warning,
    )
    val particles = remember {
        List(18) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat() * 360f,
                Random.nextInt(3),
            )
        }
    }

    Canvas(modifier) {
        particles.forEachIndexed { index, (startX, angle, colorIndex) ->
            val t = (progress.value - index * 0.015f).coerceIn(0f, 1f)
            if (t <= 0f) return@forEachIndexed
            val x = startX * size.width + cos(Math.toRadians(angle.toDouble())).toFloat() * 26f * t
            val y = size.height * 0.15f + t * size.height * 0.75f
            val alpha = (1f - t).coerceIn(0f, 1f) * 0.85f
            drawCircle(
                color = colors[colorIndex].copy(alpha = alpha),
                radius = 4f + sin(t * 6f) * 1.5f,
                center = Offset(x, y),
            )
        }
    }
}
