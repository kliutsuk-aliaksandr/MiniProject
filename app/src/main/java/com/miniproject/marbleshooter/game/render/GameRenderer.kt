package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.Vec2

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

/** Canvas renderer façade. Each visual concern is delegated to a focused layer. */
class GameRenderer : GameCanvasRendererBase(GameRenderResources()) {
    private val menuRenderer = MenuScreenRenderer(drawing)
    private val gameplayRenderer = GameplayRenderer(drawing)

    fun draw(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        drawBackground(canvas)
        when (state.screen) {
            GameScreen.MENU -> menuRenderer.drawMenu(canvas, engine, state)
            GameScreen.LEVEL_MAP -> menuRenderer.drawLevelMap(canvas, state)
            GameScreen.TUTORIAL -> menuRenderer.drawTutorial(canvas, state)
            GameScreen.SETTINGS -> menuRenderer.drawSettings(canvas, state)
            GameScreen.PRIVACY -> menuRenderer.drawPrivacy(canvas, state)
            GameScreen.GAME -> gameplayRenderer.drawGame(canvas, engine, state)
        }
    }
}
