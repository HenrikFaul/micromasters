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
    var essence: Double = 0.0,
    var essenceRefines: Int = 0,
    var lastTick: Long = 0L,
    var lifetimeCoins: Double = 0.0,
    var masteryStars: Int = 0,
    var everCleared: Boolean = false
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

    // Daily quests (counters reset per local day via rolloverDaily).
    var questDay: Long = -1L
    var qCollected: Long = 0L
    var qUpgrades: Int = 0
    var qConquered: Int = 0
    val qClaimed = BooleanArray(3)

    // Meta progression (account-wide).
    var cores: Long = 0
    val research = IntArray(Defs.RESEARCH.size)
    var collectionBonus: Boolean = false
    var milestonesShown: Long = 0L   // bitmask of one-shot milestone toasts already shown
    private val COLLECTION_BONUS = 0.05

    // Active-play: tap-combo + crit. TRANSIENT — never serialized, so saves stay byte-identical.
    var comboHits: Int = 0
    var comboExpiry: Long = 0L

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
    fun labGemsPerSec(ws: WorldState): Double = ws.buildingLevels[2] * 0.5 / 3600.0 * labGemBonus()

    /** Refinery building strongly accelerates world Essence. */
    fun refineryMult(ws: WorldState): Double = 1.0 + 0.5 * ws.buildingLevels[3]

    /** World-specific Essence produced per second (slow; Refinery multiplies it). */
    fun essencePerSec(ws: WorldState): Double =
        unitsBaseProd(ws) * 0.04 * refineryMult(ws) * territoryBonus(ws)

    // ---- research (account-wide global multiplier) ----------------------

    fun researchProdMult(): Double {
        var m = 1.0
        for (i in Defs.RESEARCH.indices) m *= 1.0 + Defs.RESEARCH[i].branchMult * research[i]
        return if (m.isFinite() && m >= 1.0) m else 1.0
    }

    fun researchCost(branch: Int): Long {
        val owned = research[branch].coerceAtMost(200)
        val raw = 1.6.pow(owned.toDouble())
        return if (!raw.isFinite() || raw >= 9.0e18) Long.MAX_VALUE else kotlin.math.ceil(raw).toLong()
    }

    fun researchMaxed(branch: Int): Boolean = research[branch] >= Defs.RESEARCH[branch].maxLevel

    fun buyResearch(branch: Int): Boolean {
        if (researchMaxed(branch)) return false
        val cost = researchCost(branch)
        if (cores < cost) return false
        cores -= cost
        research[branch] += 1
        return true
    }

    fun offlineEff(): Double = (0.5 + 0.02 * research[ResearchBranch.LOGISTICS.ordinal]).coerceAtMost(1.0)
    fun offlineCapHours(): Double = (8.0 + research[ResearchBranch.LOGISTICS.ordinal]).coerceAtMost(24.0)
    fun labGemBonus(): Double = 1.0 + 0.05 * research[ResearchBranch.SCIENCE.ordinal]
    fun expansionDiscount(): Double = (1.0 - 0.03 * research[ResearchBranch.EXPANSION.ordinal]).coerceAtLeast(0.4)

    // ---- prestige (per-world Re-Miniaturize) ----------------------------

    fun prestigeMult(ws: WorldState): Double = 1.0 + PRESTIGE_MULT_PER_STAR * ws.masteryStars

    fun totalStarsFor(lifetimeCoins: Double): Int {
        if (!lifetimeCoins.isFinite() || lifetimeCoins <= 0.0) return 0
        val raw = STAR_K * kotlin.math.sqrt(lifetimeCoins / STAR_SCALE)
        return if (!raw.isFinite()) 0 else floor(raw).toLong().coerceIn(0L, 1_000_000L).toInt()
    }

    fun prestigeStarsAvailable(id: String): Int {
        val ws = world(id)
        return (totalStarsFor(ws.lifetimeCoins) - ws.masteryStars).coerceAtLeast(0)
    }

    fun canPrestige(id: String): Boolean = prestigeStarsAvailable(id) >= 1

    fun prestige(now: Long): Int {
        val id = activeWorld
        val ws = world(id)
        val gain = prestigeStarsAvailable(id)
        if (gain < 1) return 0
        ws.masteryStars += gain
        for (i in ws.unitLevels.indices) ws.unitLevels[i] = 0
        for (i in ws.buildingLevels.indices) ws.buildingLevels[i] = 0
        ws.territories = 0
        ws.clearRewardClaimed = false
        ws.pending = 0.0
        ws.gemPending = 0.0
        ws.essence = 0.0
        ws.essenceRefines = 0
        ws.lastTick = now
        if (id == "kitchen") { ws.unitLevels[0] = 3; ws.territories = 1 }
        return gain
    }

    private fun creditLifetime(ws: WorldState, amount: Long) {
        if (amount <= 0L) return
        val v = ws.lifetimeCoins + amount
        ws.lifetimeCoins = if (v.isFinite()) v else ws.lifetimeCoins
    }

    // ---- collection / museum --------------------------------------------

    fun worldMastered(id: String): Boolean = world(id).territories >= Defs.TERRITORIES
    fun masteredCount(): Int = Defs.WORLDS.count { worldMastered(it.id) }
    fun skinsOwned(): Int = if (skinGold) 1 else 0
    fun skinsTotal(): Int = 1
    fun collectionComplete(): Boolean = skinGold && masteredCount() >= Defs.WORLDS.size
    fun collectionClaimable(): Boolean = collectionComplete() && !collectionBonus
    fun collectionMult(): Double = if (collectionBonus) 1.0 + COLLECTION_BONUS else 1.0
    fun claimCollection(): Boolean {
        if (!collectionClaimable()) return false
        collectionBonus = true
        return true
    }

    // ---- one-shot milestones --------------------------------------------
    fun milestoneSeen(bit: Long): Boolean = (milestonesShown and bit) != 0L
    /** Marks a one-shot milestone; returns true the FIRST time (caller shows the toast then). */
    fun markMilestone(bit: Long): Boolean {
        if (milestoneSeen(bit)) return false
        milestonesShown = milestonesShown or bit
        return true
    }

    // ---- recommendation engine (100% read-only over current state) ------
    /** Cheapest AFFORDABLE production upgrade in the active world, or null. */
    fun cheapestAffordableUpgrade(): ActionHint.Upgrade? {
        val ws = active()
        var best: ActionHint.Upgrade? = null
        var bestCost = Long.MAX_VALUE
        for (i in Defs.UNITS.indices) {
            val c = unitCost(ws, i)
            if (c in 1..coins && c < bestCost) { bestCost = c; best = ActionHint.Upgrade("u$i", c, 0) }
        }
        for (i in Defs.BUILDINGS.indices) {
            val c = buildingCost(ws, i)
            if (c in 1..coins && c < bestCost) { bestCost = c; best = ActionHint.Upgrade("b$i", c, 1) }
        }
        return best
    }

    /** The single most impactful action right now. Pure read-only — never mutates or rolls over. */
    fun bestActionHint(now: Long): ActionHint {
        // 1) Free rewards first — never leave gems/cores on the table.
        if (dailyAvailable(now)) return ActionHint.Claim(ClaimWhat.DAILY)
        for (i in Defs.QUESTS.indices) if (questClaimable(i)) return ActionHint.Claim(ClaimWhat.QUEST)
        if (collectionClaimable()) return ActionHint.Claim(ClaimWhat.COLLECTION)
        for (i in Defs.RESEARCH.indices) if (!researchMaxed(i) && cores >= researchCost(i)) return ActionHint.Claim(ClaimWhat.RESEARCH)
        // 2) Storage full -> collect (only if there's a real pile).
        val ws = active()
        val cap = capacity(activeWorld)
        if (cap > 0 && ws.pending >= cap - 0.5 && ws.pending >= 1.0) return ActionHint.CollectFull
        // 3) Prestige only when the world is cleared AND >=1 star is available.
        if (ws.territories >= Defs.TERRITORIES && canPrestige(activeWorld))
            return ActionHint.Prestige(prestigeStarsAvailable(activeWorld))
        // 4) Affordable production upgrade.
        cheapestAffordableUpgrade()?.let { return it }
        // 5) Conquer the next territory if affordable.
        if (ws.territories < Defs.TERRITORIES) {
            val tc = territoryCost(activeWorld)
            if (tc in 1..coins) return ActionHint.Conquer(tc, ws.territories + 1)
        }
        // 6) Unlock the cheapest still-locked world.
        val locked = Defs.WORLDS.filter { !world(it.id).unlocked }.minByOrNull { it.unlockGems }
        if (locked != null)
            return ActionHint.Unlock(locked.id, "", locked.unlockGems, gems >= locked.unlockGems)
        // 7) Nothing pressing.
        return ActionHint.Idle
    }

    /** Per-world signature bonus FRACTION (0..cap), derived only from already-saved state. */
    fun twistBonusFrac(ws: WorldState, def: WorldDef): Double {
        val t = def.twist
        val driver: Int = when (t.kind) {
            TwistKind.TERRITORIES      -> ws.territories
            TwistKind.WAREHOUSE        -> ws.buildingLevels[0]
            TwistKind.REFINES          -> ws.essenceRefines
            TwistKind.STARS            -> ws.masteryStars
            TwistKind.LAB              -> ws.buildingLevels[2]
            // assembly-line synergy: only the LOWEST building counts (every station must run)
            TwistKind.BUILDING_SYNERGY -> ws.buildingLevels.minOrNull() ?: 0
            TwistKind.UNITS_TOTAL      -> ws.unitLevels.sum()
            TwistKind.NONE             -> 0
        }
        if (driver <= 0 || t.coef <= 0.0) return 0.0
        return (t.coef * driver).coerceIn(0.0, t.cap)
    }

    /** The signature multiplier spliced into the production chain. Defaults to 1.0. */
    fun worldTwistMult(ws: WorldState, def: WorldDef): Double {
        val m = 1.0 + twistBonusFrac(ws, def)
        return if (m.isFinite() && m >= 1.0) m else 1.0
    }

    fun capacity(id: String): Double {
        val def = Defs.world(id)
        return def.baseCap * warehouseMult(world(id))
    }

    /** Canonical production formula (combo-neutral) — the ONE source of truth. */
    private fun baseProdRaw(id: String): Double {
        val def = Defs.world(id)
        val ws = world(id)
        return unitsBaseProd(ws) * def.prodMult * workshopMult(ws) * territoryBonus(ws) *
            (1.0 + 0.02 * ws.essenceRefines) * researchProdMult() * prestigeMult(ws) *
            collectionMult() * worldTwistMult(ws, def)
    }

    /** Production per second, ignoring boost and live combo (legacy callers stay combo-neutral). */
    fun baseProdPerSec(id: String): Double = baseProdRaw(id)

    /** Production per second including the live tap-combo multiplier (1.0 when no chain is live). */
    fun baseProdPerSec(id: String, now: Long): Double = baseProdRaw(id) * comboMult(now)

    // ---- Active-play: tap-combo + crit (state is transient; see fields above) ----
    fun comboActive(now: Long): Boolean = comboHits > 0 && comboExpiry > 0L && now < comboExpiry

    /** Live combo strength in [1.0 .. 1 + MAX*per-hit]; exactly 1.0 once it lapses. */
    fun comboMult(now: Long): Double {
        if (!comboActive(now)) return 1.0
        val m = 1.0 + COMBO_MULT_PER_HIT * comboHits.coerceAtMost(COMBO_MAX_HITS)
        return if (m.isFinite() && m >= 1.0) m else 1.0
    }

    fun comboFraction(now: Long): Float {
        if (!comboActive(now)) return 0f
        return (comboHits.toFloat() / COMBO_MAX_HITS).coerceIn(0f, 1f)
    }

    fun comboTimeFraction(now: Long): Float {
        if (!comboActive(now)) return 0f
        return ((comboExpiry - now).toFloat() / COMBO_WINDOW_MS).coerceIn(0f, 1f)
    }

    fun bumpCombo(now: Long, by: Int = 1) {
        comboHits = (comboHits + by).coerceIn(0, COMBO_MAX_HITS)
        comboExpiry = now + COMBO_WINDOW_MS
    }

    fun expireComboIfStale(now: Long) {
        if (comboHits > 0 && now >= comboExpiry) { comboHits = 0; comboExpiry = 0L }
    }

    /** Result of an active collect: amount paid, whether it crit, live combo, golden flag. */
    class CollectResult(val amount: Long, val crit: Boolean, val combo: Int, val golden: Boolean)

    /** Active collect: applies golden burst + crit, bumps combo, returns details for juice. */
    fun collectActive(now: Long, golden: Boolean): CollectResult {
        val ws = active()
        expireComboIfStale(now)
        // Golden burst is added to pending first so it's cashed in the same payout.
        if (golden) {
            val grant = baseProdPerSec(activeWorld, now) * GOLDEN_GRANT_SEC
            if (grant.isFinite() && grant > 0.0)
                ws.pending = min(capacity(activeWorld), ws.pending + grant)
        }
        var amount = floor(ws.pending).toLong()
        var crit = false
        if (amount > 0) {
            ws.pending -= amount
            if (Math.random() < CRIT_CHANCE) { crit = true; amount += amount }
            coins += amount
            qCollected += amount
            creditLifetime(ws, amount)
            bumpCombo(now, if (golden) GOLDEN_COMBO_BUMP else 1)
        }
        // Lab gems harvest together (same as legacy collect()).
        val g = floor(ws.gemPending).toLong()
        if (g > 0) { gems += g; ws.gemPending -= g }
        return CollectResult(amount, crit, comboHits, golden)
    }

    fun boostActive(now: Long): Boolean = now < boostExpiry && boostMult > 1.0

    fun effectiveProdPerSec(id: String, now: Long): Double {
        val base = baseProdPerSec(id, now)              // carries the live combo multiplier
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

    // ---- bulk purchasing & ROI (reuses the single growthCost formula; never forks it) ----

    private val MAX_BULK = 1000  // safety bound for Max-buy loops (prevents ANR)

    /** Saturating sum of the next [n] unit costs (same per-level growthCost). */
    fun bulkUnitCost(ws: WorldState, i: Int, n: Int): Long {
        val d = Defs.UNITS[i]
        var total = 0L
        val lvl = ws.unitLevels[i]
        for (k in 0 until n.coerceAtMost(MAX_BULK)) {
            val c = growthCost(d.baseCost, d.growth, lvl + k)
            total = if (total > Long.MAX_VALUE - c) Long.MAX_VALUE else total + c
            if (total == Long.MAX_VALUE) break
        }
        return total
    }

    fun bulkBuildingCost(ws: WorldState, i: Int, n: Int): Long {
        val d = Defs.BUILDINGS[i]
        var total = 0L
        val lvl = ws.buildingLevels[i]
        for (k in 0 until n.coerceAtMost(MAX_BULK)) {
            val c = growthCost(d.baseCost, d.growth, lvl + k)
            total = if (total > Long.MAX_VALUE - c) Long.MAX_VALUE else total + c
            if (total == Long.MAX_VALUE) break
        }
        return total
    }

    /** Largest count buyable now with current coins (0..MAX_BULK). */
    fun maxAffordableUnit(ws: WorldState, i: Int): Int {
        val d = Defs.UNITS[i]
        var n = 0; var budget = coins
        while (n < MAX_BULK) {
            val c = growthCost(d.baseCost, d.growth, ws.unitLevels[i] + n)
            if (c == Long.MAX_VALUE || budget < c) break
            budget -= c; n++
        }
        return n
    }

    fun maxAffordableBuilding(ws: WorldState, i: Int): Int {
        val d = Defs.BUILDINGS[i]
        var n = 0; var budget = coins
        while (n < MAX_BULK) {
            val c = growthCost(d.baseCost, d.growth, ws.buildingLevels[i] + n)
            if (c == Long.MAX_VALUE || budget < c) break
            budget -= c; n++
        }
        return n
    }

    /** Buys up to [n] unit levels on the active world. Returns the count actually bought. */
    fun buyUnit(i: Int, n: Int): Int {
        val ws = active()
        var bought = 0
        val want = n.coerceIn(0, MAX_BULK)
        while (bought < want) {
            val cost = unitCost(ws, i)
            if (cost == Long.MAX_VALUE || coins < cost) break
            coins -= cost; ws.unitLevels[i] += 1; bought++
        }
        qUpgrades += bought
        return bought
    }

    fun buyBuilding(i: Int, n: Int): Int {
        val ws = active()
        var bought = 0
        val want = n.coerceIn(0, MAX_BULK)
        while (bought < want) {
            val cost = buildingCost(ws, i)
            if (cost == Long.MAX_VALUE || coins < cost) break
            coins -= cost; ws.buildingLevels[i] += 1; bought++
        }
        qUpgrades += bought
        return bought
    }

    /** Extra coins/sec from ONE more level of unit i — derived from the canonical baseProdPerSec. */
    fun unitProdDelta(ws: WorldState, i: Int): Double {
        val before = baseProdPerSec(activeWorld)
        ws.unitLevels[i] += 1
        val after = baseProdPerSec(activeWorld)
        ws.unitLevels[i] -= 1
        val v = after - before
        return if (v.isFinite() && v > 0.0) v else 0.0
    }

    /** Extra coins/sec from ONE more level of building i (warehouse/workshop change a multiplier). */
    fun buildingProdDelta(ws: WorldState, i: Int): Double {
        val before = baseProdPerSec(activeWorld)
        ws.buildingLevels[i] += 1
        val after = baseProdPerSec(activeWorld)
        ws.buildingLevels[i] -= 1
        val v = after - before
        return if (v.isFinite() && v > 0.0) v else 0.0
    }

    fun territoryCost(id: String): Long {
        val def = Defs.world(id)
        val ws = world(id)
        // Gentle hyper-casual ramp; last tile still dominates (1.4^9 ~= 20x base).
        return floor(growthCost(def.territoryBaseCost, 1.4, ws.territories) * expansionDiscount()).toLong()
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
        ws.essence += essencePerSec(ws) * dt
        if (!ws.pending.isFinite()) ws.pending = before
        if (!ws.gemPending.isFinite()) ws.gemPending = 0.0
        if (!ws.essence.isFinite()) ws.essence = 0.0
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
    class OfflineResult(val coins: Long, val gems: Long, val awayMs: Long)

    fun accrueOffline(now: Long, awayMs: Long): OfflineResult {
        val ws = active()
        // Keep lastTick coherent so the live ticker doesn't double-count.
        for (w in worlds.values) w.lastTick = now
        val awaySec = min(awayMs / 1000.0, offlineCapHours() * 3600.0)
        if (awaySec <= 0.0) return OfflineResult(0L, 0L, awayMs)
        val gainedD = effectiveProdPerSec(activeWorld, now) * awaySec * offlineEff()
        val gemD = labGemsPerSec(ws) * awaySec
        val c = if (gainedD.isFinite() && gainedD >= 1.0) floor(gainedD).toLong() else 0L
        val g = if (gemD.isFinite() && gemD >= 1.0) floor(gemD).toLong() else 0L
        coins += c
        gems += g
        creditLifetime(ws, c)
        return OfflineResult(c, g, awayMs)
    }

    fun collect(): Long {
        val ws = active()
        val amount = floor(ws.pending).toLong()
        if (amount > 0) {
            coins += amount
            ws.pending -= amount
            qCollected += amount
            creditLifetime(ws, amount)
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
        qUpgrades += 1
        return true
    }

    fun upgradeBuilding(i: Int): Boolean {
        val ws = active()
        val cost = buildingCost(ws, i)
        if (coins < cost) return false
        coins -= cost
        ws.buildingLevels[i] += 1
        qUpgrades += 1
        return true
    }

    fun refineCost(ws: WorldState): Long =
        floor(10.0 * 1.7.pow(ws.essenceRefines.coerceAtMost(60))).toLong()

    /** Spends world Essence for a permanent +2% production refine in the active world. */
    fun refine(): Boolean {
        val ws = active()
        val cost = refineCost(ws)
        if (ws.essence < cost) return false
        ws.essence -= cost
        ws.essenceRefines += 1
        return true
    }

    class ConquerResult(val ok: Boolean, val clearedWorld: Boolean, val gemReward: Int, val coreReward: Int = 0)

    fun conquer(): ConquerResult {
        val def = Defs.world(activeWorld)
        val ws = active()
        if (ws.territories >= Defs.TERRITORIES) return ConquerResult(false, false, 0)
        val cost = territoryCost(activeWorld)
        if (coins < cost) return ConquerResult(false, false, 0)
        coins -= cost
        ws.territories += 1
        qConquered += 1
        var cleared = false
        var reward = 0
        var core = 0
        if (ws.territories >= Defs.TERRITORIES && !ws.clearRewardClaimed) {
            ws.clearRewardClaimed = true
            cleared = true
            // Clear gems + Cores are granted only the FIRST time a world is cleared, ever.
            // Prestige re-clears earn Mastery Stars (the loop reward), not a repeatable faucet.
            if (!ws.everCleared) {
                ws.everCleared = true
                reward = def.clearReward
                gems += reward
                core = Defs.coreReward(activeWorld)
                cores += core
            }
        }
        return ConquerResult(true, cleared, reward, core)
    }

    fun unlockWorld(id: String): Boolean {
        val def = Defs.world(id)
        val ws = world(id)
        if (ws.unlocked) return true
        if (gems < def.unlockGems) return false
        gems -= def.unlockGems
        ws.unlocked = true
        // Stamp NOW (not the stale lastSeen) so a freshly unlocked world starts from zero,
        // not an instant 8-hour cap fill on its first tick.
        ws.lastTick = System.currentTimeMillis()
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

    // ---- daily quests ---------------------------------------------------

    fun rolloverDaily(now: Long) {
        val today = epochDay(now)
        if (questDay != today) {
            questDay = today
            qCollected = 0L
            qUpgrades = 0
            qConquered = 0
            for (i in qClaimed.indices) qClaimed[i] = false
        }
    }

    fun questProgress(i: Int): Long = when (i) {
        0 -> qCollected
        1 -> qUpgrades.toLong()
        else -> qConquered.toLong()
    }

    fun questClaimable(i: Int): Boolean = !qClaimed[i] && questProgress(i) >= Defs.QUESTS[i].target

    /** Claims quest i if eligible; returns gems granted (0 if not). */
    fun claimQuest(i: Int): Int {
        if (!questClaimable(i)) return 0
        qClaimed[i] = true
        val reward = Defs.QUESTS[i].rewardGems
        gems += reward
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
        o.put("questDay", questDay)
        o.put("qCollected", qCollected)
        o.put("qUpgrades", qUpgrades)
        o.put("qConquered", qConquered)
        o.put("qClaimed", JSONArray(listOf(qClaimed[0], qClaimed[1], qClaimed[2])))
        o.put("cores", cores)
        o.put("research", JSONArray(research.toList()))
        o.put("collectionBonus", collectionBonus)
        o.put("milestonesShown", milestonesShown)
        val ws = JSONObject()
        for ((id, w) in worlds) {
            val wo = JSONObject()
            wo.put("unlocked", w.unlocked)
            wo.put("territories", w.territories)
            wo.put("clearRewardClaimed", w.clearRewardClaimed)
            wo.put("pending", safe(w.pending))
            wo.put("gemPending", safe(w.gemPending))
            wo.put("essence", safe(w.essence))
            wo.put("essenceRefines", w.essenceRefines)
            wo.put("lifetimeCoins", safe(w.lifetimeCoins))
            wo.put("masteryStars", w.masteryStars)
            wo.put("everCleared", w.everCleared)
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
        const val STAR_K = 1.0
        const val STAR_SCALE = 10_000.0
        const val PRESTIGE_MULT_PER_STAR = 0.10

        // Active-play tuning (combo / crit / golden).
        const val COMBO_WINDOW_MS = 2200L      // time to keep the chain alive
        const val COMBO_MAX_HITS = 30          // cap so the multiplier can't run away
        const val COMBO_MULT_PER_HIT = 0.02    // +2% per stacked hit -> up to +60%
        const val CRIT_CHANCE = 0.12           // 12% of non-empty collects crit
        const val GOLDEN_GRANT_SEC = 25.0      // golden burst = ~25s of base production
        const val GOLDEN_COMBO_BUMP = 6        // golden tap also fattens the combo

        // One-shot milestone bits (bitmask in milestonesShown).
        const val M_FIRST_CLEAR = 1L
        const val M_FIRST_PRESTIGE = 1L shl 1
        const val M_COINS_1M = 1L shl 2
        const val M_COINS_1B = 1L shl 3

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
            s.questDay = o.optLong("questDay", -1L)
            s.qCollected = o.optLong("qCollected", 0L).coerceAtLeast(0L)
            s.qUpgrades = o.optInt("qUpgrades", 0).coerceAtLeast(0)
            s.qConquered = o.optInt("qConquered", 0).coerceAtLeast(0)
            o.optJSONArray("qClaimed")?.let { qc ->
                for (i in 0 until min(qc.length(), s.qClaimed.size)) s.qClaimed[i] = qc.optBoolean(i, false)
            }
            s.cores = o.optLong("cores", 0L).coerceAtLeast(0L)
            readInts(o.optJSONArray("research"), s.research)
            s.collectionBonus = o.optBoolean("collectionBonus", false)
            s.milestonesShown = o.optLong("milestonesShown", 0L).coerceAtLeast(0L)
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
                    w.essence = wo.optDouble("essence", 0.0).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    w.essenceRefines = wo.optInt("essenceRefines", 0).coerceAtLeast(0)
                    w.lifetimeCoins = wo.optDouble("lifetimeCoins", 0.0).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    w.masteryStars = wo.optInt("masteryStars", 0).coerceIn(0, 1_000_000)
                    w.everCleared = wo.optBoolean("everCleared", w.territories >= Defs.TERRITORIES)
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

/** A single recommended next action surfaced by [GameState.bestActionHint]. */
sealed class ActionHint {
    /** Hire/level a unit (seg 0) or building (seg 1). labelArg is "u<i>" / "b<i>". */
    class Upgrade(val labelArg: String, val cost: Long, val seg: Int) : ActionHint()
    class Conquer(val cost: Long, val territoryNo: Int) : ActionHint()
    class Unlock(val worldId: String, val nameArg: String, val gemCost: Int, val affordable: Boolean) : ActionHint()
    class Prestige(val stars: Int) : ActionHint()
    class Claim(val what: ClaimWhat) : ActionHint()
    object CollectFull : ActionHint()
    object Idle : ActionHint()
}

enum class ClaimWhat { DAILY, QUEST, RESEARCH, COLLECTION }
