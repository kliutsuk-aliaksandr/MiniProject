package com.miniproject.marbleshooter.game.ui

import android.content.Context
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.render.GameScreen
import com.miniproject.marbleshooter.game.render.GameTextResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameNavigationControllerTest {

    @Test
    fun selectingChallengeStartsRunningGameplay() {
        val context = freshContext()
        val session = GameSessionController(context) { }
        val navigation = GameNavigationController(session, GameTextResources(context))

        try {
            // Menu -> level map -> challenges -> first challenge.
            navigation.handleTap(Vec2(210f, 535f))
            assertEquals(GameScreen.LEVEL_MAP, navigation.screen)
            navigation.handleTap(Vec2(210f, 645f))
            assertEquals(GameScreen.CHALLENGES, navigation.screen)
            navigation.handleTap(Vec2(210f, 270f))

            assertEquals(GameScreen.GAME, navigation.screen)
            assertEquals(GameStatus.RUNNING, session.engine.status)
            assertTrue("Selected challenge must stay isolated from campaign mode", session.engine.isChallenge)
            assertEquals(1, requireNotNull(session.engine.challengeDefinition).id)
        } finally {
            session.disposeGame(GameScreen.MENU)
            clearProgress(context)
        }
    }

    @Test
    fun restartingPausedTutorialStartsTutorialPracticeAgain() {
        val context = freshContext()
        val session = GameSessionController(context) { }
        val navigation = GameNavigationController(session, GameTextResources(context))

        try {
            navigation.handleTap(Vec2(210f, 603f))
            repeat(4) { navigation.handleTap(Vec2(300f, 667f)) }
            navigation.handleTap(Vec2(300f, 667f))

            assertEquals(GameScreen.GAME, navigation.screen)
            assertTrue(session.engine.isTutorial)
            assertEquals(9, session.engine.ballCount)
            assertEquals(2, session.engine.boosters.slowTime)
            assertFalse(session.hasSavedGame)

            session.engine.pauseRound()
            assertEquals(GameStatus.PAUSED, session.engine.status)
            navigation.handleTap(Vec2(210f, 545f))

            assertEquals(GameScreen.GAME, navigation.screen)
            assertEquals(GameStatus.RUNNING, session.engine.status)
            assertTrue("Restart must stay in tutorial practice", session.engine.isTutorial)
            assertEquals("Tutorial chain must be restored", 9, session.engine.ballCount)
            assertEquals(2, session.engine.boosters.slowTime)
            assertEquals(1, session.engine.boosters.bomb)
            assertFalse(session.hasSavedGame)
        } finally {
            session.disposeGame(GameScreen.MENU)
            clearProgress(context)
        }
    }

    private fun freshContext(): Context =
        RuntimeEnvironment.getApplication().also(::clearProgress)

    private fun clearProgress(context: Context) {
        listOf("mini_project", "offline_progress", "offline_challenge_progress", "level_progress")
            .forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }
    }
}
