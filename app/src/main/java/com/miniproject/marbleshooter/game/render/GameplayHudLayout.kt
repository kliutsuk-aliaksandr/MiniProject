package com.miniproject.marbleshooter.game.render

/** Shared positions for the lower booster dock and its touch/accessibility hit targets. */
internal object GameplayHudLayout {
    const val BOOSTER_PANEL_LEFT = 12f
    const val BOOSTER_PANEL_TOP = 858f
    const val BOOSTER_PANEL_RIGHT = 408f
    const val BOOSTER_PANEL_BOTTOM = 970f

    const val BOOSTER_LABEL_Y = 887f
    const val BOOSTER_CENTER_Y = 925f
    const val BOOSTER_COUNT_Y = 930f
    const val BOOSTER_BUTTON_RADIUS = 23f
    const val BOOSTER_HIT_RADIUS = 34f
    const val BOOSTER_HIT_TOP = 895f
    const val BOOSTER_HIT_BOTTOM = 955f

    val BOOSTER_CENTERS: List<Float> = listOf(40f, 150f, 270f, 380f)

    fun isBoosterHit(x: Float, y: Float): Boolean =
        y in BOOSTER_HIT_TOP..BOOSTER_HIT_BOTTOM &&
            BOOSTER_CENTERS.any { center -> kotlin.math.abs(center - x) <= BOOSTER_HIT_RADIUS }
}
