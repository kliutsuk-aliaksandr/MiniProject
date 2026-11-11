package com.miniproject.marbleshooter.game.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import com.miniproject.marbleshooter.game.audio.GameAudio
import com.miniproject.marbleshooter.game.data.GameSaveRepository
import com.miniproject.marbleshooter.game.data.ChallengeProgressRepository
import com.miniproject.marbleshooter.game.data.OfflineProgressRepository
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.ChallengeCatalog
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameEvent
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.render.ClearingBallVisual
import com.miniproject.marbleshooter.game.render.GameRenderState
import com.miniproject.marbleshooter.game.render.GameScreen
import com.miniproject.marbleshooter.game.render.ParticleVisual
import kotlin.math.cos
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

/**
 * Owns one gameplay session and its side effects: simulation, persistence,
 * progression, sound, haptics and transient effects. It has no View or Canvas API.
 */
internal class GameSessionController(
    context: Context,
    private val onHapticFeedback: () -> Unit
) {
    val engine = GameEngine()

    private val audio = GameAudio()
    private val saveRepository = GameSaveRepository(context)
    private val progressPresentation = ProgressPresentationCache(context)
    private val offlineProgress = OfflineProgressRepository(context)
    private val challengeProgress = ChallengeProgressRepository(context)
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
    private val particles = mutableListOf<MutableParticle>()
    private val clearingBalls = mutableListOf<ClearingBallVisual>()
    private val gameLoopController = GameLoopController(::updateFixedStep)
    private val startSequence = GameStartSequence()

    var language: String = preferences.getString("language", "ru") ?: "ru"
        private set
    var soundEnabled: Boolean = preferences.getBoolean("sound_enabled", true)
        private set
    var vibrationEnabled: Boolean = preferences.getBoolean("vibration_enabled", true)
        private set
    var isActive: Boolean = false
        private set

    val bestScore: Int get() = progressPresentation.bestScore
    val unlockedLevel: Int get() = progressPresentation.unlockedLevel
    val starsByLevel: Map<Int, Int> get() = progressPresentation.starsByLevel
    val hasSavedGame: Boolean get() = saveRepository.hasSavedGame()
    val isStartInputLocked: Boolean get() = startSequence.isInputLocked

    init {
        saveRepository.restore(engine)
        engine.drainEvents()
    }

    fun buildRenderState(screen: GameScreen, tutorialPage: Int): GameRenderState =
        GameRenderState(
            screen = screen,
            language = language,
            bestScore = bestScore,
            savedGameAvailable = hasSavedGame,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            tutorialPage = tutorialPage,
            unlockedLevel = unlockedLevel,
            starsByLevel = starsByLevel,
            challengeStarsById = challengeProgress.bestStarsByChallenge,
            offlineStatistics = offlineProgress.statistics,
            unlockedAchievements = offlineProgress.unlockedAchievements,
            particles = particles.map {
                ParticleVisual(it.x, it.y, it.life, it.maxLife, it.color, it.radius)
            },
            clearingBalls = clearingBalls.toList(),
            startCountdownNumber = startSequence.countdownNumber,
            showStartPrompt = startSequence.showStartPrompt
        )

    /** Starts a new monotonic timing window when the Activity becomes active. */
    fun resumeGame() {
        isActive = true
        gameLoopController.start(System.nanoTime())
    }

    /** Stops simulation and stores only a non-tutorial campaign session. */
    fun pauseGame(screen: GameScreen) {
        isActive = false
        gameLoopController.stop()
        if (screen == GameScreen.GAME && engine.status == GameStatus.RUNNING) {
            engine.pauseRound()
        }
        saveGameState(screen == GameScreen.GAME)
    }

    fun disposeGame(screen: GameScreen) {
        saveGameState(screen == GameScreen.GAME)
        audio.release()
    }

    fun advanceFrame(screen: GameScreen) {
        if (!isActive || screen != GameScreen.GAME) return
        gameLoopController.advanceFrame(System.nanoTime())
        handlePendingEvents(isGameScreen = true)
        saveBestScoreIfNeeded()
    }

    fun shouldContinueAnimating(screen: GameScreen): Boolean =
        isActive && screen == GameScreen.GAME &&
            (engine.status == GameStatus.RUNNING || particles.isNotEmpty() || clearingBalls.isNotEmpty())

    fun handlePendingEvents(isGameScreen: Boolean) {
        handleGameEvents(engine.drainEvents(), isGameScreen)
    }

    fun fireShot(target: Vec2, isGameScreen: Boolean) {
        if (engine.status != GameStatus.RUNNING || startSequence.isInputLocked) return
        engine.aimAt(target)
        engine.fireShot()
        handlePendingEvents(isGameScreen)
        saveGameState(isGameScreen)
        saveBestScoreIfNeeded()
    }

    fun activateBooster(type: BoosterType, isGameScreen: Boolean): Boolean {
        if (startSequence.isInputLocked) return false
        if (!engine.activateBooster(type, engine.aimTarget)) {
            audio.playClick(soundEnabled)
            return false
        }
        handlePendingEvents(isGameScreen)
        saveGameState(isGameScreen)
        return true
    }

    fun startNewGame() {
        gameLoopController.resetAccumulator()
        engine.startNewGame()
        startSequence.start()
        clearEffects()
        saveGameState(isGameScreen = true)
        playClick()
    }

    fun startLevel(level: Int): Boolean {
        if (!progressPresentation.isUnlocked(level)) return false
        gameLoopController.resetAccumulator()
        engine.startLevel(level)
        startSequence.start()
        clearEffects()
        saveGameState(isGameScreen = true)
        playClick()
        return true
    }

    fun startChallenge(challengeId: Int): Boolean {
        if (ChallengeCatalog.find(challengeId) == null) return false
        gameLoopController.resetAccumulator()
        if (!engine.startChallenge(challengeId)) return false
        startSequence.start()
        clearEffects()
        playClick()
        return true
    }

    fun startNextLevel(): Boolean {
        gameLoopController.resetAccumulator()
        if (!engine.startNextLevel()) return false
        startSequence.start()
        clearEffects()
        saveGameState(isGameScreen = true)
        playClick()
        return true
    }

    fun startTutorialPractice() {
        gameLoopController.resetAccumulator()
        startSequence.cancel()
        engine.startTutorial()
        clearEffects()
        playClick()
    }

    fun openSavedGame() {
        startSequence.cancel()
        if (!saveRepository.restore(engine)) {
            startNewGame()
            return
        }
        engine.resumeRound()
        clearEffects()
        playClick()
    }

    fun clearEffects() {
        particles.clear()
        clearingBalls.clear()
    }

    fun saveGameState(isGameScreen: Boolean) {
        if (isGameScreen && !engine.isTutorial && !engine.isChallenge) {
            saveRepository.save(engine)
        }
    }

    fun saveBestScoreIfNeeded() {
        // Practice scores are session-only and must not overwrite campaign records.
        if (engine.isTutorial || engine.isChallenge) return
        progressPresentation.updateBestScore(engine.score)
        offlineProgress.recordScore(engine.score)
    }

    fun playClick() {
        audio.playClick(soundEnabled)
        if (vibrationEnabled) onHapticFeedback()
    }

    fun setLanguage(code: String) {
        if (code != "ru" && code != "en") return
        language = code
        preferences.edit().putString("language", language).apply()
        playClick()
    }

    fun toggleSound() {
        soundEnabled = !soundEnabled
        preferences.edit().putBoolean("sound_enabled", soundEnabled).apply()
        playClick()
    }

    fun toggleVibration() {
        vibrationEnabled = !vibrationEnabled
        preferences.edit().putBoolean("vibration_enabled", vibrationEnabled).apply()
        playClick()
    }

    fun boosterCount(type: BoosterType): Int = engine.boosters.count(type)

    private fun handleGameEvents(events: List<GameEvent>, isGameScreen: Boolean) {
        for (event in events) {
            when (event) {
                GameEvent.ShotFired -> {
                    if (!engine.isTutorial && !engine.isChallenge) offlineProgress.recordShot()
                    audio.playShot(soundEnabled)
                }
                is GameEvent.MatchCleared -> {
                    if (!engine.isTutorial && !engine.isChallenge) offlineProgress.recordMatch(event.count)
                    audio.playMatch(soundEnabled)
                    for (position in event.removedPositions) {
                        clearingBalls += ClearingBallVisual(position, event.colorIndex, 0.24f)
                    }
                    createBurst(event)
                    saveGameState(isGameScreen)
                }
                is GameEvent.LevelStarted -> Unit
                is GameEvent.ChallengeCompleted -> {
                    challengeProgress.recordCompletion(
                        event.challengeId,
                        event.stars,
                        event.shotsFired,
                        event.remainingSeconds
                    )
                    audio.playWin(soundEnabled)
                }
                is GameEvent.LevelCompleted -> {
                    audio.playWin(soundEnabled)
                    if (!engine.isTutorial) {
                        progressPresentation.recordCompletion(event.level, event.stars, event.score)
                        offlineProgress.recordLevelCompleted(event.level, event.stars, event.score)
                        saveRepository.clear()
                    }
                }
                is GameEvent.BoosterActivated -> {
                    if (!engine.isTutorial && !engine.isChallenge) offlineProgress.recordBooster(event.type)
                    audio.playClick(soundEnabled)
                    if (vibrationEnabled) onHapticFeedback()
                    saveGameState(isGameScreen)
                }
                GameEvent.GameLost -> {
                    audio.playLose(soundEnabled)
                    if (!engine.isTutorial && !engine.isChallenge) saveRepository.clear()
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
        val startsRollout = engine.status == GameStatus.RUNNING &&
            startSequence.advance(deltaSeconds)
        if (startsRollout) engine.startFastChainRollout(
            durationSeconds = startSequence.rolloutDurationSeconds,
            speedMultiplier = 2.5f
        )
        if (!startSequence.isActive || startSequence.isRollingOut) {
            engine.update(deltaSeconds)
        }
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
}
