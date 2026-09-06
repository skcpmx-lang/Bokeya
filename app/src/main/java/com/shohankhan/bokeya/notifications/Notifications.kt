package com.shohankhan.bokeya.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shohankhan.bokeya.MainActivity
import com.shohankhan.bokeya.R

object Notifications {

    const val CHANNEL_PAYMENTS = "bokeya_payments"
    const val CHANNEL_REMINDERS = "bokeya_reminders"
    const val CHANNEL_SUMMARY = "bokeya_summary"

    const val GROUP_PAYMENTS = "bokeya.group.payments"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PAYMENTS,
                "Payment reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "কিস্তি ও বকেয়া পরিশোধের মনে করিয়ে দেওয়া" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Custom reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "আপনার নিজের তৈরি reminder" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily summary",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "দিনের হিসাবের সংক্ষিপ্ত সারাংশ" },
        )
    }

    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun deepLinkIntent(context: Context, uri: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            uri.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun show(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        body: String,
        deepLink: String = DeepLinks.HOME,
        group: String? = null,
        summary: Boolean = false,
        actions: List<NotificationCompat.Action> = emptyList(),
    ) {
        if (!canPost(context)) return
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(deepLinkIntent(context, deepLink))
        group?.let {
            builder.setGroup(it)
            if (summary) builder.setGroupSummary(true)
        }
        actions.forEach { builder.addAction(it) }
        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    fun action(context: Context, label: String, requestCode: Int, intent: Intent): NotificationCompat.Action {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, label, pending).build()
    }
}

object DeepLinks {
    const val SCHEME = "bokeya://"
    const val HOME = "bokeya://home"
    const val ACCOUNTS = "bokeya://accounts"
    const val CALENDAR = "bokeya://calendar"
    const val ADD_EXPENSE = "bokeya://add/expense"
    const val ADD_INCOME = "bokeya://add/income"
    const val ADD_DEBT = "bokeya://add/debt"
    const val PAY = "bokeya://pay"

    fun account(id: Long) = "bokeya://account/$id"
    fun pay(id: Long) = "bokeya://pay/$id"
}
