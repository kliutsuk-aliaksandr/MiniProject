package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine

/** Draws pause, win and loss overlays over the active game field. */
internal class GameOverlayRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawPauseOverlay(canvas: Canvas, state: GameRenderState) {
            overlayBackdrop(canvas)
            overlayPanel(canvas, tr(state, "ПАУЗА", "PAUSED"), tr(state, "Прогресс сохранён", "Progress saved"))
            button(canvas, 80f, 445f, 340f, 505f, tr(state, "ПРОДОЛЖИТЬ", "RESUME"), GREEN)
            button(canvas, 80f, 515f, 340f, 575f, tr(state, "НАЧАТЬ ЗАНОВО", "RESTART"), BLUE)
            button(canvas, 80f, 585f, 340f, 645f, tr(state, "К КАРТЕ УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 80f, 655f, 340f, 715f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
        }

    internal fun drawEndOverlay(canvas: Canvas, engine: GameEngine, state: GameRenderState, won: Boolean) {
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
