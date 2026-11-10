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
import java.util.Collections
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
    private var routePointsCache: List<Vec2> = LevelCatalog.route(definition.trackId)
    private var routeLengthCache: Float = calculateRouteLength(routePointsCache)
    private var obstaclesCache: List<TrackObstacle> = buildObstacles(definition, routeLengthCache)
    private var retraction: Retraction? = null
    private var slowTimer = 0f
    private var reverseTimer = 0f
    private val pendingEvents = mutableListOf<GameEvent>()

    internal val mutableBalls = mutableListOf<Ball>()
    /** Unmodifiable live view of the chain; only the simulation may change its structure. */
    val balls: List<Ball> = Collections.unmodifiableList(mutableBalls)

    internal val mutableProjectiles = mutableListOf<Projectile>()
    /** Unmodifiable live view of active shots; only the simulation may change their structure. */
    val projectiles: List<Projectile> = Collections.unmodifiableList(mutableProjectiles)
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

    /** Immutable definition for the active campaign or practice level. */
    val levelDefinition: LevelDefinition get() = definition
    /** Cached route geometry, rebuilt only when the active definition changes. */
    val routePoints: List<Vec2> get() = routePointsCache
    /** Cached total length of the active route polyline. */
    val routeLength: Float get() = routeLengthCache
    /** Obstacles projected into progress coordinates for the active level. */
    val obstacles: List<TrackObstacle> get() = obstaclesCache

    val ballCount: Int get() = mutableBalls.size
    val projectileCount: Int get() = mutableProjectiles.size
    val headProgress: Float get() = mutableBalls.firstOrNull()?.progress ?: 0f
    val activeColorCount: Int get() = definition.colorCount
    val chainSpeed: Float get() = if (isTutorial) 12f else definition.chainSpeed
    val targetBallCount: Int get() = definition.ballCount
    val slowTimeRemaining: Float get() = slowTimer
    val reverseTimeRemaining: Float get() = reverseTimer
    val isAnimatingRetraction: Boolean get() = retraction != null

    init {
        startNewGame()
    }

    /** Resets the campaign and starts level one. */
    fun startNewGame() {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory()
        isTutorial = false
        startLevelInternal(1)
    }

    /** Starts a selected level; UI callers must check unlock state first. */
    fun startLevel(levelId: Int) {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory()
        isTutorial = false
        startLevelInternal(levelId.coerceIn(1, LevelCatalog.MAX_LEVELS))
    }

    /** Advances the campaign after a win. Practice sessions cannot advance campaign levels. */
    fun startNextLevel(): Boolean {
        if (isTutorial || status != GameStatus.WON || level >= LevelCatalog.MAX_LEVELS) return false
        shotsFired = 0
        isTutorial = false
        startLevelInternal(level + 1)
        return true
    }

    /** Starts an isolated practice chain without changing campaign progression. */
    fun startTutorial() {
        score = 0
        shotsFired = 0
        lastStars = 0
        boosters = BoosterInventory(slowTime = 2, bomb = 1, colorChange = 2, reverseChain = 1)
        isTutorial = true
        setDefinition(
            LevelDefinition(
                id = 1,
                trackId = TrackId.CLASSIC,
                ballCount = 9,
                chainSpeed = 12f,
                colorCount = 4,
                obstacles = emptyList()
            )
        )
        mutableBalls.clear()
        mutableProjectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        level = 1
        status = GameStatus.RUNNING
        val tutorialColors = listOf(0, 1, 2, 2, 1, 3, 0, 3, 1)
        val start = routeLength * 0.55f
        tutorialColors.forEachIndexed { index, color ->
            mutableBalls += Ball(color, start - index * BALL_SPACING)
        }
        ammoColor = 2
        nextColor = 1
        aimTarget = Vec2(210f, 500f)
        pendingEvents += GameEvent.LevelStarted(1)
    }

    /** Refreshes route-derived caches for a newly selected definition. */
    private fun setDefinition(newDefinition: LevelDefinition) {
        definition = newDefinition
        routePointsCache = LevelCatalog.route(newDefinition.trackId)
        routeLengthCache = calculateRouteLength(routePointsCache)
        obstaclesCache = buildObstacles(newDefinition, routeLengthCache)
    }

    private fun buildObstacles(
        levelDefinition: LevelDefinition,
        totalRouteLength: Float
    ): List<TrackObstacle> = levelDefinition.obstacles.map {
        TrackObstacle(it.type, totalRouteLength * it.routeFraction)
    }

    private fun startLevelInternal(levelId: Int) {
        setDefinition(LevelCatalog.definition(levelId))
        lastStars = 0
        level = definition.id
        mutableBalls.clear()
        mutableProjectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        status = GameStatus.RUNNING
        ammoColor = random.nextInt(activeColorCount)
        nextColor = randomDifferentColor(ammoColor)
        var previous = -1
        var repeatCount = 0
        val requiredChainSpan = (targetBallCount - 1) * BALL_SPACING
        val start = max(
            routeLength * START_PROGRESS_FRACTION,
            requiredChainSpan + BALL_SPACING * 2f
        ).coerceAtMost(routeLength - BALL_RADIUS * 2f)
        repeat(targetBallCount) {
            var color = random.nextInt(activeColorCount)
            while (color == previous && repeatCount >= 2) {
                color = random.nextInt(activeColorCount)
            }
            if (color == previous) repeatCount++ else {
                previous = color
                repeatCount = 1
            }
            mutableBalls += Ball(color, start - mutableBalls.size * BALL_SPACING)
        }
        aimTarget = Vec2(210f, 500f)
        pendingEvents += GameEvent.LevelStarted(level)
    }

    /** Pauses the active simulation without discarding the round. */
    fun pauseRound() {
        if (status == GameStatus.RUNNING) status = GameStatus.PAUSED
    }

    /** Resumes a paused simulation. */
    fun resumeRound() {
        if (status == GameStatus.PAUSED) status = GameStatus.RUNNING
    }

    /** Updates the aim point after clamping it to the logical play field. */
    fun aimAt(target: Vec2) {
        val safeX = target.x.coerceIn(0f, WORLD_WIDTH)
        val safeY = if (target.y > shooterPosition.y - 35f) {
            shooterPosition.y - 180f
        } else {
            target.y.coerceIn(0f, WORLD_HEIGHT)
        }
        aimTarget = Vec2(safeX, safeY)
    }

    /** Fires the loaded marble along the current aim vector. */
    fun fireShot() {
        if (status != GameStatus.RUNNING || retraction != null) return
        val direction = (aimTarget - shooterPosition).normalized()
        mutableProjectiles += Projectile(
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

    /** Applies a booster only when it can produce a meaningful effect. Target is a world-space aim point for Bomb. */
    fun activateBooster(type: BoosterType, target: Vec2 = aimTarget): Boolean {
        if (status != GameStatus.RUNNING || retraction != null) return false

        val colorToSelect = if (type == BoosterType.COLOR_CHANGE) {
            if (mutableBalls.isEmpty()) return false
            mutableBalls.groupingBy { it.colorIndex }.eachCount().entries
                .filter { it.key != ammoColor }
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .firstOrNull()?.key ?: return false
        } else null
        if (type == BoosterType.BOMB &&
            (mutableBalls.isEmpty() || !target.x.isFinite() || !target.y.isFinite())
        ) return false

        val updatedInventory = boosters.consume(type) ?: return false
        when (type) {
            BoosterType.SLOW_TIME -> slowTimer = max(slowTimer, 5f)
            BoosterType.REVERSE_CHAIN -> reverseTimer = max(reverseTimer, 3f)
            BoosterType.COLOR_CHANGE -> ammoColor = requireNotNull(colorToSelect)
            BoosterType.BOMB -> {
                val nearestIndex = mutableBalls.indices.minByOrNull { index ->
                    val point = positionOnRoute(mutableBalls[index].progress)
                    (point - target).lengthSquared()
                } ?: return false
                val start = (nearestIndex - 2).coerceAtLeast(0)
                val end = (nearestIndex + 2).coerceAtMost(mutableBalls.lastIndex)
                removeRange(start, end, pointsPerBall = 15 * level, allowCascade = true)
            }
        }
        boosters = updatedInventory
        pendingEvents += GameEvent.BoosterActivated(type)
        return true
    }

    /** Advances simulation by at most 50 ms; callers may use a fixed smaller step. */
    fun update(deltaSeconds: Float) {
        if (status != GameStatus.RUNNING) return
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        if (delta <= 0f) return

        if (retraction != null) {
            updateRetraction(delta)
            updateBoosterTimers(delta)
            return
        }

        if (mutableBalls.isEmpty()) {
            completeLevel()
            return
        }

        updateBoosterTimers(delta)
        val movementDirection = if (reverseTimer > 0f) -0.65f else 1f
        val obstacleMultiplier = obstacleSpeedMultiplier(headProgress)
        val slowMultiplier = if (slowTimer > 0f) 0.34f else 1f
        val movement = chainSpeed * obstacleMultiplier * slowMultiplier * movementDirection * delta
        mutableBalls.forEach { ball ->
            ball.progress = (ball.progress + movement).coerceAtLeast(0f)
        }
        enforceSpacingForward(1)

        if (reverseTimer <= 0f) updateProjectiles(delta)

        if (mutableBalls.isEmpty()) {
            completeLevel()
        } else if (mutableBalls.first().progress >= routeLength - BALL_RADIUS) {
            status = GameStatus.LOST
            mutableProjectiles.clear()
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
        mutableProjectiles.clear()
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

    /** Creates a settled snapshot suitable for persistence. */
    fun saveState(): GameSaveState = GameSaveState(
        level = level,
        score = score,
        ammoColor = ammoColor,
        nextColor = nextColor,
        aimTarget = aimTarget,
        isPaused = true,
        balls = mutableBalls.map { ball ->
            val target = retraction?.moves?.firstOrNull { it.ball === ball }?.target ?: ball.progress
            Ball(ball.colorIndex, target)
        },
        boosters = boosters,
        shotsFired = shotsFired
    )

    /** Validates then restores a snapshot; invalid input leaves the engine unchanged. */
    fun restoreState(state: GameSaveState): Boolean {
        if (state.level !in 1..LevelCatalog.MAX_LEVELS || state.score < 0 || state.shotsFired < 0) return false
        if (state.balls.isEmpty() || state.balls.size > 250) return false
        if (!state.aimTarget.x.isFinite() || !state.aimTarget.y.isFinite()) return false
        if (listOf(state.boosters.slowTime, state.boosters.bomb, state.boosters.colorChange, state.boosters.reverseChain)
                .any { it !in 0..99 }
        ) return false
        if (state.balls.zipWithNext().any { (front, rear) -> front.progress <= rear.progress }) return false
        val candidate = LevelCatalog.definition(state.level)
        val candidateRouteLength = calculateRouteLength(LevelCatalog.route(candidate.trackId))
        if (state.ammoColor !in 0 until candidate.colorCount || state.nextColor !in 0 until candidate.colorCount) return false
        if (state.balls.any {
                it.colorIndex !in 0 until candidate.colorCount ||
                    !it.progress.isFinite() ||
                    it.progress < -candidateRouteLength ||
                    it.progress > candidateRouteLength
            }
        ) return false
        // 0.4.0 allowed the tail of long chains to extend beyond the route start.
        // Shift the whole chain forward on migration, preserving all spacing.
        val legacyOffset = max(0f, BALL_RADIUS * 2f - state.balls.last().progress)
        if (state.balls.first().progress + legacyOffset > candidateRouteLength - BALL_RADIUS) return false

        setDefinition(candidate)
        level = state.level
        score = state.score
        ammoColor = state.ammoColor
        nextColor = state.nextColor
        aimTarget = state.aimTarget
        shotsFired = state.shotsFired
        boosters = state.boosters
        mutableBalls.clear()
        mutableBalls.addAll(state.balls.map { Ball(it.colorIndex, it.progress + legacyOffset) })
        mutableProjectiles.clear()
        pendingEvents.clear()
        retraction = null
        slowTimer = 0f
        reverseTimer = 0f
        lastStars = 0
        isTutorial = false
        status = if (state.isPaused) GameStatus.PAUSED else GameStatus.RUNNING
        return true
    }

    /** Converts route progress into a world-space point on the active polyline. */
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
        val iterator = mutableProjectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            val previous = projectile.position
            val next = previous + projectile.velocity * delta
            var nearestIndex = -1
            var nearestDistance = Float.MAX_VALUE
            for (index in mutableBalls.indices) {
                val point = positionOnRoute(mutableBalls[index].progress)
                val distance = distanceToSegmentSquared(point, previous, next)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestIndex = index
                }
            }
            val hitDistance = BALL_RADIUS + PROJECTILE_RADIUS
            if (nearestIndex >= 0 && nearestDistance <= hitDistance * hitDistance) {
                val color = projectile.colorIndex
                val hitProgress = mutableBalls[nearestIndex].progress
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
        if (retraction != null || mutableBalls.isEmpty() || hitIndex !in mutableBalls.indices) return
        if (colorIndex !in 0 until activeColorCount) return
        val insertIndex = if (insertInFront) hitIndex else hitIndex + 1
        val hitProgress = mutableBalls[hitIndex].progress
        val newProgress = if (insertInFront) hitProgress + BALL_SPACING else hitProgress - BALL_SPACING
        mutableBalls.add(insertIndex, Ball(colorIndex, newProgress))

        if (insertInFront) {
            for (index in insertIndex - 1 downTo 0) {
                val minimum = mutableBalls[index + 1].progress + BALL_SPACING
                if (mutableBalls[index].progress < minimum) mutableBalls[index].progress = minimum
            }
            enforceSpacingForward(insertIndex + 1)
        } else {
            enforceSpacingForward(insertIndex + 1)
        }
        resolveMatchAt(insertIndex)
    }

    private fun enforceSpacingForward(startIndex: Int) {
        for (index in startIndex until mutableBalls.size) {
            val maximum = mutableBalls[index - 1].progress - BALL_SPACING
            if (mutableBalls[index].progress > maximum) mutableBalls[index].progress = maximum
        }
    }

    private fun resolveMatchAt(probe: Int) {
        if (probe !in mutableBalls.indices || retraction != null) return
        val color = mutableBalls[probe].colorIndex
        var start = probe
        var end = probe
        while (start > 0 && mutableBalls[start - 1].colorIndex == color) start--
        while (end < mutableBalls.lastIndex && mutableBalls[end + 1].colorIndex == color) end++
        if (end - start + 1 < 3) return
        removeRange(start, end, pointsPerBall = 10 * level, allowCascade = true)
    }

    private fun removeRange(start: Int, end: Int, pointsPerBall: Int, allowCascade: Boolean) {
        if (start !in mutableBalls.indices || end !in mutableBalls.indices || start > end) return
        val removed = mutableBalls.subList(start, end + 1).toList()
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
        mutableBalls.subList(start, end + 1).clear()

        if (mutableBalls.isEmpty()) {
            completeLevel()
            return
        }

        val hasFront = start > 0
        val hasRear = start < mutableBalls.size
        if (hasFront && hasRear) {
            val retreat = count * BALL_SPACING
            val moves = mutableBalls.take(start).map { ball ->
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
        for (move in animation.moves) {
            move.ball.progress = move.from + (move.target - move.from) * eased
        }
        if (fraction >= 1f) {
            for (move in animation.moves) move.ball.progress = move.target
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
