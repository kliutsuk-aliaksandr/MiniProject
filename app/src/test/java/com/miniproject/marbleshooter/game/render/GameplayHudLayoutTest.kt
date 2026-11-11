package com.miniproject.marbleshooter.game.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayHudLayoutTest {

    @Test
    fun boosterDockSitsBelowTheFrogAndInsideTheWorld() {
        val frogBottom = 810f + 32f
        assertTrue(GameplayHudLayout.BOOSTER_PANEL_TOP > frogBottom)
        assertTrue(GameplayHudLayout.BOOSTER_PANEL_BOTTOM < 980f)
        assertTrue(GameplayHudLayout.BOOSTER_PANEL_LEFT >= 0f)
        assertTrue(GameplayHudLayout.BOOSTER_PANEL_RIGHT <= 420f)
    }

    @Test
    fun boosterHitTargetsMatchTheVisibleLowerDock() {
        GameplayHudLayout.BOOSTER_CENTERS.forEach { center ->
            assertTrue(GameplayHudLayout.isBoosterHit(center, GameplayHudLayout.BOOSTER_CENTER_Y))
        }
        assertFalse(GameplayHudLayout.isBoosterHit(210f, 737f))
        assertFalse(GameplayHudLayout.isBoosterHit(40f, 800f))
        assertEquals(4, GameplayHudLayout.BOOSTER_CENTERS.size)
    }
}
