package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.BoosterInventory
import com.miniproject.marbleshooter.game.domain.model.GameSaveState
import com.miniproject.marbleshooter.game.domain.model.Vec2

/**
 * SharedPreferences adapter for one in-progress campaign round.
 * The domain layer never reads Android preferences directly.
 */
class GameSaveRepository(context: Context) {
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)

    fun hasSavedGame(): Boolean = try {
        !preferences.getString(KEY_BALLS, null).isNullOrBlank()
    } catch (_: ClassCastException) {
        false
    }

    /** Saves a settled campaign snapshot; training rounds are intentionally excluded. */
    fun save(engine: GameEngine): Boolean {
        if (engine.isTutorial) return false
        if (engine.status == com.miniproject.marbleshooter.game.domain.model.GameStatus.LOST ||
            engine.status == com.miniproject.marbleshooter.game.domain.model.GameStatus.WON ||
            engine.ballCount == 0
        ) {
            clear()
            return false
        }

        val snapshot = engine.saveState()
        val encodedBalls = GameSaveCodec.encodeBalls(snapshot.balls)
        preferences.edit()
            .putInt(KEY_SCHEMA_VERSION, GameSaveCodec.CURRENT_SCHEMA_VERSION)
            .putInt("saved_level", snapshot.level)
            .putInt("saved_score", snapshot.score)
            .putInt("saved_ammo", snapshot.ammoColor)
            .putInt("saved_next", snapshot.nextColor)
            .putFloat("saved_aim_x", snapshot.aimTarget.x)
            .putFloat("saved_aim_y", snapshot.aimTarget.y)
            .putBoolean("saved_paused", true)
            .putString(KEY_BALLS, encodedBalls)
            .putInt("saved_shots", snapshot.shotsFired)
            .putInt("booster_slow", snapshot.boosters.slowTime)
            .putInt("booster_bomb", snapshot.boosters.bomb)
            .putInt("booster_color", snapshot.boosters.colorChange)
            .putInt("booster_reverse", snapshot.boosters.reverseChain)
            .apply()
        return true
    }

    /** Restores only complete supported snapshots; malformed saves are discarded as a unit. */
    fun restore(engine: GameEngine): Boolean = try {
        restoreSnapshot(engine)
    } catch (_: ClassCastException) {
        // SharedPreferences typed getters throw when a stored value has the wrong type.
        clear()
        false
    }

    private fun restoreSnapshot(engine: GameEngine): Boolean {
        val encoded = preferences.getString(KEY_BALLS, null) ?: return false
        val schemaVersion = preferences.getInt(KEY_SCHEMA_VERSION, GameSaveCodec.LEGACY_SCHEMA_VERSION)
        if (schemaVersion !in GameSaveCodec.LEGACY_SCHEMA_VERSION..GameSaveCodec.CURRENT_SCHEMA_VERSION) {
            clear()
            return false
        }
        val restoredBalls = GameSaveCodec.decodeBalls(encoded)
        if (restoredBalls.isNullOrEmpty()) {
            clear()
            return false
        }

        val state = GameSaveState(
            level = preferences.getInt("saved_level", 1),
            score = preferences.getInt("saved_score", 0),
            ammoColor = preferences.getInt("saved_ammo", 0),
            nextColor = preferences.getInt("saved_next", 1),
            aimTarget = Vec2(
                preferences.getFloat("saved_aim_x", 210f),
                preferences.getFloat("saved_aim_y", 500f)
            ),
            isPaused = true,
            balls = restoredBalls,
            boosters = BoosterInventory(
                slowTime = preferences.getInt("booster_slow", 3).coerceAtLeast(0),
                bomb = preferences.getInt("booster_bomb", 2).coerceAtLeast(0),
                colorChange = preferences.getInt("booster_color", 3).coerceAtLeast(0),
                reverseChain = preferences.getInt("booster_reverse", 1).coerceAtLeast(0)
            ),
            shotsFired = preferences.getInt("saved_shots", 0).coerceAtLeast(0)
        )
        val restored = engine.restoreState(state)
        if (!restored) {
            clear()
        } else {
            preferences.edit().putInt(KEY_SCHEMA_VERSION, GameSaveCodec.CURRENT_SCHEMA_VERSION).apply()
        }
        return restored
    }

    /** Removes all keys owned by the in-progress campaign snapshot. */
    fun clear() {
        preferences.edit()
            .remove(KEY_BALLS)
            .remove(KEY_SCHEMA_VERSION)
            .remove("saved_level")
            .remove("saved_score")
            .remove("saved_ammo")
            .remove("saved_next")
            .remove("saved_aim_x")
            .remove("saved_aim_y")
            .remove("saved_paused")
            .remove("saved_shots")
            .remove("booster_slow")
            .remove("booster_bomb")
            .remove("booster_color")
            .remove("booster_reverse")
            .apply()
    }

    private companion object {
        const val KEY_BALLS = "saved_balls"
        const val KEY_SCHEMA_VERSION = "saved_schema_version"
    }
}
