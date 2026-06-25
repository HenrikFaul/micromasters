package com.micromasters.game

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

            b.btnPlay.setOnClickListener {
                startActivity(Intent(this, WorldSelectActivity::class.java))
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
        if (!::b.isInitialized) return
        bobbing = true
        bobPlanet(true)
    }

    override fun onPause() {
        super.onPause()
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
