package com.miniproject.marbleshooter.game.ui

import com.miniproject.marbleshooter.game.domain.model.Vec2

/** Platform-independent pointer actions understood by the game input router. */
internal enum class GamePointerAction {
    DOWN, MOVE, UP, CANCEL
}

/**
 * Converts a pointer gesture into aim, fire, booster or screen-tap callbacks.
 * Coordinate scaling and Android MotionEvent decoding remain in GameView.
 */
internal class GameInputRouter(
    private val isGameRunning: () -> Boolean,
    private val isBoosterTouch: (Vec2) -> Boolean,
    private val canStartAiming: (Vec2) -> Boolean,
    private val onAim: (Vec2) -> Unit,
    private val onFire: (Vec2) -> Unit,
    private val onBoosterTap: (Vec2) -> Unit,
    private val onScreenTap: (Vec2) -> Unit,
    private val onInvalidate: () -> Unit,
    private val onInteractionFinished: () -> Unit
) {
    private var aiming = false
    private var boosterTouch = false

    val hasActiveGesture: Boolean get() = aiming || boosterTouch

    fun cancel() {
        aiming = false
        boosterTouch = false
    }

    fun handle(action: GamePointerAction, target: Vec2): Boolean {
        when (action) {
            GamePointerAction.DOWN -> {
                boosterTouch = isGameRunning() && isBoosterTouch(target)
                if (!boosterTouch && isGameRunning() && canStartAiming(target)) {
                    aiming = true
                    onAim(target)
                    onInvalidate()
                }
            }
            GamePointerAction.MOVE -> {
                if (aiming && isGameRunning()) {
                    onAim(target)
                    onInvalidate()
                }
            }
            GamePointerAction.CANCEL -> {
                cancel()
                onInvalidate()
            }
            GamePointerAction.UP -> {
                when {
                    boosterTouch -> onBoosterTap(target)
                    aiming && isGameRunning() -> onFire(target)
                    else -> onScreenTap(target)
                }
                cancel()
                onInteractionFinished()
            }
        }
        return true
    }
}
