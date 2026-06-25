package com.micromasters.game

import android.content.Context
import org.json.JSONObject

/** Loads and persists the single save-game using SharedPreferences. */
object Game {
    private const val PREFS = "micromasters_save"
    private const val KEY = "state"

    @Volatile
    private var state: GameState? = null

    fun get(context: Context): GameState {
        state?.let { return it }
        synchronized(this) {
            state?.let { return it }
            val now = System.currentTimeMillis()
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY, null)
            val loaded = if (raw != null) {
                try {
                    GameState.fromJson(JSONObject(raw), now)
                } catch (e: Exception) {
                    GameState.newGame(now)
                }
            } else {
                GameState.newGame(now)
            }
            state = loaded
            return loaded
        }
    }

    fun save(context: Context) {
        val s = state ?: return
        s.lastSeen = System.currentTimeMillis()
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY, s.toJson().toString()).apply()
        } catch (e: Exception) {
            // Never let a serialization hiccup crash onPause; state stays in memory.
        }
    }

    fun reset(context: Context): GameState {
        val now = System.currentTimeMillis()
        val fresh = GameState.newGame(now)
        state = fresh
        save(context)
        return fresh
    }
}
