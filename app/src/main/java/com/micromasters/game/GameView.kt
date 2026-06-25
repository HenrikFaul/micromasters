package com.micromasters.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Animated "micro world" scene. Little workers roam between resource nodes;
 * tapping spawns a juicy reward burst. Purely cosmetic — the economy lives in
 * [GameState] — but it gives the screen the lively feel of the mockup.
 *
 * The render loop is gated on [resume]/[pause] (driven by the Activity) so it
 * never burns CPU while backgrounded, and onDraw is wrapped so a single bad
 * frame can never crash the app.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Worker(var x: Float, var y: Float, var tx: Float, var ty: Float, var spd: Float, var phase: Float)
    private class Node(val x: Float, val y: Float, val emoji: String, val scale: Float)
    private class Floater(var x: Float, var y: Float, val text: String, var age: Float, val life: Float, val color: Int, val size: Float, var vx: Float, var vy: Float)
    private class Star(val x: Float, val y: Float, val r: Float, val a: Int)

    private var def: WorldDef = Defs.WORLDS[0]
    private var workerCount = 5
    private var fillRatio = 0f
    private var goldSkin = false

    private val rnd = Random(7)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val workers = ArrayList<Worker>()
    private val nodes = ArrayList<Node>()
    private val floaters = ArrayList<Floater>()
    private val stars = ArrayList<Star>()
    private val byY = Comparator<Worker> { a, b -> a.y.compareTo(b.y) }

    private var bgShader: Shader? = null
    private val clip = Path()
    private val ground = Path()
    private var lastFrame = 0L
    private var running = false
    private var shakeMag = 0f

    private val frame = object : Runnable {
        override fun run() {
            step()
            invalidate()
            if (running) postOnAnimation(this)
        }
    }

    fun configure(d: WorldDef, workers: Int) {
        def = d
        workerCount = workers.coerceIn(3, 16)
        goldSkin = try { Game.get(context).skinGold } catch (e: Exception) { false }
        if (width > 0 && height > 0) rebuild()
        invalidate()
    }

    fun setFill(ratio: Float) {
        fillRatio = ratio.coerceIn(0f, 1f)
    }

    fun resume() {
        if (!running) {
            running = true
            lastFrame = 0L
            postOnAnimation(frame)
        }
    }

    fun pause() {
        running = false
        removeCallbacks(frame)
    }

    fun shake(mag: Float = dp(8f)) {
        shakeMag = mag
    }

    /** Spawns a juicy "+amount" pop plus an arcing coin burst. */
    fun spawnCollect(label: String) {
        if (width == 0) return
        val cx = width * 0.5f
        val cy = height * 0.5f
        floaters.add(Floater(cx, cy, label, 0f, 1.2f, 0xFFFFCB3D.toInt(), dp(30f), 0f, 0f))
        for (i in 0 until 9) {
            val ang = rnd.nextFloat() * 6.2832f
            val sp = dp(150f) + rnd.nextFloat() * dp(140f)
            floaters.add(
                Floater(
                    cx, cy, "💰", 0f, 0.9f + rnd.nextFloat() * 0.5f, 0, dp(16f),
                    cos(ang.toDouble()).toFloat() * sp,
                    -(dp(120f) + rnd.nextFloat() * dp(180f))
                )
            )
        }
        shake(dp(5f))
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        rebuild()
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun rebuild() {
        val w = width.toFloat()
        val h = height.toFloat()
        bgShader = LinearGradient(0f, 0f, 0f, h, def.skyTop, def.skyBottom, Shader.TileMode.CLAMP)

        val r = dp(20f)
        clip.reset()
        clip.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)

        // Ground mound is static — build it once here, not every frame.
        val gt = h * 0.64f
        ground.reset()
        ground.moveTo(0f, gt + dp(14f))
        ground.quadTo(w * 0.5f, gt - dp(20f), w, gt + dp(14f))
        ground.lineTo(w, h)
        ground.lineTo(0f, h)
        ground.close()

        stars.clear()
        for (i in 0 until 26) {
            stars.add(Star(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.6f, dp(1f) + rnd.nextFloat() * dp(2f), 60 + rnd.nextInt(120)))
        }

        nodes.clear()
        val pad = w * 0.13f
        for (i in 0 until 6) {
            val nx = pad + rnd.nextFloat() * (w - 2 * pad)
            val ny = h * 0.34f + rnd.nextFloat() * h * 0.46f
            nodes.add(Node(nx, ny, def.nodes[i % def.nodes.size], 0.85f + rnd.nextFloat() * 0.5f))
        }

        workers.clear()
        for (i in 0 until workerCount) {
            val wk = Worker(w * 0.5f, h * 0.62f, 0f, 0f, 0f, rnd.nextFloat() * 6.28f)
            pickTarget(wk)
            workers.add(wk)
        }
    }

    private fun pickTarget(wk: Worker) {
        if (nodes.isEmpty()) {
            wk.tx = width * 0.5f
            wk.ty = height * 0.6f
            return
        }
        val n = nodes[rnd.nextInt(nodes.size)]
        wk.tx = n.x + (rnd.nextFloat() - 0.5f) * dp(46f)
        wk.ty = n.y + dp(16f) + (rnd.nextFloat() - 0.5f) * dp(24f)
        wk.spd = dp(40f) + rnd.nextFloat() * dp(70f)
    }

    private fun step() {
        val now = System.nanoTime()
        var dt = if (lastFrame == 0L) 0.016f else (now - lastFrame) / 1_000_000_000f
        lastFrame = now
        if (dt > 0.05f) dt = 0.05f

        if (shakeMag > 0f) shakeMag = (shakeMag - dp(60f) * dt).coerceAtLeast(0f)

        for (wk in workers) {
            val dx = wk.tx - wk.x
            val dy = wk.ty - wk.y
            val dist = hypot(dx, dy)
            if (dist < dp(6f)) {
                pickTarget(wk)
            } else {
                val v = wk.spd * dt
                wk.x += dx / dist * v
                wk.y += dy / dist * v
            }
            wk.phase += dt * 7f
        }
        workers.sortWith(byY)

        val it = floaters.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.age += dt
            if (f.color == 0) {
                f.x += f.vx * dt
                f.y += f.vy * dt
                f.vy += dp(420f) * dt
            } else {
                f.y -= dp(80f) * (1f - f.age / f.life) * dt
            }
            if (f.age >= f.life) it.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return

        val saved = canvas.save()
        try {
            canvas.clipPath(clip)
            if (shakeMag > 0f) {
                canvas.translate((rnd.nextFloat() - 0.5f) * shakeMag, (rnd.nextFloat() - 0.5f) * shakeMag)
            }

            // sky
            paint.shader = bgShader
            paint.style = Paint.Style.FILL
            canvas.drawRect(-shakeMag, -shakeMag, w + shakeMag, h + shakeMag, paint)
            paint.shader = null

            // ambient specks
            for (s in stars) {
                paint.color = (s.a shl 24) or (def.accent and 0x00FFFFFF)
                canvas.drawCircle(s.x, s.y, s.r, paint)
            }

            // ground mound (reused path) + capacity tint
            paint.color = def.ground
            canvas.drawPath(ground, paint)
            paint.color = ((40 + (fillRatio * 90).toInt()) shl 24) or (def.accent and 0x00FFFFFF)
            canvas.drawPath(ground, paint)

            // nodes
            for (n in nodes) {
                paint.color = 0x33FFFFFF
                canvas.drawCircle(n.x, n.y, dp(20f) * n.scale, paint)
                drawGlyph(canvas, n.emoji, n.x, n.y, dp(24f) * n.scale)
            }

            // workers (already back-to-front from step)
            for (wk in workers) {
                val bob = sin(wk.phase.toDouble()).toFloat() * dp(2f)
                if (goldSkin) {
                    paint.color = 0x66FFCB3D.toInt()
                    canvas.drawCircle(wk.x, wk.y + bob - dp(2f), dp(13f), paint)
                }
                paint.color = 0x44000000
                canvas.drawOval(wk.x - dp(10f), wk.y + dp(6f), wk.x + dp(10f), wk.y + dp(12f), paint)
                drawGlyph(canvas, def.worker, wk.x, wk.y + bob, dp(22f))
            }

            // floaters
            for (f in floaters) {
                val t = (f.age / f.life).coerceIn(0f, 1f)
                val alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                if (f.color == 0) {
                    drawGlyph(canvas, f.text, f.x, f.y, f.size, alpha)
                } else {
                    val pop = if (t < 0.2f) t / 0.2f else 1f
                    val sz = f.size * (0.5f + 0.7f * pop)
                    glyph.textSize = sz
                    glyph.isFakeBoldText = true
                    glyph.color = (alpha / 2 shl 24)
                    canvas.drawText(f.text, f.x + dp(2f), f.y + dp(2f), glyph)
                    glyph.color = (alpha shl 24) or (f.color and 0x00FFFFFF)
                    canvas.drawText(f.text, f.x, f.y, glyph)
                    glyph.isFakeBoldText = false
                }
            }
        } catch (e: Throwable) {
            // A render hiccup must never crash the app — skip the frame.
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun drawGlyph(canvas: Canvas, s: String, cx: Float, cy: Float, size: Float, alpha: Int = 255) {
        glyph.textSize = size
        glyph.alpha = alpha
        canvas.drawText(s, cx, cy + size * 0.35f, glyph)
        glyph.alpha = 255
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resume()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }
}
