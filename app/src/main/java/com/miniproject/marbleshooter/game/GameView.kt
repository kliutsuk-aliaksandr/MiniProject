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
import kotlin.math.atan2
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

private enum class UiScreen {
    MENU,
    SETTINGS,
    GAME
}

class GameView(context: Context) : View(context) {

    private val engine = GameEngine()
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
    private val audio = GameAudio()
    private var bestScore = preferences.getInt("best_score", 0)
    private var soundEnabled = preferences.getBoolean("sound_enabled", true)
    private var isActive = false
    private var lastFrameNanos = 0L
    private val particles = mutableListOf<BurstParticle>()
    private var screen = UiScreen.MENU
    private var hasSavedGame = false
    private var language = preferences.getString("language", "ru") ?: "ru"
    private var isAiming = false

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
        hasSavedGame = restoreSavedGame()
        engine.drainEvents()
    }

    fun resumeGame() {
        isActive = true
        lastFrameNanos = System.nanoTime()
        invalidate()
        if (screen == UiScreen.GAME && engine.status == GameStatus.RUNNING) {
            postInvalidateOnAnimation()
        }
    }

    fun pauseGame() {
        isActive = false
        lastFrameNanos = 0L
        isAiming = false
        if (screen == UiScreen.GAME && engine.status == GameStatus.RUNNING) {
            engine.pauseRound()
        }
        if (screen == UiScreen.GAME) saveGameState()
        invalidate()
    }

    fun disposeGame() {
        if (screen == UiScreen.GAME) saveGameState()
        audio.release()
    }

    fun handleSystemBack(): Boolean {
        when (screen) {
            UiScreen.SETTINGS -> screen = UiScreen.MENU
            UiScreen.MENU -> return false
            UiScreen.GAME -> {
                when (engine.status) {
                    GameStatus.RUNNING -> engine.pauseRound()
                    GameStatus.PAUSED -> screen = UiScreen.MENU
                    GameStatus.WON, GameStatus.LOST -> screen = UiScreen.MENU
                }
                isAiming = false
                if (screen == UiScreen.MENU || engine.status == GameStatus.PAUSED) saveGameState()
                hasSavedGame = hasSavedGame || engine.status == GameStatus.PAUSED
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        var delta = 0f
        if (isActive && screen == UiScreen.GAME) {
            val now = System.nanoTime()
            delta = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now
            if (engine.status == GameStatus.RUNNING) engine.update(delta)
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
        when (screen) {
            UiScreen.MENU -> drawMenuScreen(canvas)
            UiScreen.SETTINGS -> drawSettingsScreen(canvas)
            UiScreen.GAME -> drawGameScreen(canvas)
        }

        canvas.restore()

        if (isActive && screen == UiScreen.GAME &&
            (engine.status == GameStatus.RUNNING || particles.isNotEmpty())
        ) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawGameScreen(canvas: Canvas) {
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
    }

    private fun drawMenuScreen(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 39f
        textPaint.color = Color.WHITE
        canvas.drawText("MARBLE FROG", 210f, 190f, textPaint)

        textPaint.textSize = 16f
        textPaint.color = Color.rgb(174, 193, 224)
        canvas.drawText(t("ЛЯГУШКА-МРАМОРЩИК", "THE MARBLE SHOOTER"), 210f, 225f, textPaint)

        drawShooter(canvas, Vec2(210f, 335f), Vec2(210f, 170f), engine.ammoColor)

        textPaint.textSize = 15f
        textPaint.color = Color.rgb(194, 212, 240)
        canvas.drawText(t("РЕКОРД  ", "BEST  ") + bestScore, 210f, 405f, textPaint)

        if (hasSavedGame) {
            drawUiButton(canvas, 80f, 450f, 340f, 510f, t("ПРОДОЛЖИТЬ", "CONTINUE"), Color.rgb(66, 194, 130))
            drawUiButton(canvas, 80f, 525f, 340f, 585f, t("НОВАЯ ИГРА", "NEW GAME"), Color.rgb(65, 92, 135))
            drawUiButton(canvas, 80f, 600f, 340f, 660f, t("НАСТРОЙКИ", "SETTINGS"), Color.rgb(65, 92, 135))
        } else {
            drawUiButton(canvas, 80f, 490f, 340f, 550f, t("ИГРАТЬ", "PLAY"), Color.rgb(66, 194, 130))
            drawUiButton(canvas, 80f, 570f, 340f, 630f, t("НАСТРОЙКИ", "SETTINGS"), Color.rgb(65, 92, 135))
        }

        textPaint.textSize = 12f
        textPaint.color = Color.rgb(122, 143, 177)
        canvas.drawText("OFFLINE  •  0.3.0", 210f, 825f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSettingsScreen(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 32f
        textPaint.color = Color.WHITE
        canvas.drawText(t("НАСТРОЙКИ", "SETTINGS"), 210f, 235f, textPaint)

        textPaint.textSize = 17f
        textPaint.color = Color.rgb(174, 193, 224)
        canvas.drawText(t("ЯЗЫК", "LANGUAGE"), 210f, 335f, textPaint)

        drawUiButton(
            canvas, 40f, 370f, 200f, 435f, "РУССКИЙ",
            if (language == "ru") Color.rgb(66, 194, 130) else Color.rgb(65, 92, 135)
        )
        drawUiButton(
            canvas, 220f, 370f, 380f, 435f, "ENGLISH",
            if (language == "en") Color.rgb(66, 194, 130) else Color.rgb(65, 92, 135)
        )

        drawUiButton(
            canvas, 60f, 485f, 360f, 550f,
            t("ЗВУК: ", "SOUND: ") + t(if (soundEnabled) "ВКЛ" else "ВЫКЛ", if (soundEnabled) "ON" else "OFF"),
            Color.rgb(65, 92, 135)
        )
        drawUiButton(canvas, 80f, 600f, 340f, 660f, t("НАЗАД", "BACK"), Color.rgb(66, 194, 130))
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawUiButton(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        label: String,
        color: Int
    ) {
        fillPaint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = if (label.length > 18) 13f else 16f
        textPaint.color = Color.WHITE
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 6f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun t(russian: String, english: String): String =
        if (language == "en") english else russian

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
        canvas.drawText(t("СТАРТ", "START"), start.x - 23f, start.y - 35f, textPaint)

        val finish = engine.routePoints.last()
        fillPaint.color = Color.rgb(255, 100, 110)
        canvas.drawCircle(finish.x, finish.y, 13f, fillPaint)
        textPaint.color = Color.rgb(255, 190, 194)
        canvas.drawText(t("ФИНИШ", "FINISH"), finish.x - 29f, finish.y + 39f, textPaint)
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
        drawShooter(canvas, engine.shooterPosition, engine.aimTarget, engine.ammoColor)
    }

    private fun drawShooter(canvas: Canvas, center: Vec2, target: Vec2, colorIndex: Int) {
        val direction = (target - center).normalized()
        val rotation = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat() + 90f

        canvas.save()
        canvas.translate(center.x, center.y)
        canvas.rotate(rotation)

        // The barrel, face and eyes all rotate together so the frog visibly follows the finger.
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

        // The loaded marble is drawn in the frog's mouth/muzzle and always shows the next shot.
        drawAmmoBadge(canvas, 0f, -53f, colorIndex, 13f)
        canvas.restore()
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
        canvas.drawText(t("УРОВЕНЬ ", "LEVEL ") + engine.level, 34f, 69f, textPaint)

        textPaint.textSize = 29f
        textPaint.color = Color.WHITE
        canvas.drawText(engine.score.toString(), 34f, 101f, textPaint)

        textPaint.textSize = 11f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText(t("РЕКОРД  ", "BEST  ") + bestScore, 34f, 119f, textPaint)

        drawHudButton(canvas, 322f, 46f, soundEnabled, true)
        drawHudButton(canvas, 374f, 46f, engine.status == GameStatus.PAUSED, false)

        textPaint.textSize = 11f
        textPaint.color = Color.rgb(165, 183, 216)
        canvas.drawText("NEXT", 370f, 75f, textPaint)
        drawAmmoBadge(canvas, 370f, 103f, engine.nextColor, 16f)
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
        canvas.drawText(
            t("ВЕДИ ПАЛЬЦЕМ ДЛЯ ПРИЦЕЛА • ОТПУСТИ ДЛЯ ВЫСТРЕЛА",
              "DRAG TO AIM • RELEASE TO SHOOT"),
            210f, 882f, textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawPauseOverlay(canvas: Canvas) {
        drawOverlayBackdrop(canvas)
        drawOverlayPanel(canvas, t("ПАУЗА", "PAUSED"), t("Прогресс уровня сохранён", "Level progress saved"))
        drawOverlayButton(canvas, 80f, 450f, 340f, 510f, t("ПРОДОЛЖИТЬ", "RESUME"), Color.rgb(66, 194, 130))
        drawOverlayButton(canvas, 80f, 520f, 340f, 580f, t("НАЧАТЬ ЗАНОВО", "RESTART"), Color.rgb(65, 92, 135))
        drawOverlayButton(canvas, 80f, 590f, 340f, 650f, t("В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), Color.rgb(65, 92, 135))
    }

    private fun drawEndOverlay(canvas: Canvas) {
        drawOverlayBackdrop(canvas)
        val won = engine.status == GameStatus.WON
        drawOverlayPanel(
            canvas,
            if (won) t("УРОВЕНЬ ПРОЙДЕН!", "LEVEL COMPLETE!") else t("ИГРА ОКОНЧЕНА", "GAME OVER"),
            t("УРОВЕНЬ ", "LEVEL ") + engine.level + "   •   " +
                t("СЧЁТ ", "SCORE ") + engine.score
        )
        if (won) {
            drawOverlayButton(canvas, 65f, 470f, 355f, 530f, t("СЛЕДУЮЩИЙ УРОВЕНЬ", "NEXT LEVEL"), Color.rgb(66, 194, 130))
            drawOverlayButton(canvas, 80f, 540f, 340f, 600f, t("ЗАНОВО", "RESTART"), Color.rgb(65, 92, 135))
            drawOverlayButton(canvas, 80f, 610f, 340f, 670f, t("В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), Color.rgb(65, 92, 135))
        } else {
            drawOverlayButton(canvas, 80f, 500f, 340f, 560f, t("ИГРАТЬ СНОВА", "PLAY AGAIN"), Color.rgb(66, 194, 130))
            drawOverlayButton(canvas, 80f, 575f, 340f, 635f, t("В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), Color.rgb(65, 92, 135))
        }
    }

    private fun drawOverlayPanel(canvas: Canvas, title: String, subtitle: String) {
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(35f, 285f, 385f, 710f), 30f, 30f, fillPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 26f
        textPaint.color = Color.WHITE
        canvas.drawText(title, 210f, 375f, textPaint)

        textPaint.textSize = 15f
        textPaint.color = Color.rgb(199, 213, 239)
        canvas.drawText(subtitle, 210f, 415f, textPaint)
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
        canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = if (label.length > 18) 13f else 15f
        textPaint.color = Color.WHITE
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 6f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
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
            hasSavedGame = false
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
            .putBoolean("saved_paused", true)
            .putString("saved_balls", encodedBalls)
            .apply()
        hasSavedGame = true
    }

    private fun restoreSavedGame(): Boolean {
        val encoded = preferences.getString("saved_balls", null) ?: return false
        val restoredBalls = encoded.split(";").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val color = parts[0].toIntOrNull() ?: return@mapNotNull null
            val progress = parts[1].toFloatOrNull() ?: return@mapNotNull null
            Ball(color, progress)
        }
        if (restoredBalls.isEmpty()) {
            preferences.edit().remove("saved_balls").apply()
            return false
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
        val restored = engine.restoreState(state)
        if (!restored) preferences.edit().remove("saved_balls").apply()
        return restored
    }

    private fun toggleSound() {
        audio.playClick(soundEnabled)
        soundEnabled = !soundEnabled
        preferences.edit().putBoolean("sound_enabled", soundEnabled).apply()
        invalidate()
    }

    private fun setLanguage(code: String) {
        if (code != "ru" && code != "en") return
        language = code
        preferences.edit().putString("language", language).apply()
        audio.playClick(soundEnabled)
        invalidate()
    }

    private fun startNewGame() {
        engine.startNewGame()
        particles.clear()
        screen = UiScreen.GAME
        hasSavedGame = true
        saveGameState()
        audio.playClick(soundEnabled)
        invalidate()
    }

    private fun openSavedGame() {
        screen = UiScreen.GAME
        engine.resumeRound()
        isAiming = false
        audio.playClick(soundEnabled)
        invalidate()
        if (isActive && engine.status == GameStatus.RUNNING) postInvalidateOnAnimation()
    }

    private fun handleMenuTap(target: Vec2) {
        if (hasSavedGame && target.x in 80f..340f && target.y in 450f..510f) {
            openSavedGame()
            return
        }
        val newGameRange = if (hasSavedGame) 525f..585f else 490f..550f
        val settingsRange = if (hasSavedGame) 600f..660f else 570f..630f
        if (target.x in 80f..340f && target.y in newGameRange) {
            startNewGame()
        } else if (target.x in 80f..340f && target.y in settingsRange) {
            screen = UiScreen.SETTINGS
            audio.playClick(soundEnabled)
            invalidate()
        }
    }

    private fun handleSettingsTap(target: Vec2) {
        if (target.y in 370f..435f) {
            when {
                target.x in 40f..200f -> setLanguage("ru")
                target.x in 220f..380f -> setLanguage("en")
            }
        } else if (target.x in 60f..360f && target.y in 485f..550f) {
            toggleSound()
        } else if (target.x in 80f..340f && target.y in 600f..660f) {
            screen = UiScreen.MENU
            audio.playClick(soundEnabled)
            invalidate()
        }
    }

    private fun handleGameTap(target: Vec2) {
        if (target.y in 20f..75f) {
            if (target.x in 302f..342f) {
                toggleSound()
                return
            }
            if (target.x in 354f..400f && engine.status == GameStatus.RUNNING) {
                engine.pauseRound()
                isAiming = false
                saveGameState()
                audio.playClick(soundEnabled)
                invalidate()
                return
            }
        }

        if (engine.status == GameStatus.RUNNING &&
            target.x in 340f..405f && target.y in 80f..140f
        ) {
            engine.swapAmmo()
            audio.playClick(soundEnabled)
            saveGameState()
            invalidate()
            return
        }

        when (engine.status) {
            GameStatus.PAUSED -> {
                when {
                    target.x in 80f..340f && target.y in 450f..510f -> {
                        engine.resumeRound()
                        audio.playClick(soundEnabled)
                    }
                    target.x in 80f..340f && target.y in 520f..580f -> {
                        engine.startNewGame()
                        particles.clear()
                        saveGameState()
                        audio.playClick(soundEnabled)
                    }
                    target.x in 80f..340f && target.y in 590f..650f -> {
                        screen = UiScreen.MENU
                        saveGameState()
                        audio.playClick(soundEnabled)
                    }
                }
            }
            GameStatus.WON -> {
                when {
                    target.x in 65f..355f && target.y in 470f..530f -> {
                        engine.startNextLevel()
                        particles.clear()
                        saveGameState()
                        audio.playClick(soundEnabled)
                    }
                    target.x in 80f..340f && target.y in 540f..600f -> startNewGame()
                    target.x in 80f..340f && target.y in 610f..670f -> screen = UiScreen.MENU
                }
            }
            GameStatus.LOST -> {
                when {
                    target.x in 80f..340f && target.y in 500f..560f -> startNewGame()
                    target.x in 80f..340f && target.y in 575f..635f -> screen = UiScreen.MENU
                }
            }
            GameStatus.RUNNING -> Unit
        }
        saveBestScoreIfNeeded()
        invalidate()
    }

    private fun darken(color: Int): Int {
        val red = (Color.red(color) * 0.68f).toInt()
        val green = (Color.green(color) * 0.68f).toInt()
        val blue = (Color.blue(color) * 0.68f).toInt()
        return Color.rgb(red, green, blue)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scaleFactor <= 0f) return true

        val target = Vec2(
            (event.x - offsetX) / scaleFactor,
            (event.y - offsetY) / scaleFactor
        )

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (screen == UiScreen.GAME &&
                    engine.status == GameStatus.RUNNING &&
                    target.y > 145f
                ) {
                    isAiming = true
                    engine.aimAt(target)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isAiming && screen == UiScreen.GAME && engine.status == GameStatus.RUNNING) {
                    engine.aimAt(target)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isAiming = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isAiming && screen == UiScreen.GAME && engine.status == GameStatus.RUNNING) {
                    engine.aimAt(target)
                    engine.fireShot()
                    isAiming = false
                    handleGameEvents(engine.drainEvents())
                    saveGameState()
                    saveBestScoreIfNeeded()
                } else {
                    when (screen) {
                        UiScreen.MENU -> handleMenuTap(target)
                        UiScreen.SETTINGS -> handleSettingsTap(target)
                        UiScreen.GAME -> handleGameTap(target)
                    }
                }
                performClick()
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
