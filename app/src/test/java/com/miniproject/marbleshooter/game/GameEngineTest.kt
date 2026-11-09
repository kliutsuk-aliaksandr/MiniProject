package com.miniproject.marbleshooter.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun newGameStartsWithAChainAndZeroScore() {
        val engine = GameEngine(Random(7))

        assertEquals(14, engine.ballCount)
        assertEquals(0, engine.score)
        assertEquals(GameStatus.RUNNING, engine.status)
    }

    @Test
    fun routePositionStaysInsideWorldBounds() {
        val engine = GameEngine(Random(3))

        val start = engine.positionOnRoute(0f)
        val end = engine.positionOnRoute(engine.routeLength + 100f)

        assertEquals(42f, start.x, 0.01f)
        assertEquals(160f, start.y, 0.01f)
        assertEquals(210f, end.x, 0.01f)
        assertEquals(735f, end.y, 0.01f)
        assertTrue(engine.routeLength > 1_000f)
    }

    @Test
    fun tapCreatesAProjectileAndAdvancesAmmo() {
        val engine = GameEngine(Random(11))
        val previousAmmo = engine.ammoColor

        engine.onTap(Vec2(150f, 380f))

        assertEquals(1, engine.projectileCount)
        assertNotEquals(previousAmmo, engine.ammoColor)
    }

    @Test
    fun updateMovesTheLeadBallForward() {
        val engine = GameEngine(Random(15))
        val initialProgress = engine.headProgress

        engine.update(0.05f)

        assertTrue(engine.headProgress > initialProgress)
    }

    @Test
    fun startNewGameResetsScoreAndProjectiles() {
        val engine = GameEngine(Random(21))
        engine.onTap(Vec2(150f, 380f))
        engine.update(0.01f)

        engine.startNewGame()

        assertEquals(0, engine.score)
        assertEquals(0, engine.projectileCount)
        assertEquals(14, engine.ballCount)
        assertEquals(GameStatus.RUNNING, engine.status)
    }
}
