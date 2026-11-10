package com.miniproject.marbleshooter.game

import kotlin.math.sqrt
import kotlin.random.Random

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

data class Ball(val colorIndex: Int, var progress: Float)

data class Projectile(
    val colorIndex: Int,
    var position: Vec2,
    val velocity: Vec2
)

enum class GameStatus {
    RUNNING,
    PAUSED,
    LOST,
    WON
}

data class GameSaveState(
    val level: Int,
    val score: Int,
    val ammoColor: Int,
    val nextColor: Int,
    val aimTarget: Vec2,
    val isPaused: Boolean,
    val balls: List<Ball>
)

sealed interface GameEvent {
    data object ShotFired : GameEvent
    data class MatchCleared(
        val center: Vec2,
        val colorIndex: Int,
        val count: Int,
        val removedPositions: List<Vec2> = emptyList()
    ) : GameEvent
    data class LevelStarted(val level: Int) : GameEvent
    data class LevelCompleted(val level: Int) : GameEvent
    data object GameLost : GameEvent
}

/**
 * Android-independent game rules and simulation.
 * The world is 420 x 900 logical units; progress increases toward the finish.
 */
class GameEngine(private val random: Random = Random.Default) {

    private data class RetractionMove(val ball: Ball, val fromProgress: Float, val targetProgress: Float)
    private data class RetractionAnimation(
        val moves: List<RetractionMove>,
        val durationSeconds: Float,
        val cascadeProbe: Int,
        var elapsedSeconds: Float = 0f
    )
    private var retractionAnimation: RetractionAnimation? = null

    companion object {
        const val WORLD_WIDTH = 420f
        const val WORLD_HEIGHT = 900f
        const val BALL_RADIUS = 16f
        const val BALL_SPACING = BALL_RADIUS * 2f
        const val SHOOTER_X = 210f
        const val SHOOTER_Y = 810f

        private const val PROJECTILE_RADIUS = 10f
        private const val PROJECTILE_SPEED = 720f
        private const val INITIAL_BALLS = 12
        private const val INITIAL_HEAD_PROGRESS = 620f
        private const val MAX_LEVEL = 100
    }

    val routePoints: List<Vec2> = listOf(
        Vec2(42f, 160f),
        Vec2(378f, 160f),
        Vec2(378f, 300f),
        Vec2(42f, 300f),
        Vec2(42f, 440f),
        Vec2(378f, 440f),
        Vec2(378f, 580f),
        Vec2(42f, 580f),
        Vec2(62f, 690f),
        Vec2(210f, 735f)
    )

    val routeLength: Float = run {
        var total = 0f
        for (index in 0 until routePoints.lastIndex) {
            total += (routePoints[index + 1] - routePoints[index]).length()
        }
        total
    }

    val shooterPosition = Vec2(SHOOTER_X, SHOOTER_Y)
    val balls = mutableListOf<Ball>()
    val projectiles = mutableListOf<Projectile>()

    private val pendingEvents = mutableListOf<GameEvent>()

    var status: GameStatus = GameStatus.RUNNING
        private set
    var score: Int = 0
        private set
    var level: Int = 1
        private set
    var ammoColor: Int = 0
        private set
    var nextColor: Int = 1
        private set
    var aimTarget: Vec2 = Vec2(210f, 500f)
        private set

    val ballCount: Int get() = balls.size
    val projectileCount: Int get() = projectiles.size
    val headProgress: Float get() = balls.firstOrNull()?.progress ?: 0f
    val activeColorCount: Int get() = (4 + (level - 1) / 3).coerceIn(4, 6)
    val chainSpeed: Float get() = (35f + (level - 1) * 2.5f).coerceAtMost(75f)
    val targetBallCount: Int get() = INITIAL_BALLS + (level - 1) * 2

    init {
        startNewGame()
    }

    fun startNewGame() {
        score = 0
        level = 1
        startLevel()
    }

    private fun startLevel() {
        balls.clear()
        projectiles.clear()
        pendingEvents.clear()
        retractionAnimation = null
        status = GameStatus.RUNNING
        ammoColor = random.nextInt(activeColorCount)
        nextColor = randomDifferentColor(ammoColor)

        var previousColor = -1
        var sameColorCount = 0
        repeat(targetBallCount) { index ->
            var color = random.nextInt(activeColorCount)
            while (color == previousColor && sameColorCount >= 2) {
                color = random.nextInt(activeColorCount)
            }
            if (color == previousColor) {
                sameColorCount += 1
            } else {
                previousColor = color
                sameColorCount = 1
            }
            balls += Ball(
                colorIndex = color,
                progress = INITIAL_HEAD_PROGRESS - index * BALL_SPACING
            )
        }
        pendingEvents += GameEvent.LevelStarted(level)
    }

    fun startNextLevel(): Boolean {
        if (status != GameStatus.WON || level >= MAX_LEVEL) return false
        level += 1
        startLevel()
        return true
    }

    fun pauseRound() {
        if (status == GameStatus.RUNNING) status = GameStatus.PAUSED
    }

    fun resumeRound() {
        if (status == GameStatus.PAUSED) status = GameStatus.RUNNING
    }

    fun onTap(target: Vec2) {
        when (status) {
            GameStatus.PAUSED -> {
                when {
                    target.x in 80f..340f && target.y in 440f..505f -> resumeRound()
                    target.x in 80f..340f && target.y in 520f..585f -> startNewGame()
                }
                return
            }
            GameStatus.WON -> {
                when {
                    target.x in 80f..340f && target.y in 470f..535f -> startNextLevel()
                    target.x in 80f..340f && target.y in 545f..610f -> startNewGame()
                }
                return
            }
            GameStatus.LOST -> {
                startNewGame()
                return
            }
            GameStatus.RUNNING -> Unit
        }

        // NEXT panel: swap the current and upcoming marbles.
        if (target.x in 300f..410f && target.y in 80f..145f) {
            swapAmmo()
            return
        }

        aimAt(target)
        fireShot()
    }

    /** Updates the frog's aim while the player is dragging a finger. */
    fun aimAt(target: Vec2) {
        val safeX = target.x.coerceIn(0f, WORLD_WIDTH)
        val safeY = if (target.y > shooterPosition.y - 35f) {
            shooterPosition.y - 180f
        } else {
            target.y.coerceIn(0f, WORLD_HEIGHT)
        }
        aimTarget = Vec2(safeX, safeY)
    }

    /** Fires the currently loaded marble in the last aimed direction. */
    fun fireShot() {
        if (status != GameStatus.RUNNING) return
        val direction = (aimTarget - shooterPosition).normalized()
        projectiles += Projectile(
            colorIndex = ammoColor,
            position = shooterPosition + direction * 50f,
            velocity = direction * PROJECTILE_SPEED
        )
        pendingEvents += GameEvent.ShotFired
        ammoColor = nextColor
        nextColor = randomDifferentColor(ammoColor)
    }

    fun swapAmmo() {
        if (status != GameStatus.RUNNING) return
        val previous = ammoColor
        ammoColor = nextColor
        nextColor = previous
    }

    fun drainEvents(): List<GameEvent> {
        if (pendingEvents.isEmpty()) return emptyList()
        val result = pendingEvents.toList()
        pendingEvents.clear()
        return result
    }

    fun saveState(): GameSaveState = GameSaveState(
        level = level,
        score = score,
        ammoColor = ammoColor,
        nextColor = nextColor,
        aimTarget = aimTarget,
        isPaused = status == GameStatus.PAUSED,
        // Restore a settled chain even if the app is closed mid-animation.
        balls = balls.map { ball ->
            val target = retractionAnimation?.moves
                ?.firstOrNull { it.ball === ball }
                ?.targetProgress ?: ball.progress
            Ball(ball.colorIndex, target)
        }
    )

    fun restoreState(state: GameSaveState): Boolean {
        if (state.level !in 1..MAX_LEVEL || state.score < 0) return false
        if (state.balls.isEmpty() || state.balls.size > 250) return false

        val colorCount = (4 + (state.level - 1) / 3).coerceIn(4, 6)
        if (state.ammoColor !in 0 until colorCount || state.nextColor !in 0 until colorCount) return false
        if (state.balls.any {
                it.colorIndex !in 0 until colorCount ||
                    !it.progress.isFinite() ||
                    it.progress < -routeLength ||
                    it.progress > routeLength
            }
        ) return false

        level = state.level
        score = state.score
        ammoColor = state.ammoColor
        nextColor = state.nextColor
        aimTarget = state.aimTarget
        balls.clear()
        balls.addAll(state.balls.map { Ball(it.colorIndex, it.progress) })
        projectiles.clear()
        pendingEvents.clear()
        retractionAnimation = null
        status = if (state.isPaused) GameStatus.PAUSED else GameStatus.RUNNING
        return true
    }

    fun positionOnRoute(progress: Float): Vec2 {
        var remaining = progress.coerceIn(0f, routeLength)
        for (index in 0 until routePoints.lastIndex) {
            val start = routePoints[index]
            val end = routePoints[index + 1]
            val segment = end - start
            val segmentLength = segment.length()
            if (remaining <= segmentLength || index == routePoints.lastIndex - 1) {
                if (segmentLength <= 0.0001f) return start
                return start + segment * (remaining / segmentLength).coerceIn(0f, 1f)
            }
            remaining -= segmentLength
        }
        return routePoints.last()
    }

    private fun tangentOnRoute(progress: Float): Vec2 {
        var remaining = progress.coerceIn(0f, routeLength)
        for (index in 0 until routePoints.lastIndex) {
            val segment = routePoints[index + 1] - routePoints[index]
            val segmentLength = segment.length()
            if (remaining <= segmentLength || index == routePoints.lastIndex - 1) {
                return segment.normalized()
            }
            remaining -= segmentLength
        }
        return (routePoints.last() - routePoints[routePoints.lastIndex - 1]).normalized()
    }

    fun update(deltaSeconds: Float) {
        if (status != GameStatus.RUNNING) return
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        if (delta <= 0f) return

        // Freeze the chain while a match plays. Only the frog-side/front balls retreat.
        if (retractionAnimation != null) {
            updateRetraction(delta)
            return
        }

        if (balls.isEmpty()) {
            completeLevel()
            return
        }

        val movement = chainSpeed * delta
        balls.forEach { it.progress += movement }
        enforceSpacingForward(1)
        updateProjectiles(delta)

        if (balls.isEmpty()) {
            completeLevel()
        } else if (balls.first().progress >= routeLength - BALL_RADIUS) {
            status = GameStatus.LOST
            projectiles.clear()
            pendingEvents += GameEvent.GameLost
        }
    }

    private fun completeLevel() {
        if (status == GameStatus.WON) return
        status = GameStatus.WON
        retractionAnimation = null
        projectiles.clear()
        pendingEvents += GameEvent.LevelCompleted(level)
    }

    private fun randomDifferentColor(excluded: Int): Int {
        if (activeColorCount <= 1) return 0
        var color = random.nextInt(activeColorCount)
        while (color == excluded) color = random.nextInt(activeColorCount)
        return color
    }

    private fun updateProjectiles(delta: Float) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            val previous = projectile.position
            val next = previous + projectile.velocity * delta

            var nearestIndex = -1
            var nearestDistance = Float.MAX_VALUE
            for (index in balls.indices) {
                val ballPosition = positionOnRoute(balls[index].progress)
                val distance = distanceToSegmentSquared(ballPosition, previous, next)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestIndex = index
                }
            }

            val hitDistance = BALL_RADIUS + PROJECTILE_RADIUS
            if (nearestIndex >= 0 && nearestDistance <= hitDistance * hitDistance) {
                val color = projectile.colorIndex
                val hitProgress = balls[nearestIndex].progress
                val tangent = tangentOnRoute(hitProgress)
                val movingWithChain = projectile.velocity.x * tangent.x +
                    projectile.velocity.y * tangent.y >= 0f
                iterator.remove()
                insertBallAt(nearestIndex, color, insertInFront = !movingWithChain)
                continue
            }

            projectile.position = next
            val p = projectile.position
            if (p.x < -40f || p.x > WORLD_WIDTH + 40f || p.y < -40f || p.y > WORLD_HEIGHT + 40f) {
                iterator.remove()
            }
        }
    }

    private fun distanceToSegmentSquared(point: Vec2, start: Vec2, end: Vec2): Float {
        val segment = end - start
        val lengthSquared = segment.lengthSquared()
        if (lengthSquared <= 0.0001f) return (point - start).lengthSquared()
        val projection = (((point.x - start.x) * segment.x +
            (point.y - start.y) * segment.y) / lengthSquared).coerceIn(0f, 1f)
        val closest = start + segment * projection
        return (point - closest).lengthSquared()
    }

    /**
     * Inserts a fired marble on the side of the hit marble facing the shot.
     * Kept internal so the chain rules can be unit-tested without Android UI.
     */
    internal fun insertBallAt(hitIndex: Int, colorIndex: Int, insertInFront: Boolean) {
        if (retractionAnimation != null) return
        if (balls.isEmpty() || hitIndex !in balls.indices) return
        if (colorIndex !in 0 until activeColorCount) return

        val insertIndex = if (insertInFront) hitIndex else hitIndex + 1
        val hitProgress = balls[hitIndex].progress
        val insertedProgress = if (insertInFront) {
            hitProgress + BALL_SPACING
        } else {
            hitProgress - BALL_SPACING
        }
        balls.add(insertIndex, Ball(colorIndex, insertedProgress))

        if (insertInFront) {
            for (index in insertIndex - 1 downTo 0) {
                val minimumProgress = balls[index + 1].progress + BALL_SPACING
                if (balls[index].progress < minimumProgress) {
                    balls[index].progress = minimumProgress
                }
            }
            enforceSpacingForward(insertIndex + 1)
        } else {
            enforceSpacingForward(insertIndex + 1)
        }
        resolveMatches(insertIndex)
    }

    private fun enforceSpacingForward(startIndex: Int) {
        for (index in startIndex until balls.size) {
            val maximumProgress = balls[index - 1].progress - BALL_SPACING
            if (balls[index].progress > maximumProgress) {
                balls[index].progress = maximumProgress
            }
        }
    }

    private fun resolveMatches(initialIndex: Int) {
        resolveMatchAt(initialIndex)
    }

    private fun resolveMatchAt(probe: Int) {
        if (probe !in balls.indices || retractionAnimation != null) return
        val color = balls[probe].colorIndex
        var start = probe
        var end = probe
        while (start > 0 && balls[start - 1].colorIndex == color) start -= 1
        while (end < balls.lastIndex && balls[end + 1].colorIndex == color) end += 1

        val removedCount = end - start + 1
        if (removedCount < 3) return

        val removed = balls.subList(start, end + 1).toList()
        val centerProgress = removed.map { it.progress }.average().toFloat()
        pendingEvents += GameEvent.MatchCleared(
            center = positionOnRoute(centerProgress),
            colorIndex = color,
            count = removedCount,
            removedPositions = removed.map { positionOnRoute(it.progress) }
        )
        score += removedCount * 10 * level
        balls.subList(start, end + 1).clear()

        if (balls.isEmpty()) {
            completeLevel()
            return
        }

        // Chain order goes from the frog-side/front to the far-side/rear.
        // Retract the front segment by the exact size of the removed interval.
        val hasFront = start > 0
        val hasRear = start < balls.size
        if (hasFront && hasRear) {
            val retreat = removedCount * BALL_SPACING
            val moves = balls.take(start).map { ball ->
                RetractionMove(ball, ball.progress, ball.progress - retreat)
            }
            retractionAnimation = RetractionAnimation(
                moves = moves,
                durationSeconds = (retreat / 320f).coerceIn(0.22f, 0.52f),
                cascadeProbe = start - 1
            )
        }
    }

    private fun updateRetraction(deltaSeconds: Float) {
        val animation = retractionAnimation ?: return
        animation.elapsedSeconds = (animation.elapsedSeconds + deltaSeconds)
            .coerceAtMost(animation.durationSeconds)
        val fraction = (animation.elapsedSeconds / animation.durationSeconds).coerceIn(0f, 1f)
        val eased = fraction * fraction * (3f - 2f * fraction)
        animation.moves.forEach { move ->
            move.ball.progress = move.fromProgress +
                (move.targetProgress - move.fromProgress) * eased
        }
        if (fraction >= 1f) {
            animation.moves.forEach { it.ball.progress = it.targetProgress }
            val cascadeProbe = animation.cascadeProbe
            retractionAnimation = null
            resolveMatchAt(cascadeProbe)
        }
    }
}
