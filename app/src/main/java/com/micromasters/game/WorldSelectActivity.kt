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
        b.researchCard.setOnClickListener { Dialogs.showResearch(this) { refresh() } }
        b.navWorlds.setOnClickListener { refresh() }
        b.navQuests.setOnClickListener { Dialogs.showQuests(this) { refresh() } }
        b.navFriends.setOnClickListener { Dialogs.showLeaderboard(this) }
        b.navFriends.setOnLongClickListener { Dialogs.showCollection(this) { refresh() }; true }
        b.navSettings.setOnClickListener { Dialogs.showSettings(this) { refresh() } }
    }

    override fun onResume() {
        super.onResume()
        Sound.resumeMusic(this)
        refresh()
        if (!dailyChecked) {
            dailyChecked = true
            val s = Game.get(this)
            if (s.dailyAvailable(System.currentTimeMillis())) {
                Dialogs.showDaily(this) { refresh() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Sound.pauseMusic()
        NumAnim.cancelAll()
    }

    private fun refresh() {
        val s = Game.get(this)
        NumAnim.countTo(b.coinsText, s.coins)
        NumAnim.countTo(b.gemsText, s.gems)
        val now = System.currentTimeMillis()
        s.rolloverDaily(now)
        val tz = java.util.TimeZone.getDefault()
        val msToMidnight = 86_400_000L - (now + tz.getOffset(now)) % 86_400_000L
        b.eventText.text = getString(R.string.event_fmt, Format.duration(msToMidnight))
        b.researchText.text = getString(R.string.research_hub_fmt, Format.short(s.cores))
        buildWorldList(s)
        renderGoal(s, now)
    }

    private fun renderGoal(s: GameState, now: Long) {
        val def = Defs.world(s.activeWorld)
        val worldName = getString(def.nameRes)
        b.goalAction.text = getString(R.string.goal_go)
        when (val hint = s.bestActionHint(now)) {
            is ActionHint.Claim -> {
                b.goalEmoji.text = "🎁"
                b.goalTitle.text = getString(R.string.goal_claim_title)
                b.goalText.text = when (hint.what) {
                    ClaimWhat.DAILY -> getString(R.string.goal_claim_daily)
                    ClaimWhat.QUEST -> getString(R.string.goal_claim_quest)
                    ClaimWhat.RESEARCH -> getString(R.string.goal_claim_research)
                    ClaimWhat.COLLECTION -> getString(R.string.goal_claim_collection)
                }
                b.goalCard.setOnClickListener {
                    when (hint.what) {
                        ClaimWhat.DAILY -> Dialogs.showDaily(this) { refresh() }
                        ClaimWhat.QUEST -> Dialogs.showQuests(this) { refresh() }
                        ClaimWhat.RESEARCH -> Dialogs.showResearch(this) { refresh() }
                        ClaimWhat.COLLECTION -> Dialogs.showCollection(this) { refresh() }
                    }
                }
            }
            is ActionHint.CollectFull -> {
                b.goalEmoji.text = "💰"
                b.goalTitle.text = getString(R.string.goal_collect_title)
                b.goalText.text = getString(R.string.goal_collect_text, worldName)
                b.goalAction.text = getString(R.string.enter)
                b.goalCard.setOnClickListener { enterWorld(s, s.activeWorld) }
            }
            is ActionHint.Prestige -> {
                b.goalEmoji.text = "⭐"
                b.goalTitle.text = getString(R.string.goal_prestige_title)
                b.goalText.text = getString(R.string.goal_prestige_text, worldName, hint.stars)
                b.goalCard.setOnClickListener { enterWorld(s, s.activeWorld) }
            }
            is ActionHint.Upgrade -> {
                val i = hint.labelArg.substring(1).toInt()
                val emoji = if (hint.seg == 0) Defs.UNITS[i].emoji else Defs.BUILDINGS[i].emoji
                val name = if (hint.seg == 0) getString(Defs.UNITS[i].nameRes) else getString(Defs.BUILDINGS[i].nameRes)
                b.goalEmoji.text = "⬆️"
                b.goalTitle.text = getString(R.string.goal_upgrade_title)
                b.goalText.text = getString(R.string.goal_upgrade_text, emoji, name, Format.short(hint.cost))
                val seg = hint.seg
                b.goalCard.setOnClickListener { enterWorldThenUpgrade(s, seg) }
            }
            is ActionHint.Conquer -> {
                b.goalEmoji.text = "🚩"
                b.goalTitle.text = getString(R.string.goal_conquer_title)
                b.goalText.text = getString(R.string.goal_conquer_text, hint.territoryNo, Defs.TERRITORIES, Format.short(hint.cost))
                b.goalCard.setOnClickListener { enterWorld(s, s.activeWorld) }
            }
            is ActionHint.Unlock -> {
                b.goalEmoji.text = if (hint.affordable) "🔓" else "🔒"
                b.goalTitle.text = getString(R.string.goal_unlock_title)
                val name = getString(Defs.world(hint.worldId).nameRes)
                b.goalText.text = if (hint.affordable)
                    getString(R.string.goal_unlock_ready, name, hint.gemCost)
                else getString(R.string.goal_unlock_save, name, hint.gemCost)
                b.goalAction.text = if (hint.affordable) getString(R.string.goal_go) else getString(R.string.shop_title)
                b.goalCard.setOnClickListener {
                    if (hint.affordable) {
                        if (s.unlockWorld(hint.worldId)) { Game.save(this); refresh() }
                    } else Dialogs.showShop(this) { refresh() }
                }
            }
            ActionHint.Idle -> {
                b.goalEmoji.text = "🎯"
                b.goalTitle.text = getString(R.string.goal_idle_title)
                b.goalText.text = getString(R.string.goal_idle_text, worldName)
                b.goalAction.text = getString(R.string.enter)
                b.goalCard.setOnClickListener { enterWorld(s, s.activeWorld) }
            }
        }
    }

    private fun enterWorld(s: GameState, id: String) {
        s.activeWorld = id
        Game.save(this)
        startActivity(Intent(this, Game3DActivity::class.java).putExtra("world", id))
    }

    /** Open the world's 3D base. */
    private fun enterWorldThenUpgrade(s: GameState, seg: Int) {
        Game.save(this)
        startActivity(Intent(this, Game3DActivity::class.java).putExtra("world", s.activeWorld))
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
            item.worldThumbImg.visibility = View.GONE
            item.worldThumb.visibility = View.VISIBLE

            if (ws.unlocked) {
                item.worldLock.visibility = View.GONE
                item.worldProgress.visibility = View.VISIBLE
                item.worldProgress.progress = ws.territories
                item.worldStatus.text = getString(R.string.progress_fmt, ws.territories, Defs.TERRITORIES) +
                    (if (ws.masteryStars > 0) "   ⭐ " + ws.masteryStars else "")
                item.worldAction.text = getString(R.string.enter)
                item.worldAction.backgroundTintList = green
                item.worldAction.setTextColor(0xFF08240F.toInt())
                val enter = View.OnClickListener {
                    s.activeWorld = def.id
                    Game.save(this)
                    startActivity(Intent(this, Game3DActivity::class.java).putExtra("world", def.id))
                }
                item.worldAction.setOnClickListener(enter)
                item.worldCard.setOnClickListener(enter)
            } else {
                item.worldLock.visibility = View.VISIBLE
                item.worldProgress.visibility = View.INVISIBLE
                val need = def.unlockGems - s.gems
                item.worldStatus.text = if (need > 0)
                    getString(R.string.locked_need_gems, Format.short(need))
                else getString(R.string.locked_affordable)
                item.worldAction.text = getString(R.string.unlock_for_gems, def.unlockGems)
                item.worldAction.backgroundTintList = gold
                item.worldAction.setTextColor(0xFF3A2400.toInt())
                val unlock = View.OnClickListener {
                    if (s.unlockWorld(def.id)) {
                        Sound.sfx(this, R.raw.sfx_unlock)
                        Game.save(this)
                        Toast.makeText(this, getString(def.nameRes) + " feloldva! 🎉", Toast.LENGTH_SHORT).show()
                        refresh()
                    } else {
                        Sound.sfx(this, R.raw.sfx_error)
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
