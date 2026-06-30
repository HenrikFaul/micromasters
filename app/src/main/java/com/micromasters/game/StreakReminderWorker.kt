package com.micromasters.game

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Posts the daily "come back" reminder. Runs entirely on-device (no network).
 * If notifications are not permitted, it silently no-ops.
 */
class StreakReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled() below
    override fun doWork(): Result {
        try {
            val ctx = applicationContext
            val nm = NotificationManagerCompat.from(ctx)
            if (!nm.areNotificationsEnabled()) return Result.success()

            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, TitleActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, Notifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(ctx.getString(R.string.notif_streak_title))
                .setContentText(ctx.getString(R.string.notif_streak_text))
                .setAutoCancel(true)
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(1001, n)
        } catch (e: Throwable) {
            // best-effort reminder: never fail in a way that reschedules aggressively
        }
        return Result.success()
    }
}
