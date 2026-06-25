package com.micromasters.game

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.micromasters.game.databinding.ActivityTitleBinding

class TitleActivity : AppCompatActivity() {

    private lateinit var b: ActivityTitleBinding
    private var bobbing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTitleBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Warm up / migrate the save so first gameplay frame is instant.
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
    }

    override fun onResume() {
        super.onResume()
        bobbing = true
        bobPlanet(true)
    }

    override fun onPause() {
        super.onPause()
        // Stop the self-recursive animation so it can't run after navigation or leak the Activity.
        bobbing = false
        b.planet.animate().cancel()
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
}
