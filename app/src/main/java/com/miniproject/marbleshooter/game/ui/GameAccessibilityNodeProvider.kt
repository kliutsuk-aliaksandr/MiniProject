package com.miniproject.marbleshooter.game.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.widget.Button

/** A Canvas-drawn control exposed as a virtual Android accessibility child. */
internal data class GameAccessibilityTarget(
    val id: Int,
    val label: String,
    val bounds: Rect,
    val onClick: () -> Unit
)

/** Provides named virtual buttons without placing invisible native Views over the Canvas. */
internal class GameAccessibilityNodeProvider(
    private val host: View,
    private val targetsProvider: () -> List<GameAccessibilityTarget>
) : AccessibilityNodeProvider() {

    override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
        if (virtualViewId == HOST_VIEW_ID) {
            return AccessibilityNodeInfo.obtain().apply {
                setSource(host)
                packageName = host.context.packageName
                className = host.javaClass.name
                setBoundsInParent(Rect(0, 0, host.width, host.height))
                isEnabled = host.isEnabled
                isVisibleToUser = host.isShown
                isFocusable = true
                for (target in targetsProvider()) addChild(host, target.id)
            }
        }
        val target = targetsProvider().firstOrNull { it.id == virtualViewId } ?: return null
        return AccessibilityNodeInfo.obtain().apply {
            setSource(host, target.id)
            setParent(host)
            packageName = host.context.packageName
            className = Button::class.java.name
            text = target.label
            setBoundsInParent(Rect(target.bounds))
            isVisibleToUser = host.isShown
            isFocusable = true
            isClickable = true
            isEnabled = true
            addAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        if (virtualViewId == HOST_VIEW_ID) return host.performAccessibilityAction(action, arguments)
        if (action != AccessibilityNodeInfo.ACTION_CLICK) return false
        val target = targetsProvider().firstOrNull { it.id == virtualViewId } ?: return false
        target.onClick()
        host.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        host.invalidate()
        return true
    }

    private companion object {
        const val HOST_VIEW_ID = View.NO_ID
    }
}
