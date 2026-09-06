package com.shohankhan.bokeya.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.shohankhan.bokeya.appContainer
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.notifications.DeepLinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private data class WidgetData(
    val totalDue: Money = Money.ZERO,
    val totalReceivable: Money = Money.ZERO,
    val todayPay: Money = Money.ZERO,
    val todayReceive: Money = Money.ZERO,
    val nextTitle: String? = null,
    val nextAmount: Money = Money.ZERO,
    val nextDate: String = "",
    val monthIncome: Money = Money.ZERO,
    val monthExpense: Money = Money.ZERO,
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val repo = context.appContainer.repository
    val today = Clocks.today()
    val summaries = repo.summaries.first().filter { !it.archived }
    val upcoming = repo.upcomingPayments(today, today.plusMonths(3))
    val next = upcoming.minByOrNull { it.dueDate }

    val monthStart = today.withDayOfMonth(1)
    val monthEnd = monthStart.plusMonths(1).minusDays(1)
    val monthTx = repo.transactionsBetweenOnce(monthStart, monthEnd)

    return WidgetData(
        totalDue = Money(summaries.filter { it.direction == Direction.I_OWE }.sumOf { it.remaining.poisha }),
        totalReceivable = Money(
            summaries.filter { it.direction == Direction.THEY_OWE }.sumOf { it.remaining.poisha },
        ),
        todayPay = Money(upcoming.filter { it.dueDate == today }.sumOf { it.amount.poisha }),
        todayReceive = Money(
            summaries.filter { it.direction == Direction.THEY_OWE && it.nextDueDate == today }
                .sumOf { it.remaining.poisha },
        ),
        nextTitle = next?.title,
        nextAmount = next?.amount ?: Money.ZERO,
        nextDate = next?.let { BanglaDate.relative(it.dueDate, today) } ?: "",
        monthIncome = Money(monthTx.filter { it.type == TxType.INCOME }.sumOf { it.amount }),
        monthExpense = Money(monthTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }),
    )
}

private fun money(value: Money) = CurrencyFormatter.format(value)

@androidx.compose.runtime.Composable
private fun WidgetShell(
    deepLink: String,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(androidx.glance.appwidget.action.actionStartActivity(
                com.shohankhan.bokeya.widget.widgetIntent(deepLink),
            )),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) { content() }
}

fun widgetIntent(deepLink: String): android.content.Intent =
    android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(deepLink),
    ).setClassName("com.shohankhan.bokeya", "com.shohankhan.bokeya.MainActivity")
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

@androidx.compose.runtime.Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@androidx.compose.runtime.Composable
private fun Amount(text: String, small: Boolean = false) {
    Text(
        text = text,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = if (small) 16.sp else 24.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

class TotalDueWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            GlanceTheme {
                WidgetShell(DeepLinks.HOME) {
                    Caption("মোট বকেয়া")
                    Spacer(GlanceModifier.height(6.dp))
                    Amount(money(data.totalDue))
                    Spacer(GlanceModifier.height(8.dp))
                    Caption("আমি পাব  " + money(data.totalReceivable))
                }
            }
        }
    }
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            GlanceTheme {
                WidgetShell(DeepLinks.HOME) {
                    Caption("আজকের হিসাব")
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Column(GlanceModifier.defaultWeight()) {
                            Caption("দিতে হবে")
                            Amount(money(data.todayPay), small = true)
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Caption("পাবেন")
                            Amount(money(data.todayReceive), small = true)
                        }
                    }
                }
            }
        }
    }
}

class NextPaymentWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            GlanceTheme {
                WidgetShell(DeepLinks.ACCOUNTS) {
                    Caption("পরবর্তী payment")
                    Spacer(GlanceModifier.height(6.dp))
                    if (data.nextTitle == null) {
                        Amount("—", small = true)
                        Caption("সামনে কোনো payment নেই")
                    } else {
                        Text(
                            text = data.nextTitle,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Amount(money(data.nextAmount), small = true)
                        Caption(data.nextDate)
                    }
                }
            }
        }
    }
}

class MonthlySummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            GlanceTheme {
                WidgetShell(DeepLinks.ADD_EXPENSE) {
                    Caption("এই মাস")
                    Spacer(GlanceModifier.height(6.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Column(GlanceModifier.defaultWeight()) {
                            Caption("আয়")
                            Amount(money(data.monthIncome), small = true)
                        }
                        Column(GlanceModifier.defaultWeight()) {
                            Caption("খরচ")
                            Amount(money(data.monthExpense), small = true)
                        }
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Caption("Net  " + money(data.monthIncome - data.monthExpense))
                }
            }
        }
    }
}

class TotalDueWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalDueWidget()
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class NextPaymentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPaymentWidget()
}

class MonthlySummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlySummaryWidget()
}

object BokeyaWidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching {
            TotalDueWidget().updateAll(context)
            TodayWidget().updateAll(context)
            NextPaymentWidget().updateAll(context)
            MonthlySummaryWidget().updateAll(context)
        }
    }

    fun updateAllAsync(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { updateAll(context) }
    }
}
