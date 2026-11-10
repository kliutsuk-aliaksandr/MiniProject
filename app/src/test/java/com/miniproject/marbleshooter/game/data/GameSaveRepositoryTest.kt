package com.miniproject.marbleshooter.game.data

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import kotlin.random.Random

/** Integration tests for campaign snapshot persistence and schema migration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameSaveRepositoryTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var repository: GameSaveRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        repository = GameSaveRepository(context)
    }

    @Test
    fun campaignSnapshotRoundTripsAndRestoresPaused() {
        val source = GameEngine(Random(101))
        source.aimAt(Vec2(120f, 400f))
        assertTrue(source.activateBooster(BoosterType.SLOW_TIME))

        assertTrue(repository.save(source))
        assertTrue(repository.hasSavedGame())

        val expected = source.saveState()
        val restored = GameEngine(Random(102))
        assertTrue(repository.restore(restored))

        assertEquals(expected.level, restored.level)
        assertEquals(expected.score, restored.score)
        assertEquals(expected.ammoColor, restored.ammoColor)
        assertEquals(expected.nextColor, restored.nextColor)
        assertEquals(expected.aimTarget, restored.aimTarget)
        assertEquals(expected.balls, restored.balls)
        assertEquals(expected.boosters, restored.boosters)
        assertEquals(GameStatus.PAUSED, restored.status)
        assertEquals(GameSaveCodec.CURRENT_SCHEMA_VERSION, preferences.getInt("saved_schema_version", -1))
    }

    @Test
    fun unversionedLegacySnapshotIsRestoredAndUpgradedToCurrentSchema() {
        val source = GameEngine(Random(103))
        assertTrue(repository.save(source))
        preferences.edit().remove("saved_schema_version").commit()

        val restored = GameEngine(Random(104))
        assertTrue(repository.restore(restored))

        assertEquals(source.saveState().balls, restored.balls)
        assertEquals(GameSaveCodec.CURRENT_SCHEMA_VERSION, preferences.getInt("saved_schema_version", -1))
    }

    @Test
    fun malformedChainIsRejectedAtomicallyAndCleared() {
        preferences.edit()
            .putInt("saved_level", 1)
            .putString("saved_balls", "0:620.0;broken;1:588.0")
            .commit()
        val target = GameEngine(Random(105))
        val originalBalls = target.saveState().balls

        assertFalse(repository.restore(target))

        assertEquals(originalBalls, target.saveState().balls)
        assertFalse(repository.hasSavedGame())
        assertNull(preferences.getString("saved_balls", null))
    }

    @Test
    fun unsupportedFutureSchemaIsRejectedAndCleared() {
        val source = GameEngine(Random(106))
        assertTrue(repository.save(source))
        preferences.edit().putInt("saved_schema_version", 99).commit()

        assertFalse(repository.restore(GameEngine(Random(107))))

        assertFalse(repository.hasSavedGame())
        assertNull(preferences.getString("saved_balls", null))
    }


    @Test
    fun wrongTypeBallPayloadIsHandledAndCleared() {
        preferences.edit().putInt("saved_balls", 42).commit()

        assertFalse(repository.hasSavedGame())
        assertFalse(repository.restore(GameEngine(Random(110))))

        assertFalse(preferences.contains("saved_balls"))
    }

    @Test
    fun wrongTypeSnapshotMetadataIsRejectedAtomically() {
        val source = GameEngine(Random(111))
        assertTrue(repository.save(source))
        preferences.edit().putString("saved_score", "not-an-integer").commit()
        val target = GameEngine(Random(112))
        val originalBalls = target.saveState().balls

        assertFalse(repository.restore(target))

        assertEquals(originalBalls, target.saveState().balls)
        assertFalse(repository.hasSavedGame())
        assertFalse(preferences.contains("saved_balls"))
    }

    @Test
    fun tutorialSaveCannotOverwriteCampaignSnapshot() {
        val campaign = GameEngine(Random(108))
        assertTrue(repository.save(campaign))
        val savedBalls = preferences.getString("saved_balls", null)

        val tutorial = GameEngine(Random(109))
        tutorial.startTutorial()

        assertFalse(repository.save(tutorial))
        assertEquals(savedBalls, preferences.getString("saved_balls", null))
        assertTrue(repository.hasSavedGame())
    }
}
