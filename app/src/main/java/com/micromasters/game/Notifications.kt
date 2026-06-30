package com.micromasters.game

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Local, network-free re-engagement: a once-a-day "your streak is waiting" reminder
 * scheduled with a plain inexact [AlarmManager] alarm — no WorkManager, no extra
 * permissions, no background services. Re-armed on every app launch so it survives
 * reboots without needing RECEIVE_BOOT_COMPLETED. All best-effort; failures are silent.
 */
object Notifications {
    const val CHANNEL_ID = "mm_reminders"
    private const val REQUEST = 2001
    private const val DAY_MS = 24L * 60L * 60L * 1000L

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

    /** Schedule (or refresh) the daily reminder ~20h out. Safe to call on every app start. */
    fun scheduleDailyReminder(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = PendingIntent.getBroadcast(
                ctx, REQUEST,
                Intent(ctx, StreakReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val firstAt = System.currentTimeMillis() + 20L * 60L * 60L * 1000L
            // Inexact + repeating: needs no special permission and is battery-friendly.
            am.setInexactRepeating(AlarmManager.RTC, firstAt, DAY_MS, pi)
        } catch (e: Throwable) { /* best-effort: scheduling failure must not affect gameplay */ }
    }
}
