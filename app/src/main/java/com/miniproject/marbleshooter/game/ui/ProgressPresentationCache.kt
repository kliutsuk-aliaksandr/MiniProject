package com.miniproject.marbleshooter.game.ui

import android.content.Context
import com.miniproject.marbleshooter.game.data.LevelProgressRepository
import com.miniproject.marbleshooter.game.data.SharedPreferencesLevelProgressRepository
import com.miniproject.marbleshooter.game.domain.LevelCatalog

/**
 * Keeps the small progression snapshot needed by rendering and accessibility
 * out of GameView's draw path. Preference reads happen only at construction or
 * after a campaign write, never once per frame.
 */
internal class ProgressPresentationCache(
    private val repository: LevelProgressRepository
) {
    constructor(context: Context) : this(SharedPreferencesLevelProgressRepository(context))

    var bestScore: Int = repository.bestScore
        private set
    var unlockedLevel: Int = repository.unlockedLevel
        private set
    var starsByLevel: Map<Int, Int> = readStars()
        private set

    fun isUnlocked(level: Int): Boolean = level in 1..unlockedLevel

    /** Persists a campaign completion and refreshes the cached UI snapshot. */
    fun recordCompletion(level: Int, stars: Int, score: Int) {
        repository.recordCompletion(level, stars, score)
        refresh()
    }

    /** Updates the displayed record only when the campaign repository accepts a higher score. */
    fun updateBestScore(score: Int) {
        if (score <= bestScore) return
        repository.updateBestScore(score)
        bestScore = repository.bestScore
    }

    private fun refresh() {
        bestScore = repository.bestScore
        unlockedLevel = repository.unlockedLevel
        starsByLevel = readStars()
    }

    private fun readStars(): Map<Int, Int> =
        (1..LevelCatalog.MAX_LEVELS).associateWith(repository::starsFor)
}
