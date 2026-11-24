package com.example.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.VideoQuality
import com.example.permissions.SessionRuntimeBridge
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  private var captureThread: HandlerThread? = null
  private var captureHandler: Handler? = null
  private var lastFrameTimestampMs = 0L

  @Volatile private var targetWidth = 720
  @Volatile private var jpegQuality = 65
  @Volatile private var minFrameIntervalMs = 50L

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopCapture()
        SessionRuntimeBridge.onStopScreenCaptureRequested?.invoke()
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_UPDATE_QUALITY -> {
        val quality = VideoQuality.fromWire(intent.getStringExtra(EXTRA_QUALITY))
        val fps = intent.getIntExtra(EXTRA_FPS, 20).coerceIn(5, 60)
        applyQuality(quality, fps)
        return START_STICKY
      }
      ACTION_START -> {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
          }
        val quality = VideoQuality.fromWire(intent.getStringExtra(EXTRA_QUALITY))
        val fps = intent.getIntExtra(EXTRA_FPS, 20).coerceIn(5, 60)
        applyQuality(quality, fps)

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
          )
        } else {
          startForeground(NOTIFICATION_ID, notification)
        }

        if (resultCode == Activity.RESULT_OK && resultData != null) {
          startMediaProjection(resultCode, resultData)
        } else {
          Log.w(TAG, "Missing valid MediaProjection result")
          stopSelf()
        }
      }
    }
    return START_NOT_STICKY
  }

  private fun applyQuality(quality: VideoQuality, fps: Int) {
    targetWidth = quality.maxWidth
    jpegQuality = quality.jpegQuality
    minFrameIntervalMs = (1000L / fps.coerceIn(5, 60)).coerceAtLeast(16L)
  }

  private fun startMediaProjection(resultCode: Int, data: Intent) {
    stopCapture()
    val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val projection = mgr.getMediaProjection(resultCode, data)
    if (projection == null) {
      Log.e(TAG, "getMediaProjection returned null")
      stopSelf()
      return
    }
    mediaProjection = projection
    captureThread = HandlerThread("SadiScreenCapture").apply { start() }
    captureHandler = Handler(captureThread!!.looper)

    projection.registerCallback(
      object : MediaProjection.Callback() {
        override fun onStop() {
          stopCapture()
          stopSelf()
        }
      },
      captureHandler,
    )

    val (screenW, screenH, densityDpi) = getDisplayMetrics()
    val aspect = screenH.toFloat() / screenW.coerceAtLeast(1).toFloat()
    val captureW = targetWidth.coerceAtMost(screenW)
    val captureH = (captureW * aspect).toInt().coerceAtLeast(320)

    imageReader =
      ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2).apply {
        setOnImageAvailableListener(
          { reader ->
            val now = System.currentTimeMillis()
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
              if (now - lastFrameTimestampMs < minFrameIntervalMs) {
                return@setOnImageAvailableListener
              }
              lastFrameTimestampMs = now
              val planes = image.planes
              if (planes.isEmpty()) return@setOnImageAvailableListener
              val buffer = planes[0].buffer
              val pixelStride = planes[0].pixelStride
              val rowStride = planes[0].rowStride
              val rowPadding = rowStride - pixelStride * captureW
              val rawBitmap =
                Bitmap.createBitmap(
                  captureW + rowPadding / pixelStride,
                  captureH,
                  Bitmap.Config.ARGB_8888,
                )
              rawBitmap.copyPixelsFromBuffer(buffer)
              val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, captureW, captureH)
              if (rawBitmap !== cropped) rawBitmap.recycle()
              val out = ByteArrayOutputStream()
              cropped.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
              cropped.recycle()
              SessionRuntimeBridge.publishLocalScreenFrame(out.toByteArray())
            } catch (e: Exception) {
              Log.w(TAG, "Frame capture error: ${e.message}")
            } finally {
              image.close()
            }
          },
          captureHandler,
        )
      }

    virtualDisplay =
      projection.createVirtualDisplay(
        "SadiConnectScreen",
        captureW,
        captureH,
        densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        imageReader!!.surface,
        null,
        captureHandler,
      )
    SessionRuntimeBridge.setScreenCaptureActive(true)
  }

  private fun stopCapture() {
    SessionRuntimeBridge.setScreenCaptureActive(false)
    try {
      virtualDisplay?.release()
    } catch (_: Exception) {}
    virtualDisplay = null
    try {
      imageReader?.close()
    } catch (_: Exception) {}
    imageReader = null
    try {
      mediaProjection?.stop()
    } catch (_: Exception) {}
    mediaProjection = null
    captureThread?.quitSafely()
    captureThread = null
    captureHandler = null
  }

  override fun onDestroy() {
    stopCapture()
    super.onDestroy()
  }

  private fun getDisplayMetrics(): Triple<Int, Int, Int> {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bounds = wm.currentWindowMetrics.bounds
      Triple(bounds.width(), bounds.height(), resources.displayMetrics.densityDpi)
    } else {
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      wm.defaultDisplay.getRealMetrics(metrics)
      Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.notification_channel_screen_share),
          NotificationManager.IMPORTANCE_LOW,
        )
      nm.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(): Notification {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val stopIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.notification_screen_share_title))
      .setContentText(getString(R.string.notification_screen_share_text))
      .setContentIntent(openIntent)
      .addAction(0, getString(R.string.action_stop_sharing), stopIntent)
      .setOngoing(true)
      .build()
  }

  companion object {
    private const val TAG = "ScreenCaptureService"
    private const val CHANNEL_ID = "sadi_screen_share_channel"
    private const val NOTIFICATION_ID = 4401

    const val ACTION_START = "com.example.services.ACTION_START_CAPTURE"
    const val ACTION_STOP = "com.example.services.ACTION_STOP_CAPTURE"
    const val ACTION_UPDATE_QUALITY = "com.example.services.ACTION_UPDATE_QUALITY"
    const val EXTRA_RESULT_CODE = "result_code"
    const val EXTRA_RESULT_DATA = "result_data"
    const val EXTRA_QUALITY = "quality"
    const val EXTRA_FPS = "fps"
  }
}
