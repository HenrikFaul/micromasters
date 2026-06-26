package com.micromasters.game

import android.animation.ValueAnimator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd

/**
 * Lightweight, lifecycle-safe number/bar rollups. One ValueAnimator per target View,
 * stored in the View's tag so a fresh call cancels the in-flight one (no overlap, no leak).
 * All animators are main-thread and short-lived (<= one 200ms tick), so they finish before
 * the next refresh re-issues them.
 *
 * [active] is intentionally a single process-global set shared across activities; an
 * activity's onPause -> cancelAll() snapping the other's in-flight animators is benign
 * (they snap straight to their target value on the main thread).
 */
object NumAnim {

    private val active = HashSet<ValueAnimator>()

    /** Roll [tv] from its current shown value to [target], rendering via Format.short. */
    fun countTo(tv: TextView, target: Long, durMs: Long = 180L) {
        (tv.getTag(R.id.numanim_tag) as? ValueAnimator)?.cancel()
        val from = (tv.getTag(R.id.numanim_value) as? Long) ?: target
        // First bind, a huge jump (offline/reset), or a change invisible at Format.short's
        // 3-sig-fig resolution: just snap — animating would render identical frames.
        if (from == target ||
            kotlin.math.abs(target - from) > 5_000_000_000L ||
            Format.short(from) == Format.short(target)
        ) {
            tv.setTag(R.id.numanim_value, target)
            tv.text = Format.short(target)
            return
        }
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durMs
            addUpdateListener { a ->
                val v = from + ((target - from) * a.animatedFraction).toLong()
                tv.setTag(R.id.numanim_value, v)
                tv.text = Format.short(v)
            }
            doOnEnd {
                tv.setTag(R.id.numanim_value, target)
                tv.text = Format.short(target)
                active.remove(this)
            }
        }
        tv.setTag(R.id.numanim_tag, anim)
        active.add(anim)
        anim.start()
    }

    /** Smoothly move a ProgressBar to [target] (0..max). Snaps on large deltas to stay responsive. */
    fun barTo(bar: ProgressBar, target: Int, durMs: Long = 160L) {
        (bar.getTag(R.id.numanim_tag) as? ValueAnimator)?.cancel()
        val from = bar.progress
        if (from == target) return
        if (kotlin.math.abs(target - from) > bar.max / 2) { bar.progress = target; return }
        val anim = ValueAnimator.ofInt(from, target).apply {
            duration = durMs
            addUpdateListener { a -> bar.progress = a.animatedValue as Int }
            doOnEnd { bar.progress = target; active.remove(this) }
        }
        bar.setTag(R.id.numanim_tag, anim)
        active.add(anim)
        anim.start()
    }

    /** Cancel every in-flight animator (call from onPause). Safe to call repeatedly. */
    fun cancelAll() {
        for (a in active.toList()) a.cancel()
        active.clear()
    }
}
