package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus

/** Draws the score/status HUD and booster inventory. */
internal class HudRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawHud(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
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

    internal fun drawBoosters(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
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
}
