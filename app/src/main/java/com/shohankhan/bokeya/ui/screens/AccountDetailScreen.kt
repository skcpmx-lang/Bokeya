package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.InstallmentEntity
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.InstallmentStatus
import com.shohankhan.bokeya.ui.DetailViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.BokeyaCanvas
import com.shohankhan.bokeya.ui.components.Eyebrow
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.components.ProgressRing
import com.shohankhan.bokeya.ui.components.SecondaryButton
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.components.SkeletonCard
import com.shohankhan.bokeya.ui.components.StatusBadge
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun AccountDetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onAddPurchase: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var reminderDialog by remember { mutableStateOf(false) }

    val summary = state.summary
    val account = state.account

    ScreenScaffold(
        title = account?.title ?: "হিসাব",
        subtitle = account?.type?.label,
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "আরও") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reminder যোগ করুন") },
                        leadingIcon = { Icon(Icons.Filled.NotificationsActive, null) },
                        onClick = { menu = false; reminderDialog = true },
                    )
                    DropdownMenuItem(
                        text = { Text(if (account?.archived == true) "Archive থেকে ফেরান" else "Archive করুন") },
                        leadingIcon = { Icon(Icons.Filled.Archive, null) },
                        onClick = {
                            menu = false
                            viewModel.archive(account?.archived != true)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("মুছে ফেলুন") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading || summary == null || account == null) {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonCard()
                SkeletonCard()
            }
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("hero") {
                BokeyaCanvas {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow(
                                if (summary.direction == Direction.I_OWE) "এখনো বাকি" else "এখনো পাওনা",
                                color = MaterialTheme.bokeya.onHeroMuted,
                            )
                            Spacer(Modifier.height(4.dp))
                            MoneyText(
                                summary.remaining,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.bokeya.onHero,
                                animate = true,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "মোট " + CurrencyFormatter.format(summary.total) +
                                    " · শোধ " + CurrencyFormatter.format(summary.paid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.bokeya.onHeroMuted,
                            )
                        }
                        if (summary.total.isPositive) {
                            Spacer(Modifier.width(12.dp))
                            ProgressRing(
                                progress = summary.progress,
                                size = 84.dp,
                                stroke = 8.dp,
                                color = MaterialTheme.bokeya.onHero,
                                trackColor = Color.White.copy(alpha = 0.16f),
                                caption = BanglaNumbers.toBanglaDigits(
                                    summary.progressPercent.toString(),
                                ) + "%",
                                captionColor = MaterialTheme.bokeya.onHero,
                                subCaption = "শোধ",
                            )
                        }
                    }
                }
            }

            item("summary") {
                BokeyaCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("অবস্থা", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.bokeya.muted)
                        StatusBadge(summary.status)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoRow("মোট", CurrencyFormatter.format(summary.total))
                    InfoRow(
                        "পরিশোধ হয়েছে",
                        CurrencyFormatter.format(summary.paid),
                        valueColor = MaterialTheme.bokeya.success,
                    )
                    InfoRow("বাকি", CurrencyFormatter.format(summary.remaining))
                    if (account.downPayment > 0) {
                        InfoRow("Down payment", CurrencyFormatter.format(Money(account.downPayment)))
                    }
                    if (account.interestAmount > 0) {
                        InfoRow("Interest", CurrencyFormatter.format(Money(account.interestAmount)))
                    }
                    account.institution?.let { InfoRow("Institution", it) }
                    account.seller?.let { InfoRow("Seller", it) }
                    account.frequency?.let { InfoRow("Frequency", it.label) }
                    if (account.installmentCount > 0) {
                        InfoRow(
                            "কিস্তি",
                            BanglaNumbers.toBanglaDigits(
                                state.installments.count { it.status == InstallmentStatus.PAID }.toString(),
                            ) + " / " + BanglaNumbers.toBanglaDigits(account.installmentCount.toString()),
                        )
                    }
                    summary.nextDueDate?.let { date ->
                        val days = ChronoUnit.DAYS.between(Clocks.today(), date)
                        InfoRow(
                            "পরবর্তী",
                            BanglaDate.full(date) + if (days in 0..30) {
                                " (আর ${BanglaNumbers.toBanglaDigits(days.toString())} দিন)"
                            } else {
                                ""
                            },
                        )
                    }
                    account.note?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("নোট", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item("actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = if (summary.direction == Direction.I_OWE) "পরিশোধ করুন" else "টাকা পেয়েছি",
                        onClick = onPay,
                        icon = Icons.Filled.Payments,
                        enabled = !summary.remaining.isZero,
                        modifier = Modifier.weight(1f),
                    )
                    if (account.type == AccountType.SHOP) {
                        SecondaryButton(
                            text = "বাকি যোগ",
                            onClick = onAddPurchase,
                            icon = Icons.Filled.PlaylistAdd,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (state.installments.isNotEmpty()) {
                item("sched_h") { SectionHeader("কিস্তির তালিকা") }
                items(state.installments, key = { "i_${it.id}" }) { InstallmentRow(it) }
            }

            if (state.purchases.isNotEmpty()) {
                item("pur_h") { SectionHeader("বাকির তালিকা") }
                items(state.purchases, key = { "p_${it.first.id}" }) { (purchase, items) ->
                    BokeyaCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                BanglaDate.full(LocalDate.ofEpochDay(purchase.date)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.bokeya.muted,
                            )
                            MoneyText(
                                Money(items.sumOf { it.total }),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        items.forEach { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    item.name + " · " +
                                        BanglaNumbers.toBanglaDigits(trimQty(item.quantity)) + " " + item.unit,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    CurrencyFormatter.format(Money(item.total)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        purchase.note?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.bokeya.muted)
                        }
                    }
                }
            }

            if (state.payments.isNotEmpty()) {
                item("pay_h") { SectionHeader("Payment history") }
                item("pay_list") {
                    BokeyaCard(contentPadding = 8.dp) {
                        state.payments.forEach { payment ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.bokeya.success.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Payments,
                                        null,
                                        tint = MaterialTheme.bokeya.success,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        BanglaDate.full(LocalDate.ofEpochDay(payment.date)),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        payment.method.label + " · " +
                                            BanglaDate.time(
                                                LocalTime.of(
                                                    payment.timeMinutes / 60,
                                                    payment.timeMinutes % 60,
                                                ),
                                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.bokeya.muted,
                                    )
                                }
                                MoneyText(
                                    Money(payment.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.bokeya.success,
                                )
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
            title = { Text("হিসাবটি মুছে ফেলবেন?") },
            text = {
                Text("এই হিসাবটি মুছে ফেললে এর payment history এবং কিস্তির তালিকাও মুছে যাবে।")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) { Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") }
            },
        )
    }

    if (reminderDialog && account != null) {
        val options = listOf(
            "আজ সন্ধ্যায়" to 6L,
            "আগামীকাল সকালে" to 20L,
            "৩ দিন পর" to 72L,
            "১ সপ্তাহ পর" to 168L,
        )
        AlertDialog(
            onDismissRequest = { reminderDialog = false },
            title = { Text("কখন মনে করিয়ে দেব?") },
            text = {
                Column {
                    options.forEach { (label, hours) ->
                        TextButton(onClick = {
                            viewModel.addReminder(
                                title = account.title,
                                message = "${account.title}-এর ${CurrencyFormatter.format(summary?.remaining ?: Money.ZERO)} বাকি আছে।",
                                at = Clocks.nowMillis() + hours * 3_600_000L,
                            )
                            reminderDialog = false
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reminderDialog = false }) { Text("বন্ধ করুন") }
            },
        )
    }
}

private fun trimQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun InstallmentRow(installment: InstallmentEntity) {
    val extras = MaterialTheme.bokeya
    val color = when (installment.status) {
        InstallmentStatus.PAID -> extras.success
        InstallmentStatus.OVERDUE -> extras.danger
        InstallmentStatus.DUE_TODAY -> extras.warning
        InstallmentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.primary
        InstallmentStatus.UPCOMING -> extras.muted
    }
    BokeyaCard(contentPadding = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    BanglaNumbers.toBanglaDigits(installment.number.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    BanglaDate.full(LocalDate.ofEpochDay(installment.dueDate)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    installment.status.label +
                        if (installment.paidAmount > 0 && installment.status != InstallmentStatus.PAID) {
                            " · " + CurrencyFormatter.format(Money(installment.paidAmount)) + " দেওয়া"
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
            MoneyText(Money(installment.expectedAmount), style = MaterialTheme.typography.titleSmall)
        }
    }
}
