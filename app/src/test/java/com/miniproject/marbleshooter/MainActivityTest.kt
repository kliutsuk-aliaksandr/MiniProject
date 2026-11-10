package com.miniproject.marbleshooter

import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityTest {

    @Test
    fun launchesAfterDecorViewAndContentViewAreInitialized() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)

        try {
            val activity = controller.setup().get()
            val content = activity.findViewById<ViewGroup>(android.R.id.content)

            assertNotNull(activity.window.decorView)
            assertEquals(1, content.childCount)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
