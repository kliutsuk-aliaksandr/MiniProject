package com.miniproject.marbleshooter.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import com.miniproject.marbleshooter.game.domain.TrackId
import com.miniproject.marbleshooter.game.domain.model.ObstacleType
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun newGameStartsWithAChainAndZeroScore() {
        val engine = GameEngine(Random(7))

        assertEquals(12, engine.ballCount)
        assertEquals(1, engine.level)
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
        assertTrue(engine.drainEvents().contains(GameEvent.ShotFired))
    }

    @Test
    fun aimCanBeUpdatedDuringDragWithoutFiringUntilRelease() {
        val engine = GameEngine(Random(12))

        engine.aimAt(Vec2(90f, 360f))

        assertEquals(Vec2(90f, 360f), engine.aimTarget)
        assertEquals(0, engine.projectileCount)

        engine.fireShot()

        assertEquals(1, engine.projectileCount)
        assertEquals(Vec2(90f, 360f), engine.aimTarget)
    }

    @Test
    fun aimTargetIsKeptInsideThePlayableWorld() {
        val engine = GameEngine(Random(13))

        engine.aimAt(Vec2(900f, 950f))

        assertEquals(GameEngine.WORLD_WIDTH, engine.aimTarget.x, 0.001f)
        assertEquals(GameEngine.SHOOTER_Y - 180f, engine.aimTarget.y, 0.001f)
    }

    @Test
    fun updateMovesEveryBallAndPreservesSpacing() {
        val engine = GameEngine(Random(15))
        val initialProgress = engine.balls.map { it.progress }

        engine.update(0.05f)

        engine.balls.forEachIndexed { index, ball ->
            assertTrue(ball.progress > initialProgress[index])
            if (index > 0) {
                assertEquals(
                    GameEngine.BALL_SPACING,
                    engine.balls[index - 1].progress - ball.progress,
                    0.01f
                )
            }
        }
    }

    @Test
    fun insertingBehindHitBallRemovesThreeMatchingMarbles() {
        val engine = GameEngine(Random(21))
        setChain(engine, listOf(0, 0, 1, 1, 2))
        engine.insertBallAt(hitIndex = 2, colorIndex = 1, insertInFront = false)
        val frontBefore = engine.balls.take(2).map { it.progress }
        val rearBefore = engine.balls.last().progress

        assertEquals(3, engine.ballCount)
        assertEquals(30, engine.score)
        assertEquals(listOf(0, 0, 2), engine.balls.map { it.colorIndex })
        assertEquals(3, engine.drainEvents().filterIsInstance<GameEvent.MatchCleared>().single().count)
        assertEquals(frontBefore, engine.balls.take(2).map { it.progress })
        assertEquals(rearBefore, engine.balls.last().progress)
    }

    @Test
    fun matchRetractsFrogSideBallsWhileFarSideBallsStayStill() {
        val engine = GameEngine(Random(211))
        setChain(engine, listOf(0, 0, 1, 1, 2))
        engine.insertBallAt(hitIndex = 2, colorIndex = 1, insertInFront = false)

        val frontStart = engine.balls.take(2).map { it.progress }
        val rearStart = engine.balls.last().progress
        repeat(2) { engine.update(0.05f) }

        assertTrue(engine.balls[0].progress < frontStart[0])
        assertTrue(engine.balls[1].progress < frontStart[1])
        assertEquals(rearStart, engine.balls.last().progress, 0.001f)

        repeat(4) { engine.update(0.05f) }
        assertEquals(rearStart + GameEngine.BALL_SPACING * 2, engine.balls[0].progress, 0.01f)
        assertEquals(rearStart + GameEngine.BALL_SPACING, engine.balls[1].progress, 0.01f)
        assertEquals(rearStart, engine.balls.last().progress, 0.01f)
    }

    @Test
    fun saveSnapshotDuringRetractionStoresSettledPositions() {
        val engine = GameEngine(Random(212))
        setChain(engine, listOf(0, 0, 1, 1, 2))
        engine.insertBallAt(hitIndex = 2, colorIndex = 1, insertInFront = false)
        assertEquals(listOf(704f, 672f, 640f), engine.saveState().balls.map { it.progress })
    }

    @Test
    fun insertingInFrontOfHitBallPreservesOrderAndResolvesMatch() {
        val engine = GameEngine(Random(22))
        setChain(engine, listOf(0, 1, 1, 2))

        engine.insertBallAt(hitIndex = 1, colorIndex = 1, insertInFront = true)

        assertEquals(listOf(0, 2), engine.balls.map { it.colorIndex })
        assertEquals(30, engine.score)
        assertTrue(engine.balls[0].progress > engine.balls[1].progress)
    }

    @Test
    fun removingAGroupCanTriggerACascadeOnTheNewlyJoinedGroups() {
        val engine = GameEngine(Random(23))
        setChain(engine, listOf(0, 0, 1, 1, 0, 0))

        engine.insertBallAt(hitIndex = 2, colorIndex = 1, insertInFront = false)
        assertEquals(4, engine.ballCount)
        assertEquals(30, engine.score)
        assertEquals(GameStatus.RUNNING, engine.status)

        repeat(12) { engine.update(0.05f) }

        assertEquals(0, engine.ballCount)
        assertEquals(70, engine.score)
        assertEquals(GameStatus.WON, engine.status)
        assertEquals(
            listOf(3, 4),
            engine.drainEvents().filterIsInstance<GameEvent.MatchCleared>().map { it.count }
        )
    }

    @Test
    fun pauseStopsSimulationAndSnapshotCanBeRestored() {
        val engine = GameEngine(Random(24))
        engine.update(0.05f)
        engine.pauseRound()
        val progressAtPause = engine.headProgress
        val snapshot = engine.saveState()

        engine.update(0.05f)
        assertEquals(progressAtPause, engine.headProgress, 0.001f)

        val restored = GameEngine(Random(25))
        assertTrue(restored.restoreState(snapshot))
        assertEquals(GameStatus.PAUSED, restored.status)
        assertEquals(engine.level, restored.level)
        assertEquals(engine.score, restored.score)
        assertEquals(
            engine.balls.map { it.colorIndex to it.progress },
            restored.balls.map { it.colorIndex to it.progress }
        )
        restored.resumeRound()
        assertEquals(GameStatus.RUNNING, restored.status)
    }

    @Test
    fun eachNewLevelIncreasesChainLengthAndSpeed() {
        val engine = GameEngine(Random(26))
        val firstSpeed = engine.chainSpeed
        val firstCount = engine.targetBallCount

        repeat(3) {
            engine.balls.clear()
            engine.update(0.01f)
            assertEquals(GameStatus.WON, engine.status)
            assertTrue(engine.startNextLevel())
        }

        assertEquals(4, engine.level)
        assertEquals(firstCount + 6, engine.targetBallCount)
        assertTrue(engine.chainSpeed > firstSpeed)
        assertEquals(4, engine.activeColorCount)
        assertEquals(GameStatus.RUNNING, engine.status)
    }

    @Test
    fun startNewGameResetsScoreLevelAndProjectiles() {
        val engine = GameEngine(Random(27))
        engine.onTap(Vec2(150f, 380f))
        engine.update(0.01f)

        engine.startNewGame()

        assertEquals(0, engine.score)
        assertEquals(1, engine.level)
        assertEquals(0, engine.projectileCount)
        assertEquals(12, engine.ballCount)
        assertEquals(GameStatus.RUNNING, engine.status)
    }

    @Test
    fun levelCatalogProvidesFourDifferentRoutesAndProgressivelyHarderLevels() {
        val routes = TrackId.values().map { track ->
            LevelCatalog.route(track).map { point -> point.x to point.y }
        }
        assertEquals(4, routes.distinct().size)
        assertEquals(TrackId.DOUBLE_LOOP, LevelCatalog.definition(9).trackId)
        assertEquals(TrackId.SWITCHBACK, LevelCatalog.definition(16).trackId)
        assertEquals(TrackId.SPIRAL, LevelCatalog.definition(23).trackId)
        assertTrue(LevelCatalog.definition(20).ballCount > LevelCatalog.definition(2).ballCount)
        assertTrue(LevelCatalog.definition(20).chainSpeed > LevelCatalog.definition(2).chainSpeed)
    }

    @Test
    fun slowTimeBoosterConsumesChargeAndSlowsTheChain() {
        val engine = GameEngine(Random(31))
        val before = engine.headProgress
        val charges = engine.boosters.slowTime

        assertTrue(engine.activateBooster(BoosterType.SLOW_TIME))
        engine.update(0.05f)

        assertEquals(charges - 1, engine.boosters.slowTime)
        assertTrue(engine.slowTimeRemaining > 0f)
        assertTrue(engine.headProgress - before < engine.chainSpeed * 0.05f)
    }

    @Test
    fun reverseBoosterMovesTheChainAwayFromTheFinish() {
        val engine = GameEngine(Random(32))
        val before = engine.headProgress

        assertTrue(engine.activateBooster(BoosterType.REVERSE_CHAIN))
        engine.update(0.05f)

        assertTrue(engine.reverseTimeRemaining > 0f)
        assertTrue(engine.headProgress < before)
        assertEquals(0, engine.boosters.reverseChain)
    }

    @Test
    fun bombBoosterRemovesAContiguousSectionAndStartsRetraction() {
        val engine = GameEngine(Random(33))
        setChain(engine, listOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3))
        val target = engine.positionOnRoute(engine.balls[5].progress)

        assertTrue(engine.activateBooster(BoosterType.BOMB, target))

        assertEquals(7, engine.ballCount)
        assertEquals(1, engine.boosters.bomb)
        assertTrue(engine.drainEvents().any { it is GameEvent.MatchCleared })
        repeat(12) { engine.update(0.05f) }
        assertTrue(engine.balls.zipWithNext().all { (front, rear) -> front.progress > rear.progress })
    }

    @Test
    fun colorChangeBoosterSelectsAnotherColorPresentInTheChain() {
        val engine = GameEngine(Random(34))
        setChain(engine, listOf(2, 2, 2, 2, 1, 1, 0, 3))
        val previousAmmo = engine.ammoColor

        assertTrue(engine.activateBooster(BoosterType.COLOR_CHANGE))

        assertNotEquals(previousAmmo, engine.ammoColor)
        assertTrue(engine.balls.any { it.colorIndex == engine.ammoColor })
        assertEquals(2, engine.boosters.colorChange)
    }

    @Test
    fun obstacleSlowsTheChainAtItsRoutePosition() {
        val engine = GameEngine(Random(35))
        engine.startLevel(3)
        val obstacle = engine.obstacles.first { it.type == ObstacleType.MUD }
        engine.balls.clear()
        engine.balls += Ball(0, obstacle.progress)
        engine.balls += Ball(1, obstacle.progress - GameEngine.BALL_SPACING)
        val before = engine.headProgress

        engine.update(0.05f)

        assertTrue(engine.headProgress - before < engine.chainSpeed * 0.05f)
    }

    @Test
    fun emptyingLevelAwardsThreeStarsAndEmitsCompletion() {
        val engine = GameEngine(Random(36))
        engine.balls.clear()

        engine.update(0.01f)

        assertEquals(GameStatus.WON, engine.status)
        assertEquals(3, engine.lastStars)
        val completed = engine.drainEvents().filterIsInstance<GameEvent.LevelCompleted>().single()
        assertEquals(3, completed.stars)
        assertEquals(1, completed.level)
    }

    @Test
    fun tutorialStartsWithItsOwnPracticeChainAndBoosters() {
        val engine = GameEngine(Random(37))
        engine.startTutorial()

        assertTrue(engine.isTutorial)
        assertEquals(9, engine.ballCount)
        assertEquals(2, engine.boosters.slowTime)
        assertEquals(1, engine.boosters.bomb)
        assertTrue(engine.chainSpeed < 20f)
    }

    @Test
    fun invalidSnapshotIsRejected() {
        val engine = GameEngine(Random(28))
        val invalid = GameSaveState(
            level = 0,
            score = 0,
            ammoColor = 0,
            nextColor = 1,
            aimTarget = Vec2(210f, 500f),
            isPaused = true,
            balls = listOf(Ball(0, 100f))
        )

        assertFalse(engine.restoreState(invalid))
    }

    private fun setChain(engine: GameEngine, colors: List<Int>) {
        engine.balls.clear()
        colors.forEachIndexed { index, color ->
            engine.balls += Ball(color, 800f - index * GameEngine.BALL_SPACING)
        }
    }
}
