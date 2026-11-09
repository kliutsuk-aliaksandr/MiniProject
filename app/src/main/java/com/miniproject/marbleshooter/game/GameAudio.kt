package com.miniproject.marbleshooter.game

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Lightweight offline sound effects. ToneGenerator avoids external audio assets
 * and works without network access or additional permissions.
 */
class GameAudio {

    private val generator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    }.getOrNull()

    fun playShot(enabled: Boolean) {
        play(enabled, ToneGenerator.TONE_PROP_BEEP, 45)
    }

    fun playMatch(enabled: Boolean) {
        play(enabled, ToneGenerator.TONE_PROP_ACK, 95)
    }

    fun playWin(enabled: Boolean) {
        play(enabled, ToneGenerator.TONE_PROP_PROMPT, 180)
    }

    fun playLose(enabled: Boolean) {
        play(enabled, ToneGenerator.TONE_PROP_NACK, 160)
    }

    fun playClick(enabled: Boolean) {
        play(enabled, ToneGenerator.TONE_PROP_BEEP, 30)
    }

    private fun play(enabled: Boolean, tone: Int, durationMs: Int) {
        if (enabled) generator?.startTone(tone, durationMs)
    }

    fun release() {
        generator?.release()
    }
}
