package com.example.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.RemoteSession
import com.example.presentation.components.SadiCard
import com.example.presentation.components.SadiWaveBackground
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OnlineGreen
import com.example.utils.AppStrings
import java.util.Locale

@Composable
fun VoiceCallScreen(
  currentUserId: String,
  language: AppLanguage,
  activeSession: RemoteSession?,
  callDurationSeconds: Long,
  audioAmplitude: Float,
  muted: Boolean,
  speakerOn: Boolean,
  hasMicPermission: Boolean,
  onRequestMicPermission: () -> Unit,
  onStartCall: () -> Unit,
  onAcceptCall: () -> Unit,
  onEndOrRejectCall: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onBack: () -> Unit,
) {
  val peerName =
    remember(activeSession, currentUserId) {
      if (activeSession == null) "Peer Device"
      else if (activeSession.hostUid == currentUserId) {
        activeSession.controllerName.ifBlank { activeSession.controllerDeviceName }
      } else {
        activeSession.hostName.ifBlank { activeSession.hostDeviceName }
      }
    }
  val callState = activeSession?.voiceCallState ?: "idle"
  val isIncomingRinging = callState == "ringing" && activeSession?.voiceCallerUid != currentUserId
  var showStats by remember { mutableStateOf(false) }
  val animatedAmp by animateFloatAsState(targetValue = audioAmplitude, animationSpec = tween(120), label = "amp")

  SadiWaveBackground(darkTheme = true) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = AppStrings.voiceCall.get(language),
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(Modifier.weight(0.6f))

      // Glowing Avatar
      Box(
        modifier =
          Modifier
            .size(136.dp)
            .clip(CircleShape)
            .border(3.dp, ElectricBlueBright, CircleShape)
            .background(CardNavyElevated),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Person,
          contentDescription = peerName,
          tint = ElectricBlueBright,
          modifier = Modifier.size(64.dp),
        )
      }

      Spacer(Modifier.height(20.dp))
      Text(
        text = peerName,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(8.dp))

      val formattedTimer =
        remember(callDurationSeconds) {
          val h = callDurationSeconds / 3600
          val m = (callDurationSeconds % 3600) / 60
          val s = callDurationSeconds % 60
          String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        }

      Text(
        text =
          when {
            callState == "active" -> formattedTimer
            isIncomingRinging -> AppStrings.incomingCall.get(language)
            callState == "ringing" -> AppStrings.ringing.get(language)
            else -> if (language == AppLanguage.BN) "কল শুরু করতে প্রস্তুত" else "Ready to call"
          },
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
        color = if (callState == "active") OnlineGreen else ElectricBlueBright,
      )

      Spacer(Modifier.height(28.dp))

      // Real-time Audio Waveform Driven by AudioRecord / AudioTrack RMS
      Canvas(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp)) {
        val barCount = 24
        val spacing = size.width / barCount
        val barW = spacing * 0.45f
        val centerY = size.height / 2f
        val effectiveAmp = if (callState == "active") animatedAmp.coerceAtLeast(0.08f) else 0.06f
        for (i in 0 until barCount) {
          val envelope = 1f - kotlin.math.abs((i - barCount / 2f) / (barCount / 2f))
          val barH = (size.height * 0.85f * effectiveAmp * (0.35f + 0.65f * envelope)).coerceAtLeast(8f)
          drawRoundRect(
            color = if (i % 2 == 0) CyanGlow else ElectricBlue,
            topLeft = Offset(i * spacing + (spacing - barW) / 2f, centerY - barH / 2f),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
          )
        }
      }

      if (!hasMicPermission) {
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequestMicPermission, modifier = Modifier.testTag("grant_mic_button")) {
          Icon(Icons.Rounded.Mic, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(if (language == AppLanguage.BN) "মাইক্রোফোন অনুমতি দিন" else "Grant Microphone Permission")
        }
      }

      if (showStats) {
        Spacer(Modifier.height(12.dp))
        SadiCard {
          Text(
            text = "WebRTC + Local P2P Audio • 16kHz PCM16 • Mic: ${if (muted) "Muted" else "Live"} • Speaker: ${if (speakerOn) "On" else "Earpiece"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
          )
        }
      }

      Spacer(Modifier.weight(1f))

      // Control Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        VoiceControlCircle(
          icon = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
          label = if (muted) AppStrings.micMute.get(language) else AppStrings.micUnmute.get(language),
          active = muted,
          onClick = onToggleMute,
          modifier = Modifier.testTag("voice_mute_button"),
        )
        VoiceControlCircle(
          icon = Icons.AutoMirrored.Rounded.VolumeUp,
          label = AppStrings.speaker.get(language),
          active = speakerOn,
          onClick = onToggleSpeaker,
          modifier = Modifier.testTag("voice_speaker_button"),
        )
        VoiceControlCircle(
          icon = Icons.Rounded.Dialpad,
          label = AppStrings.keypad.get(language),
          active = showStats,
          onClick = { showStats = !showStats },
          modifier = Modifier.testTag("voice_stats_button"),
        )
      }

      Spacer(Modifier.height(28.dp))

      when {
        isIncomingRinging -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Button(
              onClick = onEndOrRejectCall,
              shape = RoundedCornerShape(50),
              colors = ButtonDefaults.buttonColors(containerColor = DisconnectRed),
              modifier = Modifier.weight(1f).height(56.dp).testTag("reject_voice_call_button"),
            ) {
              Icon(Icons.Rounded.CallEnd, contentDescription = null, tint = Color.White)
              Spacer(Modifier.width(8.dp))
              Text(AppStrings.reject.get(language), color = Color.White)
            }
            Button(
              onClick = onAcceptCall,
              shape = RoundedCornerShape(50),
              colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
              modifier = Modifier.weight(1f).height(56.dp).testTag("accept_voice_call_button"),
            ) {
              Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White)
              Spacer(Modifier.width(8.dp))
              Text(AppStrings.accept.get(language), color = Color.White)
            }
          }
        }
        callState == "active" || callState == "ringing" -> {
          Button(
            onClick = onEndOrRejectCall,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = DisconnectRed),
            modifier = Modifier.width(200.dp).height(56.dp).testTag("end_voice_call_button"),
          ) {
            Icon(Icons.Rounded.CallEnd, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.endCall.get(language), color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
        else -> {
          Button(
            onClick = {
              if (!hasMicPermission) onRequestMicPermission()
              onStartCall()
            },
            enabled = activeSession != null,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
            modifier = Modifier.width(220.dp).height(56.dp).testTag("start_voice_call_button"),
          ) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.BN) "ভয়েস কল শুরু করুন" else "Start Voice Call",
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(Modifier.height(18.dp))
    }
  }
}

@Composable
private fun VoiceControlCircle(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      color = if (active) ElectricBlue else CardNavyElevated,
      border = BorderStroke(1.dp, if (active) ElectricBlueBright else Color(0xFF284377)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.White)
      }
    }
    Spacer(Modifier.height(6.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
  }
}
