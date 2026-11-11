package com.miniproject.marbleshooter.game.domain.model

import kotlin.math.sqrt

/** Position/vector in the 420x900 logical game coordinate system. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vec2(x * scale, y * scale)

    fun lengthSquared(): Float = x * x + y * y
    fun length(): Float = sqrt(lengthSquared())
    fun normalized(): Vec2 {
        val magnitude = length()
        return if (magnitude <= 0.0001f) Vec2(0f, -1f) else this * (1f / magnitude)
    }
}

/** A marble and its scalar progress along the active route. */
data class Ball(val colorIndex: Int, var progress: Float)

/** Fired marble with world-space position and velocity. */
data class Projectile(
    val colorIndex: Int,
    var position: Vec2,
    val velocity: Vec2
)

/** Lifecycle state of the active campaign or practice round. */
enum class GameStatus { RUNNING, PAUSED, LOST, WON }

/** Consumable actions exposed by the gameplay HUD. */
enum class BoosterType {
    SLOW_TIME,
    BOMB,
    COLOR_CHANGE,
    REVERSE_CHAIN
}

/** Remaining booster charges for the current round. */
data class BoosterInventory(
    val slowTime: Int = 3,
    val bomb: Int = 2,
    val colorChange: Int = 3,
    val reverseChain: Int = 1
) {
    fun count(type: BoosterType): Int = when (type) {
        BoosterType.SLOW_TIME -> slowTime
        BoosterType.BOMB -> bomb
        BoosterType.COLOR_CHANGE -> colorChange
        BoosterType.REVERSE_CHAIN -> reverseChain
    }

    fun consume(type: BoosterType): BoosterInventory? = when (type) {
        BoosterType.SLOW_TIME -> if (slowTime > 0) copy(slowTime = slowTime - 1) else null
        BoosterType.BOMB -> if (bomb > 0) copy(bomb = bomb - 1) else null
        BoosterType.COLOR_CHANGE -> if (colorChange > 0) copy(colorChange = colorChange - 1) else null
        BoosterType.REVERSE_CHAIN -> if (reverseChain > 0) copy(reverseChain = reverseChain - 1) else null
    }
}

/** Track zones that multiply chain movement speed. */
enum class ObstacleType { MUD, ICE, ROCK }

/** Active obstacle position measured along the route. */
data class TrackObstacle(
    val type: ObstacleType,
    val progress: Float,
    val radius: Float = 22f
)

/** Serializable campaign snapshot; projectiles and visual effects are transient. */
data class GameSaveState(
    val level: Int,
    val score: Int,
    val ammoColor: Int,
    val nextColor: Int,
    val aimTarget: Vec2,
    val isPaused: Boolean,
    val balls: List<Ball>,
    val boosters: BoosterInventory = BoosterInventory(),
    val shotsFired: Int = 0
)

/** Domain events consumed by the UI without Android dependencies. */
sealed interface GameEvent {
    data object ShotFired : GameEvent
    data class MatchCleared(
        val center: Vec2,
        val colorIndex: Int,
        val count: Int,
        val removedPositions: List<Vec2> = emptyList()
    ) : GameEvent
    data class LevelStarted(val level: Int) : GameEvent
    data class LevelCompleted(val level: Int, val stars: Int, val score: Int) : GameEvent
    data class BoosterActivated(val type: BoosterType) : GameEvent
    data object GameLost : GameEvent
}


/** Milestones earned from local, non-tutorial campaign activity. */
enum class OfflineAchievement {
    FIRST_SHOT,
    FIRST_MATCH,
    BIG_COMBO,
    FIRST_LEVEL,
    THREE_STARS,
    BOOSTER_COLLECTOR,
    LEVEL_TEN,
    CAMPAIGN_COMPLETE
}

/** Lifetime counters persisted separately from the current campaign save. */
data class OfflineStatistics(
    val totalShots: Int = 0,
    val matchesCleared: Int = 0,
    val marblesCleared: Int = 0,
    val largestMatch: Int = 0,
    val levelsCompleted: Int = 0,
    val threeStarLevels: Int = 0,
    val highestLevel: Int = 0,
    val bestScore: Int = 0,
    val usedBoosterMask: Int = 0
)
