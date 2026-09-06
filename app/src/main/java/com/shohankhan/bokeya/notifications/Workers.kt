package com.shohankhan.bokeya.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shohankhan.bokeya.appContainer
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.widget.BokeyaWidgetUpdater
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Daily digest: one grouped notification per day rather than one per obligation,
 * so the app can never spam the user.
 */
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val settings = container.settings.settings.first()
        container.repository.runDueRecurring()
        BokeyaWidgetUpdater.updateAll(applicationContext)

        if (!settings.notificationsEnabled) return Result.success()

        val today = Clocks.today()
        val overdue = container.repository.overduePayments(today)
        val dueToday = container.repository.upcomingPayments(today, today)

        if (overdue.isEmpty() && dueToday.isEmpty()) return Result.success()

        val totalToday = Money(dueToday.sumOf { it.amount.poisha })
        val totalOverdue = Money(overdue.sumOf { it.amount.poisha })

        val title: String
        val body: String
        when {
            dueToday.size == 1 && overdue.isEmpty() -> {
                val item = dueToday.first()
                title = "আজ ${item.title}-এর কিস্তি"
                body = "আজ ${item.title}-এর ${CurrencyFormatter.format(item.amount)} দেওয়ার দিন।"
            }
            dueToday.isNotEmpty() && overdue.isEmpty() -> {
                title = "আজ ${bn(dueToday.size)}টি payment আছে"
                body = "আজ মোট ${CurrencyFormatter.format(totalToday)} দিতে হবে।"
            }
            dueToday.isEmpty() -> {
                title = "${bn(overdue.size)}টি হিসাবের তারিখ পেরিয়েছে"
                body = "মোট ${CurrencyFormatter.format(totalOverdue)} বাকি রয়ে গেছে।"
            }
            else -> {
                title = "আজ ${bn(dueToday.size)}টি payment, ${bn(overdue.size)}টি পেরিয়েছে"
                body = "আজ ${CurrencyFormatter.format(totalToday)} এবং পেরিয়ে যাওয়া " +
                    "${CurrencyFormatter.format(totalOverdue)} বাকি আছে।"
            }
        }

        val first = dueToday.firstOrNull() ?: overdue.firstOrNull()
        val actions = buildList {
            first?.let {
                add(
                    Notifications.action(
                        applicationContext,
                        "পরিশোধ করুন",
                        it.accountId.toInt(),
                        Intent(applicationContext, ReminderActionReceiver::class.java).apply {
                            action = ReminderActionReceiver.ACTION_OPEN_PAY
                            putExtra(ReminderActionReceiver.EXTRA_ACCOUNT_ID, it.accountId)
                        },
                    ),
                )
            }
            add(
                Notifications.action(
                    applicationContext,
                    "পরে মনে করিয়ে দিন",
                    9000,
                    Intent(applicationContext, ReminderActionReceiver::class.java).apply {
                        action = ReminderActionReceiver.ACTION_SNOOZE
                        putExtra(ReminderActionReceiver.EXTRA_SNOOZE_MINUTES, 180)
                    },
                ),
            )
        }

        Notifications.show(
            context = applicationContext,
            id = 1001,
            channel = Notifications.CHANNEL_PAYMENTS,
            title = title,
            body = body,
            deepLink = DeepLinks.HOME,
            group = Notifications.GROUP_PAYMENTS,
            summary = true,
            actions = actions,
        )
        return Result.success()
    }

    private fun bn(value: Int) = BanglaNumbers.toBanglaDigits(value.toString())
}

/** Fires user-created reminders that are due. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val settings = container.settings.settings.first()
        if (!settings.notificationsEnabled) return Result.success()

        val pending = container.repository.pendingReminders()
        pending.forEach { reminder ->
            Notifications.show(
                context = applicationContext,
                id = (2000 + reminder.id).toInt(),
                channel = Notifications.CHANNEL_REMINDERS,
                title = reminder.title,
                body = reminder.message,
                deepLink = reminder.accountId?.let { DeepLinks.account(it) } ?: DeepLinks.HOME,
                group = Notifications.GROUP_PAYMENTS,
            )
            container.repository.markReminderFired(reminder.id)
        }
        return Result.success()
    }
}

class NotificationScheduler(private val context: Context) {

    fun scheduleDailyDigest(hour: Int = 9) {
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(Duration.ofHours(24))
            .setInitialDelay(initialDelayTo(hour))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofMinutes(30))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_REMINDERS,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest,
        )
    }

    fun scheduleSnooze(minutes: Int) {
        val request = OneTimeWorkRequestBuilder<DailyDigestWorker>()
            .setInitialDelay(Duration.ofMinutes(minutes.toLong()))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_SNOOZE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun runDigestNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyDigestWorker>().build(),
        )
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_DAILY)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_REMINDERS)
    }

    private fun initialDelayTo(hour: Int): Duration {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour.coerceIn(0, 23), 0))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    companion object {
        const val WORK_DAILY = "bokeya_daily_digest"
        const val WORK_REMINDERS = "bokeya_reminders"
        const val WORK_SNOOZE = "bokeya_snooze"
        const val WORK_NOW = "bokeya_digest_now"
    }
}

/** Handles notification action buttons and re-schedules everything after a device reboot. */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 60)
                NotificationScheduler(context).scheduleSnooze(minutes)
                androidx.core.app.NotificationManagerCompat.from(context).cancel(1001)
            }
            ACTION_OPEN_PAY -> {
                val accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1L)
                if (accountId > 0) {
                    runCatching {
                        Notifications.deepLinkIntent(context, DeepLinks.pay(accountId)).send()
                    }
                }
                androidx.core.app.NotificationManagerCompat.from(context).cancel(1001)
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.shohankhan.bokeya.SNOOZE"
        const val ACTION_OPEN_PAY = "com.shohankhan.bokeya.OPEN_PAY"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_ACCOUNT_ID = "account_id"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            NotificationScheduler(context).scheduleDailyDigest()
            BokeyaWidgetUpdater.updateAllAsync(context)
        }
    }
}
