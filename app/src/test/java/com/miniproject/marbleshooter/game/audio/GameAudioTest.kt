package com.miniproject.marbleshooter.game.audio

import android.media.ToneGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToneGenerator
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameAudioTest {

    @Test
    fun mutedPlaybackDoesNotCreateOrRequestAudio() {
        ShadowToneGenerator.reset()
        val audio = GameAudio()

        audio.playShot(enabled = false)
        audio.playMatch(enabled = false)
        audio.playWin(enabled = false)
        audio.playLose(enabled = false)
        audio.playClick(enabled = false)
        audio.release()

        assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
    }

    @Test
    fun enabledPlaybackRequestsExpectedTonesAndReleaseDisablesFurtherPlayback() {
        ShadowToneGenerator.reset()
        val audio = GameAudio()

        audio.playShot(enabled = true)
        audio.playMatch(enabled = true)
        audio.playWin(enabled = true)
        audio.playLose(enabled = true)
        audio.playClick(enabled = true)

        val tones = ShadowToneGenerator.getPlayedTones()
        assertEquals(
            listOf(
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_ACK,
                ToneGenerator.TONE_PROP_PROMPT,
                ToneGenerator.TONE_PROP_NACK,
                ToneGenerator.TONE_PROP_BEEP
            ),
            tones.map { it.type() }
        )
        assertEquals(
            listOf(45L, 95L, 180L, 160L, 30L).map(Duration::ofMillis),
            tones.map { it.duration() }
        )

        audio.release()
        audio.playClick(enabled = true)
        assertEquals("Released audio must not be reused", 5, ShadowToneGenerator.getPlayedTones().size)
    }

    @Test
    fun releaseBeforeFirstSoundIsSafeAndKeepsAudioDisabled() {
        ShadowToneGenerator.reset()
        val audio = GameAudio()

        audio.release()
        audio.playClick(enabled = true)

        assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
    }
}
