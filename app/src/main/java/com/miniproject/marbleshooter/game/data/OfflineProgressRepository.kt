package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.OfflineAchievement
import com.miniproject.marbleshooter.game.domain.model.OfflineStatistics

/**
 * Local lifetime progress for Stage 7. This store is independent from the active
 * campaign save and deliberately ignores tutorial activity at its call sites.
 */
class OfflineProgressRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val statistics: OfflineStatistics
        get() {
            val completedStars = (1..LevelCatalog.MAX_LEVELS).map(::starsFor)
            return OfflineStatistics(
                totalShots = readInt(KEY_TOTAL_SHOTS).coerceAtLeast(0),
                matchesCleared = readInt(KEY_MATCHES_CLEARED).coerceAtLeast(0),
                marblesCleared = readInt(KEY_MARBLES_CLEARED).coerceAtLeast(0),
                largestMatch = readInt(KEY_LARGEST_MATCH).coerceAtLeast(0),
                levelsCompleted = completedStars.count { it > 0 },
                threeStarLevels = completedStars.count { it >= 3 },
                highestLevel = completedStars.indexOfLast { it > 0 }.let { if (it < 0) 0 else it + 1 },
                bestScore = readInt(KEY_BEST_SCORE).coerceAtLeast(0),
                usedBoosterMask = readInt(KEY_USED_BOOSTERS).coerceAtLeast(0)
            )
        }

    val unlockedAchievements: Set<OfflineAchievement>
        get() {
            val stats = statistics
            val result = linkedSetOf<OfflineAchievement>()
            if (stats.totalShots >= 1) result += OfflineAchievement.FIRST_SHOT
            if (stats.matchesCleared >= 1) result += OfflineAchievement.FIRST_MATCH
            if (stats.largestMatch >= 5) result += OfflineAchievement.BIG_COMBO
            if (stats.levelsCompleted >= 1) result += OfflineAchievement.FIRST_LEVEL
            if (stats.threeStarLevels >= 1) result += OfflineAchievement.THREE_STARS
            if (stats.usedBoosterMask and ALL_BOOSTERS_MASK == ALL_BOOSTERS_MASK) {
                result += OfflineAchievement.BOOSTER_COLLECTOR
            }
            if (stats.highestLevel >= 10) result += OfflineAchievement.LEVEL_TEN
            if (starsFor(LevelCatalog.MAX_LEVELS) > 0) result += OfflineAchievement.CAMPAIGN_COMPLETE
            return result
        }

    fun recordShot() = increment(KEY_TOTAL_SHOTS, 1)

    fun recordMatch(marbleCount: Int) {
        if (marbleCount <= 0) return
        increment(KEY_MATCHES_CLEARED, 1)
        increment(KEY_MARBLES_CLEARED, marbleCount)
        val currentLargest = readInt(KEY_LARGEST_MATCH).coerceAtLeast(0)
        if (marbleCount > currentLargest) {
            preferences.edit().putInt(KEY_LARGEST_MATCH, marbleCount).apply()
        }
    }

    fun recordBooster(type: BoosterType) {
        val updatedMask = readInt(KEY_USED_BOOSTERS).coerceAtLeast(0) or (1 shl type.ordinal)
        preferences.edit().putInt(KEY_USED_BOOSTERS, updatedMask).apply()
    }

    fun recordScore(score: Int) {
        if (score < 0) return
        val best = readInt(KEY_BEST_SCORE).coerceAtLeast(0)
        if (score > best) preferences.edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    fun recordLevelCompleted(level: Int, stars: Int, score: Int) {
        if (level !in 1..LevelCatalog.MAX_LEVELS || stars !in 1..3 || score < 0) return
        val key = starsKey(level)
        val previousStars = readInt(key).coerceIn(0, 3)
        if (stars > previousStars) preferences.edit().putInt(key, stars).apply()
        recordScore(score)
    }

    fun reset() {
        preferences.edit().clear().apply()
    }

    private fun starsFor(level: Int): Int =
        if (level !in 1..LevelCatalog.MAX_LEVELS) 0 else readInt(starsKey(level)).coerceIn(0, 3)

    private fun increment(key: String, amount: Int) {
        if (amount <= 0) return
        val current = readInt(key).coerceAtLeast(0)
        val next = (current.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        preferences.edit().putInt(key, next).apply()
    }

    private fun readInt(key: String): Int {
        if (!preferences.contains(key)) return 0
        return try {
            preferences.getInt(key, 0)
        } catch (_: ClassCastException) {
            0
        }
    }

    private fun starsKey(level: Int) = "stars_$level"

    private companion object {
        const val PREFERENCES_NAME = "offline_progress"
        const val KEY_TOTAL_SHOTS = "total_shots"
        const val KEY_MATCHES_CLEARED = "matches_cleared"
        const val KEY_MARBLES_CLEARED = "marbles_cleared"
        const val KEY_LARGEST_MATCH = "largest_match"
        const val KEY_BEST_SCORE = "best_score"
        const val KEY_USED_BOOSTERS = "used_boosters"
        val ALL_BOOSTERS_MASK = BoosterType.values().fold(0) { mask, type -> mask or (1 shl type.ordinal) }
    }
}
