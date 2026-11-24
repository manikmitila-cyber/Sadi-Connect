package com.example.presentation.screens

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.AppLanguage
import com.example.domain.model.RemoteSession
import com.example.presentation.components.SadiWaveBackground
import com.example.ui.theme.CardNavy
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.utils.AppStrings
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun RemoteCameraScreen(
  currentUserId: String,
  language: AppLanguage,
  activeSession: RemoteSession?,
  hasCameraPermission: Boolean,
  remoteCameraFrame: ByteArray?,
  onRequestCameraPermission: () -> Unit,
  onToggleCameraApproval: (Boolean) -> Unit,
  onSwitchCameraFacing: () -> Unit,
  onPublishHostCameraFrame: (ByteArray) -> Unit,
  onShowMessage: (String) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val isHost = activeSession?.hostUid == currentUserId || activeSession == null
  val cameraAllowed = activeSession?.remoteCameraAllowed == true
  val cameraFacing = activeSession?.cameraFacing ?: "back"
  var isFullScreen by remember { mutableStateOf(false) }

  val remoteBitmap =
    remember(remoteCameraFrame) {
      remoteCameraFrame?.let { bytes ->
        try {
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
          null
        }
      }
    }

  SadiWaveBackground(darkTheme = true) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("camera_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Text(
            text = AppStrings.remoteCamera.get(language),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = { isFullScreen = !isFullScreen }) {
          Icon(
            imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = AppStrings.fullScreen.get(language),
            tint = Color.White,
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      // Camera Preview Surface
      Box(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF050D1E))
            .border(1.5.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
      ) {
        when {
          isHost && cameraAllowed && hasCameraPermission -> {
            CameraXHostStreamer(
              useFront = cameraFacing == "front",
              onFrameEncoded = onPublishHostCameraFrame,
              modifier = Modifier.fillMaxSize(),
            )
          }
          !isHost && remoteBitmap != null -> {
            Image(
              bitmap = remoteBitmap,
              contentDescription = "Remote Camera Live Preview",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
          }
          else -> {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.VideocamOff,
                contentDescription = null,
                tint = ElectricBlueBright,
                modifier = Modifier.size(54.dp),
              )
              Text(
                text =
                  when {
                    !hasCameraPermission ->
                      if (language == AppLanguage.BN) "ক্যামেরা প্রিভিউর জন্য ক্যামেরা পারমিশন প্রয়োজন।"
                      else "Camera permission is required for live camera preview."
                    isHost && !cameraAllowed ->
                      AppStrings.cameraConsentPrompt.get(language)
                    !isHost && !cameraAllowed ->
                      if (langIsBn(language)) "ডিভাইস মালিক ক্যামেরা চালু করার অনুমতি দিলে লাইভ ভিডিও দেখা যাবে।"
                      else "Waiting for the device owner to approve Remote Camera access."
                    else ->
                      if (langIsBn(language)) "রিমোট ক্যামেরা ফ্রেম লোড হচ্ছে..."
                      else "Waiting for remote camera stream..."
                  },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
              )
              if (!hasCameraPermission) {
                Button(
                  onClick = onRequestCameraPermission,
                  modifier = Modifier.testTag("grant_camera_button"),
                ) {
                  Text(if (langIsBn(language)) "ক্যামেরা অনুমতি দিন" else "Grant Camera Permission")
                }
              } else if (isHost && activeSession != null) {
                Button(
                  onClick = { onToggleCameraApproval(!cameraAllowed) },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = if (cameraAllowed) DisconnectRed else OnlineGreen,
                    ),
                  modifier = Modifier.testTag("host_camera_consent_button"),
                ) {
                  Icon(
                    if (cameraAllowed) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                    contentDescription = null,
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(
                    if (cameraAllowed) AppStrings.cameraOff.get(language)
                    else AppStrings.cameraOn.get(language),
                    color = Color.White,
                  )
                }
              }
            }
          }
        }

        // LIVE Badge top-left
        if (cameraAllowed) {
          Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.6f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DisconnectRed))
              Spacer(Modifier.width(6.dp))
              Text("LIVE • ${cameraFacing.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
          }
        }
      }

      if (!isFullScreen) {
        Spacer(Modifier.height(16.dp))

        // Bottom Camera Controls matching Reference UI
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(CardNavy)
              .padding(vertical = 12.dp, horizontal = 18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onSwitchCameraFacing).padding(8.dp).testTag("switch_camera_button"),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(Icons.Rounded.Cameraswitch, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(AppStrings.switchCamera.get(language), style = MaterialTheme.typography.labelSmall, color = Color.White)
          }

          // Center Shutter / Toggle Button
          Box(
            modifier =
              Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (cameraAllowed) DisconnectRed else Color.White)
                .clickable {
                  if (isHost && activeSession != null) {
                    if (!hasCameraPermission) onRequestCameraPermission()
                    else onToggleCameraApproval(!cameraAllowed)
                  } else {
                    onShowMessage(
                      if (langIsBn(language)) "স্ন্যাপশট সংরক্ষিত হয়েছে" else "Snapshot captured",
                    )
                  }
                }
                .testTag("camera_shutter_button"),
          )

          Column(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onSwitchCameraFacing).padding(8.dp).testTag("front_back_button"),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(AppStrings.frontBack.get(language), style = MaterialTheme.typography.labelSmall, color = Color.White)
          }
        }
      }
    }
  }
}

private fun langIsBn(lang: AppLanguage) = lang == AppLanguage.BN

@Composable
private fun CameraXHostStreamer(
  useFront: Boolean,
  onFrameEncoded: (ByteArray) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember { PreviewView(context) }
  val executor = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(useFront) {
    val future = ProcessCameraProvider.getInstance(context)
    var lastSentMs = 0L
    val listener = Runnable {
      try {
        val provider = future.get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis =
          ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { imgAnalysis ->
              imgAnalysis.setAnalyzer(executor) { imageProxy ->
                val now = System.currentTimeMillis()
                if (now - lastSentMs >= 110L) {
                  lastSentMs = now
                  try {
                    val yBuffer = imageProxy.planes[0].buffer
                    val uBuffer = imageProxy.planes[1].buffer
                    val vBuffer = imageProxy.planes[2].buffer
                    val ySize = yBuffer.remaining()
                    val uSize = uBuffer.remaining()
                    val vSize = vBuffer.remaining()
                    val nv21 = ByteArray(ySize + uSize + vSize)
                    yBuffer.get(nv21, 0, ySize)
                    vBuffer.get(nv21, ySize, vSize)
                    uBuffer.get(nv21, ySize + vSize, uSize)
                    val yuv = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                    val out = ByteArrayOutputStream()
                    yuv.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 55, out)
                    onFrameEncoded(out.toByteArray())
                  } catch (_: Exception) {}
                }
                imageProxy.close()
              }
            }
        val selector =
          if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
      } catch (_: Exception) {}
    }
    future.addListener(listener, ContextCompat.getMainExecutor(context))
    onDispose {
      try {
        future.get().unbindAll()
      } catch (_: Exception) {}
    }
  }

  AndroidView(factory = { previewView }, modifier = modifier)
}
