package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.TrackId
import com.miniproject.marbleshooter.game.domain.model.Vec2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Shared Paint/Path cache. Renderers share one instance and are used on the UI thread. */
internal class GameRenderResources {
    internal val palette = intArrayOf(
        Color.rgb(255, 91, 105),
        Color.rgb(255, 194, 67),
        Color.rgb(67, 205, 151),
        Color.rgb(84, 164, 255),
        Color.rgb(184, 125, 255),
        Color.rgb(255, 129, 207)
    )
    internal val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val backgroundShader = LinearGradient(
        0f, 0f, 0f, GameEngine.WORLD_HEIGHT,
        Color.rgb(24, 38, 68), Color.rgb(12, 18, 36), Shader.TileMode.CLAMP
    )
    internal val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    internal val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    internal val trackPath = Path()
    internal var cachedTrack: TrackId? = null

    internal companion object {
        val GREEN = Color.rgb(66, 194, 130)
        val BLUE = Color.rgb(65, 92, 135)
        val MUTED = Color.rgb(174, 193, 224)
    }
}

/** Common Canvas primitives shared by focused screen renderers. */
abstract class GameCanvasRendererBase internal constructor(
    internal val drawing: GameRenderResources
) {
    protected val palette get() = drawing.palette
    protected val backgroundPaint get() = drawing.backgroundPaint
    protected val backgroundShader get() = drawing.backgroundShader
    protected val fillPaint get() = drawing.fillPaint
    protected val strokePaint get() = drawing.strokePaint
    protected val textPaint get() = drawing.textPaint
    protected val trackPath get() = drawing.trackPath
    protected var cachedTrack: TrackId?
        get() = drawing.cachedTrack
        set(value) { drawing.cachedTrack = value }
    protected val GREEN get() = GameRenderResources.GREEN
    protected val BLUE get() = GameRenderResources.BLUE
    protected val MUTED get() = GameRenderResources.MUTED

    protected fun drawBackground(canvas: Canvas) {
        backgroundPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, backgroundPaint)
        backgroundPaint.shader = null
        fillPaint.color = Color.argb(28, 255, 255, 255)
        for (index in 0 until 26) {
            val x = ((index * 73) % 410 + 5).toFloat()
            val y = ((index * 137) % 880 + 8).toFloat()
            canvas.drawCircle(x, y, if (index % 3 == 0) 2.2f else 1.2f, fillPaint)
        }
    }

    protected fun drawMarble(canvas: Canvas, x: Float, y: Float, colorIndex: Int, radius: Float) {
        val color = palette[colorIndex.coerceIn(0, palette.lastIndex)]
        fillPaint.color = Color.argb(85, 0, 0, 0)
        canvas.drawCircle(x + 2f, y + 4f, radius + 1f, fillPaint)
        fillPaint.color = darken(color)
        canvas.drawCircle(x, y, radius + 1.5f, fillPaint)
        fillPaint.color = color
        canvas.drawCircle(x, y, radius, fillPaint)
        fillPaint.color = Color.argb(165, 255, 255, 255)
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.35f, radius * 0.28f, fillPaint)
        fillPaint.color = Color.argb(65, 255, 255, 255)
        canvas.drawCircle(x + radius * 0.25f, y + radius * 0.28f, radius * 0.18f, fillPaint)
    }

    protected fun drawFrog(canvas: Canvas, center: Vec2, target: Vec2, colorIndex: Int) {
        val direction = (target - center).normalized()
        val rotation = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat() + 90f
        canvas.save()
        canvas.translate(center.x, center.y)
        canvas.rotate(rotation)
        strokePaint.color = Color.rgb(17, 78, 66)
        strokePaint.strokeWidth = 25f
        canvas.drawLine(0f, -2f, 0f, -56f, strokePaint)
        fillPaint.color = Color.rgb(11, 80, 65)
        canvas.drawCircle(0f, 3f, 39f, fillPaint)
        fillPaint.color = Color.rgb(65, 203, 139)
        canvas.drawCircle(0f, 0f, 34f, fillPaint)
        fillPaint.color = Color.rgb(44, 158, 111)
        canvas.drawOval(RectF(-27f, 10f, 27f, 32f), fillPaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(-15f, -25f, 11f, fillPaint)
        canvas.drawCircle(15f, -25f, 11f, fillPaint)
        fillPaint.color = Color.rgb(19, 42, 48)
        canvas.drawCircle(-13f, -25f, 4.5f, fillPaint)
        canvas.drawCircle(17f, -25f, 4.5f, fillPaint)
        strokePaint.color = Color.rgb(20, 79, 61)
        strokePaint.strokeWidth = 3f
        canvas.drawArc(RectF(-13f, -2f, 13f, 17f), 10f, 160f, false, strokePaint)
        drawMarble(canvas, 0f, -53f, colorIndex, 13f)
        canvas.restore()
    }

    protected fun button(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, label: String, color: Int) {
        fillPaint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, fillPaint)
        centeredText(canvas, label, if (label.length > 18) 13f else 15f, Color.WHITE, (left + right) / 2f, (top + bottom) / 2f + 5f)
    }

    protected fun centeredText(canvas: Canvas, text: String, size: Float, color: Int, x: Float, y: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = size
        textPaint.color = color
        canvas.drawText(text, x, y, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    protected fun drawParagraph(canvas: Canvas, text: String, size: Float, color: Int, bounds: RectF) {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = size
        textPaint.color = color
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textPaint.measureText(candidate) > bounds.width() && current.isNotEmpty()) {
                lines += current
                current = word
            } else current = candidate
        }
        if (current.isNotEmpty()) lines += current
        val lineHeight = size * 1.4f
        val maxLines = (bounds.height() / lineHeight).toInt().coerceAtLeast(1)
        val visibleLines = lines.take(maxLines).toMutableList()
        if (lines.size > maxLines && visibleLines.isNotEmpty()) {
            var last = visibleLines.last()
            while (last.isNotEmpty() && textPaint.measureText(last + "…") > bounds.width()) {
                last = last.dropLast(1)
            }
            visibleLines[visibleLines.lastIndex] = if (last.isEmpty()) "…" else last + "…"
        }
        visibleLines.forEachIndexed { index, line ->
            canvas.drawText(line, bounds.left, bounds.top + size + index * lineHeight, textPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    protected fun drawArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 5f
        canvas.drawLine(x1, y1, x2, y2, strokePaint)
        val angle = atan2(y2 - y1, x2 - x1)
        val left = Vec2(x2 - 13f * cos(angle - 0.55f), y2 - 13f * sin(angle - 0.55f))
        val right = Vec2(x2 - 13f * cos(angle + 0.55f), y2 - 13f * sin(angle + 0.55f))
        canvas.drawLine(x2, y2, left.x, left.y, strokePaint)
        canvas.drawLine(x2, y2, right.x, right.y, strokePaint)
    }

    protected fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.68f).toInt(),
        (Color.green(color) * 0.68f).toInt(),
        (Color.blue(color) * 0.68f).toInt()
    )

    protected fun starsLine(stars: Int): String = buildString {
        repeat(3) { append(if (it < stars) "★" else "☆") }
    }

    protected fun tr(state: GameRenderState, russian: String, english: String): String =
        if (state.language == "en") english else russian
}
