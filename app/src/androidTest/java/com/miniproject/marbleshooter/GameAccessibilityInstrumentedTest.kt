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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.miniproject.marbleshooter.game.ui.GameView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
    fun uiAutomatorCanTapVirtualCanvasControlsAsAnExternalUser() {
        clearGamePreferences()

        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            // UiAutomator discovers virtual controls through Android's accessibility tree,
            // then injects real screen taps at their exposed bounds.
            clickAccessibleText(device, "Настройки")
            clickAccessibleText(device, "Политика конфиденциальности")
            assertAccessibleText(device, "Назад")
            clickAccessibleText(device, "Назад")

            // Change the app language and continue through the level map into gameplay.
            clickAccessibleText(device, "English")
            clickAccessibleText(device, "Back")
            clickAccessibleText(device, "Level map")
            clickAccessibleText(device, "Level 1")

            // Verify the externally injected taps reach the game controls too.
            clickAccessibleText(device, "Pause")
            clickAccessibleText(device, "Resume")
            assertAccessibleText(device, "Pause")
        }
    }

    private fun assertAccessibleText(device: UiDevice, text: String) {
        assertTrue(
            "Expected visible accessibility control '$text'",
            device.wait(Until.hasObject(By.text(text)), UI_TIMEOUT_MS)
        )
    }

    private fun clickAccessibleText(device: UiDevice, text: String) {
        assertAccessibleText(device, text)
        val control = device.findObject(By.text(text))
            ?: throw AssertionError("Expected accessibility control '$text'")
        control.click()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
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
