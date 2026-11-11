package com.miniproject.marbleshooter

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.ui.GameView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

/**
 * Exercises the real Activity, Canvas view and virtual accessibility actions on an emulator.
 * No test-only production hooks are used: actions travel through AccessibilityNodeProvider.
 */
@RunWith(AndroidJUnit4::class)
class GameAccessibilityInstrumentedTest {

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
    }

    @Test
    fun mainActivityExposesCanvasMenuAsAccessibleVirtualControls() {
        clearGamePreferences()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                val gameView = gameView(activity)
                assertTrue(gameView.isShown)
                assertEquals(
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES,
                    gameView.importantForAccessibility
                )

                val provider = accessibilityProvider(gameView)
                val root = provider.createAccessibilityNodeInfo(View.NO_ID)
                assertNotNull("Canvas host must expose a virtual accessibility root", root)
                assertTrue("Menu controls must be exposed as virtual children", root!!.childCount >= 4)

                val settings = node(provider, 5)
                assertEquals("Настройки", settings.text.toString())

                val bounds = Rect()
                settings.getBoundsInParent(bounds)
                assertTrue("Virtual menu controls must have visible bounds", bounds.width() > 0)
                assertTrue("Virtual menu controls must have visible bounds", bounds.height() > 0)
            }
        }
    }

    @Test
    fun accessibilityActionsNavigateSettingsTutorialBoostersAndPauseFlow() {
        clearGamePreferences()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                val provider = accessibilityProvider(gameView(activity))

                // Menu -> Settings -> Privacy -> Settings.
                click(provider, 5)
                assertEquals(
                    "Политика конфиденциальности",
                    node(provider, 34).text.toString()
                )
                click(provider, 34)
                assertEquals("Назад", node(provider, 40).text.toString())
                click(provider, 40)

                // Choose English explicitly, then return to the menu.
                click(provider, 31)
                assertEquals("Back", node(provider, 35).text.toString())
                click(provider, 35)

                // Complete the five lesson pages and enter isolated practice.
                click(provider, 4)
                repeat(4) {
                    assertEquals("Next lesson", node(provider, 22).text.toString())
                    click(provider, 22)
                }
                assertEquals("Start practice", node(provider, 22).text.toString())
                click(provider, 22)

                // All four boosters are available through virtual controls.
                assertTrue(node(provider, 60).text.toString().startsWith("Slow time, 2"))
                assertTrue(node(provider, 61).text.toString().startsWith("Bomb, 1"))
                assertTrue(node(provider, 62).text.toString().startsWith("Change color, 2"))
                assertTrue(node(provider, 63).text.toString().startsWith("Reverse chain, 1"))

                // Activate Slow Time and verify the displayed charge count changes.
                click(provider, 60)
                assertTrue(node(provider, 60).text.toString().startsWith("Slow time, 1"))

                // Pause and resume through the same accessible controls.
                assertEquals("Pause", node(provider, 51).text.toString())
                click(provider, 51)
                assertEquals("Resume", node(provider, 70).text.toString())
                click(provider, 70)
                assertEquals("Pause", node(provider, 51).text.toString())
            }
        }
    }

    @Test
    fun uiAutomatorInjectsScreenTapsThroughSettingsMapAndGameplay() {
        clearGamePreferences()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)
            instrumentation.waitForIdleSync()

            // Verify the initial menu's virtual accessibility label, then use real screen taps.
            assertTargetText(scenario, 5, "Настройки")
            tapWorldPoint(device, scenario, 210f, 671f)
            assertTargetText(scenario, 34, "Политика конфиденциальности")

            tapWorldPoint(device, scenario, 210f, 662f)
            assertTargetText(scenario, 40, "Назад")
            tapWorldPoint(device, scenario, 210f, 817f)
            assertTargetText(scenario, 35, "Назад")

            // Choose English and verify that the selected app language is reflected by controls.
            tapWorldPoint(device, scenario, 300f, 380f)
            assertTargetText(scenario, 35, "Back")
            tapWorldPoint(device, scenario, 210f, 765f)
            assertTargetText(scenario, 3, "Level map")

            // Open the map, select the first unlocked level, then pause and resume.
            tapWorldPoint(device, scenario, 210f, 535f)
            assertTargetText(scenario, 101, "Level 1")
            tapWorldPoint(device, scenario, 56f, 249f, awaitIdle = false)
            assertTargetText(scenario, 51, "Pause")

            tapWorldPoint(device, scenario, 377f, 50f)
            assertTargetText(scenario, 70, "Resume")
            tapWorldPoint(device, scenario, 210f, 475f, awaitIdle = false)
            assertTargetText(scenario, 51, "Pause")
        }
    }

    private fun assertTargetText(
        scenario: ActivityScenario<MainActivity>,
        targetId: Int,
        expectedText: String
    ) {
        scenario.onActivity { activity ->
            assertEquals(
                expectedText,
                node(accessibilityProvider(gameView(activity)), targetId).text.toString()
            )
        }
    }

    /**
     * Converts a 420x900 game-world point to the real screen and injects a UIAutomator tap.
     * This exercises Android touch dispatch rather than calling AccessibilityNodeProvider directly.
     */
    private fun tapWorldPoint(
        device: UiDevice,
        scenario: ActivityScenario<MainActivity>,
        worldX: Float,
        worldY: Float,
        awaitIdle: Boolean = true
    ) {
        var screenX = 0
        var screenY = 0
        scenario.onActivity { activity ->
            val view = gameView(activity)
            assertTrue("GameView must be laid out before injecting a tap", view.width > 0 && view.height > 0)

            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val scale = min(
                view.width / GameEngine.WORLD_WIDTH,
                view.height / GameEngine.WORLD_HEIGHT
            )
            val offsetX = (view.width - GameEngine.WORLD_WIDTH * scale) / 2f
            val offsetY = (view.height - GameEngine.WORLD_HEIGHT * scale) / 2f
            screenX = (location[0] + offsetX + worldX * scale).toInt()
            screenY = (location[1] + offsetY + worldY * scale).toInt()
        }

        assertTrue("UIAutomator must inject tap at ($screenX, $screenY)", device.click(screenX, screenY))
        if (awaitIdle) InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun clearGamePreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("mini_project", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun gameView(activity: MainActivity): GameView {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        assertEquals("MainActivity must host exactly one game view", 1, content.childCount)
        return content.getChildAt(0) as? GameView
            ?: throw AssertionError("MainActivity content must be GameView")
    }

    private fun accessibilityProvider(view: GameView): AccessibilityNodeProvider =
        view.getAccessibilityNodeProvider()

    private fun node(provider: AccessibilityNodeProvider, id: Int) =
        provider.createAccessibilityNodeInfo(id)
            ?: throw AssertionError("Expected accessibility node $id")

    private fun click(provider: AccessibilityNodeProvider, id: Int) {
        assertTrue(
            "Accessibility node $id must handle ACTION_CLICK",
            provider.performAction(id, AccessibilityNodeInfo.ACTION_CLICK, null)
        )
    }
}
