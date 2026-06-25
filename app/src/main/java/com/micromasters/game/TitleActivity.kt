package com.micromasters.game

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.micromasters.game.databinding.ActivityTitleBinding

class TitleActivity : AppCompatActivity() {

    private lateinit var b: ActivityTitleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTitleBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Warm up / migrate the save so first gameplay frame is instant.
        Game.get(this)

        // Gentle floating animation on the planet.
        b.planet.animate()
            .translationY(-24f)
            .setDuration(2200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { bobPlanet(true) }
            .start()

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

    private fun bobPlanet(up: Boolean) {
        b.planet.animate()
            .translationY(if (up) 24f else -24f)
            .setDuration(2200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { bobPlanet(!up) }
            .start()
    }
}
