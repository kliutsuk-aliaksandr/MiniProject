package com.miniproject.marbleshooter.game.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for bounded fixed-step timing and pause/resume clock resets. */
class GameLoopControllerTest {
    @Test
    fun frameTimeProducesFixedStepsAndCatchUpIsBounded() {
        val steps = mutableListOf<Float>()
        val controller = GameLoopController { steps += it }
        controller.start(1_000_000_000L)

        controller.advanceFrame(1_017_000_000L)
        assertEquals(1, steps.size)
        assertEquals(GameLoopController.FIXED_STEP_SECONDS, steps.single(), 0.000001f)

        controller.advanceFrame(2_000_000_000L)
        assertTrue(steps.size in 2..17)
        assertTrue(steps.all { it == GameLoopController.FIXED_STEP_SECONDS })
    }

    @Test
    fun stopAndRestartDiscardElapsedBackgroundTime() {
        val steps = mutableListOf<Float>()
        val controller = GameLoopController { steps += it }
        controller.start(1_000_000_000L)
        controller.advanceFrame(1_034_000_000L)
        val stepsBeforePause = steps.size
        assertTrue(stepsBeforePause > 0)

        controller.stop()
        controller.start(10_000_000_000L)
        controller.advanceFrame(10_017_000_000L)

        assertEquals(stepsBeforePause + 1, steps.size)
    }

    @Test
    fun resettingAccumulatorDoesNotResetFrameClock() {
        val steps = mutableListOf<Float>()
        val controller = GameLoopController { steps += it }
        controller.start(1_000_000_000L)
        controller.advanceFrame(1_010_000_000L)
        assertTrue(steps.isEmpty())

        controller.resetAccumulator()
        controller.advanceFrame(1_020_000_000L)
        assertTrue(steps.isEmpty())

        controller.advanceFrame(1_037_000_000L)
        assertEquals(1, steps.size)
    }
}
