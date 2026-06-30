package com.micromasters.game

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
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

    private fun View.pop() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        animate().cancel()
        scaleX = 0.85f
        scaleY = 0.85f
        animate().scaleX(1f).scaleY(1f).setDuration(150)
            .setInterpolator(OvershootInterpolator()).start()
    }

    /** A Material-styled action button matching the rest of the app's dialogs. */
    private fun mbtn(act: Activity, label: String, bgRes: Int, fg: Int): MaterialButton =
        MaterialButton(act).apply {
            text = label
            isAllCaps = false
            setTextColor(fg)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            backgroundTintList = csl(act, bgRes)
            cornerRadius = dp(act, 16)
            insetTop = 0
            insetBottom = 0
        }

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

        fun newRow(emoji: String, name: String, sub: String, artRes: Int = 0): ItemUpgradeBinding {
            val r = ItemUpgradeBinding.inflate(act.layoutInflater, v.upgradeList, false)
            r.upName.text = name
            r.upSub.text = sub
            if (artRes != 0) {
                r.upImg.setImageResource(artRes)
                r.upImg.visibility = View.VISIBLE
                r.upEmoji.visibility = View.GONE
            } else {
                r.upEmoji.text = emoji
                r.upImg.visibility = View.GONE
                r.upEmoji.visibility = View.VISIBLE
            }
            return r
        }

        var qtyMode = 0  // 0 = ×1, 1 = ×10, 2 = Max  (transient UI state, never saved)
        val qtyLabels = listOf("×1", "×10", act.getString(R.string.qty_max))

        fun qtyFor(maxN: Int): Int = when (qtyMode) {
            0 -> 1
            1 -> 10
            else -> maxN.coerceAtLeast(1)
        }

        fun paybackStr(sec: Double): String = when {
            !sec.isFinite() || sec > 86_400.0 -> ""
            sec < 60.0 -> "${sec.toLong()} mp"
            sec < 3600.0 -> "${(sec / 60).toLong()} p"
            else -> "${(sec / 3600).toLong()} ó"
        }

        fun roiLine(cost: Long, dProd: Double): String {
            if (dProd <= 0.0 || cost <= 0L) return ""
            val str = paybackStr(cost / dProd)
            return if (str.isEmpty()) "" else "  · " + act.getString(R.string.roi_payback_fmt, str)
        }

        // index of the affordable unit/building row with the best (lowest) payback → gets the ⭐
        fun bestBuyIndex(ws: WorldState): Int {
            var best = -1; var bestPb = Double.MAX_VALUE
            val count = if (seg == 0) Defs.UNITS.size else Defs.BUILDINGS.size
            for (i in 0 until count) {
                val cost = if (seg == 0) s.unitCost(ws, i) else s.buildingCost(ws, i)
                if (cost == Long.MAX_VALUE || s.coins < cost) continue
                val dProd = if (seg == 0) s.unitProdDelta(ws, i) else s.buildingProdDelta(ws, i)
                if (dProd <= 0.0) continue
                val pb = cost / dProd
                if (pb < bestPb) { bestPb = pb; best = i }
            }
            return best
        }

        fun rebuild() {
            v.upgradeList.removeAllViews()
            val now = System.currentTimeMillis()
            v.segBoostInfo.text = if (s.boostActive(now)) "⚡️ " + Format.duration(s.boostExpiry - now) else ""
            val ws = s.active()
            val def = Defs.world(s.activeWorld)

            // signature-twist strategy hint for the active world
            if (def.twist.kind != TwistKind.NONE) {
                val frac = s.twistBonusFrac(ws, def)
                v.upgradeList.addView(TextView(act).apply {
                    text = def.twist.emoji + "  " + act.getString(def.twist.nameRes) +
                        "  " + String.format(java.util.Locale.US, "×%.2f", 1.0 + frac)
                    setTextColor(csl(act, R.color.gold))
                    textSize = 13f
                    setPadding(0, dp(act, 2), 0, dp(act, 10))
                })
            }

            // bulk-buy quantity toggle (Units / Buildings only)
            if (seg == 0 || seg == 1) {
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(act, 40)
                    ).apply { bottomMargin = dp(act, 10) }
                }
                for (m in 0..2) {
                    val sel = m == qtyMode
                    val tb = mbtn(act, qtyLabels[m], if (sel) R.color.gold else R.color.panel_light, if (sel) DARK_GOLD else DIM)
                    tb.textSize = 13f
                    val lp = LinearLayout.LayoutParams(0, dp(act, 40), 1f)
                    if (m < 2) lp.marginEnd = dp(act, 6)
                    tb.setOnClickListener { qtyMode = m; rebuild() }
                    row.addView(tb, lp)
                }
                v.upgradeList.addView(row)
            }

            when (seg) {
                0 -> {
                    val best = bestBuyIndex(ws)
                    for (i in Defs.UNITS.indices) {
                        val d = Defs.UNITS[i]
                        val lvl = ws.unitLevels[i]
                        val contrib = d.baseProd * lvl * def.prodMult * s.workshopMult(ws) * s.territoryBonus(ws)
                        val baseSub = if (lvl == 0) act.getString(R.string.hire)
                        else act.getString(R.string.level_fmt, lvl) + " · +" + Format.short(contrib) + "/mp"
                        val sub = baseSub + roiLine(s.unitCost(ws, i), s.unitProdDelta(ws, i))
                        val name = if (i == best) act.getString(d.nameRes) + "  ⭐" else act.getString(d.nameRes)
                        val r = newRow(d.emoji, name, sub, d.artRes)
                        val maxN = s.maxAffordableUnit(ws, i)
                        val n = qtyFor(maxN)
                        val cost = if (n <= 1) s.unitCost(ws, i) else s.bulkUnitCost(ws, i, n)
                        r.upBtn.text = if (qtyMode == 0) Format.short(cost) else "×$n  " + Format.short(cost)
                        r.upBtn.setIconResource(R.drawable.ic_coin)
                        val afford = maxN >= 1
                        r.upBtn.backgroundTintList = csl(act, if (afford) R.color.green else R.color.panel_light)
                        r.upBtn.setTextColor(if (afford) DARK_GREEN else DIM)
                        if (i == best) r.root.setBackgroundResource(R.drawable.bg_seg_selected)
                        r.upBtn.setOnClickListener {
                            val got = s.buyUnit(i, qtyFor(s.maxAffordableUnit(ws, i)))
                            if (got > 0) {
                                it.pop(); Sound.sfx(act, R.raw.sfx_hire); onChange()
                                if (got > 1) Toast.makeText(act, act.getString(R.string.bought_fmt, got), Toast.LENGTH_SHORT).show()
                                rebuild()
                            } else { Sound.sfx(act, R.raw.sfx_error); Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show() }
                        }
                        v.upgradeList.addView(r.root)
                    }
                }

                1 -> {
                    val best = bestBuyIndex(ws)
                    for (i in Defs.BUILDINGS.indices) {
                        val d = Defs.BUILDINGS[i]
                        val lvl = ws.buildingLevels[i]
                        val extra = if (i == 2 && lvl > 0)
                            " · +" + Format.short(s.labGemsPerSec(ws) * 3600) + " 💎/h" else ""
                        val sub = act.getString(R.string.level_fmt, lvl) + " · " + act.getString(d.descRes) + extra +
                            roiLine(s.buildingCost(ws, i), s.buildingProdDelta(ws, i))
                        val name = if (i == best) act.getString(d.nameRes) + "  ⭐" else act.getString(d.nameRes)
                        val r = newRow(d.emoji, name, sub, d.artRes)
                        val maxN = s.maxAffordableBuilding(ws, i)
                        val n = qtyFor(maxN)
                        val cost = if (n <= 1) s.buildingCost(ws, i) else s.bulkBuildingCost(ws, i, n)
                        r.upBtn.text = if (qtyMode == 0) Format.short(cost) else "×$n  " + Format.short(cost)
                        r.upBtn.setIconResource(R.drawable.ic_coin)
                        val afford = maxN >= 1
                        r.upBtn.backgroundTintList = csl(act, if (afford) R.color.green else R.color.panel_light)
                        r.upBtn.setTextColor(if (afford) DARK_GREEN else DIM)
                        if (i == best) r.root.setBackgroundResource(R.drawable.bg_seg_selected)
                        r.upBtn.setOnClickListener {
                            val got = s.buyBuilding(i, qtyFor(s.maxAffordableBuilding(ws, i)))
                            if (got > 0) {
                                it.pop(); Sound.sfx(act, R.raw.sfx_upgrade); onChange()
                                if (got > 1) Toast.makeText(act, act.getString(R.string.bought_fmt, got), Toast.LENGTH_SHORT).show()
                                rebuild()
                            } else { Sound.sfx(act, R.raw.sfx_error); Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show() }
                        }
                        v.upgradeList.addView(r.root)
                    }
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
                            it.pop(); onChange(); rebuild()
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
        if (s.dailyStreak > 0) {
            v.dailySub.text = act.getString(R.string.daily_streak_fmt, s.dailyStreak)
        }
        val cols = 4
        v.dailyGrid.removeAllViews()
        v.dailyGrid.columnCount = cols

        var claimedCell: View? = null
        for (i in Defs.DAILY.indices) {
            val reward = Defs.DAILY[i]
            val cell = ItemDailyBinding.inflate(act.layoutInflater, v.dailyGrid, false)
            cell.dayLabel.text = act.getString(R.string.daily_day_fmt, i + 1)
            cell.dayIcon.text = if (reward.isGems) "💎" else "💰"
            cell.dayAmount.text = Format.short(reward.amount)

            when {
                i < idx -> cell.dayCell.alpha = 0.4f
                i == idx -> {
                    cell.dayCell.setBackgroundResource(R.drawable.bg_seg_selected)
                    cell.dayLabel.setTextColor(DARK_GOLD)
                    cell.dayAmount.setTextColor(DARK_GOLD)
                    claimedCell = cell.dayCell
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

        v.btnClaim.setOnClickListener { btn ->
            val r = s.claimDaily(System.currentTimeMillis())
            if (r != null) {
                Sound.sfx(act, R.raw.sfx_reward)
                Game.save(act)
                onChange()
                btn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val unit = if (r.isGems) " 💎" else " 💰"
                Toast.makeText(act, "+" + Format.short(r.amount) + unit, Toast.LENGTH_SHORT).show()
                val cell = claimedCell
                if (cell != null) {
                    cell.scaleX = 0.7f
                    cell.scaleY = 0.7f
                    cell.animate().scaleX(1.15f).scaleY(1.15f).setDuration(220)
                        .setInterpolator(OvershootInterpolator(4f))
                        .withEndAction {
                            cell.animate().scaleX(1f).scaleY(1f).setDuration(120)
                                .withEndAction { dialog.dismiss() }.start()
                        }.start()
                } else dialog.dismiss()
            } else dialog.dismiss()
        }
        dialog.show()
    }

    // -------------------------------------------------------------- leaderboard

    fun showLeaderboard(act: Activity) {
        val s = Game.get(act)
        var power = s.coins
        for ((id, w) in s.worlds) {
            if (w.unlocked) power += (s.baseProdPerSec(id) * 600).toLong()
        }
        val you = act.getString(R.string.you)
        val rows = listOf(
            "AntKing 🐜" to power * 3 + 4200,
            "MikroMaja" to power * 2 + 1500,
            "Cellulord" to (power * 1.4).toLong() + 800,
            "NanoNeo" to (power * 0.7).toLong() + 120,
            "BugBoss" to (power * 0.4).toLong(),
            you to power
        ).sortedByDescending { it.second }

        val pad = dp(act, 18)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(act).apply {
            text = act.getString(R.string.leaderboard_title)
            setTextColor(WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        rows.forEachIndexed { i, (name, score) ->
            val mine = name == you
            root.addView(TextView(act).apply {
                text = "${i + 1}.  $name      ${Format.short(score)}"
                setTextColor(if (mine) ContextCompat.getColor(act, R.color.gold) else WHITE)
                textSize = 16f
                setPadding(0, dp(act, 9), 0, 0)
                if (mine) setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        }
        val dialog = AlertDialog.Builder(act).setView(root)
            .setPositiveButton(R.string.close, null).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // ------------------------------------------------------------------ quests

    fun showQuests(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        s.rolloverDaily(System.currentTimeMillis())
        val pad = dp(act, 18)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(act).apply {
            text = act.getString(R.string.quests_title)
            setTextColor(WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val listView = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listView)

        val dialog = AlertDialog.Builder(act).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun rebuild() {
            listView.removeAllViews()
            for (i in Defs.QUESTS.indices) {
                val q = Defs.QUESTS[i]
                val r = ItemUpgradeBinding.inflate(act.layoutInflater, listView, false)
                r.upEmoji.text = q.emoji
                r.upName.text = act.getString(q.nameRes)
                r.upSub.text = act.getString(
                    R.string.quest_progress_fmt,
                    Format.short(s.questProgress(i).coerceAtMost(q.target)),
                    Format.short(q.target)
                )
                r.upBtn.text = "+" + q.rewardGems
                r.upBtn.setIconResource(R.drawable.ic_gem)
                when {
                    s.qClaimed[i] -> {
                        r.upBtn.text = act.getString(R.string.daily_done)
                        r.upBtn.icon = null
                        r.upBtn.isEnabled = false
                        r.upBtn.backgroundTintList = csl(act, R.color.panel_light)
                        r.upBtn.setTextColor(DIM)
                    }
                    s.questClaimable(i) -> {
                        r.upBtn.isEnabled = true
                        r.upBtn.backgroundTintList = csl(act, R.color.gold)
                        r.upBtn.setTextColor(DARK_GOLD)
                        r.upBtn.setOnClickListener {
                            val g = s.claimQuest(i)
                            if (g > 0) {
                                it.pop()
                                Sound.sfx(act, R.raw.sfx_reward)
                                Game.save(act)
                                onChange()
                                Toast.makeText(act, "+$g 💎", Toast.LENGTH_SHORT).show()
                                rebuild()
                            }
                        }
                    }
                    else -> {
                        r.upBtn.isEnabled = false
                        r.upBtn.backgroundTintList = csl(act, R.color.gold)
                        r.upBtn.setTextColor(DARK_GOLD)
                    }
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(act, 12)
                r.root.layoutParams = lp
                listView.addView(r.root)
            }
        }
        rebuild()

        root.addView(android.widget.Button(act).apply {
            text = act.getString(R.string.close)
            setBackgroundColor(0x00000000)
            setTextColor(DIM)
            setPadding(0, dp(act, 6), 0, 0)
            setOnClickListener { dialog.dismiss() }
        })
        dialog.show()
    }

    // ---------------------------------------------------------------- research

    fun showResearch(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        val pad = dp(act, 18)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
        }
        val header = TextView(act).apply {
            setTextColor(WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(header)
        val sub = TextView(act).apply {
            setTextColor(ContextCompat.getColor(act, R.color.purple))
            textSize = 14f
            setPadding(0, dp(act, 4), 0, 0)
        }
        root.addView(sub)
        val listView = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listView)
        val dialog = AlertDialog.Builder(act).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun rebuild() {
            header.text = act.getString(R.string.research_title)
            sub.text = act.getString(R.string.research_cores_fmt, Format.short(s.cores))
            listView.removeAllViews()
            for (i in Defs.RESEARCH.indices) {
                val d = Defs.RESEARCH[i]
                val lvl = s.research[i]
                val r = ItemUpgradeBinding.inflate(act.layoutInflater, listView, false)
                r.upEmoji.text = d.emoji
                r.upName.text = act.getString(d.nameRes)
                val bonusPct = (d.branchMult * lvl * 100).toInt()
                r.upSub.text = act.getString(R.string.level_fmt, lvl) + " · " +
                    act.getString(d.descRes) + " (+" + bonusPct + "%)"
                r.upBtn.setIconResource(R.drawable.ic_core)
                r.upBtn.backgroundTintList = csl(act, R.color.purple)
                r.upBtn.setTextColor(WHITE)
                if (s.researchMaxed(i)) {
                    r.upBtn.text = act.getString(R.string.daily_done)
                    r.upBtn.icon = null
                    r.upBtn.isEnabled = false
                    r.upBtn.backgroundTintList = csl(act, R.color.panel_light)
                    r.upBtn.setTextColor(DIM)
                } else {
                    r.upBtn.text = Format.short(s.researchCost(i))
                    r.upBtn.setOnClickListener {
                        if (s.buyResearch(i)) {
                            it.pop(); Game.save(act); onChange(); rebuild()
                        } else Toast.makeText(act, R.string.not_enough_cores, Toast.LENGTH_SHORT).show()
                    }
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(act, 12)
                r.root.layoutParams = lp
                listView.addView(r.root)
            }
        }
        rebuild()
        root.addView(android.widget.Button(act).apply {
            text = act.getString(R.string.close)
            setBackgroundColor(0x00000000)
            setTextColor(DIM)
            setPadding(0, dp(act, 6), 0, 0)
            setOnClickListener { dialog.dismiss() }
        })
        dialog.show()
    }

    // -------------------------------------------------------------- collection

    fun showCollection(act: Activity, onChange: () -> Unit) {
        val s = Game.get(act)
        val pad = dp(act, 18)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(act).apply {
            text = act.getString(R.string.collection_title)
            setTextColor(WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val listView = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listView)
        val dialog = AlertDialog.Builder(act).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun header(text: String) {
            listView.addView(TextView(act).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(act, R.color.gold))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(act, 14), 0, dp(act, 2))
            })
        }
        fun row(emoji: String, name: String, sub: String, done: Boolean): ItemUpgradeBinding {
            val r = ItemUpgradeBinding.inflate(act.layoutInflater, listView, false)
            r.upEmoji.text = emoji
            r.upName.text = name
            r.upSub.text = sub
            r.upBtn.icon = null
            r.upBtn.isEnabled = false
            r.upBtn.text = if (done) act.getString(R.string.collection_owned) else act.getString(R.string.collection_locked)
            r.upBtn.backgroundTintList = csl(act, if (done) R.color.green else R.color.panel_light)
            r.upBtn.setTextColor(if (done) DARK_GREEN else DIM)
            if (!done) r.root.alpha = 0.55f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(act, 10)
            r.root.layoutParams = lp
            return r
        }
        fun render() {
            listView.removeAllViews()
            header(act.getString(R.string.collection_badges_fmt, s.masteredCount(), Defs.WORLDS.size))
            for (def in Defs.WORLDS) {
                val ws = s.world(def.id)
                val mastered = s.worldMastered(def.id)
                val sub = if (mastered) act.getString(R.string.collection_mastered)
                    else act.getString(R.string.progress_fmt, ws.territories, Defs.TERRITORIES)
                listView.addView(row(def.emoji, act.getString(def.nameRes), sub, mastered).root)
            }
            header(act.getString(R.string.collection_skins_fmt, s.skinsOwned(), s.skinsTotal()))
            val skinRow = row("✨", act.getString(R.string.shop_skin), act.getString(R.string.shop_skin_desc), s.skinGold)
            if (!s.skinGold) {
                skinRow.root.alpha = 1f
                skinRow.upBtn.isEnabled = true
                skinRow.upBtn.text = act.getString(R.string.get)
                skinRow.upBtn.backgroundTintList = csl(act, R.color.gold)
                skinRow.upBtn.setTextColor(DARK_GOLD)
                skinRow.upBtn.setOnClickListener { it.pop(); dialog.dismiss(); showShop(act, onChange) }
            }
            listView.addView(skinRow.root)
            header(act.getString(R.string.collection_set_title))
            val setRow = row("🏆", act.getString(R.string.collection_set_name), act.getString(R.string.collection_set_desc), s.collectionBonus)
            if (!s.collectionBonus && s.collectionClaimable()) {
                setRow.root.alpha = 1f
                setRow.upBtn.isEnabled = true
                setRow.upBtn.text = act.getString(R.string.collection_claim)
                setRow.upBtn.backgroundTintList = csl(act, R.color.gold)
                setRow.upBtn.setTextColor(DARK_GOLD)
                setRow.upBtn.setOnClickListener {
                    if (s.claimCollection()) {
                        it.pop(); Sound.sfx(act, R.raw.sfx_mega); Game.save(act); onChange()
                        Toast.makeText(act, act.getString(R.string.collection_claimed), Toast.LENGTH_SHORT).show()
                        render()
                    }
                }
            }
            listView.addView(setRow.root)
        }
        render()
        root.addView(android.widget.Button(act).apply {
            text = act.getString(R.string.close)
            setBackgroundColor(0x00000000)
            setTextColor(DIM)
            setPadding(0, dp(act, 8), 0, 0)
            setOnClickListener { dialog.dismiss() }
        })
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
            r.upBtn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                action()
                onChange()
            }
            root.addView(r.root)
        }

        fun section(title: String) {
            root.addView(TextView(act).apply {
                text = title
                setTextColor(csl(act, R.color.gold))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(act, 16), 0, dp(act, 2))
            })
        }

        // ---- limited one-time offer (only until claimed) ----
        if (!s.starterClaimed) {
            section(act.getString(R.string.shop_section_offers))
            addRow("🎒", act.getString(R.string.shop_starter), act.getString(R.string.shop_starter_desc),
                act.getString(R.string.get), true) {
                if (s.claimStarter(System.currentTimeMillis())) {
                    Sound.sfx(act, R.raw.sfx_mega); Game.save(act)
                    Toast.makeText(act, act.getString(R.string.shop_starter) + " ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ---- rewarded ads (opt-in, value exchange — the only ad model) ----
        section(act.getString(R.string.shop_section_gems))
        addRow("🎬", act.getString(R.string.shop_free_coins), "+5 perc termelés", act.getString(R.string.watch), false) {
            val grant = max(50L, floor(s.baseProdPerSec(s.activeWorld) * 300.0).toLong())
            s.coins += grant
            Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
            Toast.makeText(act, "+" + Format.short(grant) + " 💰", Toast.LENGTH_SHORT).show()
        }
        addRow("🎬", act.getString(R.string.shop_free_gems), act.getString(R.string.shop_free_gems_desc),
            act.getString(R.string.watch), false) {
            s.addGems(15)
            Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
            Toast.makeText(act, "+15 💎", Toast.LENGTH_SHORT).show()
        }

        // ---- gem packs (consumable IAP ladder) ----
        addRow("💎", act.getString(R.string.shop_gems_small), "+25 💎", act.getString(R.string.get), true) {
            s.addGems(25); Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
            Toast.makeText(act, "+25 💎", Toast.LENGTH_SHORT).show()
        }
        addRow("🎁", act.getString(R.string.shop_gems_big), "+120 💎", act.getString(R.string.get), true) {
            s.addGems(120); Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
            Toast.makeText(act, "+120 💎", Toast.LENGTH_SHORT).show()
        }
        addRow("💰", act.getString(R.string.shop_gems_mega), "+400 💎", act.getString(R.string.get), true) {
            s.addGems(400); Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
            Toast.makeText(act, "+400 💎", Toast.LENGTH_SHORT).show()
        }

        // ---- premium (subscription + season pass + cosmetic) ----
        section(act.getString(R.string.shop_section_premium))
        addRow("👑", act.getString(R.string.shop_vip), act.getString(R.string.shop_vip_desc),
            if (s.vip) act.getString(R.string.shop_active) else act.getString(R.string.get), true) {
            if (s.buyVip()) {
                Sound.sfx(act, R.raw.sfx_mega); Game.save(act)
                Toast.makeText(act, act.getString(R.string.shop_vip) + " ✓", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(act, act.getString(R.string.shop_active), Toast.LENGTH_SHORT).show()
        }
        addRow("🎖️", act.getString(R.string.shop_battlepass), act.getString(R.string.shop_battlepass_desc),
            if (s.seasonPass) act.getString(R.string.shop_active) else act.getString(R.string.get), true) {
            if (s.buySeasonPass()) {
                Sound.sfx(act, R.raw.sfx_mega); Game.save(act)
                Toast.makeText(act, act.getString(R.string.shop_battlepass) + " ✓", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(act, act.getString(R.string.shop_active), Toast.LENGTH_SHORT).show()
        }
        addRow("✨", act.getString(R.string.shop_skin), act.getString(R.string.shop_skin_desc),
            if (s.skinGold) act.getString(R.string.shop_active) else "40", true) {
            if (!s.skinGold && s.gems >= 40) {
                s.gems -= 40
                s.skinGold = true
                Sound.sfx(act, R.raw.sfx_reward); Game.save(act)
                Toast.makeText(act, act.getString(R.string.shop_skin) + " ✓", Toast.LENGTH_SHORT).show()
            } else if (s.skinGold) {
                Toast.makeText(act, act.getString(R.string.shop_active), Toast.LENGTH_SHORT).show()
            } else {
                Sound.sfx(act, R.raw.sfx_error)
                Toast.makeText(act, act.getString(R.string.not_enough_gems), Toast.LENGTH_SHORT).show()
            }
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

    fun showMap(act: Activity, onChange: () -> Unit, onMilestone: (String) -> Unit = {}) {
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
            v.mapEssence.text = def.essenceEmoji + " " + act.getString(def.essenceName) + ": " + Format.short(ws.essence)
            v.btnRefine.text = act.getString(R.string.refine_fmt, Format.short(s.refineCost(ws))) + " " + def.essenceEmoji
            v.btnRefine.isEnabled = ws.essence >= s.refineCost(ws)
            val gain = s.prestigeStarsAvailable(s.activeWorld)
            v.btnPrestige.text = act.getString(R.string.prestige_fmt, ws.masteryStars, gain)
            v.btnPrestige.isEnabled = gain >= 1
            if (ws.territories >= Defs.TERRITORIES) {
                v.mapHint.text = if (s.canPrestige(s.activeWorld))
                    act.getString(R.string.map_hint_prestige, s.prestigeStarsAvailable(s.activeWorld))
                else act.getString(R.string.all_conquered)
                v.btnConquer.isEnabled = false
                v.btnConquer.icon = null
                v.btnConquer.text = act.getString(R.string.all_conquered)
            } else {
                val tc = s.territoryCost(s.activeWorld)
                v.mapHint.text = if (tc in 1..s.coins)
                    act.getString(R.string.map_hint_conquer_ready)
                else act.getString(R.string.territory_hint)
                v.btnConquer.isEnabled = true
                v.btnConquer.setIconResource(R.drawable.ic_coin)
                v.btnConquer.text = act.getString(R.string.conquer) + " · " + Format.short(tc)
            }
        }

        v.btnConquer.setOnClickListener {
            val res = s.conquer()
            if (res.ok) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                Sound.sfx(act, if (res.clearedWorld) R.raw.sfx_mega else R.raw.sfx_conquer)
                Game.save(act)
                onChange()
                if (res.clearedWorld) {
                    Toast.makeText(act, act.getString(R.string.world_cleared_reward, res.gemReward), Toast.LENGTH_LONG).show()
                    if (s.markMilestone(GameState.M_FIRST_CLEAR)) {
                        Game.save(act)
                        Toast.makeText(act, act.getString(R.string.milestone_first_clear), Toast.LENGTH_LONG).show()
                    }
                    onMilestone("cleared")
                }
                refresh()
            } else {
                Toast.makeText(act, R.string.not_enough, Toast.LENGTH_SHORT).show()
            }
        }
        v.btnRefine.setOnClickListener {
            if (s.refine()) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                Sound.sfx(act, R.raw.sfx_upgrade)
                Game.save(act)
                onChange()
                refresh()
            } else {
                Toast.makeText(act, R.string.not_enough_essence, Toast.LENGTH_SHORT).show()
            }
        }
        v.btnPrestige.setOnClickListener {
            val g = s.prestigeStarsAvailable(s.activeWorld)
            if (g < 1) {
                Toast.makeText(act, R.string.prestige_locked, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val after = s.active().masteryStars + g
            val mult = String.format(java.util.Locale.US, "  (×%.2f)", 1.0 + GameState.PRESTIGE_MULT_PER_STAR * after)
            val msg = act.getString(R.string.prestige_confirm, act.getString(def.nameRes), g) + "\n\n" +
                act.getString(R.string.prestige_after, after) + mult
            AlertDialog.Builder(act)
                .setTitle(R.string.prestige_title)
                .setMessage(msg)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.prestige_do) { _, _ ->
                    val got = s.prestige(System.currentTimeMillis())
                    if (got > 0) {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        Sound.sfx(act, R.raw.sfx_mega)
                        Game.save(act)
                        onChange()
                        Toast.makeText(act, "+$got ⭐", Toast.LENGTH_LONG).show()
                        if (s.markMilestone(GameState.M_FIRST_PRESTIGE)) {
                            Game.save(act)
                            Toast.makeText(act, act.getString(R.string.milestone_first_prestige), Toast.LENGTH_LONG).show()
                        }
                        onMilestone("prestige")
                        refresh()
                    }
                }
                .show()
        }
        v.btnMapClose.setOnClickListener { dialog.dismiss() }
        refresh()
        dialog.show()
    }

    // ----------------------------------------------------------------- offline

    fun showOffline(act: Activity, r: GameState.OfflineResult, onChange: () -> Unit) {
        val s = Game.get(act)
        val pad = dp(act, 20)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(act).apply {
            text = act.getString(R.string.offline_title)
            setTextColor(ContextCompat.getColor(act, R.color.gold))
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val mins = (r.awayMs / 60_000L).coerceIn(0L, 480L)
        val awayStr = if (mins >= 60) "${mins / 60} ó ${mins % 60} p" else "$mins p"
        root.addView(TextView(act).apply {
            text = act.getString(R.string.offline_away_fmt, awayStr)
            setTextColor(DIM)
            textSize = 14f
            setPadding(0, dp(act, 6), 0, dp(act, 12))
        })
        root.addView(TextView(act).apply {
            text = "+" + Format.short(r.coins) + " 💰" +
                (if (r.gems > 0) "    +" + Format.short(r.gems) + " 💎" else "")
            setTextColor(WHITE)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val dialog = AlertDialog.Builder(act).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(act, 18), 0, 0)
        }
        val doubleBtn = mbtn(act, act.getString(R.string.offline_double), R.color.gold, DARK_GOLD).apply {
            setOnClickListener {
                it.pop()
                s.coins += r.coins
                Game.save(act)
                onChange()
                Toast.makeText(act, "+" + Format.short(r.coins) + " 💰", Toast.LENGTH_SHORT).show()
                isEnabled = false
            }
        }
        // Disable the ×2 bonus when there was nothing to earn offline.
        if (r.coins <= 0L) doubleBtn.isEnabled = false
        row.addView(doubleBtn, LinearLayout.LayoutParams(0, dp(act, 50), 1f).apply {
            marginEnd = dp(act, 8)
        })
        row.addView(mbtn(act, act.getString(R.string.offline_ok), R.color.green, DARK_GREEN).apply {
            setOnClickListener { it.pop(); dialog.dismiss() }
        }, LinearLayout.LayoutParams(0, dp(act, 50), 1f))
        root.addView(row)

        dialog.show()
    }

    // ---------------------------------------------------------------- settings

    /** One-time first-run welcome explaining the core loop. */
    fun showWelcome(act: Activity) {
        AlertDialog.Builder(act)
            .setTitle(R.string.welcome_title)
            .setMessage(R.string.welcome_body)
            .setCancelable(true)
            .setPositiveButton(R.string.welcome_ok, null)
            .show()
    }

    fun showSettings(act: Activity, onChange: () -> Unit) {
        val soundState = booleanArrayOf(Sound.enabled)
        AlertDialog.Builder(act)
            .setTitle(R.string.settings)
            .setMultiChoiceItems(arrayOf(act.getString(R.string.sound_toggle)), soundState) { _, _, checked ->
                Sound.setEnabled(act, checked)
            }
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.share) { _, _ ->
                val s = Game.get(act)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, act.getString(R.string.share_text, Format.short(s.coins)))
                }
                act.startActivity(Intent.createChooser(send, act.getString(R.string.share)))
            }
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
