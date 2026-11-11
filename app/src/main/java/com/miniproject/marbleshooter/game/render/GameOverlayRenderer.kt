package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import com.miniproject.marbleshooter.R
import android.graphics.Color
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine

/** Draws pause, win and loss overlays over the active game field. */
internal class GameOverlayRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawPauseOverlay(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
            overlayBackdrop(canvas)
            overlayPanel(
                canvas,
                tr(state, R.string.pause),
                tr(state, if (engine.isChallenge) R.string.challenge_pause_subtitle else R.string.progress_saved)
            )
            button(canvas, 80f, 445f, 340f, 505f, tr(state, R.string.resume), GREEN)
            button(canvas, 80f, 515f, 340f, 575f, tr(state, R.string.restart), BLUE)
            button(
                canvas, 80f, 585f, 340f, 645f,
                tr(state, if (engine.isChallenge) R.string.challenge_list else R.string.level_map_short), BLUE
            )
            button(canvas, 80f, 655f, 340f, 715f, tr(state, R.string.main_menu), BLUE)
        }

    internal fun drawEndOverlay(canvas: Canvas, engine: GameEngine, state: GameRenderState, won: Boolean) {
            overlayBackdrop(canvas)
            val challenge = engine.challengeDefinition
            val title = when {
                engine.isChallenge && won -> tr(state, R.string.challenge_complete)
                engine.isChallenge -> tr(state, R.string.challenge_failed)
                won && engine.isTutorial -> tr(state, R.string.practice_complete)
                won -> tr(state, R.string.level_complete)
                else -> tr(state, R.string.game_over)
            }
            val subtitle = when {
                challenge != null -> tr(
                    state,
                    R.string.challenge_result_summary,
                    engine.shotsFired,
                    challenge.maxShots,
                    engine.challengeTimeRemaining.toInt()
                )
                engine.isTutorial -> tr(state, R.string.practice_score, engine.score)
                else -> tr(state, R.string.level_score, engine.level, engine.score)
            }
            overlayPanel(canvas, title, subtitle)
            if (won) {
                button(
                    canvas, 65f, 455f, 355f, 515f,
                    when {
                        engine.isChallenge -> tr(state, R.string.challenge_retry)
                        engine.isTutorial -> tr(state, R.string.retry_practice)
                        else -> tr(state, R.string.next_level)
                    }, GREEN
                )
                button(
                    canvas, 80f, 525f, 340f, 585f,
                    when {
                        engine.isChallenge -> tr(state, R.string.challenge_list)
                        engine.isTutorial -> tr(state, R.string.back_to_lessons)
                        else -> tr(state, R.string.level_map_short)
                    }, BLUE
                )
                button(canvas, 80f, 595f, 340f, 655f, tr(state, R.string.main_menu), BLUE)
                if (!engine.isTutorial) centeredText(canvas, starsLine(engine.lastStars), 27f, Color.rgb(255, 209, 88), 210f, 420f)
            } else {
                button(
                    canvas, 80f, 500f, 340f, 560f,
                    when {
                        engine.isChallenge -> tr(state, R.string.challenge_retry)
                        engine.isTutorial -> tr(state, R.string.retry_practice)
                        else -> tr(state, R.string.play_again)
                    }, GREEN
                )
                button(
                    canvas, 80f, 575f, 340f, 635f,
                    tr(state, if (engine.isChallenge) R.string.challenge_list else R.string.level_map_short), BLUE
                )
                button(canvas, 80f, 650f, 340f, 710f, tr(state, R.string.main_menu), BLUE)
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
