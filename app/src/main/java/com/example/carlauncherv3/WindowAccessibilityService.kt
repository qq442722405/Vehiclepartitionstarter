package com.example.carlauncherv3
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
class WindowAccessibilityService : AccessibilityService() {
    companion object { var instance: WindowAccessibilityService? = null }
    override fun onServiceConnected() { super.onServiceConnected(); instance=this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance=null; super.onDestroy() }
}
