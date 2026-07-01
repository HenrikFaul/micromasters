package com.micromasters.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The "micro world" scene, rebuilt as a clean isometric base: a gradient sky with
 * drifting clouds, a 2.5D platform, the world's building sprites placed on it, little
 * worker characters roaming between them, and a gold stockpile that grows as uncollected
 * production piles up. Everything is drawn procedurally (no background images), so it
 * stays crisp and cohesive. The economy lives in [GameState]; this is presentation only.
 *
 * The loop is gated on [resume]/[pause] and onDraw is wrapped so one bad frame can never
 * crash the app.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Worker(var x: Float, var y: Float, var ti: Float, var tj: Float, var i: Float, var j: Float, var spd: Float, var phase: Float, val kind: Int)
    private class Floater(var x: Float, var y: Float, val text: String, var age: Float, val life: Float, val color: Int, val size: Float, var vx: Float, var vy: Float)
    private class Cloud(var x: Float, val y: Float, val s: Float, val spd: Float)
    private class Star(val x: Float, val y: Float, val r: Float, val a: Int)

    private var def: WorldDef = Defs.WORLDS[0]
    private var workerCount = 5
    private var fillRatio = 0f
    private var goldSkin = false

    // Sprites (decoded once; null => a clean vector fallback is drawn instead).
    private val buildingBmps = arrayOfNulls<Bitmap>(4)
    private val workerBmps = arrayOfNulls<Bitmap>(4)
    private var artLoaded = false
    private val srcRect = Rect()
    private val dstRect = RectF()

    private val rnd = Random(7)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val workers = ArrayList<Worker>()
    private val floaters = ArrayList<Floater>()
    private val clouds = ArrayList<Cloud>()
    private val stars = ArrayList<Star>()
    private val byY = Comparator<Worker> { a, b -> a.y.compareTo(b.y) }

    private var skyShader: Shader? = null
    private var sunShader: Shader? = null
    private val clip = Path()
    private val topFace = Path()
    private var lastFrame = 0L
    private var running = false
    private var shakeMag = 0f

    // Isometric platform geometry (computed in rebuild).
    private var platCx = 0f
    private var platCy = 0f
    private var platHW = 0f
    private var platHH = 0f
    private var platDep = 0f
    private val bX = FloatArray(4)
    private val bY = FloatArray(4)

    // Active-play visuals (all transient; cleared on pause()).
    private var goldenX = 0f
    private var goldenY = 0f
    private var goldenLife = 0f
    private var goldenPulse = 0f
    private var goldenCooldown = 6f
    private var comboFrac = 0f
    private var comboTimeFrac = 0f
    private var comboMultShown = 1f
    private val GOLDEN = "⭐"

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
        loadArt()
        if (width > 0 && height > 0) rebuild()
        invalidate()
    }

    private fun loadArt() {
        if (artLoaded) return
        fun dec(id: Int): Bitmap? = try { BitmapFactory.decodeResource(resources, id) } catch (e: Throwable) { null }
        buildingBmps[0] = dec(R.drawable.sprite_bld_warehouse)
        buildingBmps[1] = dec(R.drawable.sprite_bld_workshop)
        buildingBmps[2] = dec(R.drawable.sprite_bld_lab)
        buildingBmps[3] = dec(R.drawable.sprite_bld_refinery)
        workerBmps[0] = dec(R.drawable.sprite_unit_miner)
        workerBmps[1] = dec(R.drawable.sprite_unit_carrier)
        workerBmps[2] = dec(R.drawable.sprite_unit_guard)
        workerBmps[3] = dec(R.drawable.sprite_unit_scientist)
        artLoaded = true
    }

    fun setFill(ratio: Float) { fillRatio = ratio.coerceIn(0f, 1f) }

    fun resume() {
        if (!running) { running = true; lastFrame = 0L; postOnAnimation(frame) }
    }

    fun pause() {
        running = false
        removeCallbacks(frame)
        goldenLife = 0f
        goldenCooldown = 4f
    }

    fun shake(mag: Float = dp(8f)) { shakeMag = mag }

    fun spawnCollect(label: String) {
        if (width == 0) return
        val cx = platCx
        val cy = platCy
        floaters.add(Floater(cx, cy, label, 0f, 1.2f, 0xFFFFCB3D.toInt(), dp(30f), 0f, 0f))
        for (i in 0 until 10) {
            val ang = rnd.nextFloat() * 6.2832f
            val sp = dp(150f) + rnd.nextFloat() * dp(140f)
            floaters.add(Floater(cx, cy, "💰", 0f, 0.9f + rnd.nextFloat() * 0.5f, 0, dp(16f),
                cos(ang.toDouble()).toFloat() * sp, -(dp(120f) + rnd.nextFloat() * dp(180f))))
        }
        shake(dp(5f))
    }

    fun setCombo(frac: Float, timeFrac: Float, mult: Float) {
        comboFrac = frac.coerceIn(0f, 1f)
        comboTimeFrac = timeFrac.coerceIn(0f, 1f)
        comboMultShown = mult.coerceAtLeast(1f)
    }

    fun goldenLive(): Boolean = goldenLife > 0f

    fun goldenHitTest(px: Float, py: Float): Boolean {
        if (goldenLife <= 0f) return false
        if (hypot(px - goldenX, py - goldenY) <= dp(36f)) {
            goldenLife = 0f
            goldenCooldown = 5f + rnd.nextFloat() * 7f
            return true
        }
        return false
    }

    private fun armGolden() {
        if (width == 0 || height == 0) return
        val i = (rnd.nextFloat() - 0.5f) * 1.4f
        val j = (rnd.nextFloat() - 0.5f) * 1.4f
        goldenX = isoX(i, j)
        goldenY = isoY(i, j) - dp(14f)
        goldenLife = 4.5f
        goldenPulse = 0f
    }

    fun spawnGolden(label: String) {
        if (width == 0) return
        val cx = if (goldenX > 0f) goldenX else platCx
        val cy = if (goldenY > 0f) goldenY else platCy
        floaters.add(Floater(cx, cy, label, 0f, 1.3f, 0xFFFFD24D.toInt(), dp(34f), 0f, 0f))
        for (i in 0 until 12) {
            val ang = rnd.nextFloat() * 6.2832f
            val sp = dp(170f) + rnd.nextFloat() * dp(160f)
            floaters.add(Floater(cx, cy, GOLDEN, 0f, 0.9f + rnd.nextFloat() * 0.5f, 0, dp(15f),
                cos(ang.toDouble()).toFloat() * sp, -(dp(140f) + rnd.nextFloat() * dp(200f))))
        }
        shake(dp(9f))
    }

    fun spawnCrit(label: String) {
        if (width == 0) return
        floaters.add(Floater(platCx, platCy - dp(30f), label, 0f, 1.1f, 0xFFFF5A4D.toInt(), dp(26f), 0f, 0f))
    }

    fun spawnCelebrate() {
        if (width == 0) return
        val cx = platCx
        val cy = platCy - dp(20f)
        floaters.add(Floater(cx, cy, "⭐", 0f, 1.4f, 0xFFFFCB3D.toInt(), dp(40f), 0f, 0f))
        for (i in 0 until 14) {
            val ang = rnd.nextFloat() * 6.2832f
            val sp = dp(120f) + rnd.nextFloat() * dp(160f)
            val g = if (i % 2 == 0) "✨" else "⭐"
            floaters.add(Floater(cx, cy, g, 0f, 1.0f + rnd.nextFloat() * 0.6f, 0, dp(18f),
                cos(ang.toDouble()).toFloat() * sp, -(dp(140f) + rnd.nextFloat() * dp(200f))))
        }
        shake(dp(7f))
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { rebuild() }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun isoX(i: Float, j: Float): Float = platCx + (i - j) * platHW * 0.5f
    private fun isoY(i: Float, j: Float): Float = platCy + (i + j) * platHH * 0.5f

    private fun rebuild() {
        val w = width.toFloat()
        val h = height.toFloat()

        skyShader = LinearGradient(0f, 0f, 0f, h, lighten(def.skyTop, 0.10f), def.skyBottom, Shader.TileMode.CLAMP)
        sunShader = RadialGradient(w * 0.78f, h * 0.20f, h * 0.34f,
            (0x66 shl 24) or (def.accent and 0x00FFFFFF), def.accent and 0x00FFFFFF, Shader.TileMode.CLAMP)

        val r = dp(20f)
        clip.reset()
        clip.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)

        // Platform footprint.
        platHW = w * 0.40f
        platHH = platHW * 0.52f
        platDep = dp(20f)
        platCx = w * 0.5f
        platCy = h * 0.60f

        topFace.reset()
        topFace.moveTo(platCx, platCy - platHH)
        topFace.lineTo(platCx + platHW, platCy)
        topFace.lineTo(platCx, platCy + platHH)
        topFace.lineTo(platCx - platHW, platCy)
        topFace.close()

        // Four building anchors on the platform.
        val anch = arrayOf(floatArrayOf(-0.5f, -0.5f), floatArrayOf(0.5f, -0.5f), floatArrayOf(-0.5f, 0.5f), floatArrayOf(0.5f, 0.5f))
        for (k in 0 until 4) { bX[k] = isoX(anch[k][0], anch[k][1]); bY[k] = isoY(anch[k][0], anch[k][1]) }

        stars.clear()
        for (i in 0 until 22) stars.add(Star(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.5f, dp(0.8f) + rnd.nextFloat() * dp(1.6f), 50 + rnd.nextInt(110)))

        clouds.clear()
        for (i in 0 until 3) clouds.add(Cloud(rnd.nextFloat() * w, h * (0.08f + rnd.nextFloat() * 0.22f), 0.7f + rnd.nextFloat() * 0.7f, dp(6f) + rnd.nextFloat() * dp(8f)))

        workers.clear()
        for (i in 0 until workerCount) {
            val wk = Worker(platCx, platCy, 0f, 0f, 0f, 0f, 0f, rnd.nextFloat() * 6.28f, i % 4)
            pickTarget(wk)
            wk.i = wk.ti; wk.j = wk.tj
            wk.x = isoX(wk.i, wk.j); wk.y = isoY(wk.i, wk.j)
            workers.add(wk)
        }
    }

    private fun pickTarget(wk: Worker) {
        wk.ti = (rnd.nextFloat() - 0.5f) * 1.5f
        wk.tj = (rnd.nextFloat() - 0.5f) * 1.5f
        wk.spd = 0.25f + rnd.nextFloat() * 0.4f
    }

    private fun step() {
        val now = System.nanoTime()
        var dt = if (lastFrame == 0L) 0.016f else (now - lastFrame) / 1_000_000_000f
        lastFrame = now
        if (dt > 0.05f) dt = 0.05f

        if (shakeMag > 0f) shakeMag = (shakeMag - dp(60f) * dt).coerceAtLeast(0f)

        for (c in clouds) {
            c.x += c.spd * dt
            if (c.x - dp(60f) * c.s > width) c.x = -dp(60f) * c.s
        }

        for (wk in workers) {
            val di = wk.ti - wk.i
            val dj = wk.tj - wk.j
            val d = hypot(di, dj)
            if (d < 0.04f) {
                pickTarget(wk)
            } else {
                val v = wk.spd * dt
                wk.i += di / d * v
                wk.j += dj / d * v
            }
            wk.x = isoX(wk.i, wk.j)
            wk.y = isoY(wk.i, wk.j)
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

        if (goldenLife > 0f) {
            goldenLife -= dt
            goldenPulse += dt * 6f
            if (goldenLife <= 0f) goldenCooldown = 5f + rnd.nextFloat() * 7f
        } else {
            goldenCooldown -= dt
            if (goldenCooldown <= 0f && Math.random() < 0.012) armGolden()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return

        val saved = canvas.save()
        try {
            canvas.clipPath(clip)
            if (shakeMag > 0f) canvas.translate((rnd.nextFloat() - 0.5f) * shakeMag, (rnd.nextFloat() - 0.5f) * shakeMag)

            // --- sky ---
            paint.style = Paint.Style.FILL
            paint.shader = skyShader
            canvas.drawRect(-shakeMag, -shakeMag, w + shakeMag, h + shakeMag, paint)
            paint.shader = sunShader
            canvas.drawRect(-shakeMag, -shakeMag, w + shakeMag, h * 0.6f, paint)
            paint.shader = null

            for (s in stars) {
                paint.color = (s.a shl 24) or 0x00FFFFFF
                canvas.drawCircle(s.x, s.y, s.r, paint)
            }
            for (c in clouds) drawCloud(canvas, c)

            // --- platform (drop shadow + side faces + top) ---
            paint.color = 0x33000000
            canvas.drawOval(platCx - platHW * 0.92f, platCy + platHH * 0.55f,
                platCx + platHW * 0.92f, platCy + platHH * 1.15f, paint)

            // left + right side faces for thickness
            paint.color = shade(def.ground, -0.42f)
            sideQuad(canvas, platCx - platHW, platCy, platCx, platCy + platHH)
            paint.color = shade(def.ground, -0.26f)
            sideQuad(canvas, platCx, platCy + platHH, platCx + platHW, platCy)

            // top face + rim highlight
            paint.color = shade(def.ground, 0.10f)
            canvas.drawPath(topFace, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2f)
            paint.color = shade(def.ground, 0.30f)
            canvas.drawPath(topFace, paint)
            paint.style = Paint.Style.FILL

            // --- buildings (back-to-front) ---
            val order = intArrayOf(0, 1, 2, 3).sortedBy { bY[it] }
            for (k in order) drawBuilding(canvas, k)

            // --- gold stockpile (uncollected production) ---
            drawStockpile(canvas)

            // --- golden bonus node ---
            if (goldenLife > 0f) {
                val pulse = 1f + 0.12f * sin(goldenPulse.toDouble()).toFloat()
                paint.color = 0x55FFD24D
                canvas.drawCircle(goldenX, goldenY, dp(26f) * pulse, paint)
                paint.color = 0xAAFFF0A0.toInt()
                canvas.drawCircle(goldenX, goldenY, dp(18f) * pulse, paint)
                drawGlyph(canvas, GOLDEN, goldenX, goldenY, dp(30f) * pulse)
            }

            // --- workers ---
            for (wk in workers) drawWorker(canvas, wk)

            // --- floaters ---
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

            // --- combo bar ---
            if (comboFrac > 0f) {
                val bw = w * 0.42f; val bh = dp(7f); val bx = dp(10f); val by = dp(10f)
                paint.color = 0x66000000
                canvas.drawRoundRect(bx, by, bx + bw, by + bh, bh, bh, paint)
                val hot = (1f - comboTimeFrac).coerceIn(0f, 1f)
                val rC = (0x43 + (0xC4 - 0x43) * hot).toInt()
                paint.color = (0xFF shl 24) or (rC shl 16) or (0xC4 shl 8) or 0x63
                canvas.drawRoundRect(bx, by, bx + bw * comboFrac, by + bh, bh, bh, paint)
                glyph.textSize = dp(13f); glyph.isFakeBoldText = true
                glyph.color = 0xFFFFF0A0.toInt(); glyph.textAlign = Paint.Align.LEFT
                canvas.drawText("×" + String.format(java.util.Locale.US, "%.2f", comboMultShown), bx, by + bh + dp(14f), glyph)
                glyph.textAlign = Paint.Align.CENTER; glyph.isFakeBoldText = false
            }
        } catch (e: Throwable) {
            // never crash on a bad frame
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun sideQuad(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val p = Path()
        p.moveTo(x1, y1)
        p.lineTo(x2, y2)
        p.lineTo(x2, y2 + platDep)
        p.lineTo(x1, y1 + platDep)
        p.close()
        canvas.drawPath(p, paint)
    }

    private fun drawCloud(canvas: Canvas, c: Cloud) {
        paint.color = 0x4CFFFFFF
        val s = c.s
        canvas.drawOval(c.x, c.y, c.x + dp(58f) * s, c.y + dp(26f) * s, paint)
        canvas.drawOval(c.x + dp(20f) * s, c.y - dp(12f) * s, c.x + dp(58f) * s, c.y + dp(18f) * s, paint)
        canvas.drawOval(c.x + dp(34f) * s, c.y, c.x + dp(78f) * s, c.y + dp(26f) * s, paint)
    }

    private fun drawBuilding(canvas: Canvas, k: Int) {
        val bx = bX[k]; val by = bY[k]
        // soft shadow
        paint.color = 0x40000000
        canvas.drawOval(bx - dp(26f), by - dp(4f), bx + dp(26f), by + dp(10f), paint)
        val bmp = buildingBmps[k]
        if (bmp != null && !bmp.isRecycled) {
            val bw = dp(74f)
            val bh = bw * bmp.height / bmp.width.toFloat()
            srcRect.set(0, 0, bmp.width, bmp.height)
            dstRect.set(bx - bw / 2f, by + dp(8f) - bh, bx + bw / 2f, by + dp(8f))
            paint.alpha = 255; paint.isFilterBitmap = true
            canvas.drawBitmap(bmp, srcRect, dstRect, paint)
        } else {
            // vector fallback: a little house
            paint.color = shade(def.accent, -0.1f)
            canvas.drawRect(bx - dp(18f), by - dp(28f), bx + dp(18f), by + dp(4f), paint)
            paint.color = 0xFFCB5A3C.toInt()
            val roof = Path()
            roof.moveTo(bx - dp(22f), by - dp(26f)); roof.lineTo(bx, by - dp(46f)); roof.lineTo(bx + dp(22f), by - dp(26f)); roof.close()
            canvas.drawPath(roof, paint)
        }
    }

    private fun drawStockpile(canvas: Canvas) {
        if (fillRatio <= 0.02f) return
        val cx = platCx
        val cy = platCy + dp(4f)
        val scale = 0.5f + fillRatio
        val coins = (3 + fillRatio * 7f).toInt()
        for (n in 0 until coins) {
            val ang = n * 2.39996f
            val rad = dp(4f) + n * dp(2.1f) * scale
            val gx = cx + cos(ang.toDouble()).toFloat() * rad
            val gy = cy + sin(ang.toDouble()).toFloat() * rad * 0.5f - n * dp(0.7f)
            paint.color = 0x33000000
            canvas.drawCircle(gx, gy + dp(2f), dp(6f) * scale, paint)
            paint.color = 0xFFFFC83D.toInt()
            canvas.drawCircle(gx, gy, dp(6f) * scale, paint)
            paint.color = 0xFFFFE9A8.toInt()
            canvas.drawCircle(gx - dp(1.5f), gy - dp(1.5f), dp(2.4f) * scale, paint)
        }
        if (fillRatio >= 0.999f) {
            paint.color = 0x33FFF0A0
            canvas.drawCircle(cx, cy - dp(6f), dp(30f), paint)
        }
    }

    private fun drawWorker(canvas: Canvas, wk: Worker) {
        val bob = sin(wk.phase.toDouble()).toFloat() * dp(2f)
        paint.color = 0x44000000
        canvas.drawOval(wk.x - dp(9f), wk.y + dp(4f), wk.x + dp(9f), wk.y + dp(9f), paint)
        if (goldSkin) {
            paint.color = 0x66FFCB3D.toInt()
            canvas.drawCircle(wk.x, wk.y + bob - dp(10f), dp(13f), paint)
        }
        val wb = workerBmps[wk.kind]
        if (wb != null && !wb.isRecycled) {
            val sz = dp(34f)
            val bottom = wk.y + bob + dp(6f)
            srcRect.set(0, 0, wb.width, wb.height)
            dstRect.set(wk.x - sz / 2f, bottom - sz, wk.x + sz / 2f, bottom)
            paint.alpha = 255; paint.isFilterBitmap = true
            canvas.drawBitmap(wb, srcRect, dstRect, paint)
        } else {
            drawGlyph(canvas, def.worker, wk.x, wk.y + bob - dp(8f), dp(22f))
        }
    }

    private fun drawGlyph(canvas: Canvas, s: String, cx: Float, cy: Float, size: Float, alpha: Int = 255) {
        glyph.textSize = size
        glyph.alpha = alpha
        canvas.drawText(s, cx, cy + size * 0.35f, glyph)
        glyph.alpha = 255
    }

    /** Blend a colour toward white (f>0) or black (f<0). */
    private fun shade(c: Int, f: Float): Int {
        val a = (c ushr 24) and 0xFF
        var r = (c ushr 16) and 0xFF
        var g = (c ushr 8) and 0xFF
        var b = c and 0xFF
        if (f >= 0f) {
            r = (r + (255 - r) * f).toInt(); g = (g + (255 - g) * f).toInt(); b = (b + (255 - b) * f).toInt()
        } else {
            val k = 1f + f; r = (r * k).toInt(); g = (g * k).toInt(); b = (b * k).toInt()
        }
        return (a shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun lighten(c: Int, f: Float): Int = shade(c, f)

    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); pause() }
}
