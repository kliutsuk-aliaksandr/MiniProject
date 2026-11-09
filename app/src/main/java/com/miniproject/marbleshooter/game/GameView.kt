package com.miniproject.marbleshooter.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class BurstParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var life: Float,
    val maxLife: Float,
    val color: Int,
    val radius: Float
)

class GameView(context: Context) : View(context) {

    private val engine = GameEngine()
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
    private val audio = GameAudio()
    private var bestScore = preferences.getInt("best_score", 0)
    private var soundEnabled = preferences.getBoolean("sound_enabled", true)
    private var isActive = false
    private var lastFrameNanos = 0L
    private val particles = mutableListOf<BurstParticle>()

    private val palette = intArrayOf(
        Color.rgb(255, 91, 105),
        Color.rgb(255, 194, 67),
        Color.rgb(67, 205, 151),
        Color.rgb(84, 164, 255),
        Color.rgb(184, 125, 255),
        Color.rgb(255, 129, 207)
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
        restoreSavedGame()
        engine.drainEvents()
    }

    fun resumeGame() {
        isActive = true
        lastFrameNanos = System.nanoTime()
        postInvalidateOnAnimation()
    }

    fun pauseGame() {
        isActive = false
        lastFrameNanos = 0L
        saveGameState()
    }

    fun disposeGame() {
        saveGameState()
        audio.release()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        var delta = 0f
        if (isActive) {
            val now = System.nanoTime()
            delta = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now
            engine.update(delta)
            updateParticles(delta)
            handleGameEvents(engine.drainEvents())
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
        drawParticles(canvas)
        drawShooter(canvas)
        drawHud(canvas)
        drawBottomHint(canvas)

        when (engine.status) {
            GameStatus.PAUSED -> drawPauseOverlay(canvas)
            GameStatus.WON, GameStatus.LOST -> drawEndOverlay(canvas)
            GameStatus.RUNNING -> Unit
        }

        canvas.restore()

        if (isActive && engine.status == GameStatus.RUNNING) {
            postInvalidateOnAnimation()
        } else if (isActive && particles.isNotEmpty()) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundPaint.shader = LinearGradient(
            0f, 0f, 0f, GameEngine.WORLD_HEIGHT,
            Color.rgb(24, 38, 68),
            Color.rgb(12, 18, 36),
            Shader.TileMode.CLAMP
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

    private fun drawParticles(canvas: Canvas) {
        for (particle in particles) {
            val fraction = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            fillPaint.color = Color.argb((fraction * 255f).toInt(), Color.red(particle.color),
                Color.green(particle.color), Color.blue(particle.color))
            canvas.drawCircle(particle.x, particle.y, particle.radius * (0.55f + fraction * 0.45f), fillPaint)
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
        fillPaint.color = Color.argb(165, 7, 13, 29)
        canvas.drawRoundRect(RectF(18f, 22f, 402f, 128f), 25f, 25f, fillPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 14f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("MARBLE FROG", 34f, 51f, textPaint)

        textPaint.textSize = 12f
        textPaint.color = Color.rgb(111, 228, 164)
        canvas.drawText("УРОВЕНЬ " + engine.level, 34f, 69f, textPaint)

        textPaint.textSize = 29f
        textPaint.color = Color.WHITE
        canvas.drawText(engine.score.toString(), 34f, 101f, textPaint)

        textPaint.textSize = 11f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("РЕКОРД  " + bestScore, 34f, 119f, textPaint)

        drawHudButton(canvas, 322f, 46f, soundEnabled, true)
        drawHudButton(canvas, 374f, 46f, engine.status == GameStatus.PAUSED, false)

        textPaint.textSize = 11f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("СЛЕДУЮЩИЙ", 275f, 75f, textPaint)

        drawAmmoBadge(canvas, 315f, 103f, engine.nextColor, 16f)
        drawAmmoBadge(canvas, 370f, 103f, engine.ammoColor, 16f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        textPaint.color = Color.rgb(202, 215, 239)
        canvas.drawText("NEXT", 315f, 126f, textPaint)
        canvas.drawText("ТЕКУЩИЙ", 370f, 126f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
    }

    private fun drawHudButton(canvas: Canvas, x: Float, y: Float, active: Boolean, isSound: Boolean) {
        fillPaint.color = if (active) Color.rgb(63, 103, 111) else Color.rgb(37, 52, 79)
        canvas.drawRoundRect(RectF(x - 19f, y - 16f, x + 19f, y + 16f), 11f, 11f, fillPaint)

        strokePaint.color = Color.rgb(207, 225, 244)
        strokePaint.strokeWidth = 2.4f
        if (isSound) {
            val speaker = Path().apply {
                moveTo(x - 9f, y - 5f)
                lineTo(x - 4f, y - 5f)
                lineTo(x + 3f, y - 11f)
                lineTo(x + 3f, y + 11f)
                lineTo(x - 4f, y + 5f)
                lineTo(x - 9f, y + 5f)
                close()
            }
            fillPaint.color = Color.rgb(220, 235, 249)
            canvas.drawPath(speaker, fillPaint)
            strokePaint.color = if (active) Color.rgb(135, 244, 183) else Color.rgb(220, 235, 249)
            canvas.drawArc(RectF(x, y - 8f, x + 12f, y + 8f), -65f, 130f, false, strokePaint)
            if (!active) {
                canvas.drawLine(x - 12f, y - 12f, x + 13f, y + 12f, strokePaint)
            }
        } else {
            fillPaint.color = Color.rgb(220, 235, 249)
            if (engine.status == GameStatus.PAUSED) {
                val triangle = Path().apply {
                    moveTo(x - 5f, y - 9f)
                    lineTo(x + 8f, y)
                    lineTo(x - 5f, y + 9f)
                    close()
                }
                canvas.drawPath(triangle, fillPaint)
            } else {
                canvas.drawRoundRect(RectF(x - 7f, y - 9f, x - 2f, y + 9f), 2f, 2f, fillPaint)
                canvas.drawRoundRect(RectF(x + 2f, y - 9f, x + 7f, y + 9f), 2f, 2f, fillPaint)
            }
        }
    }

    private fun drawAmmoBadge(canvas: Canvas, x: Float, y: Float, colorIndex: Int, radius: Float) {
        fillPaint.color = Color.argb(100, 255, 255, 255)
        canvas.drawCircle(x, y, radius + 4f, fillPaint)
        fillPaint.color = palette[colorIndex.coerceIn(0, palette.lastIndex)]
        canvas.drawCircle(x, y, radius, fillPaint)
        fillPaint.color = Color.argb(170, 255, 255, 255)
        canvas.drawCircle(x - 5f, y - 5f, 4f, fillPaint)
    }

    private fun drawBottomHint(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f
        textPaint.color = Color.rgb(174, 193, 224)
        canvas.drawText("НАЖМИ НА ПОЛЕ, ЧТОБЫ ВЫСТРЕЛИТЬ", 210f, 882f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawPauseOverlay(canvas: Canvas) {
        drawOverlayBackdrop(canvas)
        drawOverlayPanel(canvas, "ПАУЗА", "Прогресс уровня сохранён")
        drawOverlayButton(canvas, 80f, 440f, 340f, 505f, "ПРОДОЛЖИТЬ", Color.rgb(66, 194, 130))
        drawOverlayButton(canvas, 80f, 520f, 340f, 585f, "НАЧАТЬ ЗАНОВО", Color.rgb(65, 92, 135))
    }

    private fun drawEndOverlay(canvas: Canvas) {
        drawOverlayBackdrop(canvas)
        val won = engine.status == GameStatus.WON
        drawOverlayPanel(
            canvas,
            if (won) "УРОВЕНЬ ПРОЙДЕН!" else "ИГРА ОКОНЧЕНА",
            "УРОВЕНЬ " + engine.level + "   •   СЧЁТ " + engine.score
        )
        if (won) {
            drawOverlayButton(canvas, 65f, 470f, 355f, 535f, "СЛЕДУЮЩИЙ УРОВЕНЬ", Color.rgb(66, 194, 130))
            drawOverlayButton(canvas, 80f, 545f, 340f, 600f, "ЗАНОВО", Color.rgb(65, 92, 135))
        } else {
            drawOverlayButton(canvas, 80f, 490f, 340f, 555f, "ИГРАТЬ СНОВА", Color.rgb(66, 194, 130))
        }
    }

    private fun drawOverlayBackdrop(canvas: Canvas) {
        fillPaint.color = Color.argb(210, 5, 10, 24)
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, fillPaint)
    }

    private fun drawOverlayPanel(canvas: Canvas, title: String, subtitle: String) {
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(35f, 315f, 385f, 615f), 30f, 30f, fillPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 27f
        textPaint.color = Color.WHITE
        canvas.drawText(title, 210f, 388f, textPaint)

        textPaint.textSize = 16f
        textPaint.color = Color.rgb(199, 213, 239)
        canvas.drawText(subtitle, 210f, 430f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawOverlayButton(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        label: String,
        color: Int
    ) {
        fillPaint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), 20f, 20f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 16f
        textPaint.color = Color.rgb(10, 39, 31)
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 6f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
    }

    private fun handleGameEvents(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                GameEvent.ShotFired -> audio.playShot(soundEnabled)
                is GameEvent.MatchCleared -> {
                    audio.playMatch(soundEnabled)
                    createBurst(event)
                    saveGameState()
                }
                is GameEvent.LevelStarted -> Unit
                is GameEvent.LevelCompleted -> {
                    audio.playWin(soundEnabled)
                    saveGameState()
                }
                GameEvent.GameLost -> audio.playLose(soundEnabled)
            }
        }
    }

    private fun createBurst(event: GameEvent.MatchCleared) {
        val color = palette[event.colorIndex.coerceIn(0, palette.lastIndex)]
        val particleCount = (event.count * 4).coerceIn(10, 24)
        for (index in 0 until particleCount) {
            val angle = (Math.PI * 2.0 * index / particleCount).toFloat()
            val speed = 70f + (index % 4) * 24f
            particles += BurstParticle(
                x = event.center.x,
                y = event.center.y,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed,
                life = 0.55f,
                maxLife = 0.55f,
                color = color,
                radius = 3f + (index % 3)
            )
        }
    }

    private fun updateParticles(deltaSeconds: Float) {
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.life -= delta
            particle.x += particle.velocityX * delta
            particle.y += particle.velocityY * delta
            particle.velocityY += 120f * delta
            if (particle.life <= 0f) iterator.remove()
        }
    }

    private fun saveBestScoreIfNeeded() {
        if (engine.score > bestScore) {
            bestScore = engine.score
            preferences.edit().putInt("best_score", bestScore).apply()
        }
    }

    private fun saveGameState() {
        if (engine.status == GameStatus.LOST || engine.status == GameStatus.WON || engine.ballCount == 0) {
            preferences.edit().remove("saved_balls").apply()
            return
        }

        val snapshot = engine.saveState()
        val encodedBalls = snapshot.balls.joinToString(";") { ball ->
            ball.colorIndex.toString() + ":" + ball.progress.toString()
        }
        preferences.edit()
            .putInt("saved_level", snapshot.level)
            .putInt("saved_score", snapshot.score)
            .putInt("saved_ammo", snapshot.ammoColor)
            .putInt("saved_next", snapshot.nextColor)
            .putFloat("saved_aim_x", snapshot.aimTarget.x)
            .putFloat("saved_aim_y", snapshot.aimTarget.y)
            .putBoolean("saved_paused", snapshot.isPaused)
            .putString("saved_balls", encodedBalls)
            .apply()
    }

    private fun restoreSavedGame() {
        val encoded = preferences.getString("saved_balls", null) ?: return
        val restoredBalls = encoded.split(";").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val color = parts[0].toIntOrNull() ?: return@mapNotNull null
            val progress = parts[1].toFloatOrNull() ?: return@mapNotNull null
            Ball(color, progress)
        }
        if (restoredBalls.isEmpty()) {
            preferences.edit().remove("saved_balls").apply()
            return
        }

        val state = GameSaveState(
            level = preferences.getInt("saved_level", 1),
            score = preferences.getInt("saved_score", 0),
            ammoColor = preferences.getInt("saved_ammo", 0),
            nextColor = preferences.getInt("saved_next", 1),
            aimTarget = Vec2(
                preferences.getFloat("saved_aim_x", 210f),
                preferences.getFloat("saved_aim_y", 500f)
            ),
            isPaused = true,
            balls = restoredBalls
        )
        if (!engine.restoreState(state)) {
            preferences.edit().remove("saved_balls").apply()
        }
    }

    private fun toggleSound() {
        if (soundEnabled) audio.playClick(true)
        soundEnabled = !soundEnabled
        preferences.edit().putBoolean("sound_enabled", soundEnabled).apply()
        invalidate()
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
        val target = Vec2(x, y)

        if (x in 302f..342f && y in 24f..70f) {
            toggleSound()
            return true
        }

        if (x in 354f..400f && y in 24f..70f && engine.status == GameStatus.RUNNING) {
            engine.pauseRound()
            saveGameState()
            invalidate()
            return true
        }

        val previousStatus = engine.status
        engine.onTap(target)
        handleGameEvents(engine.drainEvents())
        if (previousStatus == GameStatus.PAUSED && engine.status == GameStatus.RUNNING) {
            saveGameState()
        }
        if (previousStatus == GameStatus.WON && engine.status == GameStatus.RUNNING) {
            particles.clear()
            saveGameState()
        }
        if (previousStatus == GameStatus.LOST && engine.status == GameStatus.RUNNING) {
            particles.clear()
            saveGameState()
        }
        saveBestScoreIfNeeded()
        invalidate()
        return true
    }
}
