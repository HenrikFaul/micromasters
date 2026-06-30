package com.micromasters.game

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Local, network-free re-engagement: a once-a-day "your streak is waiting" reminder
 * scheduled with WorkManager. Everything here is best-effort — if the platform refuses
 * (no permission, OEM limits), the game is unaffected. No data ever leaves the device.
 */
object Notifications {
    const val CHANNEL_ID = "mm_reminders"
    private const val WORK_NAME = "mm_streak_reminder"

    /** Create the notification channel (Android 8+). Idempotent. */
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                ch.description = ctx.getString(R.string.notif_channel_desc)
                mgr.createNotificationChannel(ch)
            }
        } catch (e: Throwable) { /* best-effort: a missing channel only disables reminders */ }
    }

    /** Schedule (or refresh) the daily reminder. Safe to call on every app start. */
    fun scheduleDailyReminder(ctx: Context) {
        try {
            val req = PeriodicWorkRequestBuilder<StreakReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(20, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        } catch (e: Throwable) { /* best-effort: scheduling failure must not affect gameplay */ }
    }
}
