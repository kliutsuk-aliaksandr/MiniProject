package com.miniproject.marbleshooter.game.domain

/** Immutable rules for one self-contained offline challenge. */
data class ChallengeDefinition(
    val id: Int,
    val levelId: Int,
    val maxShots: Int,
    val timeLimitSeconds: Float? = null
) {
    init {
        require(id > 0)
        require(levelId in 1..LevelCatalog.MAX_LEVELS)
        require(maxShots > 0)
        require(timeLimitSeconds == null || (timeLimitSeconds.isFinite() && timeLimitSeconds > 0f))
    }
}

/** Small, curated offline challenge set; content is deterministic and needs no network. */
object ChallengeCatalog {
    val all: List<ChallengeDefinition> = listOf(
        ChallengeDefinition(id = 1, levelId = 1, maxShots = 18),
        ChallengeDefinition(id = 2, levelId = 4, maxShots = 28, timeLimitSeconds = 25f),
        ChallengeDefinition(id = 3, levelId = 8, maxShots = 42, timeLimitSeconds = 45f)
    )

    fun find(id: Int): ChallengeDefinition? = all.firstOrNull { it.id == id }
}
