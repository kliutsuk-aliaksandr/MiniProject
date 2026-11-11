package com.miniproject.marbleshooter.game.ui

import android.content.Context
import com.miniproject.marbleshooter.game.data.OfflineProgressRepository
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.OfflineAchievement
import com.miniproject.marbleshooter.game.render.GameScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameSessionControllerTest {

    @Test
    fun boosterDoesNotTriggerHapticsWhenVibrationIsDisabled() {
        val context = freshContext(vibrationEnabled = false)
        var hapticCount = 0
        val session = GameSessionController(context) { hapticCount++ }

        try {
            assertTrue(session.engine.activateBooster(BoosterType.SLOW_TIME))
            session.handlePendingEvents(isGameScreen = true)

            assertEquals(0, hapticCount)
        } finally {
            session.disposeGame(GameScreen.MENU)
            clearPreferences(context)
        }
    }

    @Test
    fun boosterTriggersHapticsWhenVibrationIsEnabled() {
        val context = freshContext(vibrationEnabled = true)
        var hapticCount = 0
        val session = GameSessionController(context) { hapticCount++ }

        try {
            assertTrue(session.engine.activateBooster(BoosterType.SLOW_TIME))
            session.handlePendingEvents(isGameScreen = true)

            assertEquals(1, hapticCount)
        } finally {
            session.disposeGame(GameScreen.MENU)
            clearPreferences(context)
        }
    }


    @Test
    fun lifetimeStatisticsRecordCampaignActivityButIgnoreTutorialPractice() {
        val context = freshContext(vibrationEnabled = false)
        val session = GameSessionController(context) { }

        try {
            session.engine.fireShot()
            session.handlePendingEvents(isGameScreen = true)
            assertEquals(1, OfflineProgressRepository(context).statistics.totalShots)

            session.engine.startTutorial()
            session.engine.drainEvents()
            session.engine.fireShot()
            session.handlePendingEvents(isGameScreen = true)
            assertTrue(session.engine.activateBooster(BoosterType.SLOW_TIME))
            session.handlePendingEvents(isGameScreen = true)

            val stats = OfflineProgressRepository(context).statistics
            assertEquals(1, stats.totalShots)
            assertEquals(0, stats.usedBoosterMask)
            val achievements = OfflineProgressRepository(context).unlockedAchievements
            assertTrue(achievements.contains(OfflineAchievement.FIRST_SHOT))
            assertTrue(achievements.none { it == OfflineAchievement.BOOSTER_COLLECTOR })
        } finally {
            session.disposeGame(GameScreen.MENU)
            clearPreferences(context)
            context.getSharedPreferences("offline_progress", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    private fun freshContext(vibrationEnabled: Boolean): Context =
        RuntimeEnvironment.getApplication().also { context ->
            context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("vibration_enabled", vibrationEnabled)
                .apply()
            context.getSharedPreferences("offline_progress", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

    private fun clearPreferences(context: Context) {
        context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
