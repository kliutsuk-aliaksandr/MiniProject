package com.miniproject.marbleshooter.game.ui

import kotlin.math.ceil

/**
 * Pure timing state machine for the campaign start: 3, 2, 1, START, then a short rollout.
 * The session advances it using the same fixed simulation step as the game engine.
 */
internal class GameStartSequence(
    val countdownDurationSeconds: Float = 3f,
    val startPromptDurationSeconds: Float = 0.6f,
    val rolloutDurationSeconds: Float = 1.5f
) {
    private enum class Phase { IDLE, COUNTDOWN, START_PROMPT, ROLLOUT, COMPLETE }

    private var phase = Phase.IDLE
    private var remainingSeconds = 0f
    private val rolloutDurationSecondsInternal = rolloutDurationSeconds

    init {
        require(countdownDurationSeconds.isFinite() && countdownDurationSeconds > 0f)
        require(startPromptDurationSeconds.isFinite() && startPromptDurationSeconds > 0f)
        require(rolloutDurationSeconds.isFinite() && rolloutDurationSeconds > 0f)
    }

    val isActive: Boolean get() = phase == Phase.COUNTDOWN ||
        phase == Phase.START_PROMPT || phase == Phase.ROLLOUT

    val isInputLocked: Boolean get() = isActive

    val isRollingOut: Boolean get() = phase == Phase.ROLLOUT

    val rolloutDurationSeconds: Float get() = rolloutDurationSecondsInternal

    val countdownNumber: Int?
        get() = if (phase == Phase.COUNTDOWN) ceil(remainingSeconds).toInt().coerceIn(1, 3) else null

    val showStartPrompt: Boolean get() = phase == Phase.START_PROMPT

    fun start() {
        phase = Phase.COUNTDOWN
        remainingSeconds = countdownDurationSeconds
    }

    fun cancel() {
        phase = Phase.IDLE
        remainingSeconds = 0f
    }

    /**
     * Advances one fixed step. Returns true exactly once when the START prompt ends
     * and the engine should begin its fast chain rollout.
     */
    fun advance(deltaSeconds: Float): Boolean {
        if (!isActive || !deltaSeconds.isFinite() || deltaSeconds <= 0f) return false
        remainingSeconds = (remainingSeconds - deltaSeconds).coerceAtLeast(0f)
        when (phase) {
            Phase.COUNTDOWN -> if (remainingSeconds <= 0f) {
                phase = Phase.START_PROMPT
                remainingSeconds = startPromptDurationSeconds
            }
            Phase.START_PROMPT -> if (remainingSeconds <= 0f) {
                phase = Phase.ROLLOUT
                remainingSeconds = rolloutDurationSeconds
                return true
            }
            Phase.ROLLOUT -> if (remainingSeconds <= 0f) {
                phase = Phase.COMPLETE
            }
            Phase.IDLE, Phase.COMPLETE -> Unit
        }
        return false
    }
}
