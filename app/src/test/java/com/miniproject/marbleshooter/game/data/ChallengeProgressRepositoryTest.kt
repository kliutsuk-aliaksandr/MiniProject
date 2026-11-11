package com.miniproject.marbleshooter.game.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChallengeProgressRepositoryTest {

    @Test
    fun recordsBestStarsShotsAndRemainingTime() {
        val repository = freshRepository()

        assertTrue(repository.recordCompletion(1, stars = 2, shots = 14, remainingSeconds = 0f))
        assertEquals(2, repository.bestStars(1))
        assertEquals(14, repository.bestShots(1))
        assertNull(repository.bestRemainingSeconds(1))

        assertTrue(repository.recordCompletion(1, stars = 3, shots = 16, remainingSeconds = 12.5f))
        assertEquals(3, repository.bestStars(1))
        assertEquals(14, repository.bestShots(1))
        assertEquals(12.5f, repository.bestRemainingSeconds(1) ?: 0f, 0.001f)
    }

    @Test
    fun equalStarResultImprovesWhenUsingFewerShots() {
        val repository = freshRepository()

        assertTrue(repository.recordCompletion(2, stars = 2, shots = 22, remainingSeconds = 7f))
        assertTrue(repository.recordCompletion(2, stars = 2, shots = 18, remainingSeconds = 4f))
        assertFalse(repository.recordCompletion(2, stars = 2, shots = 20, remainingSeconds = 20f))

        assertEquals(2, repository.bestStars(2))
        assertEquals(18, repository.bestShots(2))
        assertEquals(20f, repository.bestRemainingSeconds(2) ?: 0f, 0.001f)
    }

    @Test
    fun invalidChallengeResultsAreIgnored() {
        val repository = freshRepository()

        assertFalse(repository.recordCompletion(99, stars = 3, shots = 2, remainingSeconds = 3f))
        assertFalse(repository.recordCompletion(1, stars = 0, shots = 2, remainingSeconds = 3f))
        assertFalse(repository.recordCompletion(1, stars = 2, shots = 0, remainingSeconds = 3f))
        assertFalse(repository.recordCompletion(1, stars = 2, shots = 2, remainingSeconds = Float.NaN))
        assertFalse(repository.recordCompletion(1, stars = 2, shots = 2, remainingSeconds = -1f))

        assertEquals(0, repository.bestStars(1))
        assertNull(repository.bestShots(1))
    }

    @Test
    fun challengeProgressPersistsAcrossRepositoryInstancesAndCanReset() {
        val first = freshRepository()
        first.recordCompletion(3, stars = 3, shots = 27, remainingSeconds = 45f)

        val restored = ChallengeProgressRepository(context())
        assertEquals(3, restored.bestStars(3))
        assertEquals(27, restored.bestShots(3))
        assertEquals(45f, restored.bestRemainingSeconds(3) ?: 0f, 0.001f)

        restored.reset()
        assertEquals(0, restored.bestStars(3))
        assertTrue(restored.bestStarsByChallenge.values.all { it == 0 })
    }

    private fun freshRepository(): ChallengeProgressRepository =
        context().also { app ->
            app.getSharedPreferences("offline_challenge_progress", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }.let(::ChallengeProgressRepository)

    private fun context(): Context = RuntimeEnvironment.getApplication()
}
