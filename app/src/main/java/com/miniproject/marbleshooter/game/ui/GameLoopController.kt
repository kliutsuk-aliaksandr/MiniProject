package com.miniproject.marbleshooter.game.ui

/**
 * Owns frame-time accumulation and fixed-rate simulation stepping.
 *
 * The callback is retained once at construction, so the hot frame path does not
 * allocate a new lambda. start/stop discard elapsed background time; resetAccumulator
 * starts a new round without changing the frame clock.
 */
internal class GameLoopController(
    private val onFixedStep: (Float) -> Unit
) {
    private var accumulatorSeconds = 0f
    private var lastFrameNanos = 0L

    /** Starts a fresh timing window at the supplied monotonic clock value. */
    fun start(nowNanos: Long) {
        accumulatorSeconds = 0f
        lastFrameNanos = nowNanos
    }

    /** Stops the loop and discards accumulated time so background time is not simulated. */
    fun stop() {
        accumulatorSeconds = 0f
        lastFrameNanos = 0L
    }

    /** Discards pending partial ticks while keeping the current frame clock. */
    fun resetAccumulator() {
        accumulatorSeconds = 0f
    }

    /** Adds one frame duration and executes a bounded number of fixed simulation steps. */
    fun advanceFrame(nowNanos: Long) {
        val frameDelta = if (lastFrameNanos == 0L) 0f
        else (nowNanos - lastFrameNanos) / NANOS_PER_SECOND
        lastFrameNanos = nowNanos

        accumulatorSeconds = (
            accumulatorSeconds + frameDelta.coerceIn(0f, MAX_FRAME_DELTA_SECONDS)
        ).coerceAtMost(MAX_FRAME_DELTA_SECONDS)

        var steps = 0
        while (accumulatorSeconds >= FIXED_STEP_SECONDS && steps < MAX_CATCH_UP_STEPS) {
            onFixedStep(FIXED_STEP_SECONDS)
            accumulatorSeconds -= FIXED_STEP_SECONDS
            steps++
        }
    }

    internal companion object {
        const val FIXED_STEP_SECONDS = 1f / 60f
        private const val MAX_FRAME_DELTA_SECONDS = 0.25f
        private const val MAX_CATCH_UP_STEPS = 16
        private const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
