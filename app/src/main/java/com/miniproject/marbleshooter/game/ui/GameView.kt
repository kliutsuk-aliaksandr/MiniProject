package com.miniproject.marbleshooter.game.ui

import android.content.Context
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeProvider
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.model.GameStatus
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.render.GameRenderer
import com.miniproject.marbleshooter.game.render.GameTextResources
import com.miniproject.marbleshooter.game.render.GameScreen
import kotlin.math.min

/**
 * Android View adapter for the Marble Frog game.
 *
 * Session rules, navigation and pointer gesture routing live in dedicated controllers.
 * This class owns only Android lifecycle, Canvas rendering and pixel/world conversion.
 */
class GameView(context: Context) : View(context) {
    private val session = GameSessionController(context) {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    private val renderer = GameRenderer(context)
    private val navigation = GameNavigationController(session, renderer.textResources)
    private val accessibilityProvider by lazy(LazyThreadSafetyMode.NONE) {
        GameAccessibilityNodeProvider(this) {
            navigation.accessibilityTargets(scaleFactor, offsetX, offsetY)
        }
    }
    private val inputRouter = GameInputRouter(
        isGameRunning = {
            navigation.screen == GameScreen.GAME &&
                session.engine.status == GameStatus.RUNNING &&
                !session.isStartInputLocked
        },
        isBoosterTouch = navigation::isBoosterHitTarget,
        canStartAiming = { target ->
            navigation.screen == GameScreen.GAME &&
                session.engine.status == GameStatus.RUNNING &&
                !session.isStartInputLocked &&
                navigation.canStartAiming(target)
        },
        onAim = { target ->
            session.engine.aimAt(target)
            invalidate()
        },
        onFire = { target -> session.fireShot(target, navigation.screen == GameScreen.GAME) },
        onBoosterTap = navigation::handleBoosterTap,
        onScreenTap = navigation::handleTap,
        onInvalidate = ::invalidate,
        onInteractionFinished = {
            performClick()
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            invalidate()
        }
    )

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isClickable = true
    }

    fun resumeGame() {
        session.resumeGame()
        invalidate()
        if (session.shouldContinueAnimating(navigation.screen)) postInvalidateOnAnimation()
    }

    fun pauseGame() {
        inputRouter.cancel()
        session.pauseGame(navigation.screen)
        invalidate()
    }

    fun disposeGame() {
        session.disposeGame(navigation.screen)
    }

    fun handleSystemBack(): Boolean {
        val handled = navigation.handleSystemBack()
        if (handled) {
            inputRouter.cancel()
            invalidate()
        }
        return handled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        session.advanceFrame(navigation.screen)

        val scale = min(width / GameEngine.WORLD_WIDTH, height / GameEngine.WORLD_HEIGHT)
        scaleFactor = scale
        offsetX = (width - GameEngine.WORLD_WIDTH * scale) / 2f
        offsetY = (height - GameEngine.WORLD_HEIGHT * scale) / 2f

        canvas.drawColor(android.graphics.Color.rgb(15, 22, 42))
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        renderer.draw(
            canvas,
            session.engine,
            session.buildRenderState(navigation.screen, navigation.tutorialPage)
        )
        canvas.restore()

        if (session.shouldContinueAnimating(navigation.screen)) {
            postInvalidateOnAnimation()
        }
    }

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider = accessibilityProvider

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scaleFactor <= 0f) return true
        val target = Vec2(
            (event.x - offsetX) / scaleFactor,
            (event.y - offsetY) / scaleFactor
        )
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> GamePointerAction.DOWN
            MotionEvent.ACTION_MOVE -> GamePointerAction.MOVE
            MotionEvent.ACTION_CANCEL -> GamePointerAction.CANCEL
            MotionEvent.ACTION_UP -> GamePointerAction.UP
            else -> return true
        }
        return inputRouter.handle(action, target)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
