package com.micromasters.game

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.micromasters.game.databinding.ActivityGameBinding
import kotlin.math.floor

class GameActivity : AppCompatActivity() {

    private lateinit var b: ActivityGameBinding
    private lateinit var s: GameState
    private val handler = Handler(Looper.getMainLooper())
    private var shownWorkers = -1
    private var lastEmptyToast = 0L

    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            s.tick(now)
            refreshLive(now)
            handler.postDelayed(this, 200L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGameBinding.inflate(layoutInflater)
        setContentView(b.root)
        s = Game.get(this)

        // Offline earnings since last session (decoupled from the live storage cap).
        val now = System.currentTimeMillis()
        val away = now - s.lastSeen
        val earned = if (away > 60_000L) s.accrueOffline(now, away) else { s.tick(now); 0.0 }
        if (earned >= 1.0) {
            Toast.makeText(this, getString(R.string.welcome_back, Format.short(earned)), Toast.LENGTH_LONG).show()
        }

        b.btnBack.setOnClickListener { finish() }
        b.btnShop.setOnClickListener { it.bounce(); Dialogs.showShop(this) { onStateChanged() } }
        b.btnGameMenu.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 2) { onStateChanged() } }
        b.btnCollect.setOnClickListener { b.btnCollect.bounce(); collect() }
        b.gameView.setOnClickListener { collect() }
        b.btnUpgrade.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnUnits.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnMap.setOnClickListener { it.bounce(); Dialogs.showMap(this) { onStateChanged() } }
    }

    override fun onResume() {
        super.onResume()
        s = Game.get(this)
        val def = Defs.world(s.activeWorld)
        b.worldTitle.text = def.emoji + "  " + getString(def.nameRes)
        shownWorkers = -1
        syncWorkers()
        b.gameView.resume()
        refreshLive(System.currentTimeMillis())
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        b.gameView.pause()
        Game.save(this)
    }

    private fun View.bounce() {
        animate().cancel()
        scaleX = 0.9f
        scaleY = 0.9f
        animate().scaleX(1f).scaleY(1f).setDuration(170)
            .setInterpolator(OvershootInterpolator(3f)).start()
    }

    private fun workerCount(): Int {
        var sum = 0
        for (l in s.active().unitLevels) sum += l
        return (3 + sum / 2).coerceIn(3, 16)
    }

    private fun syncWorkers() {
        val wc = workerCount()
        if (wc != shownWorkers) {
            shownWorkers = wc
            b.gameView.configure(Defs.world(s.activeWorld), wc)
        }
    }

    private fun onStateChanged() {
        syncWorkers()
        refreshLive(System.currentTimeMillis())
        Game.save(this)
    }

    private fun collect() {
        val now = System.currentTimeMillis()
        s.tick(now)
        val amount = s.collect()
        if (amount > 0) {
            b.gameView.spawnCollect("+" + Format.short(amount))
            b.btnCollect.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Game.save(this)
        } else {
            b.btnCollect.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            b.gameView.shake()
            if (now - lastEmptyToast > 1500L) {
                lastEmptyToast = now
                Toast.makeText(this, getString(R.string.storage_empty), Toast.LENGTH_SHORT).show()
            }
        }
        refreshLive(now)
    }

    private fun refreshLive(now: Long) {
        b.coinsText.text = Format.short(s.coins)
        b.gemsText.text = Format.short(s.gems)

        val ws = s.active()
        val cap = s.capacity(s.activeWorld)
        val ratio = if (cap > 0) (ws.pending / cap).toFloat() else 0f
        b.storageBar.progress = (ratio * 1000).toInt().coerceIn(0, 1000)
        b.gameView.setFill(ratio)

        val full = ws.pending >= cap - 0.5
        b.storageText.text = if (full) getString(R.string.storage_full)
        else getString(R.string.storage_fmt, Format.short(ws.pending), Format.short(cap))

        val prod = s.effectiveProdPerSec(s.activeWorld, now)
        b.prodText.text = "▲ " + getString(R.string.per_sec_fmt, Format.short(prod))

        val pend = floor(ws.pending).toLong()
        b.btnCollect.text = if (pend >= 1) getString(R.string.collect) + "  +" + Format.short(pend)
        else getString(R.string.collect)
    }
}
