package com.miniproject.marbleshooter.game.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Small offline sound adapter; no media assets, network access or permissions.
 *
 * ToneGenerator is created on demand so muted sessions do not keep an unused native
 * audio track alive. Audio is best-effort: emulator or device audio-service failures
 * must never interrupt gameplay or accessibility actions.
 */
class GameAudio {
    private var generator: ToneGenerator? = null
    private var unavailable = false

    fun playShot(enabled: Boolean) = play(enabled, ToneGenerator.TONE_PROP_BEEP, 45)
    fun playMatch(enabled: Boolean) = play(enabled, ToneGenerator.TONE_PROP_ACK, 95)
    fun playWin(enabled: Boolean) = play(enabled, ToneGenerator.TONE_PROP_PROMPT, 180)
    fun playLose(enabled: Boolean) = play(enabled, ToneGenerator.TONE_PROP_NACK, 160)
    fun playClick(enabled: Boolean) = play(enabled, ToneGenerator.TONE_PROP_BEEP, 30)

    private fun play(enabled: Boolean, tone: Int, durationMs: Int) {
        if (!enabled || unavailable) return

        val activeGenerator = generator ?: runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, 55)
        }.getOrElse {
            unavailable = true
            return
        }.also { generator = it }

        val started = runCatching {
            activeGenerator.startTone(tone, durationMs)
        }.getOrDefault(false)

        if (!started) {
            generator = null
            unavailable = true
            runCatching { activeGenerator.release() }
        }
    }

    /** Releases the platform audio handle during Activity teardown. */
    fun release() {
        val activeGenerator = generator
        generator = null
        unavailable = true
        if (activeGenerator != null) {
            runCatching { activeGenerator.release() }
        }
    }
}
