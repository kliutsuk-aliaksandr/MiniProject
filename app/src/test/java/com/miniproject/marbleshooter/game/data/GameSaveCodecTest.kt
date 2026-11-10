package com.miniproject.marbleshooter.game.data

import com.miniproject.marbleshooter.game.domain.model.Ball
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression tests for strict snapshot parsing and compatibility with unversioned saves. */
class GameSaveCodecTest {
    @Test
    fun validLegacyPayloadRoundTrips() {
        val legacy = "0:620.0;1:588.0;3:556.0"
        val decoded = GameSaveCodec.decodeBalls(legacy)
        assertEquals(listOf(Ball(0, 620f), Ball(1, 588f), Ball(3, 556f)), decoded)
        assertEquals(legacy, GameSaveCodec.encodeBalls(requireNotNull(decoded)))
    }

    @Test
    fun malformedTokenRejectsTheEntireChain() {
        assertNull(GameSaveCodec.decodeBalls("0:620.0;not-a-ball;1:588.0"))
        assertNull(GameSaveCodec.decodeBalls("0:620.0;1:NaN"))
        assertNull(GameSaveCodec.decodeBalls("0:620.0;1:Infinity"))
        assertNull(GameSaveCodec.decodeBalls("0:620.0;1:588.0;"))
    }

    @Test
    fun blankPayloadIsRejected() {
        assertNull(GameSaveCodec.decodeBalls(""))
        assertNull(GameSaveCodec.decodeBalls("   "))
        assertNull(GameSaveCodec.decodeBalls(";;"))
    }

    @Test
    fun encodedPayloadContainsEveryBall() {
        val encoded = GameSaveCodec.encodeBalls(listOf(Ball(2, 500f), Ball(1, 468f)))
        assertNotNull(GameSaveCodec.decodeBalls(encoded))
        assertEquals(2, requireNotNull(GameSaveCodec.decodeBalls(encoded)).size)
    }
}
