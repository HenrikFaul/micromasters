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
    fun labMult(ws: WorldState): Double = 1.0 + 0.25 * ws.buildingLevels[2]
    fun territoryBonus(ws: WorldState): Double = 1.0 + 0.05 * ws.territories

    fun capacity(id: String): Double {
        val def = Defs.world(id)
        return def.baseCap * warehouseMult(world(id))
    }

    /** Production per second of a world, ignoring any active boost. */
    fun baseProdPerSec(id: String): Double {
        val def = Defs.world(id)
        val ws = world(id)
        return unitsBaseProd(ws) * def.prodMult * workshopMult(ws) * labMult(ws) * territoryBonus(ws)
    }

    fun boostActive(now: Long): Boolean = now < boostExpiry && boostMult > 1.0

    fun effectiveProdPerSec(id: String, now: Long): Double {
        val base = baseProdPerSec(id)
        return if (boostActive(now)) base * boostMult else base
    }

    fun unitCost(ws: WorldState, i: Int): Long {
        val d = Defs.UNITS[i]
        return floor(d.baseCost * d.growth.pow(ws.unitLevels[i])).toLong()
    }

    fun buildingCost(ws: WorldState, i: Int): Long {
        val d = Defs.BUILDINGS[i]
        return floor(d.baseCost * d.growth.pow(ws.buildingLevels[i])).toLong()
    }

    fun territoryCost(id: String): Long {
        val def = Defs.world(id)
        val ws = world(id)
        return floor(def.territoryBaseCost * 1.7.pow(ws.territories)).toLong()
    }

    // ---- simulation -----------------------------------------------------

    /** Advances the active world's pending resources. Returns coins gained. */
    fun tick(now: Long): Double {
        val ws = active()
        if (ws.lastTick == 0L) ws.lastTick = now
        var dt = (now - ws.lastTick) / 1000.0
        ws.lastTick = now
        if (dt <= 0.0) return 0.0
        // Idle cap: offline earnings accrue for at most 8 hours.
        dt = min(dt, 8.0 * 3600.0)
        val rate = effectiveProdPerSec(activeWorld, now)
        val cap = capacity(activeWorld)
        val before = ws.pending
        ws.pending = min(cap, ws.pending + rate * dt)
        return ws.pending - before
    }

    fun collect(): Long {
        val ws = active()
        val amount = floor(ws.pending).toLong()
        if (amount <= 0) return 0
        coins += amount
        ws.pending -= amount
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
        val streak = if (lastClaimDay == today - 1L || lastClaimDay == -1L) dailyStreak else 0
        return streak % Defs.DAILY.size
    }

    /** Claims today's reward. Returns the granted reward, or null if already claimed. */
    fun claimDaily(now: Long): Defs.DailyReward? {
        val today = epochDay(now)
        if (today == lastClaimDay) return null
        val continues = lastClaimDay == today - 1L
        if (!continues) dailyStreak = 0
        val idx = dailyStreak % Defs.DAILY.size
        val reward = Defs.DAILY[idx]
        if (reward.isGems) gems += reward.amount else coins += reward.amount
        dailyStreak += 1
        lastClaimDay = today
        return reward
    }

    // ---- persistence ----------------------------------------------------

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("coins", coins)
        o.put("gems", gems)
        o.put("activeWorld", activeWorld)
        o.put("dailyStreak", dailyStreak)
        o.put("lastClaimDay", lastClaimDay)
        o.put("boostMult", boostMult)
        o.put("boostExpiry", boostExpiry)
        o.put("lastSeen", lastSeen)
        o.put("created", created)
        val ws = JSONObject()
        for ((id, w) in worlds) {
            val wo = JSONObject()
            wo.put("unlocked", w.unlocked)
            wo.put("territories", w.territories)
            wo.put("clearRewardClaimed", w.clearRewardClaimed)
            wo.put("pending", w.pending)
            wo.put("lastTick", w.lastTick)
            wo.put("units", JSONArray(w.unitLevels.toList()))
            wo.put("buildings", JSONArray(w.buildingLevels.toList()))
            ws.put(id, wo)
        }
        o.put("worlds", ws)
        return o
    }

    companion object {
        fun newGame(now: Long): GameState {
            val s = GameState()
            s.created = now
            s.lastSeen = now
            for (def in Defs.WORLDS) {
                val ws = WorldState(unlocked = def.unlockedByDefault)
                ws.lastTick = now
                s.worlds[def.id] = ws
            }
            // Starter state echoes the mockup: kitchen has a miner and one cleared tile.
            s.world("kitchen").unitLevels[0] = 1
            s.world("kitchen").territories = 1
            return s
        }

        fun fromJson(o: JSONObject, now: Long): GameState {
            val s = GameState()
            s.coins = o.optLong("coins", 80)
            s.gems = o.optLong("gems", 25)
            s.activeWorld = o.optString("activeWorld", "kitchen")
            s.dailyStreak = o.optInt("dailyStreak", 0)
            s.lastClaimDay = o.optLong("lastClaimDay", -1L)
            s.boostMult = o.optDouble("boostMult", 1.0)
            s.boostExpiry = o.optLong("boostExpiry", 0L)
            s.lastSeen = o.optLong("lastSeen", now)
            s.created = o.optLong("created", now)
            val ws = o.optJSONObject("worlds")
            for (def in Defs.WORLDS) {
                val w = WorldState(unlocked = def.unlockedByDefault)
                w.lastTick = now
                val wo = ws?.optJSONObject(def.id)
                if (wo != null) {
                    w.unlocked = wo.optBoolean("unlocked", def.unlockedByDefault)
                    w.territories = wo.optInt("territories", 0)
                    w.clearRewardClaimed = wo.optBoolean("clearRewardClaimed", false)
                    w.pending = wo.optDouble("pending", 0.0)
                    w.lastTick = wo.optLong("lastTick", now)
                    readInts(wo.optJSONArray("units"), w.unitLevels)
                    readInts(wo.optJSONArray("buildings"), w.buildingLevels)
                }
                s.worlds[def.id] = w
            }
            if (!s.worlds.containsKey(s.activeWorld) || !s.world(s.activeWorld).unlocked) {
                s.activeWorld = "kitchen"
            }
            return s
        }

        private fun readInts(arr: JSONArray?, into: IntArray) {
            if (arr == null) return
            val n = min(arr.length(), into.size)
            for (i in 0 until n) into[i] = arr.optInt(i, 0)
        }
    }
}
