package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import com.miniproject.marbleshooter.R
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus

/** Draws the score/status HUD and booster inventory. */
internal class HudRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawHud(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
            drawPanel(canvas, RectF(18f, 20f, 402f, 132f), 23f, Color.argb(205, 7, 13, 29))
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 13f
            textPaint.color = MUTED
            canvas.drawText(tr(state, R.string.game_title), 32f, 46f, textPaint)
            textPaint.textSize = 12f
            textPaint.color = Color.rgb(111, 228, 164)
            canvas.drawText(tr(state, R.string.level_score_label, engine.level), 32f, 65f, textPaint)
            textPaint.textSize = 27f
            textPaint.color = Color.WHITE
            canvas.drawText(engine.score.toString(), 32f, 98f, textPaint)
            textPaint.textSize = if (engine.isChallenge && requireNotNull(engine.challengeDefinition).minimumCascades > 0) 8f else if (engine.isChallenge) 9f else 10f
            textPaint.color = MUTED
            if (engine.isChallenge) {
                val challenge = requireNotNull(engine.challengeDefinition)
                val hud = when {
                    challenge.minimumCascades > 0 -> tr(
                        state,
                        R.string.challenge_hud_cascade_timed,
                        engine.challengeCascadesCompleted,
                        challenge.minimumCascades,
                        engine.shotsFired,
                        challenge.maxShots,
                        engine.challengeTimeRemaining.toInt()
                    )
                    challenge.timeLimitSeconds == null -> tr(
                        state,
                        R.string.challenge_hud_shots,
                        engine.shotsFired,
                        challenge.maxShots
                    )
                    else -> tr(
                        state,
                        R.string.challenge_hud_timed,
                        engine.shotsFired,
                        challenge.maxShots,
                        engine.challengeTimeRemaining.toInt()
                    )
                }
                canvas.drawText(hud, 32f, 118f, textPaint)
            } else {
                canvas.drawText(tr(state, R.string.best_score_hud, state.bestScore), 32f, 118f, textPaint)
            }
    
            drawSmallIconButton(canvas, 320f, 48f, state.soundEnabled, true)
            drawSmallIconButton(canvas, 374f, 48f, engine.status == GameStatus.PAUSED, false)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10f
            textPaint.color = MUTED
            canvas.drawText(tr(state, R.string.next), 370f, 80f, textPaint)
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

    internal fun drawBoosters(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
            drawPanel(
                canvas,
                RectF(
                    GameplayHudLayout.BOOSTER_PANEL_LEFT,
                    GameplayHudLayout.BOOSTER_PANEL_TOP,
                    GameplayHudLayout.BOOSTER_PANEL_RIGHT,
                    GameplayHudLayout.BOOSTER_PANEL_BOTTOM
                ),
                22f,
                Color.argb(190, 8, 17, 34)
            )
            val items = listOf(
                Triple(BoosterType.SLOW_TIME, tr(state, R.string.booster_slow), engine.boosters.slowTime),
                Triple(BoosterType.BOMB, tr(state, R.string.booster_bomb), engine.boosters.bomb),
                Triple(BoosterType.COLOR_CHANGE, tr(state, R.string.booster_color), engine.boosters.colorChange),
                Triple(BoosterType.REVERSE_CHAIN, tr(state, R.string.booster_reverse), engine.boosters.reverseChain)
            )
            GameplayHudLayout.BOOSTER_CENTERS.forEachIndexed { index, x ->
                val item = items[index]
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.textSize = 10f
                textPaint.color = MUTED
                canvas.drawText(item.second, x, GameplayHudLayout.BOOSTER_LABEL_Y, textPaint)
                fillPaint.color = if (item.third > 0) Color.rgb(54, 77, 111) else Color.rgb(39, 45, 59)
                canvas.drawCircle(x, GameplayHudLayout.BOOSTER_CENTER_Y, GameplayHudLayout.BOOSTER_BUTTON_RADIUS, fillPaint)
                strokePaint.color = if (item.third > 0) Color.rgb(123, 214, 170) else Color.rgb(90, 97, 112)
                strokePaint.strokeWidth = 2f
                canvas.drawCircle(x, GameplayHudLayout.BOOSTER_CENTER_Y, GameplayHudLayout.BOOSTER_BUTTON_RADIUS, strokePaint)
                centeredText(
                    canvas, item.third.toString(), 15f,
                    if (item.third > 0) Color.WHITE else MUTED, x, GameplayHudLayout.BOOSTER_COUNT_Y
                )
            }
            textPaint.textAlign = Paint.Align.LEFT
        }
}
