package com.micromasters.game

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.micromasters.game.databinding.ActivityGameBinding
import kotlin.math.floor

class GameActivity : AppCompatActivity() {

    private lateinit var b: ActivityGameBinding
    private lateinit var s: GameState
    private val handler = Handler(Looper.getMainLooper())
    private var shownWorkers = -1

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

        // Offline earnings since last session.
        val now = System.currentTimeMillis()
        val ws = s.active()
        val before = ws.pending
        s.tick(now)
        val earned = ws.pending - before
        if (earned >= 1.0 && now - s.lastSeen > 60_000L) {
            Toast.makeText(this, getString(R.string.welcome_back, Format.short(earned)), Toast.LENGTH_LONG).show()
        }

        b.btnBack.setOnClickListener { finish() }
        b.btnShop.setOnClickListener { Dialogs.showShop(this) { onStateChanged() } }
        b.btnGameMenu.setOnClickListener { Dialogs.showSettings(this) { onStateChanged() } }
        b.btnCollect.setOnClickListener { collect() }
        b.gameView.setOnClickListener { collect() }
        b.btnUpgrade.setOnClickListener { Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnUnits.setOnClickListener { Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnMap.setOnClickListener { Dialogs.showMap(this) { onStateChanged() } }
    }

    override fun onResume() {
        super.onResume()
        s = Game.get(this)
        val def = Defs.world(s.activeWorld)
        b.worldTitle.text = def.emoji + "  " + getString(def.nameRes)
        shownWorkers = -1
        syncWorkers()
        refreshLive(System.currentTimeMillis())
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        Game.save(this)
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
            Game.save(this)
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
