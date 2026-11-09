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
    LOST,
    WON
}

/**
 * Platform-independent rules and simulation for the marble shooter.
 * Coordinates are expressed in the fixed 420 x 900 game world.
 */
class GameEngine(private val random: Random = Random.Default) {

    companion object {
        const val WORLD_WIDTH = 420f
        const val WORLD_HEIGHT = 900f
        const val BALL_RADIUS = 16f
        const val BALL_SPACING = 32f
        const val SHOOTER_X = 210f
        const val SHOOTER_Y = 810f
        private const val PROJECTILE_RADIUS = 10f
        private const val PROJECTILE_SPEED = 720f
        private const val CHAIN_SPEED = 43f
        private const val INITIAL_BALLS = 14
        private const val INITIAL_HEAD_PROGRESS = 620f
        private const val COLOR_COUNT = 5
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

    var status: GameStatus = GameStatus.RUNNING
        private set
    var score: Int = 0
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

    init {
        startNewGame()
    }

    fun startNewGame() {
        balls.clear()
        projectiles.clear()
        score = 0
        status = GameStatus.RUNNING
        ammoColor = random.nextInt(COLOR_COUNT)
        nextColor = randomDifferentColor(ammoColor)

        var previousColor = -1
        var sameColorCount = 0
        repeat(INITIAL_BALLS) { index ->
            var color = random.nextInt(COLOR_COUNT)
            while (color == previousColor && sameColorCount >= 2) {
                color = random.nextInt(COLOR_COUNT)
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
    }

    fun onTap(target: Vec2) {
        if (status != GameStatus.RUNNING) {
            startNewGame()
            return
        }

        // The NEXT panel doubles as an ammo-swap control.
        if (target.x in 300f..410f && target.y in 90f..150f) {
            swapAmmo()
            return
        }

        val safeTarget = if (target.y > shooterPosition.y - 35f) {
            Vec2(target.x, shooterPosition.y - 180f)
        } else {
            target
        }
        aimTarget = safeTarget

        val direction = (safeTarget - shooterPosition).normalized()
        projectiles += Projectile(
            colorIndex = ammoColor,
            position = shooterPosition + direction * 50f,
            velocity = direction * PROJECTILE_SPEED
        )

        ammoColor = nextColor
        nextColor = randomDifferentColor(ammoColor)
    }

    fun swapAmmo() {
        val previous = ammoColor
        ammoColor = nextColor
        nextColor = previous
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

    fun update(deltaSeconds: Float) {
        if (status != GameStatus.RUNNING) return
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        if (delta <= 0f) return

        if (balls.isEmpty()) {
            status = GameStatus.WON
            return
        }

        // All balls advance together; spacing constraints keep the chain connected.
        val movement = CHAIN_SPEED * delta
        balls.forEach { it.progress += movement }
        enforceSpacing(1)
        updateProjectiles(delta)

        if (balls.isEmpty()) {
            status = GameStatus.WON
        } else if (balls.first().progress >= routeLength - BALL_RADIUS) {
            status = GameStatus.LOST
            projectiles.clear()
        }
    }

    private fun randomDifferentColor(excluded: Int): Int {
        var color = random.nextInt(COLOR_COUNT)
        while (color == excluded) color = random.nextInt(COLOR_COUNT)
        return color
    }

    private fun updateProjectiles(delta: Float) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.position += projectile.velocity * delta

            var nearestIndex = -1
            var nearestDistance = Float.MAX_VALUE
            for (index in balls.indices) {
                val ballPosition = positionOnRoute(balls[index].progress)
                val distance = (ballPosition - projectile.position).lengthSquared()
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestIndex = index
                }
            }

            val hitDistance = BALL_RADIUS + PROJECTILE_RADIUS
            if (nearestIndex >= 0 && nearestDistance <= hitDistance * hitDistance) {
                val color = projectile.colorIndex
                iterator.remove()
                insertBallBehind(nearestIndex, color)
                continue
            }

            val p = projectile.position
            if (p.x < -40f || p.x > WORLD_WIDTH + 40f || p.y < -40f || p.y > WORLD_HEIGHT + 40f) {
                iterator.remove()
            }
        }
    }

    private fun insertBallBehind(hitIndex: Int, colorIndex: Int) {
        if (balls.isEmpty() || hitIndex !in balls.indices) return
        val insertIndex = hitIndex + 1
        val insertedProgress = balls[hitIndex].progress - BALL_SPACING
        balls.add(insertIndex, Ball(colorIndex, insertedProgress))
        enforceSpacing(insertIndex + 1)
        resolveMatches(insertIndex)
    }

    private fun enforceSpacing(startIndex: Int) {
        for (index in startIndex until balls.size) {
            val maximumProgress = balls[index - 1].progress - BALL_SPACING
            if (balls[index].progress > maximumProgress) {
                balls[index].progress = maximumProgress
            }
        }
    }

    private fun resolveMatches(initialIndex: Int) {
        var probe = initialIndex
        while (probe in balls.indices) {
            val color = balls[probe].colorIndex
            var start = probe
            var end = probe

            while (start > 0 && balls[start - 1].colorIndex == color) start -= 1
            while (end < balls.lastIndex && balls[end + 1].colorIndex == color) end += 1

            val removedCount = end - start + 1
            if (removedCount < 3) return

            score += removedCount * 10
            balls.subList(start, end + 1).clear()
            for (index in start until balls.size) {
                balls[index].progress += removedCount * BALL_SPACING
            }

            if (balls.isEmpty()) {
                status = GameStatus.WON
                return
            }
            probe = (start - 1).coerceAtLeast(0)
        }
    }
}
