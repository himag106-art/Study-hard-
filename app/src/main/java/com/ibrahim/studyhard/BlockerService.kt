package com.ibrahim.studyhard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (!Store.active(this)) return
        val blocked = if (Store.hard(this)) Store.locked(this) + Store.HARD_BLOCK else Store.locked(this)
        if (pkg !in blocked) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(Intent(this, BlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onInterrupt() {}
}
