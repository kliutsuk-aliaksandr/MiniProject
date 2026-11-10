package com.miniproject.marbleshooter.game.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeProvider
import com.miniproject.marbleshooter.game.audio.GameAudio
import com.miniproject.marbleshooter.game.data.GameSaveRepository
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameEvent
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.render.ClearingBallVisual
import com.miniproject.marbleshooter.game.render.GameRenderer
import com.miniproject.marbleshooter.game.render.GameRenderState
import com.miniproject.marbleshooter.game.render.GameScreen
import com.miniproject.marbleshooter.game.render.ParticleVisual
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class MutableParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var life: Float,
    val maxLife: Float,
    val color: Int,
    val radius: Float
)

/** Android View coordinator for simulation, navigation, input, rendering and side effects. */
class GameView(context: Context) : View(context) {
    private val engine = GameEngine()
    private val renderer = GameRenderer()
    private val audio = GameAudio()
    private val saveRepository = GameSaveRepository(context)
    private val progressPresentation = ProgressPresentationCache(context)
    private val accessibilityProvider by lazy(LazyThreadSafetyMode.NONE) {
        GameAccessibilityNodeProvider(this, ::accessibilityTargets)
    }

    private var screen = GameScreen.MENU
    private var language = preferences.getString("language", "ru") ?: "ru"
    private var soundEnabled = preferences.getBoolean("sound_enabled", true)
    private var vibrationEnabled = preferences.getBoolean("vibration_enabled", true)
    private var isActive = false
    private var isAiming = false
    private var isBoosterTouch = false
    private var tutorialPage = 0
    private val particles = mutableListOf<MutableParticle>()
    private val clearingBalls = mutableListOf<ClearingBallVisual>()
    private val gameLoopController = GameLoopController(::updateFixedStep)

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        saveRepository.restore(engine)
        engine.drainEvents()
    }

    /** Restarts the view-driven frame loop after the Activity becomes active. */
    fun resumeGame() {
        isActive = true
        gameLoopController.start(System.nanoTime())
        invalidate()
        if (screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
            postInvalidateOnAnimation()
        }
    }

    /** Stops simulation and persists the current non-tutorial session. */
    fun pauseGame() {
        isActive = false
        gameLoopController.stop()
        isAiming = false
        isBoosterTouch = false
        if (screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
            engine.pauseRound()
        }
        saveGameState()
        invalidate()
    }

    /** Releases platform audio resources during Activity teardown. */
    fun disposeGame() {
        saveGameState()
        audio.release()
    }

    /** Handles in-game Back navigation; false asks the Activity to finish. */
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
                isAiming = false
                isBoosterTouch = false
                saveGameState()
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        if (isActive && screen == GameScreen.GAME) {
            gameLoopController.advanceFrame(System.nanoTime())
            handleGameEvents(engine.drainEvents())
            saveBestScoreIfNeeded()
        }

        val scale = min(width / GameEngine.WORLD_WIDTH, height / GameEngine.WORLD_HEIGHT)
        scaleFactor = scale
        offsetX = (width - GameEngine.WORLD_WIDTH * scale) / 2f
        offsetY = (height - GameEngine.WORLD_HEIGHT * scale) / 2f

        canvas.drawColor(android.graphics.Color.rgb(15, 22, 42))
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        renderer.draw(canvas, engine, buildRenderState())
        canvas.restore()

        if (isActive && screen == GameScreen.GAME &&
            (engine.status == GameStatus.RUNNING || particles.isNotEmpty() || clearingBalls.isNotEmpty())
        ) {
            postInvalidateOnAnimation()
        }
    }

    private fun buildRenderState() = GameRenderState(
        screen = screen,
        language = language,
        bestScore = progressPresentation.bestScore,
        savedGameAvailable = saveRepository.hasSavedGame(),
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        tutorialPage = tutorialPage,
        unlockedLevel = progressPresentation.unlockedLevel,
        starsByLevel = progressPresentation.starsByLevel,
        particles = particles.map {
            ParticleVisual(it.x, it.y, it.life, it.maxLife, it.color, it.radius)
        },
        clearingBalls = clearingBalls.toList()
    )

    private fun handleGameEvents(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                GameEvent.ShotFired -> audio.playShot(soundEnabled)
                is GameEvent.MatchCleared -> {
                    audio.playMatch(soundEnabled)
                    for (position in event.removedPositions) {
                        clearingBalls += ClearingBallVisual(position, event.colorIndex, 0.24f)
                    }
                    createBurst(event)
                    saveGameState()
                }
                is GameEvent.LevelStarted -> Unit
                is GameEvent.LevelCompleted -> {
                    audio.playWin(soundEnabled)
                    if (!engine.isTutorial) {
                        progressPresentation.recordCompletion(event.level, event.stars, event.score)
                        saveRepository.clear()
                    }
                }
                is GameEvent.BoosterActivated -> {
                    audio.playClick(soundEnabled)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    saveGameState()
                }
                GameEvent.GameLost -> {
                    audio.playLose(soundEnabled)
                    if (!engine.isTutorial) saveRepository.clear()
                }
            }
        }
    }

    private fun createBurst(event: GameEvent.MatchCleared) {
        val color = marbleColor(event.colorIndex)
        val count = (event.count * 4).coerceIn(10, 24)
        repeat(count) { index ->
            val angle = (Math.PI * 2.0 * index / count).toFloat()
            val speed = 70f + (index % 4) * 24f
            particles += MutableParticle(
                x = event.center.x,
                y = event.center.y,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed,
                life = 0.55f,
                maxLife = 0.55f,
                color = color,
                radius = 3f + (index % 3)
            )
        }
    }

    private fun updateFixedStep(deltaSeconds: Float) {
        engine.update(deltaSeconds)
        updateEffects(deltaSeconds)
    }

    private fun updateEffects(deltaSeconds: Float) {
        val delta = deltaSeconds.coerceIn(0f, GameLoopController.FIXED_STEP_SECONDS)
        // Reverse iteration keeps removal O(1) and avoids indexOf on duplicate effects.
        for (index in clearingBalls.lastIndex downTo 0) {
            val ball = clearingBalls[index]
            val remainingLife = ball.lifeSeconds - delta
            if (remainingLife <= 0f) clearingBalls.removeAt(index)
            else clearingBalls[index] = ball.copy(lifeSeconds = remainingLife)
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.life -= delta
            particle.x += particle.velocityX * delta
            particle.y += particle.velocityY * delta
            particle.velocityY += 120f * delta
            if (particle.life <= 0f) iterator.remove()
        }
    }

    private fun marbleColor(index: Int): Int {
        val colors = intArrayOf(
            android.graphics.Color.rgb(255, 91, 105),
            android.graphics.Color.rgb(255, 194, 67),
            android.graphics.Color.rgb(67, 205, 151),
            android.graphics.Color.rgb(84, 164, 255),
            android.graphics.Color.rgb(184, 125, 255),
            android.graphics.Color.rgb(255, 129, 207)
        )
        return colors[index.coerceIn(0, colors.lastIndex)]
    }

    private fun saveBestScoreIfNeeded() {
        // Practice scores are session-only and must not overwrite campaign records.
        if (engine.isTutorial) return
        progressPresentation.updateBestScore(engine.score)
    }

    private fun saveGameState() {
        if (screen == GameScreen.GAME && !engine.isTutorial) {
            saveRepository.save(engine)
        }
    }

    private fun startNewGame() {
        gameLoopController.resetAccumulator()
        engine.startNewGame()
        clearEffects()
        screen = GameScreen.GAME
        saveGameState()
        playClick()
    }

    private fun startLevel(level: Int) {
        if (!progressPresentation.isUnlocked(level)) return
        engine.startLevel(level)
        clearEffects()
        screen = GameScreen.GAME
        saveGameState()
        playClick()
    }

    private fun startTutorialPractice() {
        gameLoopController.resetAccumulator()
        engine.startTutorial()
        clearEffects()
        screen = GameScreen.GAME
        playClick()
    }

    private fun openSavedGame() {
        if (!saveRepository.restore(engine)) {
            startNewGame()
            return
        }
        engine.resumeRound()
        clearEffects()
        screen = GameScreen.GAME
        playClick()
        if (isActive && engine.status == GameStatus.RUNNING) postInvalidateOnAnimation()
    }

    private fun clearEffects() {
        particles.clear()
        clearingBalls.clear()
    }

    private fun playClick() {
        audio.playClick(soundEnabled)
        if (vibrationEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        invalidate()
    }

    private fun setLanguage(code: String) {
        if (code != "ru" && code != "en") return
        language = code
        preferences.edit().putString("language", language).apply()
        playClick()
    }

    private fun toggleSound() {
        soundEnabled = !soundEnabled
        preferences.edit().putBoolean("sound_enabled", soundEnabled).apply()
        playClick()
    }

    private fun toggleVibration() {
        vibrationEnabled = !vibrationEnabled
        preferences.edit().putBoolean("vibration_enabled", vibrationEnabled).apply()
        playClick()
    }

    private fun handleMenuTap(target: Vec2) {
        if (saveRepository.hasSavedGame()) {
            when {
                inButton(target, 410f, 464f) -> openSavedGame()
                inButton(target, 474f, 528f) -> startNewGame()
                inButton(target, 538f, 592f) -> {
                    screen = GameScreen.LEVEL_MAP
                    playClick()
                }
                inButton(target, 602f, 656f) -> openTutorial()
                inButton(target, 666f, 720f) -> openSettings()
            }
        } else {
            when {
                inButton(target, 438f, 496f) -> startNewGame()
                inButton(target, 506f, 564f) -> {
                    screen = GameScreen.LEVEL_MAP
                    playClick()
                }
                inButton(target, 574f, 632f) -> openTutorial()
                inButton(target, 642f, 700f) -> openSettings()
            }
        }
    }

    private fun openTutorial() {
        tutorialPage = 0
        screen = GameScreen.TUTORIAL
        playClick()
    }

    private fun openSettings() {
        screen = GameScreen.SETTINGS
        playClick()
    }

    private fun handleLevelMapTap(target: Vec2) {
        if (inButton(target, 750f, 810f)) {
            screen = GameScreen.MENU
            playClick()
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
        if (progressPresentation.isUnlocked(level)) startLevel(level)
    }

    private fun handleTutorialTap(target: Vec2) {
        when {
            inButton(target, 775f, 825f) -> {
                screen = GameScreen.MENU
                playClick()
            }
            inButton(target, 712f, 760f) -> {
                tutorialPage = (tutorialPage - 1).coerceAtLeast(0)
                playClick()
            }
            inButton(target, 640f, 695f) && tutorialPage < 4 -> {
                tutorialPage++
                playClick()
            }
            inButton(target, 640f, 695f) && tutorialPage == 4 -> startTutorialPractice()
        }
    }

    private fun handleSettingsTap(target: Vec2) {
        when {
            target.y in 350f..410f && target.x in 35f..200f -> setLanguage("ru")
            target.y in 350f..410f && target.x in 220f..385f -> setLanguage("en")
            inButton(target, 465f, 525f) -> toggleSound()
            inButton(target, 540f, 600f) -> toggleVibration()
            inButton(target, 635f, 690f) -> {
                screen = GameScreen.PRIVACY
                playClick()
            }
            inButton(target, 735f, 795f) -> {
                screen = GameScreen.MENU
                playClick()
            }
        }
    }

    private fun handleBoosterTap(target: Vec2) {
        val centers = listOf(40f, 150f, 270f, 380f)
        if (target.y !in 710f..770f) return
        val nearest = centers.indices.minByOrNull { index ->
            kotlin.math.abs(centers[index] - target.x)
        } ?: return
        if (kotlin.math.abs(centers[nearest] - target.x) > 27f) return
        val type = when (nearest) {
            0 -> BoosterType.SLOW_TIME
            1 -> BoosterType.BOMB
            2 -> BoosterType.COLOR_CHANGE
            else -> BoosterType.REVERSE_CHAIN
        }
        // The HUD touch coordinate is not a world-space target. Bomb uses the aim point.
        if (engine.activateBooster(type, engine.aimTarget)) {
            handleGameEvents(engine.drainEvents())
            saveGameState()
        } else {
            audio.playClick(soundEnabled)
        }
        invalidate()
    }

    private fun handleGameTap(target: Vec2) {
        if (target.y in 20f..75f) {
            when {
                target.x in 302f..342f -> {
                    toggleSound()
                    return
                }
                target.x in 354f..400f && engine.status == GameStatus.RUNNING -> {
                    engine.pauseRound()
                    saveGameState()
                    isAiming = false
                    playClick()
                    return
                }
            }
        }

        if (engine.status == GameStatus.RUNNING && target.x in 340f..405f && target.y in 80f..140f) {
            engine.swapAmmo()
            playClick()
            saveGameState()
            return
        }

        when (engine.status) {
            GameStatus.PAUSED -> when {
                inButton(target, 445f, 505f) -> {
                    engine.resumeRound()
                    playClick()
                }
                inButton(target, 515f, 575f) -> startNewGame()
                inButton(target, 585f, 645f) -> {
                    screen = GameScreen.LEVEL_MAP
                    saveGameState()
                    playClick()
                }
                inButton(target, 655f, 715f) -> {
                    screen = GameScreen.MENU
                    saveGameState()
                    playClick()
                }
            }
            GameStatus.WON -> when {
                inButton(target, 455f, 515f) -> {
                    if (engine.isTutorial) {
                        startTutorialPractice()
                    } else {
                        if (!engine.startNextLevel()) screen = GameScreen.LEVEL_MAP
                        clearEffects()
                        saveGameState()
                        playClick()
                    }
                }
                inButton(target, 525f, 585f) -> {
                    if (engine.isTutorial) openTutorial() else {
                        screen = GameScreen.LEVEL_MAP
                        playClick()
                    }
                }
                inButton(target, 595f, 655f) -> {
                    screen = GameScreen.MENU
                    playClick()
                }
            }
            GameStatus.LOST -> when {
                inButton(target, 500f, 560f) -> {
                    if (engine.isTutorial) startTutorialPractice() else startNewGame()
                }
                inButton(target, 575f, 635f) -> {
                    screen = GameScreen.LEVEL_MAP
                    playClick()
                }
                inButton(target, 650f, 710f) -> {
                    screen = GameScreen.MENU
                    playClick()
                }
            }
            GameStatus.RUNNING -> Unit
        }
        saveBestScoreIfNeeded()
        saveGameState()
        invalidate()
    }

    private fun inButton(target: Vec2, top: Float, bottom: Float): Boolean =
        target.x in 60f..360f && target.y in top..bottom

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider = accessibilityProvider

    /**
     * Describes visible Canvas controls as named virtual buttons for TalkBack and
     * switch access. Bounds are converted from the logical 420x900 world to View pixels.
     */
    private fun accessibilityTargets(): List<GameAccessibilityTarget> {
        val result = mutableListOf<GameAccessibilityTarget>()
        fun localized(ru: String, en: String) = if (language == "ru") ru else en
        fun add(id: Int, label: String, l: Float, t: Float, r: Float, b: Float, action: () -> Unit) {
            result += GameAccessibilityTarget(
                id, label,
                Rect(
                    (offsetX + l * scaleFactor).toInt(), (offsetY + t * scaleFactor).toInt(),
                    (offsetX + r * scaleFactor).toInt(), (offsetY + b * scaleFactor).toInt()
                ), action
            )
        }

        when (screen) {
            GameScreen.MENU -> {
                if (saveRepository.hasSavedGame()) {
                    add(1, localized("Продолжить игру", "Continue game"), 78f, 410f, 342f, 464f) { handleMenuTap(Vec2(210f, 437f)) }
                    add(2, localized("Новая игра", "New game"), 78f, 474f, 342f, 528f) { handleMenuTap(Vec2(210f, 501f)) }
                    add(3, localized("Карта уровней", "Level map"), 78f, 538f, 342f, 592f) { handleMenuTap(Vec2(210f, 565f)) }
                    add(4, localized("Обучение", "Tutorial"), 78f, 602f, 342f, 656f) { handleMenuTap(Vec2(210f, 629f)) }
                    add(5, localized("Настройки", "Settings"), 78f, 666f, 342f, 720f) { handleMenuTap(Vec2(210f, 693f)) }
                } else {
                    add(1, localized("Играть", "Play"), 78f, 438f, 342f, 496f) { handleMenuTap(Vec2(210f, 467f)) }
                    add(3, localized("Карта уровней", "Level map"), 78f, 506f, 342f, 564f) { handleMenuTap(Vec2(210f, 535f)) }
                    add(4, localized("Обучение", "Tutorial"), 78f, 574f, 342f, 632f) { handleMenuTap(Vec2(210f, 603f)) }
                    add(5, localized("Настройки", "Settings"), 78f, 642f, 342f, 700f) { handleMenuTap(Vec2(210f, 671f)) }
                }
            }
            GameScreen.LEVEL_MAP -> {
                add(10, localized("В главное меню", "Main menu"), 78f, 750f, 342f, 810f) { handleLevelMapTap(Vec2(210f, 780f)) }
                for (level in 1..progressPresentation.unlockedLevel) {
                    val index = level - 1
                    val left = 22f + (index % 5) * 76f
                    val top = 220f + (index / 5) * 66f
                    add(100 + level, localized("Уровень $level", "Level $level"), left, top, left + 68f, top + 58f) {
                        handleLevelMapTap(Vec2(left + 34f, top + 29f))
                    }
                }
            }
            GameScreen.TUTORIAL -> {
                add(20, localized("Закрыть обучение", "Close tutorial"), 290f, 775f, 380f, 835f) { handleTutorialTap(Vec2(330f, 805f)) }
                if (tutorialPage > 0) add(21, localized("Предыдущий урок", "Previous lesson"), 60f, 712f, 190f, 760f) { handleTutorialTap(Vec2(120f, 736f)) }
                add(22, if (tutorialPage == 4) localized("Начать тренировку", "Start practice") else localized("Следующий урок", "Next lesson"), 230f, 640f, 370f, 695f) { handleTutorialTap(Vec2(300f, 667f)) }
            }
            GameScreen.SETTINGS -> {
                add(30, "Русский", 35f, 350f, 200f, 410f) { handleSettingsTap(Vec2(100f, 380f)) }
                add(31, "English", 220f, 350f, 385f, 410f) { handleSettingsTap(Vec2(300f, 380f)) }
                add(32, localized("Звук", "Sound"), 60f, 465f, 360f, 525f) { handleSettingsTap(Vec2(210f, 495f)) }
                add(33, localized("Вибрация", "Vibration"), 60f, 540f, 360f, 600f) { handleSettingsTap(Vec2(210f, 570f)) }
                add(34, localized("Политика конфиденциальности", "Privacy policy"), 60f, 635f, 360f, 690f) { handleSettingsTap(Vec2(210f, 662f)) }
                add(35, localized("Назад", "Back"), 60f, 735f, 360f, 795f) { handleSettingsTap(Vec2(210f, 765f)) }
            }
            GameScreen.PRIVACY -> add(40, localized("Назад", "Back"), 80f, 790f, 340f, 845f) {
                screen = GameScreen.SETTINGS; playClick()
            }
            GameScreen.GAME -> when (engine.status) {
                GameStatus.RUNNING -> {
                    add(50, localized("Переключить звук", "Toggle sound"), 302f, 20f, 342f, 75f) { handleGameTap(Vec2(322f, 50f)) }
                    add(51, localized("Пауза", "Pause"), 354f, 20f, 400f, 75f) { handleGameTap(Vec2(377f, 50f)) }
                    add(52, localized("Поменять шар", "Swap marbles"), 340f, 80f, 405f, 140f) { handleGameTap(Vec2(370f, 110f)) }
                    val types = listOf(BoosterType.SLOW_TIME, BoosterType.BOMB, BoosterType.COLOR_CHANGE, BoosterType.REVERSE_CHAIN)
                    val centers = listOf(40f, 150f, 270f, 380f)
                    val labels = listOf(localized("Замедление", "Slow time"), localized("Бомба", "Bomb"), localized("Смена цвета", "Change color"), localized("Реверс цепочки", "Reverse chain"))
                    centers.forEachIndexed { index, center ->
                        add(60 + index, "${labels[index]}, ${engine.boosters.count(types[index])}", center - 28f, 708f, center + 28f, 766f) {
                            handleBoosterTap(Vec2(center, 737f))
                        }
                    }
                }
                GameStatus.PAUSED -> {
                    add(70, localized("Продолжить", "Resume"), 80f, 445f, 340f, 505f) { handleGameTap(Vec2(210f, 475f)) }
                    add(71, localized("Начать заново", "Restart"), 80f, 515f, 340f, 575f) { handleGameTap(Vec2(210f, 545f)) }
                    add(72, localized("К карте уровней", "Level map"), 80f, 585f, 340f, 645f) { handleGameTap(Vec2(210f, 615f)) }
                    add(73, localized("В главное меню", "Main menu"), 80f, 655f, 340f, 715f) { handleGameTap(Vec2(210f, 685f)) }
                }
                GameStatus.WON -> {
                    add(80, if (engine.isTutorial) localized("Повторить тренировку", "Retry practice") else localized("Следующий уровень", "Next level"), 65f, 455f, 355f, 515f) { handleGameTap(Vec2(210f, 485f)) }
                    add(81, if (engine.isTutorial) localized("К урокам", "Back to lessons") else localized("К карте уровней", "Level map"), 80f, 525f, 340f, 585f) { handleGameTap(Vec2(210f, 555f)) }
                    add(82, localized("В главное меню", "Main menu"), 80f, 595f, 340f, 655f) { handleGameTap(Vec2(210f, 625f)) }
                }
                GameStatus.LOST -> {
                    add(90, if (engine.isTutorial) localized("Повторить тренировку", "Retry practice") else localized("Играть снова", "Play again"), 80f, 500f, 340f, 560f) { handleGameTap(Vec2(210f, 530f)) }
                    add(91, localized("К карте уровней", "Level map"), 80f, 575f, 340f, 635f) { handleGameTap(Vec2(210f, 605f)) }
                    add(92, localized("В главное меню", "Main menu"), 80f, 650f, 340f, 710f) { handleGameTap(Vec2(210f, 680f)) }
                }
            }
        }
        return result
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scaleFactor <= 0f) return true
        val target = Vec2(
            (event.x - offsetX) / scaleFactor,
            (event.y - offsetY) / scaleFactor
        )

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isBoosterTouch = screen == GameScreen.GAME &&
                    engine.status == GameStatus.RUNNING &&
                    target.y in 710f..770f &&
                    listOf(40f, 150f, 270f, 380f).any { kotlin.math.abs(it - target.x) <= 27f }
                if (!isBoosterTouch && screen == GameScreen.GAME &&
                    engine.status == GameStatus.RUNNING && target.y > 145f &&
                    target.y !in 700f..780f && !engine.isAnimatingRetraction
                ) {
                    isAiming = true
                    engine.aimAt(target)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isAiming && screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
                    engine.aimAt(target)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isAiming = false
                isBoosterTouch = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                when {
                    isBoosterTouch -> handleBoosterTap(target)
                    isAiming && screen == GameScreen.GAME && engine.status == GameStatus.RUNNING -> {
                        engine.aimAt(target)
                        engine.fireShot()
                        handleGameEvents(engine.drainEvents())
                        saveGameState()
                        saveBestScoreIfNeeded()
                    }
                    else -> when (screen) {
                        GameScreen.MENU -> handleMenuTap(target)
                        GameScreen.LEVEL_MAP -> handleLevelMapTap(target)
                        GameScreen.TUTORIAL -> handleTutorialTap(target)
                        GameScreen.SETTINGS -> handleSettingsTap(target)
                        GameScreen.PRIVACY -> if (inButton(target, 790f, 845f)) {
                            screen = GameScreen.SETTINGS
                            playClick()
                        }
                        GameScreen.GAME -> handleGameTap(target)
                    }
                }
                isAiming = false
                isBoosterTouch = false
                performClick()
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
