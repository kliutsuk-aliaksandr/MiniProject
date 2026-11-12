package com.miniproject.marbleshooter.game.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStartSequenceTest {

    @Test
    fun countsDownThreeTwoOneThenShowsStartPrompt() {
        val sequence = GameStartSequence()
        sequence.start()

        assertEquals(3, sequence.countdownNumber)
        assertFalse(sequence.advance(1f))
        assertEquals(2, sequence.countdownNumber)
        assertFalse(sequence.advance(1f))
        assertEquals(1, sequence.countdownNumber)
        assertFalse(sequence.advance(1f))

        assertNull(sequence.countdownNumber)
        assertTrue(sequence.showStartPrompt)
        assertTrue(sequence.isInputLocked)
    }

    @Test
    fun startsFastRolloutOnceAfterStartPromptAndUnlocksAfterRollout() {
        val sequence = GameStartSequence()
        sequence.start()
        repeat(3) { sequence.advance(1f) }
        assertTrue(sequence.showStartPrompt)

        assertTrue(sequence.advance(0.6f))
        assertTrue(sequence.isRollingOut)
        assertFalse(sequence.advance(1.5f))

        assertFalse(sequence.isActive)
        assertFalse(sequence.isInputLocked)
        assertFalse(sequence.advance(1f))
    }

    @Test
    fun cancelImmediatelyUnlocksAndClearsTheCountdown() {
        val sequence = GameStartSequence()
        sequence.start()
        repeat(20) { sequence.advance(1f / 60f) }

        sequence.cancel()

        assertFalse(sequence.isActive)
        assertFalse(sequence.isInputLocked)
        assertNull(sequence.countdownNumber)
        assertFalse(sequence.showStartPrompt)
    }
}
