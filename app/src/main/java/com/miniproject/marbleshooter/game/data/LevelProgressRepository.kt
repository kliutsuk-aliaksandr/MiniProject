package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.LevelCatalog

/** Local campaign progress contract; completion updates must be monotonic. */
interface LevelProgressRepository {
    val bestScore: Int
    val unlockedLevel: Int
    fun starsFor(level: Int): Int
    fun recordCompletion(level: Int, stars: Int, score: Int)
    fun updateBestScore(score: Int)
    fun isUnlocked(level: Int): Boolean = level in 1..unlockedLevel
}

/** SharedPreferences-backed progress with fallback reads for legacy 0.4 saves. */
class SharedPreferencesLevelProgressRepository(context: Context) : LevelProgressRepository {
    private val preferences = context.getSharedPreferences("level_progress", Context.MODE_PRIVATE)
    private val legacyPreferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)

    override val bestScore: Int
        get() = preferences.getInt(KEY_BEST_SCORE, legacyPreferences.getInt("best_score", 0))

    override val unlockedLevel: Int
        get() = preferences.getInt(
            KEY_UNLOCKED_LEVEL,
            legacyPreferences.getInt("saved_level", 1)
        ).coerceIn(1, LevelCatalog.MAX_LEVELS)

    override fun starsFor(level: Int): Int =
        preferences.getInt(starsKey(level), 0).coerceIn(0, 3)

    /** Records best stars, next unlock and best score without reducing stored progress. */
    override fun recordCompletion(level: Int, stars: Int, score: Int) {
        val safeLevel = level.coerceIn(1, LevelCatalog.MAX_LEVELS)
        val safeStars = stars.coerceIn(1, 3)
        val nextUnlocked = (safeLevel + 1).coerceAtMost(LevelCatalog.MAX_LEVELS)
        preferences.edit()
            .putInt(starsKey(safeLevel), maxOf(starsFor(safeLevel), safeStars))
            .putInt(KEY_UNLOCKED_LEVEL, maxOf(unlockedLevel, nextUnlocked))
            .putInt(KEY_BEST_SCORE, maxOf(bestScore, score))
            .apply()
    }

    override fun updateBestScore(score: Int) {
        if (score > bestScore) preferences.edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    private fun starsKey(level: Int) = "stars_${level.coerceIn(1, LevelCatalog.MAX_LEVELS)}"

    private companion object {
        const val KEY_BEST_SCORE = "best_score"
        const val KEY_UNLOCKED_LEVEL = "unlocked_level"
    }
}
