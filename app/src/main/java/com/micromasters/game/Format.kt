package com.micromasters.game

import java.util.Locale

/** Compact number formatting: 850 -> "850", 12500 -> "12.5k", 2_300_000 -> "2.3M". */
object Format {
    private val UNITS = arrayOf("", "k", "M", "B", "T", "Q")

    fun short(n: Long): String {
        if (n < 1000) return n.toString()
        var v = n.toDouble()
        var tier = 0
        while (v >= 1000.0 && tier < UNITS.size - 1) {
            v /= 1000.0
            tier++
        }
        val s = if (v < 100.0) String.format(Locale.US, "%.1f", v)
        else String.format(Locale.US, "%.0f", v)
        val trimmed = if (s.endsWith(".0")) s.substring(0, s.length - 2) else s
        return trimmed + UNITS[tier]
    }

    fun short(n: Double): String = short(n.toLong())

    /** Mm:ss style remaining time. */
    fun duration(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}
