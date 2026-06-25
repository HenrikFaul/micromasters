package com.micromasters.game

import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Per-world progress. */
class WorldState(
    var unlocked: Boolean,
    var territories: Int = 0,
    var clearRewardClaimed: Boolean = false,
    val unitLevels: IntArray = IntArray(Defs.UNITS.size),
    val buildingLevels: IntArray = IntArray(Defs.BUILDINGS.size),
    var pending: Double = 0.0,
    var gemPending: Double = 0.0,
    var lastTick: Long = 0L
)

/** Full save-game and the simulation rules that operate on it. */
class GameState {
    var coins: Long = 80
    var gems: Long = 25
    var activeWorld: String = "kitchen"
    val worlds = LinkedHashMap<String, WorldState>()
    var dailyStreak: Int = 0
    var lastClaimDay: Long = -1L
    var boostMult: Double = 1.0
    var boostExpiry: Long = 0L
    var lastSeen: Long = 0L
    var created: Long = 0L
    var skinGold: Boolean = false

    fun world(id: String): WorldState = worlds.getValue(id)
    fun active(): WorldState = world(activeWorld)

    // ---- derived values -------------------------------------------------

    private fun unitsBaseProd(ws: WorldState): Double {
        var sum = 0.0
        for (i in Defs.UNITS.indices) sum += Defs.UNITS[i].baseProd * ws.unitLevels[i]
        return sum
    }

    fun warehouseMult(ws: WorldState): Double = 1.0 + 0.75 * ws.buildingLevels[0]
    fun workshopMult(ws: WorldState): Double = 1.0 + 0.12 * ws.buildingLevels[1]
    fun territoryBonus(ws: WorldState): Double = 1.0 + 0.05 * ws.territories

    /** Lab building turns into a slow gem faucet (gems per second). */
    fun labGemsPerSec(ws: WorldState): Double = ws.buildingLevels[2] * 0.5 / 3600.0

    fun capacity(id: String): Double {
        val def = Defs.world(id)
        return def.baseCap * warehouseMult(world(id))
    }

    /** Production per second of a world, ignoring any active boost. */
    fun baseProdPerSec(id: String): Double {
        val def = Defs.world(id)
        val ws = world(id)
        return unitsBaseProd(ws) * def.prodMult * workshopMult(ws) * territoryBonus(ws)
    }

    fun boostActive(now: Long): Boolean = now < boostExpiry && boostMult > 1.0

    fun effectiveProdPerSec(id: String, now: Long): Double {
        val base = baseProdPerSec(id)
        return if (boostActive(now)) base * boostMult else base
    }

    // Costs are capped so the exponential never overflows Double->Long (frozen Long.MAX).
    private fun growthCost(base: Double, growth: Double, level: Int): Long {
        val raw = base * growth.pow(level.coerceAtMost(200))
        return if (!raw.isFinite() || raw >= 9.0e18) Long.MAX_VALUE else floor(raw).toLong()
    }

    fun unitCost(ws: WorldState, i: Int): Long {
        val d = Defs.UNITS[i]
        return growthCost(d.baseCost, d.growth, ws.unitLevels[i])
    }

    fun buildingCost(ws: WorldState, i: Int): Long {
        val d = Defs.BUILDINGS[i]
        return growthCost(d.baseCost, d.growth, ws.buildingLevels[i])
    }

    fun territoryCost(id: String): Long {
        val def = Defs.world(id)
        val ws = world(id)
        // Gentle hyper-casual ramp; last tile still dominates (1.4^9 ~= 20x base).
        return growthCost(def.territoryBaseCost, 1.4, ws.territories)
    }

    // ---- simulation -----------------------------------------------------

    private fun advance(ws: WorldState, id: String, now: Long, dtCapSec: Double): Double {
        if (ws.lastTick == 0L) ws.lastTick = now
        var dt = (now - ws.lastTick) / 1000.0
        ws.lastTick = now
        if (dt <= 0.0) return 0.0
        dt = min(dt, dtCapSec)
        val cap = capacity(id)
        val before = ws.pending
        ws.pending = min(cap, ws.pending + effectiveProdPerSec(id, now) * dt)
        ws.gemPending += labGemsPerSec(ws) * dt
        if (!ws.pending.isFinite()) ws.pending = before
        if (!ws.gemPending.isFinite()) ws.gemPending = 0.0
        return ws.pending - before
    }

    /** Live tick: advances every unlocked world's pending (capped). Returns active gain. */
    fun tick(now: Long): Double {
        var activeGain = 0.0
        for ((id, w) in worlds) {
            if (!w.unlocked) continue
            val gain = advance(w, id, now, 8.0 * 3600.0)
            if (id == activeWorld) activeGain = gain
        }
        return activeGain
    }

    /**
     * Resume-only offline bonus for the active world, decoupled from the storage cap so the
     * 8-hour idle window is actually meaningful. Credits coins directly and returns the amount.
     */
    fun accrueOffline(now: Long, awayMs: Long): Double {
        val ws = active()
        // Keep lastTick coherent so the live ticker doesn't double-count.
        for (w in worlds.values) w.lastTick = now
        val awaySec = min(awayMs / 1000.0, 8.0 * 3600.0)
        if (awaySec <= 0.0) return 0.0
        val gained = effectiveProdPerSec(activeWorld, now) * awaySec * 0.5
        val gemGain = labGemsPerSec(ws) * awaySec
        if (gained.isFinite() && gained >= 1.0) coins += floor(gained).toLong()
        if (gemGain.isFinite() && gemGain >= 1.0) gems += floor(gemGain).toLong()
        return if (gained.isFinite()) gained else 0.0
    }

    fun collect(): Long {
        val ws = active()
        val amount = floor(ws.pending).toLong()
        if (amount > 0) {
            coins += amount
            ws.pending -= amount
        }
        // Lab gems are harvested together with coins.
        val g = floor(ws.gemPending).toLong()
        if (g > 0) {
            gems += g
            ws.gemPending -= g
        }
        return amount
    }

    fun upgradeUnit(i: Int): Boolean {
        val ws = active()
        val cost = unitCost(ws, i)
        if (coins < cost) return false
        coins -= cost
        ws.unitLevels[i] += 1
        return true
    }

    fun upgradeBuilding(i: Int): Boolean {
        val ws = active()
        val cost = buildingCost(ws, i)
        if (coins < cost) return false
        coins -= cost
        ws.buildingLevels[i] += 1
        return true
    }

    class ConquerResult(val ok: Boolean, val clearedWorld: Boolean, val gemReward: Int)

    fun conquer(): ConquerResult {
        val def = Defs.world(activeWorld)
        val ws = active()
        if (ws.territories >= Defs.TERRITORIES) return ConquerResult(false, false, 0)
        val cost = territoryCost(activeWorld)
        if (coins < cost) return ConquerResult(false, false, 0)
        coins -= cost
        ws.territories += 1
        var cleared = false
        var reward = 0
        if (ws.territories >= Defs.TERRITORIES && !ws.clearRewardClaimed) {
            ws.clearRewardClaimed = true
            cleared = true
            reward = def.clearReward
            gems += reward
        }
        return ConquerResult(true, cleared, reward)
    }

    fun unlockWorld(id: String): Boolean {
        val def = Defs.world(id)
        val ws = world(id)
        if (ws.unlocked) return true
        if (gems < def.unlockGems) return false
        gems -= def.unlockGems
        ws.unlocked = true
        ws.lastTick = lastSeen
        return true
    }

    fun applyBoost(def: BoostDef, now: Long): Boolean {
        when (def.kind) {
            BoostKind.FILL -> {
                if (gems < def.gemCost) return false
                gems -= def.gemCost
                val ws = active()
                ws.pending = capacity(activeWorld)
            }
            BoostKind.MULTIPLIER -> {
                if (gems < def.gemCost) return false
                gems -= def.gemCost
                boostMult = def.multiplier
                boostExpiry = max(boostExpiry, now) + def.durationMs
            }
            BoostKind.AD -> {
                // Simulated rewarded ad: grant a chunk of production instantly.
                val ws = active()
                val grant = baseProdPerSec(activeWorld) * (def.durationMs / 1000.0)
                ws.pending = min(capacity(activeWorld), ws.pending + grant + 1.0)
            }
        }
        return true
    }

    // ---- daily reward ---------------------------------------------------

    private fun epochDay(now: Long): Long {
        val tz = TimeZone.getDefault()
        return (now + tz.getOffset(now)) / 86_400_000L
    }

    fun dailyAvailable(now: Long): Boolean = epochDay(now) != lastClaimDay

    /** Index (0..6) of the reward that the *next* claim will grant. */
    fun dailyIndex(now: Long): Int {
        val today = epochDay(now)
        // One missed day is forgiven before the streak resets.
        val streak = if (lastClaimDay >= today - 2L || lastClaimDay == -1L) dailyStreak else 0
        return streak.mod(Defs.DAILY.size)
    }

    /** Claims today's reward. Returns the granted reward, or null if already claimed. */
    fun claimDaily(now: Long): Defs.DailyReward? {
        val today = epochDay(now)
        if (today == lastClaimDay) return null
        val continues = lastClaimDay >= today - 2L && lastClaimDay != -1L
        if (!continues) dailyStreak = 0
        val idx = dailyStreak.mod(Defs.DAILY.size)
        val reward = Defs.DAILY[idx]
        if (reward.isGems) gems += reward.amount else coins += reward.amount
        dailyStreak += 1
        lastClaimDay = today
        return reward
    }

    // ---- persistence ----------------------------------------------------

    private fun safe(d: Double): Double = if (d.isFinite()) d else 0.0

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("schema", SCHEMA)
        o.put("coins", coins)
        o.put("gems", gems)
        o.put("activeWorld", activeWorld)
        o.put("dailyStreak", dailyStreak)
        o.put("lastClaimDay", lastClaimDay)
        o.put("boostMult", safe(boostMult))
        o.put("boostExpiry", boostExpiry)
        o.put("lastSeen", lastSeen)
        o.put("created", created)
        o.put("skinGold", skinGold)
        val ws = JSONObject()
        for ((id, w) in worlds) {
            val wo = JSONObject()
            wo.put("unlocked", w.unlocked)
            wo.put("territories", w.territories)
            wo.put("clearRewardClaimed", w.clearRewardClaimed)
            wo.put("pending", safe(w.pending))
            wo.put("gemPending", safe(w.gemPending))
            wo.put("lastTick", w.lastTick)
            wo.put("units", JSONArray(w.unitLevels.toList()))
            wo.put("buildings", JSONArray(w.buildingLevels.toList()))
            ws.put(id, wo)
        }
        o.put("worlds", ws)
        return o
    }

    companion object {
        const val SCHEMA = 2

        fun newGame(now: Long): GameState {
            val s = GameState()
            s.created = now
            s.lastSeen = now
            for (def in Defs.WORLDS) {
                val ws = WorldState(unlocked = def.unlockedByDefault)
                ws.lastTick = now
                s.worlds[def.id] = ws
            }
            // Punchy start: the kitchen already has a few miners and one cleared tile.
            s.world("kitchen").unitLevels[0] = 3
            s.world("kitchen").territories = 1
            return s
        }

        fun fromJson(o: JSONObject, now: Long): GameState {
            // Reject a save written by a NEWER build than this one; caller falls back to newGame.
            if (o.optInt("schema", 1) > SCHEMA) {
                throw IllegalStateException("save schema newer than app")
            }
            val s = GameState()
            s.coins = o.optLong("coins", 80L).coerceAtLeast(0L)
            s.gems = o.optLong("gems", 25L).coerceAtLeast(0L)
            s.activeWorld = o.optString("activeWorld", "kitchen")
            s.dailyStreak = o.optInt("dailyStreak", 0).coerceAtLeast(0)
            s.lastClaimDay = o.optLong("lastClaimDay", -1L)
            s.boostMult = o.optDouble("boostMult", 1.0).let { if (it.isFinite()) it else 1.0 }
            s.boostExpiry = o.optLong("boostExpiry", 0L)
            s.lastSeen = o.optLong("lastSeen", now)
            s.created = o.optLong("created", now)
            s.skinGold = o.optBoolean("skinGold", false)
            val ws = o.optJSONObject("worlds")
            for (def in Defs.WORLDS) {
                val w = WorldState(unlocked = def.unlockedByDefault)
                w.lastTick = now
                val wo = ws?.optJSONObject(def.id)
                if (wo != null) {
                    w.unlocked = wo.optBoolean("unlocked", def.unlockedByDefault)
                    w.territories = wo.optInt("territories", 0).coerceIn(0, Defs.TERRITORIES)
                    w.clearRewardClaimed = wo.optBoolean("clearRewardClaimed", false)
                    w.pending = wo.optDouble("pending", 0.0).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    w.gemPending = wo.optDouble("gemPending", 0.0).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    w.lastTick = wo.optLong("lastTick", now)
                    readInts(wo.optJSONArray("units"), w.unitLevels)
                    readInts(wo.optJSONArray("buildings"), w.buildingLevels)
                }
                // Worlds that ship unlocked can never load as locked (avoids soft-lock).
                if (def.unlockedByDefault) w.unlocked = true
                s.worlds[def.id] = w
            }
            // Guarantee the active world exists AND is unlocked.
            if (s.worlds[s.activeWorld]?.unlocked != true) {
                s.activeWorld = s.worlds.entries.firstOrNull { it.value.unlocked }?.key
                    ?: Defs.WORLDS.first().id
            }
            return s
        }

        private fun readInts(arr: JSONArray?, into: IntArray) {
            if (arr == null) return
            val n = min(arr.length(), into.size)
            for (i in 0 until n) into[i] = arr.optInt(i, 0).coerceAtLeast(0)
        }
    }
}
