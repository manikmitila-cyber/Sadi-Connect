package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.permissions.SessionRuntimeBridge

class SadiAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    SessionRuntimeBridge.setAccessibilityConnected(true)
    Log.i(TAG, "SadiAccessibilityService connected")
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Only dispatch gestures during explicitly approved remote sessions; no passive content scraping.
  }

  override fun onInterrupt() {
    SessionRuntimeBridge.setAccessibilityConnected(false)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance === this) {
      instance = null
    }
    SessionRuntimeBridge.setAccessibilityConnected(false)
  }

  fun executeNormalizedTap(normX: Double, normY: Double): Boolean {
    if (!SessionRuntimeBridge.remoteTouchApproved.value) {
      Log.w(TAG, "Blocked remote tap: session remoteTouchApproved is false")
      return false
    }
    val (w, h) = getScreenDimensions()
    val px = (normX.coerceIn(0.0, 1.0) * w).toFloat()
    val py = (normY.coerceIn(0.0, 1.0) * h).toFloat()
    val path = Path().apply { moveTo(px, py) }
    val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
    return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
  }

  fun executeNormalizedSwipe(
    startX: Double,
    startY: Double,
    endX: Double,
    endY: Double,
    durationMs: Long = 260L,
  ): Boolean {
    if (!SessionRuntimeBridge.remoteTouchApproved.value) {
      Log.w(TAG, "Blocked remote swipe: session remoteTouchApproved is false")
      return false
    }
    val (w, h) = getScreenDimensions()
    val sx = (startX.coerceIn(0.0, 1.0) * w).toFloat()
    val sy = (startY.coerceIn(0.0, 1.0) * h).toFloat()
    val ex = (endX.coerceIn(0.0, 1.0) * w).toFloat()
    val ey = (endY.coerceIn(0.0, 1.0) * h).toFloat()
    val path =
      Path().apply {
        moveTo(sx, sy)
        lineTo(ex, ey)
      }
    val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(80L, 800L))
    return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
  }

  fun executeGlobalNav(actionType: String): Boolean {
    if (!SessionRuntimeBridge.remoteTouchApproved.value) {
      Log.w(TAG, "Blocked global navigation $actionType: session remoteTouchApproved is false")
      return false
    }
    return when (actionType) {
      "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
      "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
      "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
      "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
      else -> false
    }
  }

  private fun getScreenDimensions(): Pair<Int, Int> {
    val wm = getSystemService(WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bounds = wm.currentWindowMetrics.bounds
      bounds.width() to bounds.height()
    } else {
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      wm.defaultDisplay.getRealMetrics(metrics)
      metrics.widthPixels to metrics.heightPixels
    }
  }

  companion object {
    private const val TAG = "SadiAccessibility"
    @Volatile var instance: SadiAccessibilityService? = null
      private set
  }
}
