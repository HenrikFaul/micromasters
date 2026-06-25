package com.micromasters.game

/** Static content definitions for MicroMasters. */

class UnitDef(
    val id: String,
    val nameRes: Int,
    val emoji: String,
    val baseProd: Double,   // coins/sec contributed per level
    val baseCost: Double,   // cost of the first level (hire)
    val growth: Double = 1.18
)

class BuildingDef(
    val id: String,
    val nameRes: Int,
    val descRes: Int,
    val emoji: String,
    val baseCost: Double,
    val growth: Double
)

enum class BoostKind { MULTIPLIER, FILL, AD }

class BoostDef(
    val id: String,
    val nameRes: Int,
    val descRes: Int,
    val emoji: String,
    val kind: BoostKind,
    val gemCost: Int,          // 0 means free / ad
    val durationMs: Long = 0,  // for MULTIPLIER / AD
    val multiplier: Double = 1.0
)

class WorldDef(
    val id: String,
    val nameRes: Int,
    val emoji: String,
    val prodMult: Double,
    val baseCap: Double,
    val territoryBaseCost: Double,
    val unlockGems: Int,
    val unlockedByDefault: Boolean,
    val clearReward: Int,        // gems for conquering all territories
    val skyTop: Int,
    val skyBottom: Int,
    val ground: Int,
    val accent: Int,
    val worker: String,
    val nodes: Array<String>
)

object Defs {
    const val TERRITORIES = 10

    // NOTE: every emoji below is Unicode <= 7 (renders on Android 8.0+, no EmojiCompat needed).
    val UNITS = listOf(
        UnitDef("miner", R.string.unit_miner, "🐜", baseProd = 0.9, baseCost = 15.0),
        UnitDef("carrier", R.string.unit_carrier, "📦", baseProd = 2.4, baseCost = 90.0),
        UnitDef("guard", R.string.unit_guard, "🛡️", baseProd = 9.0, baseCost = 1000.0),
        UnitDef("scientist", R.string.unit_scientist, "🔬", baseProd = 32.0, baseCost = 7200.0)
    )

    val BUILDINGS = listOf(
        BuildingDef("warehouse", R.string.bld_warehouse, R.string.bld_warehouse_desc, "📦", 150.0, 1.45),
        BuildingDef("workshop", R.string.bld_workshop, R.string.bld_workshop_desc, "🔧", 420.0, 1.55),
        BuildingDef("lab", R.string.bld_lab, R.string.bld_lab_desc, "💡", 2600.0, 1.7)
    )

    val BOOSTS = listOf(
        BoostDef("x2", R.string.boost_2x, R.string.boost_2x_desc, "⚡️", BoostKind.MULTIPLIER, gemCost = 15, durationMs = 15 * 60_000L, multiplier = 2.0),
        BoostDef("fill", R.string.boost_fill, R.string.boost_fill_desc, "💧", BoostKind.FILL, gemCost = 8),
        BoostDef("ad", R.string.boost_ad, R.string.boost_ad_desc, "🎬", BoostKind.AD, gemCost = 0, durationMs = 5 * 60_000L, multiplier = 1.0)
    )

    // Colors are plain ARGB ints so the canvas view can use them directly.
    val WORLDS = listOf(
        WorldDef(
            "kitchen", R.string.world_kitchen, "🍳",
            prodMult = 1.0, baseCap = 120.0, territoryBaseCost = 220.0,
            unlockGems = 0, unlockedByDefault = true, clearReward = 80,
            skyTop = 0xFFE7B57A.toInt(), skyBottom = 0xFF7A4A24.toInt(),
            ground = 0xFF8A5A2E.toInt(), accent = 0xFFFFD27A.toInt(),
            worker = "🐜", nodes = arrayOf("🍞", "🍅", "🥚", "🍶", "🍪")
        ),
        WorldDef(
            "bathroom", R.string.world_bathroom, "🛁",
            prodMult = 6.0, baseCap = 1600.0, territoryBaseCost = 2400.0,
            unlockGems = 0, unlockedByDefault = true, clearReward = 150,
            skyTop = 0xFF7FD8E6.toInt(), skyBottom = 0xFF1E6E78.toInt(),
            ground = 0xFF2C8A93.toInt(), accent = 0xFFBFF3FA.toInt(),
            worker = "🐌", nodes = arrayOf("💧", "🛁", "🚿", "💧", "🔵")
        ),
        WorldDef(
            "garden", R.string.world_garden, "🌷",
            prodMult = 42.0, baseCap = 13000.0, territoryBaseCost = 9000.0,
            unlockGems = 120, unlockedByDefault = false, clearReward = 300,
            skyTop = 0xFF9FE07A.toInt(), skyBottom = 0xFF2E7A2E.toInt(),
            ground = 0xFF3C8A3C.toInt(), accent = 0xFFE6FFC2.toInt(),
            worker = "🐛", nodes = arrayOf("🌱", "🍄", "🌷", "🐞", "🍀")
        ),
        WorldDef(
            "spaceship", R.string.world_spaceship, "🚀",
            prodMult = 320.0, baseCap = 95000.0, territoryBaseCost = 150000.0,
            unlockGems = 400, unlockedByDefault = false, clearReward = 800,
            skyTop = 0xFF3A4E8C.toInt(), skyBottom = 0xFF0E1430.toInt(),
            ground = 0xFF243056.toInt(), accent = 0xFF9FB8FF.toInt(),
            worker = "👽", nodes = arrayOf("🔩", "🔋", "💾", "⚙️", "🛰️")
        )
    )

    fun world(id: String): WorldDef = WORLDS.first { it.id == id }
    fun worldIndex(id: String): Int = WORLDS.indexOfFirst { it.id == id }

    /** Daily reward table. type true = gems, false = coins. Mirrors the mockup. */
    data class DailyReward(val amount: Long, val isGems: Boolean)

    val DAILY = listOf(
        DailyReward(500, false),
        DailyReward(700, false),
        DailyReward(1000, false),
        DailyReward(1500, false),
        DailyReward(2500, false),
        DailyReward(200, true),
        DailyReward(5000, true)
    )
}
