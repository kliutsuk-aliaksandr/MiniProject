package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.TrackId
import com.miniproject.marbleshooter.game.domain.model.ObstacleType

/** Draws cached route geometry and the obstacles projected onto that route. */
internal class TrackRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawTrack(canvas: Canvas, engine: GameEngine) {
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

    internal fun drawObstacles(canvas: Canvas, engine: GameEngine) {
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
}
