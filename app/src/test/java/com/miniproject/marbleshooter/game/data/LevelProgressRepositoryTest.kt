package com.miniproject.marbleshooter.game.data

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Integration tests for campaign progression backed by real SharedPreferences semantics. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LevelProgressRepositoryTest {
    private lateinit var context: Context
    private lateinit var progressPreferences: SharedPreferences
    private lateinit var legacyPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        progressPreferences = context.getSharedPreferences("level_progress", Context.MODE_PRIVATE)
        legacyPreferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
        progressPreferences.edit().clear().commit()
        legacyPreferences.edit().clear().commit()
    }

    @Test
    fun validCompletionsUnlockNextLevelAndNeverReduceProgress() {
        val repository = SharedPreferencesLevelProgressRepository(context)

        repository.recordCompletion(level = 4, stars = 2, score = 900)
        repository.recordCompletion(level = 2, stars = 1, score = 250)

        assertEquals(5, repository.unlockedLevel)
        assertTrue(repository.isUnlocked(5))
        assertFalse(repository.isUnlocked(6))
        assertEquals(2, repository.starsFor(4))
        assertEquals(1, repository.starsFor(2))
        assertEquals(900, repository.bestScore)
    }

    @Test
    fun invalidCompletionArgumentsAreIgnoredInsteadOfBeingClamped() {
        val repository = SharedPreferencesLevelProgressRepository(context)

        repository.recordCompletion(level = 0, stars = 3, score = 900)
        repository.recordCompletion(level = 999, stars = 3, score = 900)
        repository.recordCompletion(level = 1, stars = 0, score = 900)
        repository.recordCompletion(level = 1, stars = 4, score = 900)
        repository.recordCompletion(level = 1, stars = 3, score = -1)

        assertEquals(1, repository.unlockedLevel)
        assertEquals(0, repository.bestScore)
        assertEquals(0, repository.starsFor(1))
        assertFalse(repository.isUnlocked(2))
    }

    @Test
    fun invalidLevelLookupsDoNotAliasFirstOrLastLevel() {
        val repository = SharedPreferencesLevelProgressRepository(context)
        progressPreferences.edit().putInt("stars_1", 3).commit()
        progressPreferences.edit().putInt("stars_30", 2).commit()

        assertEquals(0, repository.starsFor(0))
        assertEquals(0, repository.starsFor(31))
        assertEquals(3, repository.starsFor(1))
        assertEquals(2, repository.starsFor(30))
    }

    @Test
    fun legacyProgressRemainsReadableUntilNewProgressIsWritten() {
        legacyPreferences.edit()
            .putInt("saved_level", 7)
            .putInt("best_score", 640)
            .commit()
        val repository = SharedPreferencesLevelProgressRepository(context)

        assertEquals(7, repository.unlockedLevel)
        assertEquals(640, repository.bestScore)
        assertEquals(0, repository.starsFor(7))

        repository.recordCompletion(level = 2, stars = 3, score = 700)

        assertEquals(7, repository.unlockedLevel)
        assertEquals(700, repository.bestScore)
        assertEquals(3, repository.starsFor(2))
    }

    @Test
    fun malformedAndOutOfRangeStoredValuesFallBackSafely() {
        progressPreferences.edit()
            .putInt("unlocked_level", 999)
            .putInt("best_score", -50)
            .putString("stars_1", "not-an-int")
            .commit()
        val repository = SharedPreferencesLevelProgressRepository(context)

        assertEquals(1, repository.unlockedLevel)
        assertEquals(0, repository.bestScore)
        assertEquals(0, repository.starsFor(1))
        assertFalse(repository.isUnlocked(2))
    }

    @Test
    fun bestScoreCannotBeReducedOrSetToNegativeValue() {
        val repository = SharedPreferencesLevelProgressRepository(context)

        repository.updateBestScore(500)
        repository.updateBestScore(200)
        repository.updateBestScore(-1)

        assertEquals(500, repository.bestScore)
    }
}
