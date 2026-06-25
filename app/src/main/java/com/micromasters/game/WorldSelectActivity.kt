package com.micromasters.game

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micromasters.game.databinding.ActivityWorldSelectBinding
import com.micromasters.game.databinding.ItemWorldBinding

class WorldSelectActivity : AppCompatActivity() {

    private lateinit var b: ActivityWorldSelectBinding
    private var dailyChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityWorldSelectBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnShop.setOnClickListener { Dialogs.showShop(this) { refresh() } }
        b.eventBanner.setOnClickListener { Dialogs.showShop(this) { refresh() } }
        b.navWorlds.setOnClickListener { refresh() }
        b.navFriends.setOnClickListener { Dialogs.showLeaderboard(this) }
        b.navSettings.setOnClickListener { Dialogs.showSettings(this) { refresh() } }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (!dailyChecked) {
            dailyChecked = true
            val s = Game.get(this)
            if (s.dailyAvailable(System.currentTimeMillis())) {
                Dialogs.showDaily(this) { refresh() }
            }
        }
    }

    private fun refresh() {
        val s = Game.get(this)
        b.coinsText.text = Format.short(s.coins)
        b.gemsText.text = Format.short(s.gems)
        val now = System.currentTimeMillis()
        val tz = java.util.TimeZone.getDefault()
        val msToMidnight = 86_400_000L - (now + tz.getOffset(now)) % 86_400_000L
        b.eventText.text = getString(R.string.event_fmt, Format.duration(msToMidnight))
        buildWorldList(s)
    }

    private fun buildWorldList(s: GameState) {
        b.worldList.removeAllViews()
        val green = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))
        val gold = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gold))
        for (def in Defs.WORLDS) {
            val item = ItemWorldBinding.inflate(layoutInflater, b.worldList, false)
            val ws = s.world(def.id)
            item.worldThumb.text = def.emoji
            item.worldName.text = getString(def.nameRes)

            if (ws.unlocked) {
                item.worldLock.visibility = View.GONE
                item.worldProgress.visibility = View.VISIBLE
                item.worldProgress.progress = ws.territories
                item.worldStatus.text = getString(R.string.progress_fmt, ws.territories, Defs.TERRITORIES)
                item.worldAction.text = getString(R.string.enter)
                item.worldAction.backgroundTintList = green
                item.worldAction.setTextColor(0xFF08240F.toInt())
                val enter = View.OnClickListener {
                    s.activeWorld = def.id
                    Game.save(this)
                    startActivity(Intent(this, GameActivity::class.java))
                }
                item.worldAction.setOnClickListener(enter)
                item.worldCard.setOnClickListener(enter)
            } else {
                item.worldLock.visibility = View.VISIBLE
                item.worldProgress.visibility = View.INVISIBLE
                item.worldStatus.text = getString(R.string.locked)
                item.worldAction.text = getString(R.string.unlock_for_gems, def.unlockGems)
                item.worldAction.backgroundTintList = gold
                item.worldAction.setTextColor(0xFF3A2400.toInt())
                val unlock = View.OnClickListener {
                    if (s.unlockWorld(def.id)) {
                        Game.save(this)
                        Toast.makeText(this, getString(def.nameRes) + " feloldva! 🎉", Toast.LENGTH_SHORT).show()
                        refresh()
                    } else {
                        Toast.makeText(this, getString(R.string.not_enough_gems), Toast.LENGTH_SHORT).show()
                    }
                }
                item.worldAction.setOnClickListener(unlock)
                item.worldCard.setOnClickListener(unlock)
            }
            b.worldList.addView(item.root)
        }
    }
}
