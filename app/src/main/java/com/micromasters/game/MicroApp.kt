package com.micromasters.game

import android.app.Application
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Installs a process-wide uncaught-exception handler so that, instead of the
 * system "app keeps stopping" dialog, we record the stack trace and surface it
 * in [CrashActivity]. This turns any user's device into a diagnostic source.
 */
class MicroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Sound.init(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()
                getSharedPreferences("crash", MODE_PRIVATE).edit()
                    .putString("trace", trace).apply()
                val i = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("trace", trace)
                startActivity(i)
            } catch (_: Throwable) {
                previous?.uncaughtException(thread, error)
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
