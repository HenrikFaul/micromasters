package com.micromasters.game

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Fired by a daily inexact AlarmManager alarm (see [Notifications]). Posts the
 * "come back" reminder entirely on-device — no network, no services, no extra
 * permissions beyond POST_NOTIFICATIONS. Silently no-ops if notifications are off.
 */
class StreakReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled() below
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) return
            Notifications.ensureChannel(context)

            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, TitleActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, Notifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(context.getString(R.string.notif_streak_title))
                .setContentText(context.getString(R.string.notif_streak_text))
                .setAutoCancel(true)
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(1001, n)
        } catch (e: Throwable) {
            // best-effort reminder: must never crash on a background broadcast
        }
    }
}
