package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.TrackId
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.ObstacleType
import com.miniproject.marbleshooter.game.domain.model.Vec2

/** Renders the active game field, HUD, boosters, animated effects and result overlays. */
internal class GameplayRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawGame(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
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
                11f, Color.rgb(255, 223, 122), 210f, 675f
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
        for (obstacle in engine.obstacles) {
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
        for (ball in engine.balls) {
            val point = engine.positionOnRoute(ball.progress)
            drawMarble(canvas, point.x, point.y, ball.colorIndex, GameEngine.BALL_RADIUS)
        }
    }

    private fun drawClearingBalls(canvas: Canvas, state: GameRenderState) {
        for (ball in state.clearingBalls) {
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
        for (projectile in engine.projectiles) {
            fillPaint.color = Color.argb(70, 255, 255, 255)
            canvas.drawCircle(projectile.position.x, projectile.position.y, 15f, fillPaint)
            drawMarble(canvas, projectile.position.x, projectile.position.y, projectile.colorIndex, 10f)
        }
    }

    private fun drawParticles(canvas: Canvas, state: GameRenderState) {
        for (particle in state.particles) {
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
        val title = when {
            won && engine.isTutorial -> tr(state, "ОБУЧЕНИЕ ПРОЙДЕНО!", "PRACTICE COMPLETE!")
            won -> tr(state, "УРОВЕНЬ ПРОЙДЕН!", "LEVEL COMPLETE!")
            else -> tr(state, "ИГРА ОКОНЧЕНА", "GAME OVER")
        }
        val subtitle = if (engine.isTutorial) {
            tr(state, "Тренировка • счёт ", "Practice • score ") + engine.score
        } else {
            tr(state, "Уровень ", "Level ") + engine.level + "  •  " +
                tr(state, "Счёт ", "Score ") + engine.score
        }
        overlayPanel(canvas, title, subtitle)
        if (won) {
            button(
                canvas, 65f, 455f, 355f, 515f,
                if (engine.isTutorial) tr(state, "ПОВТОРИТЬ ТРЕНИРОВКУ", "RETRY PRACTICE")
                else tr(state, "СЛЕДУЮЩИЙ УРОВЕНЬ", "NEXT LEVEL"), GREEN
            )
            button(
                canvas, 80f, 525f, 340f, 585f,
                if (engine.isTutorial) tr(state, "К УРОКАМ", "BACK TO LESSONS")
                else tr(state, "К КАРТЕ УРОВНЕЙ", "LEVEL MAP"), BLUE
            )
            button(canvas, 80f, 595f, 340f, 655f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
            if (!engine.isTutorial) centeredText(canvas, starsLine(engine.lastStars), 27f, Color.rgb(255, 209, 88), 210f, 420f)
        } else {
            button(
                canvas, 80f, 500f, 340f, 560f,
                if (engine.isTutorial) tr(state, "ПОВТОРИТЬ ТРЕНИРОВКУ", "RETRY PRACTICE")
                else tr(state, "ИГРАТЬ СНОВА", "PLAY AGAIN"), GREEN
            )
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
}
