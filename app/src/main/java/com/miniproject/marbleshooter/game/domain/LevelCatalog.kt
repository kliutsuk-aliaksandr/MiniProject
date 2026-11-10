package com.miniproject.marbleshooter.game.domain

import com.miniproject.marbleshooter.game.domain.model.ObstacleType
import com.miniproject.marbleshooter.game.domain.model.Vec2

enum class TrackId { CLASSIC, DOUBLE_LOOP, SWITCHBACK, SPIRAL }

data class ObstacleDefinition(
    val type: ObstacleType,
    val routeFraction: Float
)

data class LevelDefinition(
    val id: Int,
    val trackId: TrackId,
    val ballCount: Int,
    val chainSpeed: Float,
    val colorCount: Int,
    val obstacles: List<ObstacleDefinition>
)

/**
 * Declarative level data. Adding a level or a track does not require editing the
 * simulation or rendering code.
 */
object LevelCatalog {
    const val MAX_LEVELS = 30

    fun definition(levelId: Int): LevelDefinition {
        val id = levelId.coerceIn(1, MAX_LEVELS)
        val track = when {
            id <= 8 -> TrackId.CLASSIC
            id <= 15 -> TrackId.DOUBLE_LOOP
            id <= 22 -> TrackId.SWITCHBACK
            else -> TrackId.SPIRAL
        }
        val sectionLevel = when (track) {
            TrackId.CLASSIC -> id
            TrackId.DOUBLE_LOOP -> id - 8
            TrackId.SWITCHBACK -> id - 15
            TrackId.SPIRAL -> id - 22
        }
        val obstacles = when (track) {
            TrackId.CLASSIC -> buildList {
                if (sectionLevel >= 3) add(ObstacleDefinition(ObstacleType.MUD, 0.58f))
                if (sectionLevel >= 6) add(ObstacleDefinition(ObstacleType.ROCK, 0.82f))
            }
            TrackId.DOUBLE_LOOP -> buildList {
                add(ObstacleDefinition(ObstacleType.MUD, 0.54f))
                if (sectionLevel >= 3) add(ObstacleDefinition(ObstacleType.ICE, 0.86f))
            }
            TrackId.SWITCHBACK -> buildList {
                add(ObstacleDefinition(ObstacleType.ROCK, 0.52f))
                add(ObstacleDefinition(ObstacleType.ICE, 0.70f))
                if (sectionLevel >= 5) add(ObstacleDefinition(ObstacleType.MUD, 0.88f))
            }
            TrackId.SPIRAL -> buildList {
                add(ObstacleDefinition(ObstacleType.MUD, 0.50f))
                add(ObstacleDefinition(ObstacleType.ROCK, 0.67f))
                add(ObstacleDefinition(ObstacleType.ICE, 0.78f))
            }
        }

        return LevelDefinition(
            id = id,
            trackId = track,
            ballCount = (12 + (id - 1) * 2).coerceAtMost(70),
            chainSpeed = (32f + (id - 1) * 1.7f).coerceAtMost(78f),
            colorCount = (4 + (id - 1) / 5).coerceIn(4, 6),
            obstacles = obstacles
        )
    }

    fun route(trackId: TrackId): List<Vec2> = when (trackId) {
        TrackId.CLASSIC -> listOf(
            Vec2(42f, 160f), Vec2(378f, 160f), Vec2(378f, 300f),
            Vec2(42f, 300f), Vec2(42f, 440f), Vec2(378f, 440f),
            Vec2(378f, 580f), Vec2(42f, 580f), Vec2(62f, 690f),
            Vec2(210f, 735f)
        )
        TrackId.DOUBLE_LOOP -> listOf(
            Vec2(48f, 150f), Vec2(372f, 150f), Vec2(372f, 280f),
            Vec2(92f, 280f), Vec2(92f, 370f), Vec2(328f, 370f),
            Vec2(328f, 500f), Vec2(48f, 500f), Vec2(48f, 625f),
            Vec2(210f, 740f)
        )
        TrackId.SWITCHBACK -> listOf(
            Vec2(34f, 145f), Vec2(386f, 145f), Vec2(386f, 250f),
            Vec2(68f, 250f), Vec2(68f, 365f), Vec2(352f, 365f),
            Vec2(352f, 480f), Vec2(68f, 480f), Vec2(68f, 595f),
            Vec2(352f, 595f), Vec2(352f, 700f), Vec2(210f, 740f)
        )
        TrackId.SPIRAL -> listOf(
            Vec2(48f, 145f), Vec2(372f, 145f), Vec2(372f, 710f),
            Vec2(48f, 710f), Vec2(48f, 300f), Vec2(320f, 300f),
            Vec2(320f, 620f), Vec2(100f, 620f), Vec2(100f, 405f),
            Vec2(270f, 405f), Vec2(270f, 545f), Vec2(210f, 740f)
        )
    }
}
