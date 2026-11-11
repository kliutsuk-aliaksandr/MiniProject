package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.OfflineAchievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineProgressRepositoryTest {

    @Test
    fun recordsLifetimeShotsMatchesAndLargestCombo() {
        val repository = freshRepository()

        repository.recordShot()
        repository.recordShot()
        repository.recordMatch(3)
        repository.recordMatch(5)

        assertEquals(2, repository.statistics.totalShots)
        assertEquals(2, repository.statistics.matchesCleared)
        assertEquals(8, repository.statistics.marblesCleared)
        assertEquals(5, repository.statistics.largestMatch)
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.FIRST_SHOT))
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.FIRST_MATCH))
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.BIG_COMBO))
    }

    @Test
    fun levelCompletionCountsEachLevelOnceAndKeepsBestStarsAndScore() {
        val repository = freshRepository()

        repository.recordLevelCompleted(level = 3, stars = 1, score = 250)
        repository.recordLevelCompleted(level = 3, stars = 3, score = 180)
        repository.recordLevelCompleted(level = 1, stars = 3, score = 300)

        assertEquals(2, repository.statistics.levelsCompleted)
        assertEquals(2, repository.statistics.threeStarLevels)
        assertEquals(3, repository.statistics.highestLevel)
        assertEquals(300, repository.statistics.bestScore)
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.FIRST_LEVEL))
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.THREE_STARS))
    }

    @Test
    fun allFourBoostersUnlockTheToolboxAchievement() {
        val repository = freshRepository()

        BoosterType.values().forEach(repository::recordBooster)

        assertEquals(15, repository.statistics.usedBoosterMask)
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.BOOSTER_COLLECTOR))
    }

    @Test
    fun campaignMilestonesAndInvalidValuesAreHandledSafely() {
        val repository = freshRepository()

        repository.recordLevelCompleted(level = 0, stars = 3, score = 100)
        repository.recordLevelCompleted(level = 31, stars = 3, score = 100)
        repository.recordLevelCompleted(level = 10, stars = 2, score = -1)
        repository.recordMatch(0)

        assertEquals(0, repository.statistics.levelsCompleted)
        assertEquals(0, repository.statistics.matchesCleared)
        assertFalse(repository.unlockedAchievements.contains(OfflineAchievement.LEVEL_TEN))

        repository.recordLevelCompleted(level = 10, stars = 2, score = 100)
        repository.recordLevelCompleted(level = 30, stars = 3, score = 400)

        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.LEVEL_TEN))
        assertTrue(repository.unlockedAchievements.contains(OfflineAchievement.CAMPAIGN_COMPLETE))
        assertEquals(2, repository.statistics.levelsCompleted)
        assertEquals(1, repository.statistics.threeStarLevels)
    }

    @Test
    fun statisticsSurviveRepositoryRecreationAndCanBeReset() {
        val first = freshRepository()
        first.recordShot()
        first.recordMatch(4)
        first.recordLevelCompleted(2, 2, 90)

        val restored = OfflineProgressRepository(context())
        assertEquals(first.statistics, restored.statistics)
        assertEquals(first.unlockedAchievements, restored.unlockedAchievements)

        restored.reset()
        assertEquals(0, restored.statistics.totalShots)
        assertEquals(0, restored.statistics.levelsCompleted)
        assertTrue(restored.unlockedAchievements.isEmpty())
    }

    private fun freshRepository(): OfflineProgressRepository =
        context().also { app ->
            app.getSharedPreferences("offline_progress", Context.MODE_PRIVATE).edit().clear().apply()
        }.let(::OfflineProgressRepository)

    private fun context(): Context = RuntimeEnvironment.getApplication()
}
