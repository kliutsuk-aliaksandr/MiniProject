package com.miniproject.marbleshooter.game.ui

import com.miniproject.marbleshooter.game.domain.model.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameInputRouterTest {

    @Test
    fun aimingUpdatesDuringDragAndFiresOnlyOnRelease() {
        val aimedAt = mutableListOf<Vec2>()
        val firedAt = mutableListOf<Vec2>()
        var screenTaps = 0
        val router = GameInputRouter(
            isGameRunning = { true },
            isBoosterTouch = { false },
            canStartAiming = { it.y > 145f },
            onAim = { aimedAt += it },
            onFire = { firedAt += it },
            onBoosterTap = {},
            onScreenTap = { screenTaps++ },
            onInvalidate = {},
            onInteractionFinished = {}
        )

        router.handle(GamePointerAction.DOWN, Vec2(100f, 300f))
        assertTrue(router.hasActiveGesture)
        assertTrue(firedAt.isEmpty())

        router.handle(GamePointerAction.MOVE, Vec2(120f, 320f))
        assertEquals(listOf(Vec2(100f, 300f), Vec2(120f, 320f)), aimedAt)
        assertTrue(firedAt.isEmpty())

        router.handle(GamePointerAction.UP, Vec2(140f, 340f))
        assertEquals(listOf(Vec2(140f, 340f)), firedAt)
        assertFalse(router.hasActiveGesture)
        assertEquals(0, screenTaps)
    }

    @Test
    fun boosterGestureDoesNotStartAimingOrFireShot() {
        var boosterTaps = 0
        var shots = 0
        val router = GameInputRouter(
            isGameRunning = { true },
            isBoosterTouch = { it.y in 710f..770f },
            canStartAiming = { true },
            onAim = {},
            onFire = { shots++ },
            onBoosterTap = { boosterTaps++ },
            onScreenTap = {},
            onInvalidate = {},
            onInteractionFinished = {}
        )

        router.handle(GamePointerAction.DOWN, Vec2(150f, 740f))
        assertTrue(router.hasActiveGesture)
        router.handle(GamePointerAction.UP, Vec2(150f, 740f))

        assertEquals(1, boosterTaps)
        assertEquals(0, shots)
        assertFalse(router.hasActiveGesture)
    }

    @Test
    fun cancelClearsGestureFlags() {
        var shots = 0
        val router = GameInputRouter(
            isGameRunning = { true },
            isBoosterTouch = { false },
            canStartAiming = { true },
            onAim = {},
            onFire = { shots++ },
            onBoosterTap = {},
            onScreenTap = {},
            onInvalidate = {},
            onInteractionFinished = {}
        )

        router.handle(GamePointerAction.DOWN, Vec2(200f, 300f))
        router.handle(GamePointerAction.CANCEL, Vec2(200f, 300f))

        assertFalse(router.hasActiveGesture)
        assertEquals(0, shots)
    }
}
