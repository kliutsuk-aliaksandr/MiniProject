package com.miniproject.marbleshooter.game.ui

import android.content.Context
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.miniproject.marbleshooter.game.audio.GameAudio
import com.miniproject.marbleshooter.game.data.GameSaveRepository
import com.miniproject.marbleshooter.game.data.LevelProgressRepository
import com.miniproject.marbleshooter.game.data.SharedPreferencesLevelProgressRepository
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.LevelCatalog
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

class GameView(context: Context) : View(context) {
    private val engine = GameEngine()
    private val renderer = GameRenderer()
    private val audio = GameAudio()
    private val saveRepository = GameSaveRepository(context)
    private val progressRepository: LevelProgressRepository =
        SharedPreferencesLevelProgressRepository(context)
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)

    private var screen = GameScreen.MENU
    private var language = preferences.getString("language", "ru") ?: "ru"
    private var soundEnabled = preferences.getBoolean("sound_enabled", true)
    private var vibrationEnabled = preferences.getBoolean("vibration_enabled", true)
    private var isActive = false
    private var isAiming = false
    private var isBoosterTouch = false
    private var tutorialPage = 0
    private var lastFrameNanos = 0L
    private var bestScore = progressRepository.bestScore
    private val particles = mutableListOf<MutableParticle>()
    private val clearingBalls = mutableListOf<ClearingBallVisual>()

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        isFocusable = true
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        saveRepository.restore(engine)
        engine.drainEvents()
    }

    fun resumeGame() {
        isActive = true
        lastFrameNanos = System.nanoTime()
        invalidate()
        if (screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
            postInvalidateOnAnimation()
        }
    }

    fun pauseGame() {
        isActive = false
        lastFrameNanos = 0L
        isAiming = false
        isBoosterTouch = false
        if (screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
            engine.pauseRound()
        }
        saveGameState()
        invalidate()
    }

    fun disposeGame() {
        saveGameState()
        audio.release()
    }

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
            val now = System.nanoTime()
            val delta = if (lastFrameNanos == 0L) 0f
                else (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now
            engine.update(delta)
            updateEffects(delta)
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
        bestScore = bestScore,
        savedGameAvailable = saveRepository.hasSavedGame(),
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        tutorialPage = tutorialPage,
        unlockedLevel = progressRepository.unlockedLevel,
        starsByLevel = (1..LevelCatalog.MAX_LEVELS).associateWith(progressRepository::starsFor),
        particles = particles.map {
            ParticleVisual(it.x, it.y, it.life, it.maxLife, it.color, it.radius)
        },
        clearingBalls = clearingBalls.toList()
    )

    private fun handleGameEvents(events: List<GameEvent>) {
        events.forEach { event ->
            when (event) {
                GameEvent.ShotFired -> audio.playShot(soundEnabled)
                is GameEvent.MatchCleared -> {
                    audio.playMatch(soundEnabled)
                    event.removedPositions.forEach {
                        clearingBalls += ClearingBallVisual(it, event.colorIndex, 0.24f)
                    }
                    createBurst(event)
                    saveGameState()
                }
                is GameEvent.LevelStarted -> Unit
                is GameEvent.LevelCompleted -> {
                    audio.playWin(soundEnabled)
                    if (!engine.isTutorial) {
                        progressRepository.recordCompletion(event.level, event.stars, event.score)
                        bestScore = progressRepository.bestScore
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

    private fun updateEffects(deltaSeconds: Float) {
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        val clearingIterator = clearingBalls.iterator()
        while (clearingIterator.hasNext()) {
            val ball = clearingIterator.next()
            val life = (ball.lifeSeconds - delta)
            if (life <= 0f) clearingIterator.remove()
            else {
                val index = clearingBalls.indexOf(ball)
                clearingBalls[index] = ball.copy(lifeSeconds = life)
            }
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
        if (engine.score > bestScore) {
            bestScore = engine.score
            progressRepository.updateBestScore(bestScore)
        }
    }

    private fun saveGameState() {
        if (screen == GameScreen.GAME && !engine.isTutorial) {
            saveRepository.save(engine)
        }
    }

    private fun startNewGame() {
        engine.startNewGame()
        clearEffects()
        screen = GameScreen.GAME
        saveGameState()
        playClick()
    }

    private fun startLevel(level: Int) {
        if (!progressRepository.isUnlocked(level)) return
        engine.startLevel(level)
        clearEffects()
        screen = GameScreen.GAME
        saveGameState()
        playClick()
    }

    private fun startTutorialPractice() {
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
        if (progressRepository.isUnlocked(level)) startLevel(level)
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
        if (engine.activateBooster(type, target)) {
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
                    if (!engine.startNextLevel()) screen = GameScreen.LEVEL_MAP
                    clearEffects()
                    saveGameState()
                    playClick()
                }
                inButton(target, 525f, 585f) -> {
                    screen = GameScreen.LEVEL_MAP
                    playClick()
                }
                inButton(target, 595f, 655f) -> {
                    screen = GameScreen.MENU
                    playClick()
                }
            }
            GameStatus.LOST -> when {
                inButton(target, 500f, 560f) -> startNewGame()
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
