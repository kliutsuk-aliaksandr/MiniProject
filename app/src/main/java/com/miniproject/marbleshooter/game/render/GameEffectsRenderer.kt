package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import com.miniproject.marbleshooter.game.domain.GameEngine

/** Draws transient feedback visuals independently from persistent game entities. */
internal class GameEffectsRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawClearingBalls(canvas: Canvas, state: GameRenderState) {
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

    internal fun drawParticles(canvas: Canvas, state: GameRenderState) {
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
}
