package com.miniproject.marbleshooter.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.miniproject.marbleshooter.game.domain.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameRendererTest {

    @Test
    fun drawsEveryScreenWithSharedRendererResources() {
        val bitmap = Bitmap.createBitmap(420, 900, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val renderer = GameRenderer(RuntimeEnvironment.getApplication())
            val engine = GameEngine()

            GameScreen.values().forEach { screen ->
                renderer.draw(
                    canvas,
                    engine,
                    GameRenderState(
                        screen = screen,
                        language = "en",
                        bestScore = 1200,
                        savedGameAvailable = false,
                        soundEnabled = true,
                        vibrationEnabled = true,
                        tutorialPage = 2,
                        unlockedLevel = 10,
                        starsByLevel = mapOf(1 to 3, 2 to 2, 10 to 1)
                    )
                )
            }
        } finally {
            bitmap.recycle()
        }
    }
    
    @Test
    fun rebuildsCachedPathWhenChangingLevelsWithinTheSameTrackFamily() {
        val context = RuntimeEnvironment.getApplication()
        val resources = GameRenderResources(GameTextResources(context))
        val trackRenderer = TrackRenderer(resources)
        val engine = GameEngine()
        val firstTrackFamily = engine.levelDefinition.trackId
        val bitmap = Bitmap.createBitmap(420, 980, Bitmap.Config.ARGB_8888)

        try {
            trackRenderer.drawTrack(Canvas(bitmap), engine)
            assertEquals(1, resources.cachedLevelId)
            val firstRoutePath = resources.trackPath.approximate(0.5f)

            engine.startLevel(2)
            assertEquals(firstTrackFamily, engine.levelDefinition.trackId)
            trackRenderer.drawTrack(Canvas(bitmap), engine)

            assertEquals(2, resources.cachedLevelId)
            val secondRoutePath = resources.trackPath.approximate(0.5f)
            assertFalse(
                "Changing levels within one track family must rebuild the level-specific Path",
                firstRoutePath.contentEquals(secondRoutePath)
            )
        } finally {
            bitmap.recycle()
        }
    }
}
