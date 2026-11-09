package com.miniproject.marbleshooter.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val engine = GameEngine()
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
    private var bestScore = preferences.getInt("best_score", 0)
    private var isActive = false
    private var lastFrameNanos = 0L

    private val palette = intArrayOf(
        Color.rgb(255, 91, 105),
        Color.rgb(255, 194, 67),
        Color.rgb(67, 205, 151),
        Color.rgb(84, 164, 255),
        Color.rgb(184, 125, 255)
    )

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val routePath = Path().apply {
        engine.routePoints.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        isFocusable = true
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun resumeGame() {
        isActive = true
        lastFrameNanos = System.nanoTime()
        postInvalidateOnAnimation()
    }

    fun pauseGame() {
        isActive = false
        lastFrameNanos = 0L
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        if (isActive) {
            val now = System.nanoTime()
            val delta = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now
            engine.update(delta)
            saveBestScoreIfNeeded()
        }

        canvas.drawColor(Color.rgb(15, 22, 42))
        val scale = min(width / GameEngine.WORLD_WIDTH, height / GameEngine.WORLD_HEIGHT)
        scaleFactor = scale
        offsetX = (width - GameEngine.WORLD_WIDTH * scale) / 2f
        offsetY = (height - GameEngine.WORLD_HEIGHT * scale) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        drawBackground(canvas)
        drawTrack(canvas)
        drawBalls(canvas)
        drawProjectiles(canvas)
        drawShooter(canvas)
        drawHud(canvas)
        drawBottomHint(canvas)
        if (engine.status != GameStatus.RUNNING) drawEndOverlay(canvas)

        canvas.restore()

        if (isActive && engine.status == GameStatus.RUNNING) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, GameEngine.WORLD_HEIGHT,
            Color.rgb(24, 38, 68),
            Color.rgb(12, 18, 36),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, backgroundPaint)
        backgroundPaint.shader = null

        fillPaint.color = Color.argb(30, 255, 255, 255)
        for (index in 0 until 24) {
            val x = ((index * 73) % 410 + 5).toFloat()
            val y = ((index * 137) % 880 + 8).toFloat()
            canvas.drawCircle(x, y, if (index % 3 == 0) 2.2f else 1.2f, fillPaint)
        }
    }

    private fun drawTrack(canvas: Canvas) {
        strokePaint.color = Color.rgb(9, 14, 27)
        strokePaint.strokeWidth = 62f
        canvas.drawPath(routePath, strokePaint)

        strokePaint.color = Color.rgb(53, 68, 95)
        strokePaint.strokeWidth = 50f
        canvas.drawPath(routePath, strokePaint)

        strokePaint.color = Color.rgb(92, 111, 144)
        strokePaint.strokeWidth = 2f
        canvas.drawPath(routePath, strokePaint)

        val start = engine.routePoints.first()
        fillPaint.color = Color.rgb(109, 229, 157)
        canvas.drawCircle(start.x, start.y, 11f, fillPaint)
        textPaint.textSize = 13f
        textPaint.color = Color.rgb(190, 255, 211)
        canvas.drawText("СТАРТ", start.x - 23f, start.y - 35f, textPaint)

        val finish = engine.routePoints.last()
        fillPaint.color = Color.rgb(255, 100, 110)
        canvas.drawCircle(finish.x, finish.y, 13f, fillPaint)
        textPaint.color = Color.rgb(255, 190, 194)
        canvas.drawText("ФИНИШ", finish.x - 29f, finish.y + 39f, textPaint)
        textPaint.color = Color.WHITE
    }

    private fun drawBalls(canvas: Canvas) {
        for (ball in engine.balls) {
            val position = engine.positionOnRoute(ball.progress)
            val color = palette[ball.colorIndex % palette.size]

            fillPaint.color = Color.argb(80, 0, 0, 0)
            canvas.drawCircle(position.x + 2f, position.y + 4f, GameEngine.BALL_RADIUS + 1f, fillPaint)

            fillPaint.color = darken(color)
            canvas.drawCircle(position.x, position.y, GameEngine.BALL_RADIUS + 1.5f, fillPaint)
            fillPaint.color = color
            canvas.drawCircle(position.x, position.y, GameEngine.BALL_RADIUS, fillPaint)

            fillPaint.color = Color.argb(145, 255, 255, 255)
            canvas.drawCircle(position.x - 5f, position.y - 6f, 4.8f, fillPaint)
            fillPaint.color = Color.argb(65, 255, 255, 255)
            canvas.drawCircle(position.x + 5f, position.y + 6f, 3.2f, fillPaint)
        }
    }

    private fun drawProjectiles(canvas: Canvas) {
        for (projectile in engine.projectiles) {
            fillPaint.color = Color.argb(70, 255, 255, 255)
            canvas.drawCircle(projectile.position.x, projectile.position.y, 15f, fillPaint)
            fillPaint.color = palette[projectile.colorIndex % palette.size]
            canvas.drawCircle(projectile.position.x, projectile.position.y, 10f, fillPaint)
            fillPaint.color = Color.argb(180, 255, 255, 255)
            canvas.drawCircle(projectile.position.x - 3f, projectile.position.y - 3f, 3f, fillPaint)
        }
    }

    private fun drawShooter(canvas: Canvas) {
        val center = engine.shooterPosition
        val direction = (engine.aimTarget - center).normalized()

        strokePaint.color = Color.rgb(17, 78, 66)
        strokePaint.strokeWidth = 27f
        canvas.drawLine(
            center.x + direction.x * 7f,
            center.y - 10f + direction.y * 7f,
            center.x + direction.x * 63f,
            center.y - 10f + direction.y * 63f,
            strokePaint
        )

        fillPaint.color = Color.rgb(11, 80, 65)
        canvas.drawCircle(center.x, center.y + 3f, 39f, fillPaint)
        fillPaint.color = Color.rgb(65, 203, 139)
        canvas.drawCircle(center.x, center.y, 34f, fillPaint)
        fillPaint.color = Color.rgb(44, 158, 111)
        canvas.drawOval(RectF(center.x - 27f, center.y + 10f, center.x + 27f, center.y + 32f), fillPaint)

        fillPaint.color = Color.WHITE
        canvas.drawCircle(center.x - 15f, center.y - 25f, 11f, fillPaint)
        canvas.drawCircle(center.x + 15f, center.y - 25f, 11f, fillPaint)
        fillPaint.color = Color.rgb(19, 42, 48)
        canvas.drawCircle(center.x - 13f, center.y - 25f, 4.5f, fillPaint)
        canvas.drawCircle(center.x + 17f, center.y - 25f, 4.5f, fillPaint)

        strokePaint.color = Color.rgb(20, 79, 61)
        strokePaint.strokeWidth = 3f
        canvas.drawArc(
            RectF(center.x - 13f, center.y - 2f, center.x + 13f, center.y + 17f),
            10f, 160f, false, strokePaint
        )
    }

    private fun drawHud(canvas: Canvas) {
        fillPaint.color = Color.argb(150, 7, 13, 29)
        canvas.drawRoundRect(RectF(18f, 22f, 402f, 128f), 25f, 25f, fillPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 14f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("MARBLE FROG", 39f, 52f, textPaint)

        textPaint.textSize = 30f
        textPaint.color = Color.WHITE
        canvas.drawText(engine.score.toString(), 39f, 91f, textPaint)

        textPaint.textSize = 13f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("РЕКОРД  $bestScore", 39f, 113f, textPaint)

        textPaint.textSize = 12f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("СЛЕДУЮЩИЙ", 272f, 54f, textPaint)

        drawAmmoBadge(canvas, 315f, 91f, engine.nextColor, 18f)
        drawAmmoBadge(canvas, 365f, 91f, engine.ammoColor, 18f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 10f
        textPaint.color = Color.rgb(202, 215, 239)
        canvas.drawText("NEXT", 315f, 121f, textPaint)
        canvas.drawText("ТЕКУЩИЙ", 365f, 121f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
    }

    private fun drawAmmoBadge(canvas: Canvas, x: Float, y: Float, colorIndex: Int, radius: Float) {
        fillPaint.color = Color.argb(100, 255, 255, 255)
        canvas.drawCircle(x, y, radius + 4f, fillPaint)
        fillPaint.color = palette[colorIndex % palette.size]
        canvas.drawCircle(x, y, radius, fillPaint)
        fillPaint.color = Color.argb(170, 255, 255, 255)
        canvas.drawCircle(x - 6f, y - 6f, 5f, fillPaint)
    }

    private fun drawBottomHint(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f
        textPaint.color = Color.rgb(174, 193, 224)
        canvas.drawText("НАЖМИ НА ПОЛЕ, ЧТОБЫ ВЫСТРЕЛИТЬ", 210f, 882f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawEndOverlay(canvas: Canvas) {
        fillPaint.color = Color.argb(205, 5, 10, 24)
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, fillPaint)

        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(35f, 315f, 385f, 590f), 30f, 30f, fillPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 31f
        textPaint.color = if (engine.status == GameStatus.WON) {
            Color.rgb(113, 235, 163)
        } else {
            Color.WHITE
        }
        canvas.drawText(
            if (engine.status == GameStatus.WON) "ПОБЕДА!" else "ИГРА ОКОНЧЕНА",
            210f, 390f, textPaint
        )

        textPaint.textSize = 19f
        textPaint.color = Color.rgb(199, 213, 239)
        canvas.drawText("СЧЁТ:  " + engine.score, 210f, 445f, textPaint)

        fillPaint.color = Color.rgb(66, 194, 130)
        canvas.drawRoundRect(RectF(86f, 490f, 334f, 552f), 22f, 22f, fillPaint)
        textPaint.textSize = 18f
        textPaint.color = Color.rgb(10, 39, 31)
        canvas.drawText("ИГРАТЬ СНОВА", 210f, 529f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
    }

    private fun saveBestScoreIfNeeded() {
        if (engine.score > bestScore) {
            bestScore = engine.score
            preferences.edit().putInt("best_score", bestScore).apply()
        }
    }

    private fun darken(color: Int): Int {
        val red = (Color.red(color) * 0.68f).toInt()
        val green = (Color.green(color) * 0.68f).toInt()
        val blue = (Color.blue(color) * 0.68f).toInt()
        return Color.rgb(red, green, blue)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        if (scaleFactor <= 0f) return true

        val x = (event.x - offsetX) / scaleFactor
        val y = (event.y - offsetY) / scaleFactor
        engine.onTap(Vec2(x, y))
        invalidate()
        return true
    }
}
