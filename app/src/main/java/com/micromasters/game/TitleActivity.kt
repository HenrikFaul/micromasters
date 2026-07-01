package com.micromasters.game

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micromasters.game.databinding.ActivityTitleBinding
import java.io.PrintWriter
import java.io.StringWriter

class TitleActivity : AppCompatActivity() {

    private lateinit var b: ActivityTitleBinding
    private var bobbing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (showPendingCrash()) return
        try {
            b = ActivityTitleBinding.inflate(layoutInflater)
            setContentView(b.root)

            // Warm up / migrate the save so the first gameplay frame is instant.
            Game.get(this)

            // Local, on-device daily reminder (no network). Idempotent on every launch.
            Notifications.ensureChannel(this)
            Notifications.scheduleDailyReminder(this)
            maybeAskNotifPermission()

            b.btnPlay.setOnClickListener {
                Sound.sfx(this, R.raw.sfx_tap)
                // Straight into the discovery game — the core loop (combine → discover)
                // is the whole point, so don't bury it behind a menu detour.
                val world = Game.get(this).activeWorld
                startActivity(Intent(this, Game3DActivity::class.java).putExtra("world", world))
            }
            b.btnPlay.setOnLongClickListener {
                // Power-user shortcut to the legacy world hub (still available, just not in the way).
                startActivity(Intent(this, WorldSelectActivity::class.java))
                true
            }
            b.btnLogin.setOnClickListener {
                Toast.makeText(this, getString(R.string.title_login) + " ✓", Toast.LENGTH_SHORT).show()
            }
            b.titleSettings.setOnClickListener {
                Dialogs.showSettings(this) { }
            }
        } catch (e: Throwable) {
            crashTo(e)
        }
    }

    override fun onResume() {
        super.onResume()
        Sound.resumeMusic(this)
        if (!::b.isInitialized) return
        bobbing = true
        bobPlanet(true)
    }

    override fun onPause() {
        super.onPause()
        Sound.pauseMusic()
        bobbing = false
        if (::b.isInitialized) b.planet.animate().cancel()
    }

    private fun bobPlanet(up: Boolean) {
        if (!bobbing) return
        b.planet.animate()
            .translationY(if (up) 24f else -24f)
            .setDuration(2200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { bobPlanet(!up) }
            .start()
    }

    /** Ask once for notification permission on Android 13+ (declined gracefully = no reminders). */
    private fun maybeAskNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            try { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001) } catch (e: Throwable) { /* user can grant later in settings */ }
        }
    }

    /** If a crash was recorded, show it instead of risking another crash loop. */
    private fun showPendingCrash(): Boolean {
        val t = getSharedPreferences("crash", MODE_PRIVATE).getString("trace", null) ?: return false
        startActivity(Intent(this, CrashActivity::class.java).putExtra("trace", t))
        finish()
        return true
    }

    private fun crashTo(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val trace = sw.toString()
        getSharedPreferences("crash", MODE_PRIVATE).edit().putString("trace", trace).apply()
        startActivity(Intent(this, CrashActivity::class.java).putExtra("trace", trace))
        finish()
    }
}
