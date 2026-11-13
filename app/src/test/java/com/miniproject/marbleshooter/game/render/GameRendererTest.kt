package com.miniproject.marbleshooter.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.miniproject.marbleshooter.game.domain.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun redrawsRouteWhenChangingLevelsWithinTheSameTrackFamily() {
        val context = RuntimeEnvironment.getApplication()
        val resources = GameRenderResources(GameTextResources(context))
        val trackRenderer = TrackRenderer(resources)
        val engine = GameEngine()
        val firstTrackFamily = engine.levelDefinition.trackId
        val firstBitmap = Bitmap.createBitmap(420, 980, Bitmap.Config.ARGB_8888)
        val secondBitmap = Bitmap.createBitmap(420, 980, Bitmap.Config.ARGB_8888)

        try {
            trackRenderer.drawTrack(Canvas(firstBitmap), engine)
            val firstPixels = IntArray(firstBitmap.width * firstBitmap.height)
            firstBitmap.getPixels(firstPixels, 0, firstBitmap.width, 0, 0, firstBitmap.width, firstBitmap.height)

            engine.startLevel(2)
            assertEquals(firstTrackFamily, engine.levelDefinition.trackId)
            trackRenderer.drawTrack(Canvas(secondBitmap), engine)
            val secondPixels = IntArray(secondBitmap.width * secondBitmap.height)
            secondBitmap.getPixels(secondPixels, 0, secondBitmap.width, 0, 0, secondBitmap.width, secondBitmap.height)

            assertTrue(
                "Changing levels within one track family must redraw the level-specific route",
                firstPixels.indices.any { firstPixels[it] != secondPixels[it] }
            )
        } finally {
            firstBitmap.recycle()
            secondBitmap.recycle()
        }
    }
}
