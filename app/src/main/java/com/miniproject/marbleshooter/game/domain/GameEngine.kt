package com.miniproject.marbleshooter.game.domain

import com.miniproject.marbleshooter.game.domain.model.Ball
import com.miniproject.marbleshooter.game.domain.model.BoosterInventory
import com.miniproject.marbleshooter.game.domain.model.BoosterType
import com.miniproject.marbleshooter.game.domain.model.GameEvent
import com.miniproject.marbleshooter.game.domain.model.GameSaveState
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.ObstacleType
import com.miniproject.marbleshooter.game.domain.model.Projectile
import com.miniproject.marbleshooter.game.domain.model.TrackObstacle
import com.miniproject.marbleshooter.game.domain.model.Vec2
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Platform-independent simulation. It has no Android dependencies, so rules can
 * be tested on the JVM. Rendering, persistence and input live in separate layers.
 */
class GameEngine(private val random: Random = Random.Default) {

    private data class RetractionMove(
        val ball: Ball,
        val from: Float,
        val target: Float
    )

    private data class Retraction(
        val moves: List<RetractionMove>,
        val duration: Float,
        val cascadeProbe: Int,
        var elapsed: Float = 0f
    )

    companion object {
        const val WORLD_WIDTH = 420f
        const val WORLD_HEIGHT = 900f
        const val BALL_RADIUS = 16f
        const val BALL_SPACING = BALL_RADIUS * 2f
        const val SHOOTER_X = 210f
        const val SHOOTER_Y = 810f
        private const val PROJECTILE_RADIUS = 10f
        private const val PROJECTILE_SPEED = 720f
        private const val START_PROGRESS_FRACTION = 0.42f
    }

    private var definition: LevelDefinition = LevelCatalog.definition(1)
    private var retraction: Retraction? = null
    private var slowTimer = 0f
    private var reverseTimer = 0f
    private val pendingEvents = mutableListOf<GameEvent>()

    val balls = mutableListOf<Ball>()
    val projectiles = mutableListOf<Projectile>()
    val shooterPosition = Vec2(SHOOTER_X, SHOOTER_Y)

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
    var shotsFired: Int = 0
        private set
    var lastStars: Int = 0
        private set
    var boosters: BoosterInventory = BoosterInventory()
        private set
    var isTutorial: Boolean = false
        private set

    val levelDefinition: LevelDefinition get() = definition
    val routePoints: List<Vec2> get() = LevelCatalog.route(definition.trackId)
    val routeLength: Float
        get() = calculateRouteLength(routePoints)
    val obstacles: List<TrackObstacle>
        get() = definition.obstacles.map {
            TrackObstacle(it.type, routeLength * it.routeFraction)
        }

    val ballCount: Int get() = balls.size
    val projectileCount: Int get() = projectiles.size
    val headProgress: Float get() = balls.firstOrNull()?.progress ?: 0f
    val activeColorCount: Int get() = definition.colorCount
    val chainSpeed: Float get() = if (isTutorial) 12f else definition.chainSpeed
    val targetBallCount: Int get() = definition.ballCount
    val slowTimeRemaining: Float get() = slowTimer
    val reverseTimeRemaining: Float get() = reverseTimer
    val isAnimatingRetraction: Boolean get() = retraction != null

    init {
        startNewGame()
    }

    fun startNewGame() {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory()
        isTutorial = false
        startLevelInternal(1)
    }

    fun startLevel(levelId: Int) {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory()
        isTutorial = false
        startLevelInternal(levelId.coerceIn(1, LevelCatalog.MAX_LEVELS))
    }

    fun startNextLevel(): Boolean {
        if (status != GameStatus.WON || level >= LevelCatalog.MAX_LEVELS) return false
        shotsFired = 0
        isTutorial = false
        startLevelInternal(level + 1)
        return true
    }

    fun startTutorial() {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory(slowTime = 2, bomb = 1, colorChange = 2, reverseChain = 1)
        isTutorial = true
        definition = LevelDefinition(
            id = 1,
            trackId = TrackId.CLASSIC,
            ballCount = 9,
            chainSpeed = 12f,
            colorCount = 4,
            obstacles = emptyList()
        )
        balls.clear()
        projectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        level = 1
        status = GameStatus.RUNNING
        val tutorialColors = listOf(0, 1, 2, 2, 1, 3, 0, 3, 1)
        val start = routeLength * 0.55f
        tutorialColors.forEachIndexed { index, color ->
            balls += Ball(color, start - index * BALL_SPACING)
        }
        ammoColor = 2
        nextColor = 1
        aimTarget = Vec2(210f, 500f)
        pendingEvents += GameEvent.LevelStarted(1)
    }

    private fun startLevelInternal(levelId: Int) {
        definition = LevelCatalog.definition(levelId)
        lastStars = 0
        level = definition.id
        balls.clear()
        projectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        status = GameStatus.RUNNING
        ammoColor = random.nextInt(activeColorCount)
        nextColor = randomDifferentColor(ammoColor)
        var previous = -1
        var repeatCount = 0
        val start = routeLength * START_PROGRESS_FRACTION
        repeat(targetBallCount) {
            var color = random.nextInt(activeColorCount)
            while (color == previous && repeatCount >= 2) {
                color = random.nextInt(activeColorCount)
            }
            if (color == previous) repeatCount++ else {
                previous = color
                repeatCount = 1
            }
            balls += Ball(color, start - balls.size * BALL_SPACING)
        }
        aimTarget = Vec2(210f, 500f)
        pendingEvents += GameEvent.LevelStarted(level)
    }

    fun pauseRound() {
        if (status == GameStatus.RUNNING) status = GameStatus.PAUSED
    }

    fun resumeRound() {
        if (status == GameStatus.PAUSED) status = GameStatus.RUNNING
    }

    fun aimAt(target: Vec2) {
        val safeX = target.x.coerceIn(0f, WORLD_WIDTH)
        val safeY = if (target.y > shooterPosition.y - 35f) {
            shooterPosition.y - 180f
        } else {
            target.y.coerceIn(0f, WORLD_HEIGHT)
        }
        aimTarget = Vec2(safeX, safeY)
    }

    fun fireShot() {
        if (status != GameStatus.RUNNING || retraction != null) return
        val direction = (aimTarget - shooterPosition).normalized()
        projectiles += Projectile(
            colorIndex = ammoColor,
            position = shooterPosition + direction * 50f,
            velocity = direction * PROJECTILE_SPEED
        )
        shotsFired++
        pendingEvents += GameEvent.ShotFired
        ammoColor = nextColor
        nextColor = randomDifferentColor(ammoColor)
    }

    fun onTap(target: Vec2) {
        if (status != GameStatus.RUNNING) return
        if (target.x in 300f..410f && target.y in 80f..145f) {
            swapAmmo()
            return
        }
        aimAt(target)
        fireShot()
    }

    fun swapAmmo() {
        if (status != GameStatus.RUNNING) return
        val old = ammoColor
        ammoColor = nextColor
        nextColor = old
    }

    fun activateBooster(type: BoosterType, target: Vec2 = aimTarget): Boolean {
        if (status != GameStatus.RUNNING || retraction != null) return false
        val updatedInventory = boosters.consume(type) ?: return false

        when (type) {
            BoosterType.SLOW_TIME -> slowTimer = max(slowTimer, 5f)
            BoosterType.REVERSE_CHAIN -> reverseTimer = max(reverseTimer, 3f)
            BoosterType.COLOR_CHANGE -> {
                val frequencies = balls.groupingBy { it.colorIndex }.eachCount()
                val ordered = frequencies.entries.sortedWith(
                    compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key }
                )
                val selected = ordered.firstOrNull { it.key != ammoColor }?.key
                    ?: ordered.firstOrNull()?.key
                if (selected != null) ammoColor = selected
            }
            BoosterType.BOMB -> {
                if (balls.isNotEmpty()) {
                    val nearestIndex = balls.indices.minByOrNull { index ->
                        val point = positionOnRoute(balls[index].progress)
                        (point - target).lengthSquared()
                    } ?: 0
                    val start = (nearestIndex - 2).coerceAtLeast(0)
                    val end = (nearestIndex + 2).coerceAtMost(balls.lastIndex)
                    removeRange(start, end, pointsPerBall = 15 * level, allowCascade = true)
                }
            }
        }

        boosters = updatedInventory
        pendingEvents += GameEvent.BoosterActivated(type)
        return true
    }

    fun update(deltaSeconds: Float) {
        if (status != GameStatus.RUNNING) return
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        if (delta <= 0f) return

        if (retraction != null) {
            updateRetraction(delta)
            updateBoosterTimers(delta)
            return
        }

        if (balls.isEmpty()) {
            completeLevel()
            return
        }

        updateBoosterTimers(delta)
        val movementDirection = if (reverseTimer > 0f) -0.65f else 1f
        val obstacleMultiplier = obstacleSpeedMultiplier(headProgress)
        val slowMultiplier = if (slowTimer > 0f) 0.34f else 1f
        val movement = chainSpeed * obstacleMultiplier * slowMultiplier * movementDirection * delta
        balls.forEach { ball ->
            ball.progress = (ball.progress + movement).coerceAtLeast(0f)
        }
        enforceSpacingForward(1)

        if (reverseTimer <= 0f) updateProjectiles(delta)

        if (balls.isEmpty()) {
            completeLevel()
        } else if (balls.first().progress >= routeLength - BALL_RADIUS) {
            status = GameStatus.LOST
            projectiles.clear()
            pendingEvents += GameEvent.GameLost
        }
    }

    private fun updateBoosterTimers(delta: Float) {
        slowTimer = (slowTimer - delta).coerceAtLeast(0f)
        reverseTimer = (reverseTimer - delta).coerceAtLeast(0f)
    }

    private fun obstacleSpeedMultiplier(progress: Float): Float {
        val active = obstacles.firstOrNull {
            progress >= it.progress - it.radius && progress <= it.progress + it.radius
        } ?: return 1f
        return when (active.type) {
            ObstacleType.MUD -> 0.55f
            ObstacleType.ICE -> 1.2f
            ObstacleType.ROCK -> 0.72f
        }
    }

    private fun completeLevel() {
        if (status == GameStatus.WON) return
        status = GameStatus.WON
        retraction = null
        projectiles.clear()
        val threeStarLimit = max(4, (targetBallCount * 0.45f).toInt())
        val twoStarLimit = max(7, (targetBallCount * 0.8f).toInt())
        val stars = when {
            shotsFired <= threeStarLimit -> 3
            shotsFired <= twoStarLimit -> 2
            else -> 1
        }
        lastStars = stars
        pendingEvents += GameEvent.LevelCompleted(level, stars, score)
    }

    fun drainEvents(): List<GameEvent> {
        if (pendingEvents.isEmpty()) return emptyList()
        return pendingEvents.toList().also { pendingEvents.clear() }
    }

    fun saveState(): GameSaveState = GameSaveState(
        level = level,
        score = score,
        ammoColor = ammoColor,
        nextColor = nextColor,
        aimTarget = aimTarget,
        isPaused = true,
        balls = balls.map { ball ->
            val target = retraction?.moves?.firstOrNull { it.ball === ball }?.target ?: ball.progress
            Ball(ball.colorIndex, target)
        },
        boosters = boosters,
        shotsFired = shotsFired
    )

    fun restoreState(state: GameSaveState): Boolean {
        if (state.level !in 1..LevelCatalog.MAX_LEVELS || state.score < 0 || state.shotsFired < 0) return false
        if (state.balls.isEmpty() || state.balls.size > 250) return false
        if (!state.aimTarget.x.isFinite() || !state.aimTarget.y.isFinite()) return false
        if (listOf(state.boosters.slowTime, state.boosters.bomb, state.boosters.colorChange, state.boosters.reverseChain)
                .any { it !in 0..99 }
        ) return false
        if (state.balls.zipWithNext().any { (front, rear) -> front.progress <= rear.progress }) return false
        val candidate = LevelCatalog.definition(state.level)
        if (state.ammoColor !in 0 until candidate.colorCount || state.nextColor !in 0 until candidate.colorCount) return false
        if (state.balls.any {
                it.colorIndex !in 0 until candidate.colorCount ||
                    !it.progress.isFinite() ||
                    it.progress < 0f ||
                    it.progress > calculateRouteLength(LevelCatalog.route(candidate.trackId))
            }
        ) return false

        definition = candidate
        level = state.level
        score = state.score
        ammoColor = state.ammoColor
        nextColor = state.nextColor
        aimTarget = state.aimTarget
        shotsFired = state.shotsFired
        boosters = state.boosters
        balls.clear()
        balls.addAll(state.balls.map { Ball(it.colorIndex, it.progress) })
        projectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        lastStars = 0
        isTutorial = false
        status = if (state.isPaused) GameStatus.PAUSED else GameStatus.RUNNING
        return true
    }

    fun positionOnRoute(progress: Float): Vec2 {
        val route = routePoints
        var remaining = progress.coerceIn(0f, routeLength)
        for (index in 0 until route.lastIndex) {
            val start = route[index]
            val end = route[index + 1]
            val segment = end - start
            val length = segment.length()
            if (remaining <= length || index == route.lastIndex - 1) {
                return if (length <= 0.0001f) start else start + segment * (remaining / length).coerceIn(0f, 1f)
            }
            remaining -= length
        }
        return route.last()
    }

    private fun tangentOnRoute(progress: Float): Vec2 {
        val route = routePoints
        var remaining = progress.coerceIn(0f, routeLength)
        for (index in 0 until route.lastIndex) {
            val segment = route[index + 1] - route[index]
            val length = segment.length()
            if (remaining <= length || index == route.lastIndex - 1) return segment.normalized()
            remaining -= length
        }
        return (route.last() - route[route.lastIndex - 1]).normalized()
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
                val point = positionOnRoute(balls[index].progress)
                val distance = distanceToSegmentSquared(point, previous, next)
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
                val movingWithChain =
                    projectile.velocity.x * tangent.x + projectile.velocity.y * tangent.y >= 0f
                iterator.remove()
                insertBallAt(nearestIndex, color, insertInFront = !movingWithChain)
                if (retraction != null || status != GameStatus.RUNNING) break
                continue
            }

            projectile.position = next
            if (next.x < -40f || next.x > WORLD_WIDTH + 40f ||
                next.y < -40f || next.y > WORLD_HEIGHT + 40f
            ) iterator.remove()
        }
    }

    internal fun insertBallAt(hitIndex: Int, colorIndex: Int, insertInFront: Boolean) {
        if (retraction != null || balls.isEmpty() || hitIndex !in balls.indices) return
        if (colorIndex !in 0 until activeColorCount) return
        val insertIndex = if (insertInFront) hitIndex else hitIndex + 1
        val hitProgress = balls[hitIndex].progress
        val newProgress = if (insertInFront) hitProgress + BALL_SPACING else hitProgress - BALL_SPACING
        balls.add(insertIndex, Ball(colorIndex, newProgress))

        if (insertInFront) {
            for (index in insertIndex - 1 downTo 0) {
                val minimum = balls[index + 1].progress + BALL_SPACING
                if (balls[index].progress < minimum) balls[index].progress = minimum
            }
            enforceSpacingForward(insertIndex + 1)
        } else {
            enforceSpacingForward(insertIndex + 1)
        }
        resolveMatchAt(insertIndex)
    }

    private fun enforceSpacingForward(startIndex: Int) {
        for (index in startIndex until balls.size) {
            val maximum = balls[index - 1].progress - BALL_SPACING
            if (balls[index].progress > maximum) balls[index].progress = maximum
        }
    }

    private fun resolveMatchAt(probe: Int) {
        if (probe !in balls.indices || retraction != null) return
        val color = balls[probe].colorIndex
        var start = probe
        var end = probe
        while (start > 0 && balls[start - 1].colorIndex == color) start--
        while (end < balls.lastIndex && balls[end + 1].colorIndex == color) end++
        if (end - start + 1 < 3) return
        removeRange(start, end, pointsPerBall = 10 * level, allowCascade = true)
    }

    private fun removeRange(start: Int, end: Int, pointsPerBall: Int, allowCascade: Boolean) {
        if (start !in balls.indices || end !in balls.indices || start > end) return
        val removed = balls.subList(start, end + 1).toList()
        val count = removed.size
        val color = removed[count / 2].colorIndex
        val centerProgress = removed.map { it.progress }.average().toFloat()
        pendingEvents += GameEvent.MatchCleared(
            center = positionOnRoute(centerProgress),
            colorIndex = color,
            count = count,
            removedPositions = removed.map { positionOnRoute(it.progress) }
        )
        score += count * pointsPerBall
        balls.subList(start, end + 1).clear()

        if (balls.isEmpty()) {
            completeLevel()
            return
        }

        val hasFront = start > 0
        val hasRear = start < balls.size
        if (hasFront && hasRear) {
            val retreat = count * BALL_SPACING
            val moves = balls.take(start).map { ball ->
                RetractionMove(ball, ball.progress, ball.progress - retreat)
            }
            retraction = Retraction(
                moves = moves,
                duration = (retreat / 320f).coerceIn(0.22f, 0.55f),
                cascadeProbe = (start - 1).coerceAtLeast(0)
            )
        } else if (allowCascade) {
            resolveMatchAt((start - 1).coerceAtLeast(0))
        }
    }

    private fun updateRetraction(delta: Float) {
        val animation = retraction ?: return
        animation.elapsed = (animation.elapsed + delta).coerceAtMost(animation.duration)
        val fraction = (animation.elapsed / animation.duration).coerceIn(0f, 1f)
        val eased = fraction * fraction * (3f - 2f * fraction)
        animation.moves.forEach { move ->
            move.ball.progress = move.from + (move.target - move.from) * eased
        }
        if (fraction >= 1f) {
            animation.moves.forEach { it.ball.progress = it.target }
            val cascade = animation.cascadeProbe
            retraction = null
            resolveMatchAt(cascade)
        }
    }

    private fun randomDifferentColor(excluded: Int): Int {
        if (activeColorCount <= 1) return 0
        var value = random.nextInt(activeColorCount)
        while (value == excluded) value = random.nextInt(activeColorCount)
        return value
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

    private fun calculateRouteLength(points: List<Vec2>): Float {
        var total = 0f
        for (index in 0 until points.lastIndex) total += (points[index + 1] - points[index]).length()
        return total
    }
}
