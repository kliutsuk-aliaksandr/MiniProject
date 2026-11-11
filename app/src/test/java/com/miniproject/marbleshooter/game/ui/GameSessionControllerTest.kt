package com.miniproject.marbleshooter.game.ui

import android.content.Context
import com.miniproject.marbleshooter.game.domain.model.BoosterType
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

    private fun freshContext(vibrationEnabled: Boolean): Context =
        RuntimeEnvironment.getApplication().also { context ->
            context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("vibration_enabled", vibrationEnabled)
                .apply()
        }

    private fun clearPreferences(context: Context) {
        context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
