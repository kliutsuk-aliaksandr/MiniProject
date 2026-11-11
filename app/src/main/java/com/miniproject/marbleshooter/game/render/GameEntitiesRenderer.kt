package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import com.miniproject.marbleshooter.game.domain.GameEngine

/** Draws the dynamic chain, projectiles and player shooter. */
internal class GameEntitiesRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawBalls(canvas: Canvas, engine: GameEngine) {
            for (ball in engine.balls) {
                val point = engine.positionOnRoute(ball.progress)
                drawMarble(canvas, point.x, point.y, ball.colorIndex, GameEngine.BALL_RADIUS)
            }
        }

    internal fun drawProjectiles(canvas: Canvas, engine: GameEngine) {
            for (projectile in engine.projectiles) {
                fillPaint.color = Color.argb(70, 255, 255, 255)
                canvas.drawCircle(projectile.position.x, projectile.position.y, 15f, fillPaint)
                drawMarble(canvas, projectile.position.x, projectile.position.y, projectile.colorIndex, 10f)
            }
        }

    fun drawShooter(canvas: Canvas, engine: GameEngine) {
        drawFrog(canvas, engine.shooterPosition, engine.aimTarget, engine.ammoColor)
    }
}
