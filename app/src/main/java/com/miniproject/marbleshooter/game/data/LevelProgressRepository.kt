package com.miniproject.marbleshooter.game.data

import android.content.Context
import android.content.SharedPreferences
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

/**
 * SharedPreferences-backed campaign progress.
 *
 * Invalid completion input is ignored rather than coerced to a valid level:
 * silently clamping an invalid ID could otherwise unlock the final level.
 * Invalid or mistyped persisted values fall back to safe defaults/legacy data.
 */
class SharedPreferencesLevelProgressRepository(context: Context) : LevelProgressRepository {
    private val preferences = context.getSharedPreferences("level_progress", Context.MODE_PRIVATE)
    private val legacyPreferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)

    override val bestScore: Int
        get() = (
            preferences.readIntOrNull(KEY_BEST_SCORE)
                ?: legacyPreferences.readIntOrNull(LEGACY_BEST_SCORE)
                ?: 0
            ).coerceAtLeast(0)

    override val unlockedLevel: Int
        get() =
            preferences.readIntOrNull(KEY_UNLOCKED_LEVEL)
                ?.takeIf { it in 1..LevelCatalog.MAX_LEVELS }
                ?: legacyPreferences.readIntOrNull(LEGACY_SAVED_LEVEL)
                    ?.takeIf { it in 1..LevelCatalog.MAX_LEVELS }
                ?: 1

    override fun starsFor(level: Int): Int {
        if (level !in 1..LevelCatalog.MAX_LEVELS) return 0
        return (preferences.readIntOrNull(starsKey(level)) ?: 0).coerceIn(0, 3)
    }

    /** Records only valid completions and never reduces stored stars, unlocks or score. */
    override fun recordCompletion(level: Int, stars: Int, score: Int) {
        if (level !in 1..LevelCatalog.MAX_LEVELS || stars !in 1..3 || score < 0) return

        val nextUnlocked = (level + 1).coerceAtMost(LevelCatalog.MAX_LEVELS)
        preferences.edit()
            .putInt(starsKey(level), maxOf(starsFor(level), stars))
            .putInt(KEY_UNLOCKED_LEVEL, maxOf(unlockedLevel, nextUnlocked))
            .putInt(KEY_BEST_SCORE, maxOf(bestScore, score))
            .apply()
    }

    override fun updateBestScore(score: Int) {
        if (score >= 0 && score > bestScore) {
            preferences.edit().putInt(KEY_BEST_SCORE, score).apply()
        }
    }

    private fun starsKey(level: Int) = "stars_$level"

    private fun SharedPreferences.readIntOrNull(key: String): Int? {
        if (!contains(key)) return null
        return try {
            getInt(key, 0)
        } catch (_: ClassCastException) {
            null
        }
    }

    private companion object {
        const val KEY_BEST_SCORE = "best_score"
        const val KEY_UNLOCKED_LEVEL = "unlocked_level"
        const val LEGACY_BEST_SCORE = "best_score"
        const val LEGACY_SAVED_LEVEL = "saved_level"
    }
}
