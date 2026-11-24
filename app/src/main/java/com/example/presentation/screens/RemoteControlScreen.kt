package com.example.presentation.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.ConnectionTransportState
import com.example.domain.model.RemoteSession
import com.example.domain.model.VideoQuality
import com.example.presentation.components.SadiCard
import com.example.presentation.components.StatusPillBadge
import com.example.ui.theme.CardNavy
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.DisconnectRedBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.WarningAmber
import com.example.utils.AppStrings
import kotlin.math.abs

@Composable
fun RemoteControlScreen(
  currentUserId: String,
  preferences: AppPreferences,
  activeSession: RemoteSession?,
  p2pTransportState: ConnectionTransportState,
  webRtcIceState: ConnectionTransportState,
  p2pLatencyMs: Long?,
  screenCaptureActive: Boolean,
  remoteScreenFrame: ByteArray?,
  localScreenPreviewFrame: ByteArray?,
  onBackClick: () -> Unit,
  onConnectByInput: (String) -> Unit,
  onDisconnect: () -> Unit,
  onSendCommand: (type: String, x: Double, y: Double, endX: Double, endY: Double, value: Double) -> Unit,
  onBrightnessChange: (Int) -> Unit,
  onQualityChange: (VideoQuality, Int) -> Unit,
  onStartScreenShare: () -> Unit,
  onStopScreenShare: () -> Unit,
  onToggleHostTouch: (Boolean) -> Unit,
  onOpenVoiceCall: () -> Unit,
  onOpenRemoteCamera: () -> Unit,
  contentPadding: PaddingValues,
) {
  val context = LocalContext.current
  val lang = preferences.language
  val isHost = activeSession?.hostUid == currentUserId
  var isFullScreen by remember { mutableStateOf(false) }
  var brightness by remember(activeSession?.brightnessPercent) {
    mutableFloatStateOf((activeSession?.brightnessPercent ?: 70L).toFloat())
  }
  var codeInput by remember { mutableStateOf("") }

  val audioManager = remember(context) {
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  }
  var volumePercent by remember {
    mutableFloatStateOf(
      audioManager?.let {
        val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        (it.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f) / max
      } ?: 60f
    )
  }

  val frameBytes = if (isHost) localScreenPreviewFrame else remoteScreenFrame
  val frameBitmap =
    remember(frameBytes) {
      frameBytes?.let { bytes ->
        try {
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
          null
        }
      }
    }

  val connectionHealthy =
    p2pTransportState == ConnectionTransportState.CONNECTED ||
      webRtcIceState == ConnectionTransportState.CONNECTED ||
      activeSession?.status == "approved"

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(
          top = contentPadding.calculateTopPadding() + 6.dp,
          bottom = if (isFullScreen) 8.dp else contentPadding.calculateBottomPadding(),
        )
        .padding(horizontal = 14.dp),
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick, modifier = Modifier.testTag("remote_back_nav")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column {
          Text(
            text = AppStrings.remoteControl.get(lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          if (activeSession != null) {
            val peerLabel =
              if (isHost) activeSession.controllerDeviceName.ifBlank { activeSession.controllerName }
              else activeSession.hostDeviceName
            val rttText = p2pLatencyMs?.let { " • ${it}ms" } ?: ""
            Text(
              text = "$peerLabel • ${activeSession.status.uppercase()}$rttText",
              style = MaterialTheme.typography.labelSmall,
              color = if (connectionHealthy) OnlineGreen else WarningAmber,
            )
          }
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
          onClick = { isFullScreen = !isFullScreen },
          modifier = Modifier.testTag("toggle_fullscreen_button"),
        ) {
          Icon(
            imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = AppStrings.fullScreen.get(lang),
          )
        }
        if (activeSession != null) {
          Surface(
            modifier =
              Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onDisconnect)
                .testTag("disconnect_session_button"),
            shape = RoundedCornerShape(50),
            color = DisconnectRedBg,
            border = BorderStroke(1.dp, DisconnectRed.copy(alpha = 0.6f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.Close, contentDescription = null, tint = DisconnectRed, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(
                text = AppStrings.disconnect.get(lang),
                style = MaterialTheme.typography.labelLarge,
                color = DisconnectRed,
              )
            }
          }
        }
      }
    }

    if (activeSession == null) {
      // Connect-by-code landing card when no active session
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        SadiCard {
          Text(
            text = AppStrings.connectToDevice.get(lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = AppStrings.restrictedScreenNotice.get(lang),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(14.dp))
          OutlinedTextField(
            value = codeInput,
            onValueChange = { codeInput = it },
            label = { Text(AppStrings.enter9DigitCode.get(lang)) },
            placeholder = { Text("469-752-988") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("remote_connect_input"),
          )
          Spacer(Modifier.height(14.dp))
          Button(
            onClick = { onConnectByInput(codeInput) },
            enabled = codeInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("remote_connect_submit"),
          ) {
            Text(AppStrings.sendRequest.get(lang))
          }
        }
      }
      return@Column
    }

    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (!isFullScreen) {
        // 4 Navigation / Notification Quick Tiles (matching Reference UI)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          RemoteNavTile(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            label = AppStrings.back.get(lang),
            onClick = { onSendCommand("back", 0.0, 0.0, 0.0, 0.0, 0.0) },
            modifier = Modifier.weight(1f).testTag("remote_nav_back"),
          )
          RemoteNavTile(
            icon = Icons.Rounded.Home,
            label = AppStrings.home.get(lang),
            onClick = { onSendCommand("home", 0.0, 0.0, 0.0, 0.0, 0.0) },
            modifier = Modifier.weight(1f).testTag("remote_nav_home"),
          )
          RemoteNavTile(
            icon = Icons.Rounded.ViewAgenda,
            label = AppStrings.recent.get(lang),
            onClick = { onSendCommand("recents", 0.0, 0.0, 0.0, 0.0, 0.0) },
            modifier = Modifier.weight(1f).testTag("remote_nav_recent"),
          )
          RemoteNavTile(
            icon = Icons.Rounded.Notifications,
            label = AppStrings.notification.get(lang),
            onClick = { onSendCommand("notifications", 0.0, 0.0, 0.0, 0.0, 0.0) },
            modifier = Modifier.weight(1f).testTag("remote_nav_notifications"),
          )
        }

        // Brightness & Volume Card
        SadiCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.Brightness6, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.brightness.get(lang), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text("${brightness.toInt()}%", style = MaterialTheme.typography.labelLarge, color = ElectricBlueBright)
          }
          Slider(
            value = brightness,
            onValueChange = { brightness = it },
            onValueChangeFinished = { onBrightnessChange(brightness.toInt()) },
            valueRange = 5f..100f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ElectricBlueBright),
            modifier = Modifier.testTag("brightness_slider"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = ElectricBlueBright, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.volume.get(lang), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(10.dp))
            Slider(
              value = volumePercent,
              onValueChange = { pct ->
                volumePercent = pct
                audioManager?.let {
                  val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                  it.setStreamVolume(AudioManager.STREAM_MUSIC, ((pct / 100f) * max).toInt(), 0)
                }
              },
              valueRange = 0f..100f,
              modifier = Modifier.weight(1f).testTag("volume_slider"),
            )
          }

          // Video Quality & FPS selector
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(AppStrings.quality.get(lang), style = MaterialTheme.typography.labelSmall)
            VideoQuality.entries.forEach { q ->
              FilterChip(
                selected = preferences.videoQuality == q,
                onClick = { onQualityChange(q, preferences.targetFps) },
                label = { Text(q.name, style = MaterialTheme.typography.labelSmall) },
              )
            }
            Spacer(Modifier.weight(1f))
            listOf(15, 24, 30).forEach { fps ->
              FilterChip(
                selected = preferences.targetFps == fps,
                onClick = { onQualityChange(preferences.videoQuality, fps) },
                label = { Text("${fps}fps", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }

      // Host Controls if this phone is the Host Device
      if (isHost && activeSession.status == "approved") {
        SadiCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Rounded.ScreenShare, contentDescription = null, tint = ElectricBlueBright)
              Spacer(Modifier.width(8.dp))
              Text(
                text = if (screenCaptureActive) AppStrings.stopSharing.get(lang) else AppStrings.shareMyScreen.get(lang),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Button(
              onClick = { if (screenCaptureActive) onStopScreenShare() else onStartScreenShare() },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (screenCaptureActive) DisconnectRed else ElectricBlue,
                ),
              modifier = Modifier.testTag("host_screen_share_toggle"),
            ) {
              Icon(
                if (screenCaptureActive) Icons.Rounded.StopCircle else Icons.Rounded.ScreenShare,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(if (screenCaptureActive) AppStrings.stopSharing.get(lang) else AppStrings.shareMyScreen.get(lang))
            }
          }
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = OnlineGreen)
              Spacer(Modifier.width(8.dp))
              Text(AppStrings.allowTouchControl.get(lang), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
              checked = activeSession.remoteTouchAllowed,
              onCheckedChange = onToggleHostTouch,
              modifier = Modifier.testTag("host_touch_permission_switch"),
            )
          }
        }
      }

      // Interactive Live Screen Frame (Phone Bezel)
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.height(480.dp) else Modifier.aspectRatio(9f / 14f))
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF050D1E))
            .border(
              width = 2.dp,
              color = if (connectionHealthy) ElectricBlue.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
              shape = RoundedCornerShape(28.dp),
            )
            .pointerInput(activeSession.id, activeSession.status, activeSession.remoteTouchAllowed, isHost) {
              if (!isHost && activeSession.status == "approved" && activeSession.remoteTouchAllowed) {
                detectTapGestures { offset ->
                  val nx = (offset.x / size.width.coerceAtLeast(1)).toDouble()
                  val ny = (offset.y / size.height.coerceAtLeast(1)).toDouble()
                  onSendCommand("tap", nx, ny, nx, ny, 0.0)
                }
              }
            }
            .pointerInput(activeSession.id, activeSession.status, activeSession.remoteTouchAllowed, isHost) {
              if (!isHost && activeSession.status == "approved" && activeSession.remoteTouchAllowed) {
                var startOffset = Offset.Zero
                var endOffset = Offset.Zero
                detectDragGestures(
                  onDragStart = { start ->
                    startOffset = start
                    endOffset = start
                  },
                  onDrag = { change, _ ->
                    change.consume()
                    endOffset = change.position
                  },
                  onDragEnd = {
                    val w = size.width.coerceAtLeast(1).toDouble()
                    val h = size.height.coerceAtLeast(1).toDouble()
                    val sx = (startOffset.x / w).coerceIn(0.0, 1.0)
                    val sy = (startOffset.y / h).coerceIn(0.0, 1.0)
                    val ex = (endOffset.x / w).coerceIn(0.0, 1.0)
                    val ey = (endOffset.y / h).coerceIn(0.0, 1.0)
                    if (abs(ex - sx) > 0.03 || abs(ey - sy) > 0.03) {
                      onSendCommand("swipe", sx, sy, ex, ey, 0.0)
                    }
                  },
                )
              }
            }
            .testTag("live_remote_screen_surface"),
        contentAlignment = Alignment.Center,
      ) {
        if (frameBitmap != null) {
          Image(
            bitmap = frameBitmap,
            contentDescription = "Live Screen Stream",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
          )
        } else {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            StatusPillBadge(
              text =
                when (activeSession.status) {
                  "pending" -> if (lang == AppLanguage.BN) "অনুমোদনের অপেক্ষায়" else "Waiting Approval"
                  "approved" -> if (activeSession.screenShareAllowed) "LIVE STREAM READY" else "CONNECTED"
                  else -> activeSession.status.uppercase()
                },
              positive = activeSession.status == "approved",
            )
            Text(
              text =
                when {
                  activeSession.status == "pending" -> AppStrings.waitingApproval.get(lang)
                  isHost && !screenCaptureActive ->
                    if (lang == AppLanguage.BN) "লাইভ স্ক্রিন শেয়ারিং শুরু করতে উপরের 'আমার স্ক্রিন শেয়ার করুন' বাটনে চাপ দিন।"
                    else "Tap 'Share My Screen' above to begin MediaProjection streaming."
                  !isHost && !activeSession.screenShareAllowed ->
                    if (lang == AppLanguage.BN) "হোস্ট এখনো স্ক্রিন শেয়ারিং শুরু করেনি।"
                    else "Waiting for the host device to start MediaProjection screen sharing."
                  else ->
                    if (lang == AppLanguage.BN) "লাইভ স্ক্রিন ফ্রেম পাওয়া মাত্রই এখানে প্রদর্শিত হবে..."
                    else "Receiving live frames via Local P2P / WebRTC..."
                },
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFCBD5E1),
              textAlign = TextAlign.Center,
            )
            if (isHost && !screenCaptureActive && activeSession.status == "approved") {
              Button(onClick = onStartScreenShare) {
                Text(AppStrings.shareMyScreen.get(lang))
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    // Bottom Floating Dock (Call, Camera, Switch Camera)
    Surface(
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
      shape = RoundedCornerShape(22.dp),
      color = CardNavy,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        DockActionButton(
          icon = Icons.Rounded.Call,
          label = AppStrings.call.get(lang),
          onClick = onOpenVoiceCall,
          modifier = Modifier.testTag("dock_voice_call_button"),
        )
        DockActionButton(
          icon = Icons.Rounded.PhotoCamera,
          label = AppStrings.camera.get(lang),
          onClick = onOpenRemoteCamera,
          modifier = Modifier.testTag("dock_remote_camera_button"),
        )
        DockActionButton(
          icon = Icons.Rounded.Cameraswitch,
          label = AppStrings.switchCamera.get(lang),
          onClick = onOpenRemoteCamera,
          modifier = Modifier.testTag("dock_switch_camera_button"),
        )
      }
    }
  }
}

@Composable
private fun RemoteNavTile(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(64.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = CardNavyElevated,
    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(icon, contentDescription = label, tint = ElectricBlueBright, modifier = Modifier.size(22.dp))
      Spacer(Modifier.height(4.dp))
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
  }
}

@Composable
private fun DockActionButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier.size(44.dp).clip(CircleShape).background(ElectricBlue.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = label, tint = ElectricBlueBright)
    }
    Spacer(Modifier.height(4.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
  }
}
