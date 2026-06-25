package com.micromasters.game

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.micromasters.game.databinding.DialogDailyBinding
import com.micromasters.game.databinding.DialogMapBinding
import com.micromasters.game.databinding.ItemDailyBinding
import com.micromasters.game.databinding.ItemUpgradeBinding
import com.micromasters.game.databinding.SheetUpgradesBinding
import kotlin.math.floor
import kotlin.math.max

object Dialogs {

    private fun csl(act: Activity, colorRes: Int): ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(act, colorRes))

    private fun dp(act: Activity, v: Int): Int =
        (v * act.resources.displayMetrics.density).toInt()

    private const val DARK_GREEN = 0xFF08240F.toInt()
    private const val DARK_GOLD = 0xFF3A2400.toInt()
    private const val DIM = 0xFFAFBEDC.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    // ---------------------------------------------------------------- upgrades

    fun showUpgrades(act: Activity, startSeg: Int, onChange: () -> Unit) {
        val s = Game.get(act)
        val v = SheetUpgradesBinding.inflate(act.layoutInflater)
        val dialog = BottomSheetDialog(act)
        dialog.setContentView(v.root)

        var seg = startSeg.coerceIn(0, 2)
        val segs = listOf(v.segUnits, v.segBuildings, v.segBoosts)

        fun styleSegs() {
            for (i in segs.indices) {
                val sel = i == seg
                segs[i].backgroundTintList = csl(act, if (sel) R.color.gold else R.color.panel_light)
                segs[i].setTextColor(if (sel) DARK_GOLD else DIM)
            }
        }

        fun newRow(emoji: String, name: String, sub: String): ItemUpgradeBinding {
            val r = ItemUpgradeBinding.inflate(act.layoutInflater, v.upgradeList, false)
            r.upEmoji.text = emoji
            r.upName.text = name
            r.upSub.text = sub
            return r
        }

        fun rebuild() {
            v.upgradeList.removeAllViews()
            val now = System.currentTimeMillis()
            v.segBoostInfo.text = if (s.boostActive(now)) "⚡ " + Format.duration(s.boostExpiry - now) else ""
            val ws = s.active()
            val def = Defs.world(s.activeWorld)

            when (seg) {
                0 -> for (i in Defs.UNITS.indices) {
                    val d = Defs.UNITS[i]
                    val lvl = ws.unitLevels[i]
                    val contrib = d.baseProd * lvl * def.prodMult * s.workshopMult(ws) * s.labMult(ws) * s.territoryBonus(ws)
                    val sub = if (lvl == 0) act.getString(R.string.hire)
                    else act.getString(R.string.level_fmt, lvl) + " · +" + Format.short(contrib) + "/mp"
                    val r = newRow(d.emoji, act.getString(d.nameRes), sub)
                    r.upBtn.text = Format.short(s.unitCost(ws, i))
                    r.upBtn.setIconResource(R.drawable.ic_coin)
                    r.upBtn.backgroundTintList = csl(act, R.color.green)
                    r.upBtn.setTextColor(DARK_GREEN)
                    r.upBtn.setOnClickListener {
                        if (s.upgradeUnit(i)) { onChange(); rebuild() }
                        else Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show()
                    }
                    v.upgradeList.addView(r.root)
                }

                1 -> for (i in Defs.BUILDINGS.indices) {
                    val d = Defs.BUILDINGS[i]
                    val lvl = ws.buildingLevels[i]
                    val sub = act.getString(R.string.level_fmt, lvl) + " · " + act.getString(d.descRes)
                    val r = newRow(d.emoji, act.getString(d.nameRes), sub)
                    r.upBtn.text = Format.short(s.buildingCost(ws, i))
                    r.upBtn.setIconResource(R.drawable.ic_coin)
                    r.upBtn.backgroundTintList = csl(act, R.color.green)
                    r.upBtn.setTextColor(DARK_GREEN)
                    r.upBtn.setOnClickListener {
                        if (s.upgradeBuilding(i)) { onChange(); rebuild() }
                        else Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show()
                    }
                    v.upgradeList.addView(r.root)
                }

                else -> for (d in Defs.BOOSTS) {
                    val r = newRow(d.emoji, act.getString(d.nameRes), act.getString(d.descRes))
                    if (d.kind == BoostKind.AD) {
                        r.upBtn.text = act.getString(R.string.watch)
                        r.upBtn.icon = null
                        r.upBtn.backgroundTintList = csl(act, R.color.blue)
                        r.upBtn.setTextColor(WHITE)
                    } else {
                        r.upBtn.text = d.gemCost.toString()
                        r.upBtn.setIconResource(R.drawable.ic_gem)
                        r.upBtn.backgroundTintList = csl(act, R.color.gold)
                        r.upBtn.setTextColor(DARK_GOLD)
                    }
                    r.upBtn.setOnClickListener {
                        if (s.applyBoost(d, System.currentTimeMillis())) {
                            onChange(); rebuild()
                            Toast.makeText(act, act.getString(d.nameRes) + " ✓", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(act, R.string.not_enough_gems, Toast.LENGTH_SHORT).show()
                    }
                    v.upgradeList.addView(r.root)
                }
            }
        }

        for (i in segs.indices) segs[i].setOnClickListener { seg = i; styleSegs(); rebuild() }
        styleSegs()
        rebuild()
        dialog.show()
    }

    // ------------------------------------------------------------------- daily

    fun showDaily(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        val v = DialogDailyBinding.inflate(act.layoutInflater)
        val dialog = AlertDialog.Builder(act).setView(v.root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val now = System.currentTimeMillis()
        val idx = s.dailyIndex(now)
        val cols = 4
        v.dailyGrid.removeAllViews()
        v.dailyGrid.columnCount = cols

        for (i in Defs.DAILY.indices) {
            val reward = Defs.DAILY[i]
            val cell = ItemDailyBinding.inflate(act.layoutInflater, v.dailyGrid, false)
            cell.dayLabel.text = act.getString(R.string.daily_day_fmt, i + 1)
            cell.dayIcon.text = if (reward.isGems) "💎" else "🪙"
            cell.dayAmount.text = Format.short(reward.amount)

            when {
                i < idx -> cell.dayCell.alpha = 0.4f
                i == idx -> {
                    cell.dayCell.setBackgroundResource(R.drawable.bg_seg_selected)
                    cell.dayLabel.setTextColor(DARK_GOLD)
                    cell.dayAmount.setTextColor(DARK_GOLD)
                }
            }

            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.columnSpec = GridLayout.spec(i % cols, 1f)
            lp.rowSpec = GridLayout.spec(i / cols)
            val m = dp(act, 4)
            lp.setMargins(m, m, m, m)
            cell.root.layoutParams = lp
            v.dailyGrid.addView(cell.root)
        }

        v.btnClaim.setOnClickListener {
            val r = s.claimDaily(System.currentTimeMillis())
            if (r != null) {
                Game.save(act)
                onChange()
                val unit = if (r.isGems) " 💎" else " 🪙"
                Toast.makeText(act, "+" + Format.short(r.amount) + unit, Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    // -------------------------------------------------------------------- shop

    fun showShop(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        val pad = dp(act, 18)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(act).apply {
            text = act.getString(R.string.shop_title)
            setTextColor(0xFFF4F7FF.toInt())
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val dialog = AlertDialog.Builder(act).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun addRow(emoji: String, name: String, sub: String, btn: String, gem: Boolean, action: () -> Unit) {
            val r = ItemUpgradeBinding.inflate(act.layoutInflater, root, false)
            r.upEmoji.text = emoji
            r.upName.text = name
            r.upSub.text = sub
            r.upBtn.text = btn
            if (gem) {
                r.upBtn.setIconResource(R.drawable.ic_gem)
                r.upBtn.backgroundTintList = csl(act, R.color.gold)
                r.upBtn.setTextColor(DARK_GOLD)
            } else {
                r.upBtn.icon = null
                r.upBtn.backgroundTintList = csl(act, R.color.blue)
                r.upBtn.setTextColor(WHITE)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(act, 12)
            r.root.layoutParams = lp
            r.upBtn.setOnClickListener { action(); onChange() }
            root.addView(r.root)
        }

        addRow("🎬", act.getString(R.string.shop_free_coins), "+5 perc termelés", act.getString(R.string.watch), false) {
            val grant = max(50L, floor(s.baseProdPerSec(s.activeWorld) * 300.0).toLong())
            s.coins += grant
            Game.save(act)
            Toast.makeText(act, "+" + Format.short(grant) + " 🪙", Toast.LENGTH_SHORT).show()
        }
        addRow("💎", act.getString(R.string.shop_gems_small), "+25 💎", act.getString(R.string.get), true) {
            s.gems += 25
            Game.save(act)
            Toast.makeText(act, "+25 💎", Toast.LENGTH_SHORT).show()
        }
        addRow("🧰", act.getString(R.string.shop_gems_big), "+120 💎", act.getString(R.string.get), true) {
            s.gems += 120
            Game.save(act)
            Toast.makeText(act, "+120 💎", Toast.LENGTH_SHORT).show()
        }

        root.addView(TextView(act).apply {
            text = act.getString(R.string.shop_note)
            setTextColor(DIM)
            textSize = 11f
            val t = dp(act, 12)
            setPadding(0, t, 0, dp(act, 4))
        })
        root.addView(android.widget.Button(act).apply {
            text = act.getString(R.string.close)
            setBackgroundColor(0x00000000)
            setTextColor(DIM)
            setOnClickListener { dialog.dismiss() }
        })

        dialog.show()
    }

    // --------------------------------------------------------------------- map

    fun showMap(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        val v = DialogMapBinding.inflate(act.layoutInflater)
        val dialog = AlertDialog.Builder(act).setView(v.root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val def = Defs.world(s.activeWorld)

        fun refresh() {
            val ws = s.active()
            v.mapTitle.text = act.getString(R.string.map_title, act.getString(def.nameRes))
            v.mapEmoji.text = def.emoji
            v.mapProgress.text = act.getString(R.string.progress_fmt, ws.territories, Defs.TERRITORIES)
            v.mapBar.progress = ws.territories
            if (ws.territories >= Defs.TERRITORIES) {
                v.mapHint.text = act.getString(R.string.all_conquered)
                v.btnConquer.isEnabled = false
                v.btnConquer.icon = null
                v.btnConquer.text = act.getString(R.string.all_conquered)
            } else {
                v.mapHint.text = "Minden meghódított terület +5% termelés"
                v.btnConquer.isEnabled = true
                v.btnConquer.setIconResource(R.drawable.ic_coin)
                v.btnConquer.text = act.getString(R.string.conquer) + " · " + Format.short(s.territoryCost(s.activeWorld))
            }
        }

        v.btnConquer.setOnClickListener {
            val res = s.conquer()
            if (res.ok) {
                Game.save(act)
                onChange()
                if (res.clearedWorld) {
                    Toast.makeText(act, act.getString(R.string.world_cleared_reward, res.gemReward), Toast.LENGTH_LONG).show()
                }
                refresh()
            } else {
                Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show()
            }
        }
        v.btnMapClose.setOnClickListener { dialog.dismiss() }
        refresh()
        dialog.show()
    }

    // ---------------------------------------------------------------- settings

    fun showSettings(act: Activity, onChange: () -> Unit) {
        AlertDialog.Builder(act)
            .setTitle(R.string.settings)
            .setMessage("MicroMasters v1.0\nHyper-casual · Idle · Strategy\n\nÉpítsd, fejleszd és irányítsd apró egységeidet mikroszkopikus világokban!")
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.reset) { _, _ ->
                AlertDialog.Builder(act)
                    .setMessage(R.string.reset_confirm)
                    .setPositiveButton("OK") { _, _ ->
                        Game.reset(act)
                        val i = Intent(act, TitleActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        act.startActivity(i)
                        onChange()
                    }
                    .setNegativeButton(R.string.close, null)
                    .show()
            }
            .show()
    }
}
