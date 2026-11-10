package com.miniproject.marbleshooter.game.ui

import com.miniproject.marbleshooter.game.data.LevelProgressRepository
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the cached progression snapshot independently of Android preferences. */
class ProgressPresentationCacheTest {
    @Test
    fun constructionLoadsBestScoreUnlocksAndStars() {
        val repository = InMemoryProgressRepository().apply {
            unlocked = 3
            best = 450
            stars[2] = 2
        }
        val cache = ProgressPresentationCache(repository)

        assertEquals(450, cache.bestScore)
        assertEquals(3, cache.unlockedLevel)
        assertEquals(2, cache.starsByLevel[2])
        assertTrue(cache.isUnlocked(3))
        assertFalse(cache.isUnlocked(4))
    }

    @Test
    fun completionRefreshesCachedStarsUnlocksAndRecord() {
        val repository = InMemoryProgressRepository()
        val cache = ProgressPresentationCache(repository)

        cache.recordCompletion(level = 4, stars = 3, score = 900)

        assertEquals(5, cache.unlockedLevel)
        assertEquals(3, cache.starsByLevel[4])
        assertEquals(900, cache.bestScore)
        assertTrue(cache.isUnlocked(5))
    }

    @Test
    fun bestScoreRemainsMonotonic() {
        val repository = InMemoryProgressRepository().apply { best = 600 }
        val cache = ProgressPresentationCache(repository)

        cache.updateBestScore(250)
        assertEquals(600, cache.bestScore)

        cache.updateBestScore(700)
        assertEquals(700, cache.bestScore)
        assertEquals(700, repository.bestScore)
    }

    private class InMemoryProgressRepository : LevelProgressRepository {
        var best = 0
        var unlocked = 1
        val stars = mutableMapOf<Int, Int>()

        override val bestScore: Int get() = best
        override val unlockedLevel: Int get() = unlocked
        override fun starsFor(level: Int): Int = stars[level] ?: 0

        override fun recordCompletion(level: Int, stars: Int, score: Int) {
            if (level !in 1..LevelCatalog.MAX_LEVELS || stars !in 1..3 || score < 0) return
            this.stars[level] = maxOf(starsFor(level), stars)
            unlocked = maxOf(unlocked, (level + 1).coerceAtMost(LevelCatalog.MAX_LEVELS))
            best = maxOf(best, score)
        }

        override fun updateBestScore(score: Int) {
            if (score >= 0) best = maxOf(best, score)
        }
    }
}
