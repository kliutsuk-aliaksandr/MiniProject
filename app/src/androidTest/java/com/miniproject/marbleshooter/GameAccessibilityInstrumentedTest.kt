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
import com.miniproject.marbleshooter.game.ui.GameView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Activity, Canvas view and virtual accessibility actions on an emulator.
 * Accessibility provider actions and externally injected touch taps are tested separately.
 */
@RunWith(AndroidJUnit4::class)
class GameAccessibilityInstrumentedTest {

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
    fun uiAutomatorInjectsTapsAtAccessibleBoundsThroughSettingsMapAndGameplay() {
        clearGamePreferences()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)
            instrumentation.waitForIdleSync()

            // Use the virtual control's screen bounds, but inject a real coordinate tap.
            tapAccessibilityTarget(device, scenario, 5)
            assertTargetText(scenario, 34, "Политика конфиденциальности")

            tapAccessibilityTarget(device, scenario, 34)
            assertTargetText(scenario, 40, "Назад")
            tapAccessibilityTarget(device, scenario, 40)
            assertTargetText(scenario, 35, "Назад")

            // Choose English and verify the selected app language is reflected by controls.
            tapAccessibilityTarget(device, scenario, 31)
            assertTargetText(scenario, 35, "Back")
            tapAccessibilityTarget(device, scenario, 35)
            assertTargetText(scenario, 3, "Level map")

            // Open the map and verify its first unlocked level with a real screen tap.
            tapAccessibilityTarget(device, scenario, 3)
            assertTargetText(scenario, 101, "Level 1")
            tapAccessibilityTarget(device, scenario, 10)
            assertTargetText(scenario, 4, "Tutorial")

            // Enter the slower, isolated practice round to avoid a timing-sensitive campaign run.
            tapAccessibilityTarget(device, scenario, 4)
            repeat(4) { tapAccessibilityTarget(device, scenario, 22) }
            assertTargetText(scenario, 22, "Start practice")
            tapAccessibilityTarget(device, scenario, 22, awaitIdle = false)
            assertTargetText(scenario, 51, "Pause")

            tapAccessibilityTarget(device, scenario, 51)
            assertTargetText(scenario, 70, "Resume")
            tapAccessibilityTarget(device, scenario, 70, awaitIdle = false)
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
     * Uses the actual bounds exposed by a virtual accessibility control, then injects
     * a screen-coordinate tap with UiDevice. This exercises Android touch dispatch.
     */
    private fun tapAccessibilityTarget(
        device: UiDevice,
        scenario: ActivityScenario<MainActivity>,
        targetId: Int,
        awaitIdle: Boolean = true
    ) {
        var screenX = 0
        var screenY = 0
        var screenBounds = Rect()
        scenario.onActivity { activity ->
            val target = node(accessibilityProvider(gameView(activity)), targetId)
            target.getBoundsInScreen(screenBounds)
            assertTrue(
                "Accessibility target $targetId must expose non-empty screen bounds: $screenBounds",
                screenBounds.width() > 0 && screenBounds.height() > 0
            )
            screenX = screenBounds.centerX()
            screenY = screenBounds.centerY()
        }

        assertTrue(
            "UIAutomator must inject tap for accessibility target $targetId at ($screenX, $screenY)",
            device.click(screenX, screenY)
        )
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
