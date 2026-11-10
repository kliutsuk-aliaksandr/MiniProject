package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import com.miniproject.marbleshooter.game.domain.TrackId
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.ObstacleType
import com.miniproject.marbleshooter.game.domain.model.Vec2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class GameScreen {
    MENU, LEVEL_MAP, TUTORIAL, SETTINGS, PRIVACY, GAME
}

data class ParticleVisual(
    val x: Float,
    val y: Float,
    val life: Float,
    val maxLife: Float,
    val color: Int,
    val radius: Float
)

data class ClearingBallVisual(
    val position: Vec2,
    val colorIndex: Int,
    val lifeSeconds: Float
)

data class GameRenderState(
    val screen: GameScreen,
    val language: String,
    val bestScore: Int,
    val savedGameAvailable: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val tutorialPage: Int,
    val unlockedLevel: Int,
    val starsByLevel: Map<Int, Int>,
    val particles: List<ParticleVisual> = emptyList(),
    val clearingBalls: List<ClearingBallVisual> = emptyList()
)

/** Canvas-only drawing layer. It receives state and never handles input or persistence. */
class GameRenderer {
    private val palette = intArrayOf(
        Color.rgb(255, 91, 105),
        Color.rgb(255, 194, 67),
        Color.rgb(67, 205, 151),
        Color.rgb(84, 164, 255),
        Color.rgb(184, 125, 255),
        Color.rgb(255, 129, 207)
    )
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val trackPath = Path()
    private var cachedTrack: TrackId? = null

    fun draw(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        drawBackground(canvas)
        when (state.screen) {
            GameScreen.MENU -> drawMenu(canvas, engine, state)
            GameScreen.LEVEL_MAP -> drawLevelMap(canvas, state)
            GameScreen.TUTORIAL -> drawTutorial(canvas, state)
            GameScreen.SETTINGS -> drawSettings(canvas, state)
            GameScreen.PRIVACY -> drawPrivacy(canvas, state)
            GameScreen.GAME -> drawGame(canvas, engine, state)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundPaint.shader = LinearGradient(
            0f, 0f, 0f, GameEngine.WORLD_HEIGHT,
            Color.rgb(24, 38, 68), Color.rgb(12, 18, 36), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, backgroundPaint)
        backgroundPaint.shader = null
        fillPaint.color = Color.argb(28, 255, 255, 255)
        for (index in 0 until 26) {
            val x = ((index * 73) % 410 + 5).toFloat()
            val y = ((index * 137) % 880 + 8).toFloat()
            canvas.drawCircle(x, y, if (index % 3 == 0) 2.2f else 1.2f, fillPaint)
        }
    }

    private fun drawMenu(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        centeredText(canvas, "MARBLE FROG", 39f, Color.WHITE, 210f, 145f)
        centeredText(
            canvas,
            tr(state, "ЦВЕТНАЯ ГОЛОВОЛОМКА С ЛЯГУШКОЙ", "A COLORFUL FROG MARBLE PUZZLE"),
            14f, Color.rgb(174, 193, 224), 210f, 180f
        )
        drawFrog(canvas, Vec2(210f, 305f), Vec2(210f, 155f), engine.ammoColor)
        centeredText(
            canvas,
            tr(state, "РЕКОРД  ", "BEST  ") + state.bestScore,
            15f, Color.rgb(194, 212, 240), 210f, 378f
        )

        if (state.savedGameAvailable) {
            button(canvas, 78f, 410f, 342f, 464f, tr(state, "ПРОДОЛЖИТЬ", "CONTINUE"), GREEN)
            button(canvas, 78f, 474f, 342f, 528f, tr(state, "НОВАЯ ИГРА", "NEW GAME"), BLUE)
            button(canvas, 78f, 538f, 342f, 592f, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 78f, 602f, 342f, 656f, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), BLUE)
            button(canvas, 78f, 666f, 342f, 720f, tr(state, "НАСТРОЙКИ", "SETTINGS"), BLUE)
        } else {
            button(canvas, 78f, 438f, 342f, 496f, tr(state, "ИГРАТЬ", "PLAY"), GREEN)
            button(canvas, 78f, 506f, 342f, 564f, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 78f, 574f, 342f, 632f, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), BLUE)
            button(canvas, 78f, 642f, 342f, 700f, tr(state, "НАСТРОЙКИ", "SETTINGS"), BLUE)
        }
        centeredText(canvas, "OFFLINE  •  0.5.0", 12f, Color.rgb(122, 143, 177), 210f, 835f)
    }

    private fun drawLevelMap(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), 29f, Color.WHITE, 210f, 160f)
        centeredText(
            canvas,
            tr(state, "Открыто уровней: ", "Levels unlocked: ") + state.unlockedLevel +
                " / " + LevelCatalog.MAX_LEVELS,
            14f, MUTED, 210f, 192f
        )

        val cellWidth = 68f
        val cellHeight = 58f
        val gapX = 8f
        val gapY = 8f
        val left = 22f
        val top = 220f

        for (level in 1..LevelCatalog.MAX_LEVELS) {
            val index = level - 1
            val column = index % 5
            val row = index / 5
            val x = left + column * (cellWidth + gapX)
            val y = top + row * (cellHeight + gapY)
            val unlocked = level <= state.unlockedLevel
            fillPaint.color = when {
                !unlocked -> Color.rgb(40, 49, 68)
                level == state.unlockedLevel -> Color.rgb(66, 194, 130)
                else -> Color.rgb(52, 81, 119)
            }
            canvas.drawRoundRect(RectF(x, y, x + cellWidth, y + cellHeight), 12f, 12f, fillPaint)
            centeredText(
                canvas,
                if (unlocked) level.toString() else "•",
                19f, if (unlocked) Color.WHITE else Color.rgb(123, 137, 158),
                x + cellWidth / 2f, y + 25f
            )
            val stars = state.starsByLevel[level] ?: 0
            centeredText(
                canvas,
                starsLine(stars),
                11f, Color.rgb(255, 209, 88),
                x + cellWidth / 2f, y + 46f
            )
        }

        drawTrackLegend(canvas, state)
        button(canvas, 78f, 750f, 342f, 810f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), GREEN)
    }

    private fun drawTrackLegend(canvas: Canvas, state: GameRenderState) {
        centeredText(
            canvas,
            tr(state, "Новые типы дорожек открываются по мере прохождения",
                "New track styles unlock as you progress"),
            11f, MUTED, 210f, 695f
        )
        textPaint.textSize = 11f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(178, 196, 221)
        canvas.drawText(
            tr(state, "1–8: Классика   9–15: Петли   16–22: Зигзаг   23–30: Спираль",
                "1–8: Classic   9–15: Loops   16–22: Switchback   23–30: Spiral"),
            210f, 720f, textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawTutorial(canvas: Canvas, state: GameRenderState) {
        val page = state.tutorialPage.coerceIn(0, 4)
        val titlesRu = listOf(
            "Прицелься пальцем",
            "Выбери нужный шар",
            "Собирай комбинации",
            "Используй бустеры",
            "Следи за дорожкой"
        )
        val titlesEn = listOf(
            "Aim with your finger",
            "Choose the right marble",
            "Make matches",
            "Use boosters",
            "Watch the track"
        )
        val bodiesRu = listOf(
            "Коснись поля и веди пальцем — лягушка повернётся за ним. Отпусти палец, чтобы выстрелить.",
            "В пасти лягушки показан текущий шар. Кнопка NEXT меняет его со следующим.",
            "Попади в группу из трёх или более шаров одного цвета. Передняя часть цепочки откатится назад, дальняя останется на месте.",
            "Замедление даёт время на прицел. Бомба убирает участок цепочки. Смена цвета выбирает наиболее частый цвет. Реверс временно двигает цепочку назад.",
            "Грязь замедляет цепочку, лёд ускоряет её, камни создают опасные участки. Каждая группа уровней меняет форму дорожки."
        )
        val bodiesEn = listOf(
            "Touch the field and drag. The frog follows your finger. Release to shoot.",
            "The marble in the frog's mouth is the loaded shot. NEXT swaps it with the upcoming marble.",
            "Hit a group of three or more marbles of the same color. The front segment rolls backward while the far side stays still.",
            "Slow Time gives you room to aim. Bomb clears a chain segment. Color Change selects the most common chain color. Reverse moves the chain backward briefly.",
            "Mud slows the chain, ice speeds it up, and rocks mark hazardous sections. Track layouts change between level groups."
        )

        centeredText(canvas, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), 29f, Color.WHITE, 210f, 145f)
        centeredText(canvas, (page + 1).toString() + " / 5", 13f, MUTED, 210f, 177f)
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(26f, 210f, 394f, 625f), 24f, 24f, fillPaint)
        drawTutorialIllustration(canvas, page)
        centeredText(
            canvas,
            if (state.language == "en") titlesEn[page] else titlesRu[page],
            22f, Color.WHITE, 210f, 405f
        )
        drawParagraph(
            canvas,
            if (state.language == "en") bodiesEn[page] else bodiesRu[page],
            15f, Color.rgb(205, 218, 240), RectF(55f, 430f, 365f, 570f)
        )

        if (page == 4) {
            button(canvas, 60f, 640f, 360f, 695f, tr(state, "ПОПРОБОВАТЬ В ИГРЕ", "TRY IT IN GAME"), GREEN)
        } else {
            button(canvas, 80f, 640f, 340f, 695f, tr(state, "ДАЛЕЕ", "NEXT"), GREEN)
        }
        button(canvas, 80f, 712f, 340f, 760f, tr(state, "НАЗАД", "BACK"), BLUE)
        button(canvas, 80f, 775f, 340f, 825f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
    }

    private fun drawTutorialIllustration(canvas: Canvas, page: Int) {
        when (page) {
            0 -> {
                drawFrog(canvas, Vec2(210f, 330f), Vec2(120f, 235f), 2)
                drawArrow(canvas, 240f, 255f, 320f, 220f, Color.rgb(111, 228, 164))
            }
            1 -> {
                drawMarble(canvas, 165f, 290f, 0, 19f)
                drawMarble(canvas, 255f, 290f, 3, 19f)
                centeredText(canvas, "NEXT", 14f, MUTED, 210f, 345f)
            }
            2 -> {
                drawMarble(canvas, 145f, 285f, 1, 19f)
                drawMarble(canvas, 190f, 285f, 1, 19f)
                drawMarble(canvas, 235f, 285f, 1, 19f)
                drawArrow(canvas, 280f, 285f, 330f, 285f, Color.rgb(111, 228, 164))
            }
            3 -> {
                val colors = listOf(Color.rgb(84, 164, 255), Color.rgb(255, 91, 105),
                    Color.rgb(255, 194, 67), Color.rgb(184, 125, 255))
                colors.forEachIndexed { index, color ->
                    fillPaint.color = color
                    canvas.drawCircle(75f + index * 90f, 290f, 27f, fillPaint)
                }
                centeredText(canvas, "×", 24f, Color.WHITE, 75f, 298f)
                centeredText(canvas, "✦", 24f, Color.WHITE, 165f, 298f)
                centeredText(canvas, "↻", 24f, Color.WHITE, 255f, 298f)
                centeredText(canvas, "◀", 24f, Color.WHITE, 345f, 298f)
            }
            else -> {
                fillPaint.color = Color.rgb(73, 91, 121)
                canvas.drawRoundRect(RectF(75f, 250f, 345f, 335f), 28f, 28f, fillPaint)
                fillPaint.color = Color.rgb(140, 112, 75)
                canvas.drawCircle(135f, 292f, 20f, fillPaint)
                fillPaint.color = Color.rgb(113, 190, 232)
                canvas.drawCircle(210f, 292f, 20f, fillPaint)
                fillPaint.color = Color.rgb(118, 117, 125)
                canvas.drawCircle(285f, 292f, 20f, fillPaint)
            }
        }
    }

    private fun drawSettings(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "НАСТРОЙКИ", "SETTINGS"), 30f, Color.WHITE, 210f, 225f)
        centeredText(canvas, tr(state, "ЯЗЫК", "LANGUAGE"), 16f, MUTED, 210f, 320f)
        button(
            canvas, 35f, 350f, 200f, 410f, "РУССКИЙ",
            if (state.language == "ru") GREEN else BLUE
        )
        button(
            canvas, 220f, 350f, 385f, 410f, "ENGLISH",
            if (state.language == "en") GREEN else BLUE
        )
        button(
            canvas, 60f, 465f, 360f, 525f,
            tr(state, "ЗВУК: ", "SOUND: ") + tr(state, "ВКЛ", "ON").takeIf { state.soundEnabled }
                .orEmpty().ifBlank { tr(state, "ВЫКЛ", "OFF") },
            BLUE
        )
        button(
            canvas, 60f, 540f, 360f, 600f,
            tr(state, "ВИБРАЦИЯ: ", "HAPTICS: ") + tr(state, "ВКЛ", "ON").takeIf { state.vibrationEnabled }
                .orEmpty().ifBlank { tr(state, "ВЫКЛ", "OFF") },
            BLUE
        )
        button(canvas, 60f, 635f, 360f, 690f, tr(state, "ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ", "PRIVACY POLICY"), BLUE)
        button(canvas, 80f, 735f, 340f, 795f, tr(state, "НАЗАД", "BACK"), GREEN)
    }

    private fun drawPrivacy(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "КОНФИДЕНЦИАЛЬНОСТЬ", "PRIVACY"), 24f, Color.WHITE, 210f, 150f)
        val lines = if (state.language == "en") {
            listOf(
                "Marble Frog works offline. No account or network access is needed.",
                "The game does not collect, transmit or share personal data.",
                "No ads, analytics SDKs or server sync are included.",
                "Best score, saved game, level stars, language and settings",
                "are stored locally on this device with Android preferences.",
                "Cloud backup and device transfer of these preferences are disabled.",
                "Local data can be removed in Android app settings or by uninstalling.",
                "Support: alex.kliutsuk@gmail.com"
            )
        } else {
            listOf(
                "Marble Frog работает без интернета. Аккаунт не требуется.",
                "Игра не собирает, не передаёт и не раскрывает персональные данные.",
                "Рекламы, аналитических SDK и синхронизации с сервером нет.",
                "Рекорд, сохранённая игра, звёзды, язык и настройки",
                "хранятся локально на этом устройстве в Android preferences.",
                "Облачное резервирование и перенос этих данных отключены.",
                "Данные можно удалить в настройках Android или удалив игру.",
                "Поддержка: alex.kliutsuk@gmail.com"
            )
        }
        lines.forEachIndexed { index, line ->
            drawParagraph(
                canvas, line, 13f, Color.rgb(205, 218, 240),
                RectF(36f, 220f + index * 62f, 384f, 270f + index * 62f)
            )
        }
        button(canvas, 80f, 790f, 340f, 845f, tr(state, "НАЗАД", "BACK"), GREEN)
    }

    private fun drawGame(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        drawTrack(canvas, engine)
        drawObstacles(canvas, engine)
        drawBalls(canvas, engine)
        drawClearingBalls(canvas, state)
        drawProjectiles(canvas, engine)
        drawParticles(canvas, state)
        drawFrog(canvas, engine.shooterPosition, engine.aimTarget, engine.ammoColor)
        drawHud(canvas, engine, state)
        drawBoosters(canvas, engine, state)
        if (engine.isTutorial) {
            centeredText(
                canvas,
                tr(state, "ТРЕНИРОВКА • ПОПРОБУЙ БУСТЕРЫ И КОМБИНАЦИИ",
                    "PRACTICE • TRY BOOSTERS AND MATCHES"),
                11f, Color.rgb(255, 223, 122), 210f, 705f
            )
        } else {
            centeredText(
                canvas,
                tr(state, "ВЕДИ ПАЛЬЦЕМ ДЛЯ ПРИЦЕЛА • ОТПУСТИ ДЛЯ ВЫСТРЕЛА",
                    "DRAG TO AIM • RELEASE TO SHOOT"),
                11f, MUTED, 210f, 880f
            )
        }

        when (engine.status) {
            GameStatus.PAUSED -> drawPauseOverlay(canvas, state)
            GameStatus.WON -> drawEndOverlay(canvas, engine, state, true)
            GameStatus.LOST -> drawEndOverlay(canvas, engine, state, false)
            GameStatus.RUNNING -> Unit
        }
    }

    private fun drawTrack(canvas: Canvas, engine: GameEngine) {
        if (cachedTrack != engine.levelDefinition.trackId) {
            trackPath.reset()
            engine.routePoints.forEachIndexed { index, point ->
                if (index == 0) trackPath.moveTo(point.x, point.y)
                else trackPath.lineTo(point.x, point.y)
            }
            cachedTrack = engine.levelDefinition.trackId
        }
        strokePaint.color = Color.rgb(9, 14, 27)
        strokePaint.strokeWidth = 64f
        canvas.drawPath(trackPath, strokePaint)
        strokePaint.color = when (engine.levelDefinition.trackId) {
            TrackId.CLASSIC -> Color.rgb(53, 68, 95)
            TrackId.DOUBLE_LOOP -> Color.rgb(67, 80, 99)
            TrackId.SWITCHBACK -> Color.rgb(77, 68, 90)
            TrackId.SPIRAL -> Color.rgb(58, 88, 82)
        }
        strokePaint.strokeWidth = 50f
        canvas.drawPath(trackPath, strokePaint)
        strokePaint.color = Color.rgb(92, 111, 144)
        strokePaint.strokeWidth = 2f
        canvas.drawPath(trackPath, strokePaint)

        val start = engine.routePoints.first()
        fillPaint.color = Color.rgb(109, 229, 157)
        canvas.drawCircle(start.x, start.y, 10f, fillPaint)
        val finish = engine.routePoints.last()
        fillPaint.color = Color.rgb(255, 100, 110)
        canvas.drawCircle(finish.x, finish.y, 12f, fillPaint)
    }

    private fun drawObstacles(canvas: Canvas, engine: GameEngine) {
        engine.obstacles.forEach { obstacle ->
            val point = engine.positionOnRoute(obstacle.progress)
            when (obstacle.type) {
                ObstacleType.MUD -> {
                    fillPaint.color = Color.rgb(123, 86, 51)
                    canvas.drawOval(RectF(point.x - 24f, point.y - 13f, point.x + 24f, point.y + 13f), fillPaint)
                    fillPaint.color = Color.rgb(151, 110, 65)
                    canvas.drawCircle(point.x - 8f, point.y - 2f, 4f, fillPaint)
                }
                ObstacleType.ICE -> {
                    val diamond = Path().apply {
                        moveTo(point.x, point.y - 22f)
                        lineTo(point.x + 18f, point.y)
                        lineTo(point.x, point.y + 22f)
                        lineTo(point.x - 18f, point.y)
                        close()
                    }
                    fillPaint.color = Color.rgb(129, 208, 242)
                    canvas.drawPath(diamond, fillPaint)
                    strokePaint.color = Color.rgb(225, 248, 255)
                    strokePaint.strokeWidth = 2f
                    canvas.drawPath(diamond, strokePaint)
                }
                ObstacleType.ROCK -> {
                    fillPaint.color = Color.rgb(113, 119, 135)
                    canvas.drawCircle(point.x, point.y, 18f, fillPaint)
                    fillPaint.color = Color.rgb(158, 165, 177)
                    canvas.drawCircle(point.x - 5f, point.y - 6f, 6f, fillPaint)
                }
            }
        }
    }

    private fun drawBalls(canvas: Canvas, engine: GameEngine) {
        engine.balls.forEach { ball ->
            val point = engine.positionOnRoute(ball.progress)
            drawMarble(canvas, point.x, point.y, ball.colorIndex, GameEngine.BALL_RADIUS)
        }
    }

    private fun drawMarble(canvas: Canvas, x: Float, y: Float, colorIndex: Int, radius: Float) {
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

    private fun drawClearingBalls(canvas: Canvas, state: GameRenderState) {
        state.clearingBalls.forEach { ball ->
            val fraction = (ball.lifeSeconds / 0.24f).coerceIn(0f, 1f)
            val radius = GameEngine.BALL_RADIUS * fraction
            val color = palette[ball.colorIndex.coerceIn(0, palette.lastIndex)]
            fillPaint.color = Color.argb(
                (fraction * 225f).toInt(), Color.red(color), Color.green(color), Color.blue(color)
            )
            canvas.drawCircle(ball.position.x, ball.position.y, radius, fillPaint)
            strokePaint.color = Color.argb((fraction * 230f).toInt(), 255, 245, 190)
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(ball.position.x, ball.position.y, radius + 3f, strokePaint)
        }
    }

    private fun drawProjectiles(canvas: Canvas, engine: GameEngine) {
        engine.projectiles.forEach { projectile ->
            fillPaint.color = Color.argb(70, 255, 255, 255)
            canvas.drawCircle(projectile.position.x, projectile.position.y, 15f, fillPaint)
            drawMarble(canvas, projectile.position.x, projectile.position.y, projectile.colorIndex, 10f)
        }
    }

    private fun drawParticles(canvas: Canvas, state: GameRenderState) {
        state.particles.forEach { particle ->
            val fraction = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            fillPaint.color = Color.argb(
                (fraction * 255f).toInt(),
                Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)
            )
            canvas.drawCircle(
                particle.x, particle.y,
                particle.radius * (0.55f + fraction * 0.45f), fillPaint
            )
        }
    }

    private fun drawFrog(canvas: Canvas, center: Vec2, target: Vec2, colorIndex: Int) {
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

    private fun drawHud(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        fillPaint.color = Color.argb(165, 7, 13, 29)
        canvas.drawRoundRect(RectF(18f, 20f, 402f, 132f), 23f, 23f, fillPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 13f
        textPaint.color = MUTED
        canvas.drawText("MARBLE FROG", 32f, 46f, textPaint)
        textPaint.textSize = 12f
        textPaint.color = Color.rgb(111, 228, 164)
        canvas.drawText(tr(state, "УРОВЕНЬ ", "LEVEL ") + engine.level, 32f, 65f, textPaint)
        textPaint.textSize = 27f
        textPaint.color = Color.WHITE
        canvas.drawText(engine.score.toString(), 32f, 98f, textPaint)
        textPaint.textSize = 10f
        textPaint.color = MUTED
        canvas.drawText(tr(state, "РЕКОРД ", "BEST ") + state.bestScore, 32f, 118f, textPaint)

        drawSmallIconButton(canvas, 320f, 48f, state.soundEnabled, true)
        drawSmallIconButton(canvas, 374f, 48f, engine.status == GameStatus.PAUSED, false)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 10f
        textPaint.color = MUTED
        canvas.drawText("NEXT", 370f, 80f, textPaint)
        drawMarble(canvas, 370f, 106f, engine.nextColor, 15f)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSmallIconButton(canvas: Canvas, x: Float, y: Float, active: Boolean, sound: Boolean) {
        fillPaint.color = if (active) Color.rgb(63, 103, 111) else Color.rgb(37, 52, 79)
        canvas.drawRoundRect(RectF(x - 19f, y - 16f, x + 19f, y + 16f), 11f, 11f, fillPaint)
        if (sound) {
            centeredText(canvas, if (active) "♪" else "×", 19f, Color.WHITE, x, y + 6f)
        } else {
            centeredText(
                canvas,
                if (active) "▶" else "Ⅱ",
                14f, Color.WHITE, x, y + 5f
            )
        }
    }

    private fun drawBoosters(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        val items = listOf(
            Triple(BoosterType.SLOW_TIME, tr(state, "МЕДЛ", "SLOW"), engine.boosters.slowTime),
            Triple(BoosterType.BOMB, tr(state, "БОМБА", "BOMB"), engine.boosters.bomb),
            Triple(BoosterType.COLOR_CHANGE, tr(state, "ЦВЕТ", "COLOR"), engine.boosters.colorChange),
            Triple(BoosterType.REVERSE_CHAIN, tr(state, "РЕВЕРС", "REVERSE"), engine.boosters.reverseChain)
        )
        val centers = listOf(40f, 150f, 270f, 380f)
        items.forEachIndexed { index, item ->
            val x = centers[index]
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10f
            textPaint.color = MUTED
            canvas.drawText(item.second, x, 701f, textPaint)
            fillPaint.color = if (item.third > 0) Color.rgb(54, 77, 111) else Color.rgb(39, 45, 59)
            canvas.drawCircle(x, 737f, 23f, fillPaint)
            strokePaint.color = if (item.third > 0) Color.rgb(123, 214, 170) else Color.rgb(90, 97, 112)
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(x, 737f, 23f, strokePaint)
            centeredText(
                canvas, item.third.toString(), 15f,
                if (item.third > 0) Color.WHITE else MUTED, x, 742f
            )
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawPauseOverlay(canvas: Canvas, state: GameRenderState) {
        overlayBackdrop(canvas)
        overlayPanel(canvas, tr(state, "ПАУЗА", "PAUSED"), tr(state, "Прогресс сохранён", "Progress saved"))
        button(canvas, 80f, 445f, 340f, 505f, tr(state, "ПРОДОЛЖИТЬ", "RESUME"), GREEN)
        button(canvas, 80f, 515f, 340f, 575f, tr(state, "НАЧАТЬ ЗАНОВО", "RESTART"), BLUE)
        button(canvas, 80f, 585f, 340f, 645f, tr(state, "К КАРТЕ УРОВНЕЙ", "LEVEL MAP"), BLUE)
        button(canvas, 80f, 655f, 340f, 715f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
    }

    private fun drawEndOverlay(canvas: Canvas, engine: GameEngine, state: GameRenderState, won: Boolean) {
        overlayBackdrop(canvas)
        overlayPanel(
            canvas,
            if (won) tr(state, "УРОВЕНЬ ПРОЙДЕН!", "LEVEL COMPLETE!") else tr(state, "ИГРА ОКОНЧЕНА", "GAME OVER"),
            tr(state, "Уровень ", "Level ") + engine.level + "  •  " +
                tr(state, "Счёт ", "Score ") + engine.score
        )
        if (won) {
            button(canvas, 65f, 455f, 355f, 515f, tr(state, "СЛЕДУЮЩИЙ УРОВЕНЬ", "NEXT LEVEL"), GREEN)
            button(canvas, 80f, 525f, 340f, 585f, tr(state, "К КАРТЕ УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 80f, 595f, 340f, 655f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
            centeredText(canvas, starsLine(engine.lastStars), 27f, Color.rgb(255, 209, 88), 210f, 420f)
        } else {
            button(canvas, 80f, 500f, 340f, 560f, tr(state, "ИГРАТЬ СНОВА", "PLAY AGAIN"), GREEN)
            button(canvas, 80f, 575f, 340f, 635f, tr(state, "К КАРТЕ УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 80f, 650f, 340f, 710f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
        }
    }

    private fun overlayBackdrop(canvas: Canvas) {
        fillPaint.color = Color.argb(210, 5, 10, 24)
        canvas.drawRect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, fillPaint)
    }

    private fun overlayPanel(canvas: Canvas, title: String, subtitle: String) {
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(35f, 285f, 385f, 720f), 30f, 30f, fillPaint)
        centeredText(canvas, title, 25f, Color.WHITE, 210f, 365f)
        centeredText(canvas, subtitle, 14f, Color.rgb(199, 213, 239), 210f, 405f)
    }

    private fun button(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, label: String, color: Int) {
        fillPaint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, fillPaint)
        centeredText(canvas, label, if (label.length > 18) 13f else 15f, Color.WHITE, (left + right) / 2f, (top + bottom) / 2f + 5f)
    }

    private fun centeredText(canvas: Canvas, text: String, size: Float, color: Int, x: Float, y: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = size
        textPaint.color = color
        canvas.drawText(text, x, y, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawParagraph(canvas: Canvas, text: String, size: Float, color: Int, bounds: RectF) {
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
        lines.take((bounds.height() / lineHeight).toInt().coerceAtLeast(1)).forEachIndexed { index, line ->
            canvas.drawText(line, bounds.left, bounds.top + size + index * lineHeight, textPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 5f
        canvas.drawLine(x1, y1, x2, y2, strokePaint)
        val angle = atan2(y2 - y1, x2 - x1)
        val left = Vec2(x2 - 13f * cos(angle - 0.55f), y2 - 13f * sin(angle - 0.55f))
        val right = Vec2(x2 - 13f * cos(angle + 0.55f), y2 - 13f * sin(angle + 0.55f))
        canvas.drawLine(x2, y2, left.x, left.y, strokePaint)
        canvas.drawLine(x2, y2, right.x, right.y, strokePaint)
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.68f).toInt(),
        (Color.green(color) * 0.68f).toInt(),
        (Color.blue(color) * 0.68f).toInt()
    )

    private fun starsLine(stars: Int): String = buildString {
        repeat(3) { append(if (it < stars) "★" else "☆") }
    }

    private fun tr(state: GameRenderState, russian: String, english: String): String =
        if (state.language == "en") english else russian

    private companion object {
        val GREEN = Color.rgb(66, 194, 130)
        val BLUE = Color.rgb(65, 92, 135)
        val MUTED = Color.rgb(174, 193, 224)
    }
}
