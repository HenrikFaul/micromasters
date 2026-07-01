package com.micromasters.game

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
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
    private var pendingOffline: GameState.OfflineResult? = null
    private var lastTapX = 0f
    private var lastTapY = 0f

    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            s.tick(now)
            s.expireComboIfStale(now)
            b.gameView.setCombo(s.comboFraction(now), s.comboTimeFraction(now), s.comboMult(now).toFloat())
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
        if (away > 60_000L) pendingOffline = s.accrueOffline(now, away) else s.tick(now)

        b.btnBack.setOnClickListener { finish() }
        b.btnShop.setOnClickListener { it.bounce(); Dialogs.showShop(this) { onStateChanged() } }
        b.btnGameMenu.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 2) { onStateChanged() } }
        b.btnCollect.setOnClickListener { b.btnCollect.bounce(); collect() }
        // Tapping the scene: a golden node takes priority, otherwise a normal active collect.
        b.gameView.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { lastTapX = ev.x; lastTapY = ev.y }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    val now = System.currentTimeMillis()
                    if (b.gameView.goldenHitTest(ev.x, ev.y)) goldenCollect(now) else collect()
                }
            }
            true
        }
        b.btnUpgrade.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnUnits.setOnClickListener { it.bounce(); Dialogs.showUpgrades(this, 0) { onStateChanged() } }
        b.btnMap.setOnClickListener {
            it.bounce()
            Dialogs.showMap(this, onChange = { onStateChanged() }) {
                b.gameView.spawnCelebrate()
                b.btnCollect.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Sound.resumeMusic(this)
        s = Game.get(this)
        val def = Defs.world(s.activeWorld)
        b.worldTitle.text = def.emoji + "  " + getString(def.nameRes)
        shownWorkers = -1
        syncWorkers()
        b.gameView.resume()
        refreshLive(System.currentTimeMillis())
        handler.removeCallbacks(ticker)
        handler.post(ticker)

        pendingOffline?.let { r ->
            pendingOffline = null
            if (r.coins >= 1 || r.gems >= 1) Dialogs.showOffline(this, r) { onStateChanged() }
        }

        // Honour the hub goal-card "fejleszd" shortcut: pop the upgrades sheet once.
        val seg = intent.getIntExtra("openUpgradeSeg", -1)
        if (seg >= 0) {
            intent.removeExtra("openUpgradeSeg")
            b.gameView.post { Dialogs.showUpgrades(this, seg) { onStateChanged() } }
        }

        // First-ever entry: a one-time welcome explaining the loop.
        if (s.markMilestone(GameState.M_ONBOARDED)) {
            Game.save(this)
            Dialogs.showWelcome(this)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        b.gameView.pause()
        Sound.pauseMusic()
        NumAnim.cancelAll()
        s.comboHits = 0; s.comboExpiry = 0L   // drop the transient combo chain on background
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
        val r = s.collectActive(now, golden = false)
        if (r.amount > 0) {
            b.gameView.spawnCollect("+" + Format.short(r.amount))
            Sound.sfx(this, if (r.crit) R.raw.sfx_mega else R.raw.sfx_collect)
            if (r.crit) b.gameView.spawnCrit(getString(R.string.crit_pop))
            b.btnCollect.performHapticFeedback(
                if (r.crit) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY)
            Game.save(this)
            maybeMilestone()
        } else {
            b.btnCollect.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            b.gameView.shake()
            if (now - lastEmptyToast > 1500L) {
                lastEmptyToast = now
                Sound.sfx(this, R.raw.sfx_error)
                Toast.makeText(this, getString(R.string.storage_empty), Toast.LENGTH_SHORT).show()
            }
        }
        refreshLive(now)
    }

    /** One-shot celebratory toast when the active world crosses a lifetime-coin milestone. */
    private fun maybeMilestone() {
        val lc = s.active().lifetimeCoins
        val bit = when {
            lc >= 1_000_000_000.0 -> GameState.M_COINS_1B
            lc >= 1_000_000.0 -> GameState.M_COINS_1M
            else -> 0L
        }
        if (bit != 0L && s.markMilestone(bit)) {
            Game.save(this)
            val amt = if (bit == GameState.M_COINS_1B) "1B" else "1M"
            Toast.makeText(this, getString(R.string.milestone_coins, amt), Toast.LENGTH_LONG).show()
        }
    }

    /** Golden-node cash-in: a production burst plus its own juice. */
    private fun goldenCollect(now: Long) {
        s.tick(now)
        val r = s.collectActive(now, golden = true)
        if (r.amount > 0) {
            Sound.sfx(this, R.raw.sfx_mega)
            b.gameView.spawnGolden("+" + Format.short(r.amount))
            if (r.crit) b.gameView.spawnCrit(getString(R.string.crit_pop))
            b.btnCollect.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            Game.save(this)
            Toast.makeText(this, getString(R.string.golden_pop), Toast.LENGTH_SHORT).show()
        }
        refreshLive(now)
    }

    private fun refreshLive(now: Long) {
        NumAnim.countTo(b.coinsText, s.coins)
        NumAnim.countTo(b.gemsText, s.gems)

        if (s.cores > 0) {
            b.coresPill.visibility = View.VISIBLE
            NumAnim.countTo(b.coresText, s.cores)
        } else b.coresPill.visibility = View.GONE

        val ws = s.active()
        b.starText.text = if (ws.masteryStars > 0) "⭐ " + ws.masteryStars else ""
        b.essenceText.text = Defs.world(s.activeWorld).essenceEmoji + " " + Format.short(ws.essence)
        val cap = s.capacity(s.activeWorld)
        val ratio = if (cap > 0) (ws.pending / cap).toFloat() else 0f
        NumAnim.barTo(b.storageBar, (ratio * 1000).toInt().coerceIn(0, 1000))
        b.gameView.setFill(ratio)

        val full = ws.pending >= cap - 0.5
        b.storageText.text = if (full) getString(R.string.storage_full)
        else getString(R.string.storage_fmt, Format.short(ws.pending), Format.short(cap))

        val prod = s.effectiveProdPerSec(s.activeWorld, now)
        b.prodText.text = "▲ " + getString(R.string.per_sec_fmt, Format.short(prod))

        // signature-twist live badge (only when the player has invested in the driver)
        val def = Defs.world(s.activeWorld)
        val frac = s.twistBonusFrac(ws, def)
        if (frac > 0.0) {
            b.prodText.text = b.prodText.text.toString() + "   " + def.twist.emoji + " " +
                getString(def.twist.nameRes) + " " + String.format(java.util.Locale.US, "×%.2f", 1.0 + frac)
        }

        val pend = floor(ws.pending).toLong()
        b.btnCollect.text = if (pend >= 1) getString(R.string.collect) + "  +" + Format.short(pend)
        else getString(R.string.collect)
    }
}
