package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.Ball
import com.miniproject.marbleshooter.game.domain.model.BoosterInventory
import com.miniproject.marbleshooter.game.domain.model.GameSaveState
import com.miniproject.marbleshooter.game.domain.model.Vec2

/**
 * Version-tolerant SharedPreferences adapter. The domain layer never reads Android
 * preferences directly. Legacy 0.4.0 keys are retained for smooth upgrades.
 */
class GameSaveRepository(context: Context) {
    private val preferences = context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)

    fun hasSavedGame(): Boolean = !preferences.getString(KEY_BALLS, null).isNullOrBlank()

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
        val encodedBalls = snapshot.balls.joinToString(";") {
            "${it.colorIndex}:${it.progress}"
        }
        preferences.edit()
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

    fun restore(engine: GameEngine): Boolean {
        val encoded = preferences.getString(KEY_BALLS, null) ?: return false
        val restoredBalls = encoded.split(";").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val color = parts[0].toIntOrNull() ?: return@mapNotNull null
            val progress = parts[1].toFloatOrNull() ?: return@mapNotNull null
            if (!progress.isFinite()) return@mapNotNull null
            Ball(color, progress)
        }
        if (restoredBalls.isEmpty()) {
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
        if (!restored) clear()
        return restored
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_BALLS)
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
    }
}
