package com.example.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.accessibility.SadiAccessibilityService
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionSnapshot(
  val accessibilityGranted: Boolean = false,
  val screenCaptureActive: Boolean = false,
  val cameraGranted: Boolean = false,
  val microphoneGranted: Boolean = false,
  val notificationGranted: Boolean = true,
  val networkConnected: Boolean = false,
  val writeSettingsGranted: Boolean = false,
  val localIpAddress: String = "",
)

object PermissionStatusHelper {

  fun isAccessibilityEnabled(context: Context): Boolean {
    val expectedComponent = ComponentName(context, SadiAccessibilityService::class.java).flattenToString()
    val enabledServices =
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
    return enabledServices.split(':').any {
      it.equals(expectedComponent, ignoreCase = true) ||
        it.endsWith(SadiAccessibilityService::class.java.simpleName, ignoreCase = true)
    }
  }

  fun openAccessibilitySettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  fun openWriteSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED

  fun hasMicrophonePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED

  fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val active = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  fun getLocalIpv4Address(): String {
    return try {
      val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
      while (interfaces.hasMoreElements()) {
        val intf = interfaces.nextElement()
        if (!intf.isUp || intf.isLoopback) continue
        val addrs = intf.inetAddresses
        while (addrs.hasMoreElements()) {
          val addr = addrs.nextElement()
          if (addr is Inet4Address && !addr.isLoopbackAddress) {
            val host = addr.hostAddress ?: continue
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
              return host
            }
          }
        }
      }
      ""
    } catch (_: Exception) {
      ""
    }
  }

  fun readSnapshot(context: Context, screenCaptureActive: Boolean): PermissionSnapshot {
    return PermissionSnapshot(
      accessibilityGranted = isAccessibilityEnabled(context),
      screenCaptureActive = screenCaptureActive,
      cameraGranted = hasCameraPermission(context),
      microphoneGranted = hasMicrophonePermission(context),
      notificationGranted = hasNotificationPermission(context),
      networkConnected = isNetworkConnected(context),
      writeSettingsGranted = Settings.System.canWrite(context),
      localIpAddress = getLocalIpv4Address(),
    )
  }
}

object SessionRuntimeBridge {
  private val _screenCaptureActive = MutableStateFlow(false)
  val screenCaptureActive: StateFlow<Boolean> = _screenCaptureActive.asStateFlow()

  private val _remoteTouchApproved = MutableStateFlow(false)
  val remoteTouchApproved: StateFlow<Boolean> = _remoteTouchApproved.asStateFlow()

  private val _remoteScreenFrame = MutableStateFlow<ByteArray?>(null)
  val remoteScreenFrame: StateFlow<ByteArray?> = _remoteScreenFrame.asStateFlow()

  private val _localScreenPreviewFrame = MutableStateFlow<ByteArray?>(null)
  val localScreenPreviewFrame: StateFlow<ByteArray?> = _localScreenPreviewFrame.asStateFlow()

  private val _remoteCameraFrame = MutableStateFlow<ByteArray?>(null)
  val remoteCameraFrame: StateFlow<ByteArray?> = _remoteCameraFrame.asStateFlow()

  private val _audioAmplitude = MutableStateFlow(0f)
  val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

  private val _accessibilityConnected = MutableStateFlow(false)
  val accessibilityConnected: StateFlow<Boolean> = _accessibilityConnected.asStateFlow()

  var onLocalScreenFrameCaptured: ((ByteArray) -> Unit)? = null
  var onStopScreenCaptureRequested: (() -> Unit)? = null

  fun setScreenCaptureActive(active: Boolean) {
    _screenCaptureActive.value = active
    if (!active) {
      _localScreenPreviewFrame.value = null
    }
  }

  fun setRemoteTouchApproved(approved: Boolean) {
    _remoteTouchApproved.value = approved
  }

  fun publishLocalScreenFrame(jpeg: ByteArray) {
    _localScreenPreviewFrame.value = jpeg
    onLocalScreenFrameCaptured?.invoke(jpeg)
  }

  fun publishRemoteScreenFrame(jpeg: ByteArray) {
    _remoteScreenFrame.value = jpeg
  }

  fun publishRemoteCameraFrame(jpeg: ByteArray) {
    _remoteCameraFrame.value = jpeg
  }

  fun clearRemoteStreams() {
    _remoteScreenFrame.value = null
    _remoteCameraFrame.value = null
    _audioAmplitude.value = 0f
  }

  fun setAudioAmplitude(level: Float) {
    _audioAmplitude.value = level.coerceIn(0f, 1f)
  }

  fun setAccessibilityConnected(connected: Boolean) {
    _accessibilityConnected.value = connected
  }
}
