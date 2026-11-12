package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import com.miniproject.marbleshooter.R
import android.graphics.Color
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.GameStatus

/**
 * Coordinates focused render layers for one gameplay frame.
 * Rendering layers share one resource instance and keep their Paint/Path objects off the hot allocation path.
 */
internal class GameplayRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    private val trackRenderer = TrackRenderer(resources)
    private val entitiesRenderer = GameEntitiesRenderer(resources)
    private val effectsRenderer = GameEffectsRenderer(resources)
    private val hudRenderer = HudRenderer(resources)
    private val overlayRenderer = GameOverlayRenderer(resources)

    internal fun drawGame(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        trackRenderer.drawTrack(canvas, engine)
        trackRenderer.drawObstacles(canvas, engine)
        entitiesRenderer.drawBalls(canvas, engine)
        effectsRenderer.drawClearingBalls(canvas, state)
        entitiesRenderer.drawProjectiles(canvas, engine)
        effectsRenderer.drawParticles(canvas, state)
        entitiesRenderer.drawShooter(canvas, engine)
        hudRenderer.drawHud(canvas, engine, state)
        hudRenderer.drawBoosters(canvas, engine, state)

        if (engine.isTutorial) {
            centeredText(
                canvas,
                tr(state, R.string.practice_footer),
                11f, Color.rgb(255, 223, 122), 210f, 675f
            )
        }

        if (state.startCountdownNumber != null || state.showStartPrompt) {
            overlayRenderer.drawStartSequenceOverlay(canvas, state)
        }

        when (engine.status) {
            GameStatus.PAUSED -> overlayRenderer.drawPauseOverlay(canvas, engine, state)
            GameStatus.WON -> overlayRenderer.drawEndOverlay(canvas, engine, state, true)
            GameStatus.LOST -> overlayRenderer.drawEndOverlay(canvas, engine, state, false)
            GameStatus.RUNNING -> Unit
        }
    }
}
