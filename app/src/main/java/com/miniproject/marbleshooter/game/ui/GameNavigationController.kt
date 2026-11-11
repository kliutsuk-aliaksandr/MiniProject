package com.miniproject.marbleshooter.game.ui

import android.graphics.Rect
import com.miniproject.marbleshooter.R
import com.miniproject.marbleshooter.game.render.GameTextResources
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.render.GameScreen
import kotlin.math.abs

/**
 * Owns screen state and all logical navigation/tap hit areas.
 * It delegates campaign and gameplay mutations to GameSessionController.
 */
internal class GameNavigationController(
    private val session: GameSessionController,
    private val textResources: GameTextResources
) {
    var screen: GameScreen = GameScreen.MENU
        private set
    var tutorialPage: Int = 0
        private set

    private val engine get() = session.engine

    fun handleSystemBack(): Boolean {
        when (screen) {
            GameScreen.SETTINGS, GameScreen.PRIVACY, GameScreen.LEVEL_MAP, GameScreen.TUTORIAL -> {
                screen = if (screen == GameScreen.PRIVACY) GameScreen.SETTINGS else GameScreen.MENU
            }
            GameScreen.MENU -> return false
            GameScreen.GAME -> {
                when (engine.status) {
                    GameStatus.RUNNING -> engine.pauseRound()
                    GameStatus.PAUSED -> screen = GameScreen.MENU
                    GameStatus.WON, GameStatus.LOST -> screen = GameScreen.MENU
                }
                session.saveGameState(screen == GameScreen.GAME)
            }
        }
        return true
    }

    fun handleTap(target: Vec2) {
        when (screen) {
            GameScreen.MENU -> handleMenuTap(target)
            GameScreen.LEVEL_MAP -> handleLevelMapTap(target)
            GameScreen.TUTORIAL -> handleTutorialTap(target)
            GameScreen.SETTINGS -> handleSettingsTap(target)
            GameScreen.PRIVACY -> if (inButton(target, 790f, 845f)) {
                screen = GameScreen.SETTINGS
                session.playClick()
            }
            GameScreen.GAME -> handleGameTap(target)
        }
    }

    fun handleBoosterTap(target: Vec2) {
        val centers = BOOSTER_CENTERS
        if (target.y !in 710f..770f) return
        val nearest = centers.indices.minByOrNull { index -> abs(centers[index] - target.x) } ?: return
        if (abs(centers[nearest] - target.x) > 27f) return
        val type = when (nearest) {
            0 -> BoosterType.SLOW_TIME
            1 -> BoosterType.BOMB
            2 -> BoosterType.COLOR_CHANGE
            else -> BoosterType.REVERSE_CHAIN
        }
        // The HUD touch coordinate is not a world-space target. Bomb uses the aim point.
        session.activateBooster(type, isGameScreen = screen == GameScreen.GAME)
    }

    fun isBoosterHitTarget(target: Vec2): Boolean =
        target.y in 710f..770f && BOOSTER_CENTERS.any { abs(it - target.x) <= 27f }

    fun canStartAiming(target: Vec2): Boolean =
        target.y > 145f && target.y !in 700f..780f && !engine.isAnimatingRetraction

    /** Supplies current Canvas controls to the virtual accessibility-node provider. */
    fun accessibilityTargets(scale: Float, offsetX: Float, offsetY: Float): List<GameAccessibilityTarget> {
        val result = mutableListOf<GameAccessibilityTarget>()
        fun localized(resourceId: Int, vararg formatArgs: Any) =
            textResources.getString(session.language, resourceId, *formatArgs)
        fun add(id: Int, label: String, l: Float, t: Float, r: Float, b: Float, action: () -> Unit) {
            result += GameAccessibilityTarget(
                id, label,
                Rect(
                    (offsetX + l * scale).toInt(), (offsetY + t * scale).toInt(),
                    (offsetX + r * scale).toInt(), (offsetY + b * scale).toInt()
                ), action
            )
        }

        when (screen) {
            GameScreen.MENU -> {
                if (session.hasSavedGame) {
                    add(1, localized(R.string.a11y_continue_game), 78f, 410f, 342f, 464f) { handleMenuTap(Vec2(210f, 437f)) }
                    add(2, localized(R.string.a11y_new_game), 78f, 474f, 342f, 528f) { handleMenuTap(Vec2(210f, 501f)) }
                    add(3, localized(R.string.a11y_level_map), 78f, 538f, 342f, 592f) { handleMenuTap(Vec2(210f, 565f)) }
                    add(4, localized(R.string.a11y_tutorial), 78f, 602f, 342f, 656f) { handleMenuTap(Vec2(210f, 629f)) }
                    add(5, localized(R.string.a11y_settings), 78f, 666f, 342f, 720f) { handleMenuTap(Vec2(210f, 693f)) }
                } else {
                    add(1, localized(R.string.a11y_play), 78f, 438f, 342f, 496f) { handleMenuTap(Vec2(210f, 467f)) }
                    add(3, localized(R.string.a11y_level_map), 78f, 506f, 342f, 564f) { handleMenuTap(Vec2(210f, 535f)) }
                    add(4, localized(R.string.a11y_tutorial), 78f, 574f, 342f, 632f) { handleMenuTap(Vec2(210f, 603f)) }
                    add(5, localized(R.string.a11y_settings), 78f, 642f, 342f, 700f) { handleMenuTap(Vec2(210f, 671f)) }
                }
            }
            GameScreen.LEVEL_MAP -> {
                add(10, localized(R.string.a11y_main_menu), 78f, 750f, 342f, 810f) { handleLevelMapTap(Vec2(210f, 780f)) }
                for (level in 1..session.unlockedLevel) {
                    val index = level - 1
                    val left = 22f + (index % 5) * 76f
                    val top = 220f + (index / 5) * 66f
                    add(100 + level, localized(R.string.a11y_level, level), left, top, left + 68f, top + 58f) {
                        handleLevelMapTap(Vec2(left + 34f, top + 29f))
                    }
                }
            }
            GameScreen.TUTORIAL -> {
                add(20, localized(R.string.a11y_close_tutorial), 290f, 775f, 380f, 835f) { handleTutorialTap(Vec2(330f, 805f)) }
                if (tutorialPage > 0) add(21, localized(R.string.a11y_previous_lesson), 60f, 712f, 190f, 760f) { handleTutorialTap(Vec2(120f, 736f)) }
                add(22, if (tutorialPage == 4) localized(R.string.a11y_start_practice) else localized(R.string.a11y_next_lesson), 230f, 640f, 370f, 695f) { handleTutorialTap(Vec2(300f, 667f)) }
            }
            GameScreen.SETTINGS -> {
                add(30, localized(R.string.language_russian), 35f, 350f, 200f, 410f) { handleSettingsTap(Vec2(100f, 380f)) }
                add(31, localized(R.string.language_english), 220f, 350f, 385f, 410f) { handleSettingsTap(Vec2(300f, 380f)) }
                add(32, localized(R.string.a11y_sound), 60f, 465f, 360f, 525f) { handleSettingsTap(Vec2(210f, 495f)) }
                add(33, localized(R.string.a11y_vibration), 60f, 540f, 360f, 600f) { handleSettingsTap(Vec2(210f, 570f)) }
                add(34, localized(R.string.a11y_privacy), 60f, 635f, 360f, 690f) { handleSettingsTap(Vec2(210f, 662f)) }
                add(35, localized(R.string.a11y_back), 60f, 735f, 360f, 795f) { handleSettingsTap(Vec2(210f, 765f)) }
            }
            GameScreen.PRIVACY -> add(40, localized(R.string.a11y_back), 80f, 790f, 340f, 845f) {
                screen = GameScreen.SETTINGS
                session.playClick()
            }
            GameScreen.GAME -> when (engine.status) {
                GameStatus.RUNNING -> {
                    add(50, localized(R.string.a11y_toggle_sound), 302f, 20f, 342f, 75f) { handleGameTap(Vec2(322f, 50f)) }
                    add(51, localized(R.string.a11y_pause), 354f, 20f, 400f, 75f) { handleGameTap(Vec2(377f, 50f)) }
                    add(52, localized(R.string.a11y_swap_marbles), 340f, 80f, 405f, 140f) { handleGameTap(Vec2(370f, 110f)) }
                    val types = listOf(BoosterType.SLOW_TIME, BoosterType.BOMB, BoosterType.COLOR_CHANGE, BoosterType.REVERSE_CHAIN)
                    val centers = listOf(40f, 150f, 270f, 380f)
                    val labels = listOf(localized(R.string.a11y_slow_time), localized(R.string.a11y_bomb), localized(R.string.a11y_color_change), localized(R.string.a11y_reverse_chain))
                    centers.forEachIndexed { index, center ->
                        add(60 + index, localized(R.string.a11y_booster_count, labels[index], session.boosterCount(types[index])), center - 28f, 708f, center + 28f, 766f) {
                            handleBoosterTap(Vec2(center, 737f))
                        }
                    }
                }
                GameStatus.PAUSED -> {
                    add(70, localized(R.string.a11y_resume), 80f, 445f, 340f, 505f) { handleGameTap(Vec2(210f, 475f)) }
                    add(71, localized(R.string.a11y_restart), 80f, 515f, 340f, 575f) { handleGameTap(Vec2(210f, 545f)) }
                    add(72, localized(R.string.a11y_level_map), 80f, 585f, 340f, 645f) { handleGameTap(Vec2(210f, 615f)) }
                    add(73, localized(R.string.a11y_main_menu), 80f, 655f, 340f, 715f) { handleGameTap(Vec2(210f, 685f)) }
                }
                GameStatus.WON -> {
                    add(80, if (engine.isTutorial) localized(R.string.retry_practice) else localized(R.string.a11y_next_level), 65f, 455f, 355f, 515f) { handleGameTap(Vec2(210f, 485f)) }
                    add(81, if (engine.isTutorial) localized(R.string.a11y_back_lessons) else localized(R.string.a11y_level_map), 80f, 525f, 340f, 585f) { handleGameTap(Vec2(210f, 555f)) }
                    add(82, localized(R.string.a11y_main_menu), 80f, 595f, 340f, 655f) { handleGameTap(Vec2(210f, 625f)) }
                }
                GameStatus.LOST -> {
                    add(90, if (engine.isTutorial) localized(R.string.retry_practice) else localized(R.string.a11y_play_again), 80f, 500f, 340f, 560f) { handleGameTap(Vec2(210f, 530f)) }
                    add(91, localized(R.string.a11y_level_map), 80f, 575f, 340f, 635f) { handleGameTap(Vec2(210f, 605f)) }
                    add(92, localized(R.string.a11y_main_menu), 80f, 650f, 340f, 710f) { handleGameTap(Vec2(210f, 680f)) }
                }
            }
        }
        return result
    }

    private fun handleMenuTap(target: Vec2) {
        if (session.hasSavedGame) {
            when {
                inButton(target, 410f, 464f) -> {
                    session.openSavedGame()
                    screen = GameScreen.GAME
                }
                inButton(target, 474f, 528f) -> {
                    session.startNewGame()
                    screen = GameScreen.GAME
                }
                inButton(target, 538f, 592f) -> {
                    screen = GameScreen.LEVEL_MAP
                    session.playClick()
                }
                inButton(target, 602f, 656f) -> openTutorial()
                inButton(target, 666f, 720f) -> openSettings()
            }
        } else {
            when {
                inButton(target, 438f, 496f) -> {
                    session.startNewGame()
                    screen = GameScreen.GAME
                }
                inButton(target, 506f, 564f) -> {
                    screen = GameScreen.LEVEL_MAP
                    session.playClick()
                }
                inButton(target, 574f, 632f) -> openTutorial()
                inButton(target, 642f, 700f) -> openSettings()
            }
        }
    }

    private fun openTutorial() {
        tutorialPage = 0
        screen = GameScreen.TUTORIAL
        session.playClick()
    }

    private fun openSettings() {
        screen = GameScreen.SETTINGS
        session.playClick()
    }

    private fun handleLevelMapTap(target: Vec2) {
        if (inButton(target, 750f, 810f)) {
            screen = GameScreen.MENU
            session.playClick()
            return
        }
        val left = 22f
        val top = 220f
        val cellWidth = 68f
        val cellHeight = 58f
        val gapX = 8f
        val gapY = 8f
        val column = ((target.x - left) / (cellWidth + gapX)).toInt()
        val row = ((target.y - top) / (cellHeight + gapY)).toInt()
        if (column !in 0..4 || row !in 0..5) return
        val x = left + column * (cellWidth + gapX)
        val y = top + row * (cellHeight + gapY)
        if (target.x !in x..(x + cellWidth) || target.y !in y..(y + cellHeight)) return
        val level = row * 5 + column + 1
        if (session.startLevel(level)) screen = GameScreen.GAME
    }

    private fun handleTutorialTap(target: Vec2) {
        when {
            inButton(target, 775f, 825f) -> {
                screen = GameScreen.MENU
                session.playClick()
            }
            inButton(target, 712f, 760f) -> {
                tutorialPage = (tutorialPage - 1).coerceAtLeast(0)
                session.playClick()
            }
            inButton(target, 640f, 695f) && tutorialPage < 4 -> {
                tutorialPage++
                session.playClick()
            }
            inButton(target, 640f, 695f) && tutorialPage == 4 -> {
                session.startTutorialPractice()
                screen = GameScreen.GAME
            }
        }
    }

    private fun handleSettingsTap(target: Vec2) {
        when {
            target.y in 350f..410f && target.x in 35f..200f -> session.setLanguage("ru")
            target.y in 350f..410f && target.x in 220f..385f -> session.setLanguage("en")
            inButton(target, 465f, 525f) -> session.toggleSound()
            inButton(target, 540f, 600f) -> session.toggleVibration()
            inButton(target, 635f, 690f) -> {
                screen = GameScreen.PRIVACY
                session.playClick()
            }
            inButton(target, 735f, 795f) -> {
                screen = GameScreen.MENU
                session.playClick()
            }
        }
    }

    private fun handleGameTap(target: Vec2) {
        if (target.y in 20f..75f) {
            when {
                target.x in 302f..342f -> {
                    session.toggleSound()
                    return
                }
                target.x in 354f..400f && engine.status == GameStatus.RUNNING -> {
                    engine.pauseRound()
                    session.saveGameState(isGameScreen = true)
                    session.playClick()
                    return
                }
            }
        }

        if (engine.status == GameStatus.RUNNING && target.x in 340f..405f && target.y in 80f..140f) {
            engine.swapAmmo()
            session.playClick()
            session.saveGameState(isGameScreen = true)
            return
        }

        when (engine.status) {
            GameStatus.PAUSED -> when {
                inButton(target, 445f, 505f) -> {
                    engine.resumeRound()
                    session.playClick()
                }
                inButton(target, 515f, 575f) -> {
                    session.startNewGame()
                    screen = GameScreen.GAME
                }
                inButton(target, 585f, 645f) -> {
                    screen = GameScreen.LEVEL_MAP
                    session.playClick()
                }
                inButton(target, 655f, 715f) -> {
                    screen = GameScreen.MENU
                    session.playClick()
                }
            }
            GameStatus.WON -> when {
                inButton(target, 455f, 515f) -> {
                    if (engine.isTutorial) {
                        session.startTutorialPractice()
                        screen = GameScreen.GAME
                    } else {
                        if (!engine.startNextLevel()) screen = GameScreen.LEVEL_MAP
                        session.clearEffects()
                        session.saveGameState(screen == GameScreen.GAME)
                        session.playClick()
                    }
                }
                inButton(target, 525f, 585f) -> {
                    if (engine.isTutorial) {
                        openTutorial()
                    } else {
                        screen = GameScreen.LEVEL_MAP
                        session.playClick()
                    }
                }
                inButton(target, 595f, 655f) -> {
                    screen = GameScreen.MENU
                    session.playClick()
                }
            }
            GameStatus.LOST -> when {
                inButton(target, 500f, 560f) -> {
                    if (engine.isTutorial) {
                        session.startTutorialPractice()
                    } else {
                        session.startNewGame()
                    }
                    screen = GameScreen.GAME
                }
                inButton(target, 575f, 635f) -> {
                    screen = GameScreen.LEVEL_MAP
                    session.playClick()
                }
                inButton(target, 650f, 710f) -> {
                    screen = GameScreen.MENU
                    session.playClick()
                }
            }
            GameStatus.RUNNING -> Unit
        }
        session.saveBestScoreIfNeeded()
        session.saveGameState(screen == GameScreen.GAME)
    }

    private fun inButton(target: Vec2, top: Float, bottom: Float): Boolean =
        target.x in 60f..360f && target.y in top..bottom

    private companion object {
        val BOOSTER_CENTERS = listOf(40f, 150f, 270f, 380f)
    }
}
