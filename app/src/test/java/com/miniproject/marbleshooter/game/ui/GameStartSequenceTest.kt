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
        repeat(48) { sequence.advance(1f / 60f) }
        assertEquals(2, sequence.countdownNumber)
        repeat(60) { sequence.advance(1f / 60f) }
        assertEquals(1, sequence.countdownNumber)
        repeat(72) { sequence.advance(1f / 60f) }

        assertNull(sequence.countdownNumber)
        assertTrue(sequence.showStartPrompt)
        assertTrue(sequence.isInputLocked)
    }

    @Test
    fun startsFastRolloutOnceAfterStartPromptAndUnlocksAfterRollout() {
        val sequence = GameStartSequence()
        sequence.start()
        repeat(180) { sequence.advance(1f / 60f) }
        assertTrue(sequence.showStartPrompt)

        repeat(36) { assertFalse(sequence.advance(1f / 60f)) }
        assertTrue(sequence.isRollingOut)
        assertTrue(sequence.advance(1f / 60f).not())
        repeat(89) { sequence.advance(1f / 60f) }

        assertFalse(sequence.isActive)
        assertFalse(sequence.isInputLocked)
        assertFalse(sequence.advance(1f / 60f))
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
