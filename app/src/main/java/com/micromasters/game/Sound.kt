package com.micromasters.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Tiny audio manager: a [SoundPool] for one-shot SFX and a looping [MediaPlayer] for music.
 * Every call is a no-op when sound is off, and every native call is wrapped so audio can
 * never crash gameplay. Lives for the process lifetime (a singleton), so there is nothing
 * to leak — the OS reclaims it on process death.
 */
object Sound {
    private const val PREFS = "mm_audio"
    private const val KEY_ON = "sound_on"

    private val SFX = intArrayOf(
        R.raw.sfx_collect, R.raw.sfx_tap, R.raw.sfx_hire, R.raw.sfx_upgrade,
        R.raw.sfx_conquer, R.raw.sfx_reward, R.raw.sfx_mega, R.raw.sfx_error, R.raw.sfx_unlock
    )

    private var pool: SoundPool? = null
    private val soundIds = HashMap<Int, Int>()
    private var music: MediaPlayer? = null

    var enabled = true
        private set

    fun init(ctx: Context) {
        if (pool != null) return
        val app = ctx.applicationContext
        enabled = try {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, true)
        } catch (e: Throwable) { true }
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val p = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
            for (r in SFX) soundIds[r] = p.load(app, r, 1)
            pool = p
        } catch (e: Throwable) {
            pool = null
        }
    }

    /** Play a one-shot effect (R.raw.sfx_*). Silent when disabled or not yet loaded. */
    fun sfx(ctx: Context, resId: Int) {
        if (!enabled) return
        init(ctx)
        try { soundIds[resId]?.let { pool?.play(it, 1f, 1f, 1, 0, 1f) } } catch (e: Throwable) {}
    }

    /** Start or resume the looping background music (no-op if disabled). */
    fun resumeMusic(ctx: Context) {
        if (!enabled) return
        try {
            var m = music
            if (m == null) {
                m = MediaPlayer.create(ctx.applicationContext, R.raw.bgm_loop) ?: return
                m.isLooping = true
                m.setVolume(0.45f, 0.45f)
                music = m
            }
            if (!m.isPlaying) m.start()
        } catch (e: Throwable) {}
    }

    /** Pause music (call from Activity.onPause) so it stops when the app is backgrounded. */
    fun pauseMusic() {
        try { music?.let { if (it.isPlaying) it.pause() } } catch (e: Throwable) {}
    }

    fun setEnabled(ctx: Context, on: Boolean) {
        enabled = on
        try {
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ON, on).apply()
        } catch (e: Throwable) {}
        if (on) resumeMusic(ctx) else pauseMusic()
    }
}
