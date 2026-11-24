package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.ChatMessage
import com.example.domain.model.RemoteSession
import com.example.presentation.components.SadiCard
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.OnlineGreen
import com.example.utils.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
  currentUserId: String,
  language: AppLanguage,
  activeSession: RemoteSession?,
  messages: List<ChatMessage>,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
  onOpenVoiceCall: () -> Unit,
  onOpenRemoteCamera: () -> Unit,
  onBack: () -> Unit,
  contentPadding: PaddingValues,
) {
  var messageInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val peerName =
    remember(activeSession, currentUserId) {
      if (activeSession == null) "Sadi Connect"
      else if (activeSession.hostUid == currentUserId) {
        activeSession.controllerName.ifBlank { activeSession.controllerDeviceName }
      } else {
        activeSession.hostName.ifBlank { activeSession.hostDeviceName }
      }
    }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(
          top = contentPadding.calculateTopPadding(),
          bottom = contentPadding.calculateBottomPadding(),
        ),
  ) {
    // Top Peer Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
      }
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ElectricBlue.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.Person, contentDescription = peerName, tint = ElectricBlueBright)
      }
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
          text =
            if (activeSession != null) AppStrings.online.get(language)
            else AppStrings.offline.get(language),
          style = MaterialTheme.typography.labelSmall,
          color = if (activeSession != null) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (activeSession != null) {
        IconButton(onClick = onOpenVoiceCall, modifier = Modifier.testTag("chat_call_button")) {
          Icon(Icons.Rounded.Call, contentDescription = AppStrings.voiceCall.get(language), tint = ElectricBlueBright)
        }
        IconButton(onClick = onOpenRemoteCamera, modifier = Modifier.testTag("chat_camera_button")) {
          Icon(Icons.Rounded.Videocam, contentDescription = AppStrings.remoteCamera.get(language), tint = ElectricBlueBright)
        }
        IconButton(onClick = onClearChat, modifier = Modifier.testTag("chat_clear_button")) {
          Icon(Icons.Rounded.DeleteSweep, contentDescription = AppStrings.clearChat.get(language))
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

    if (activeSession == null) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        SadiCard {
          Text(
            text = AppStrings.noActiveSessionChat.get(language),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(messages, key = { it.id }) { msg ->
          val isMine = msg.senderUid == currentUserId
          val timeText = msg.createdAt?.toDate()?.let { timeFormat.format(it) } ?: timeFormat.format(Date())
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              modifier = Modifier.widthIn(max = 290.dp),
              shape =
                RoundedCornerShape(
                  topStart = 18.dp,
                  topEnd = 18.dp,
                  bottomStart = if (isMine) 18.dp else 4.dp,
                  bottomEnd = if (isMine) 4.dp else 18.dp,
                ),
              color = Color.Transparent,
              border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
              Box(
                modifier =
                  Modifier
                    .background(
                      brush =
                        if (isMine) Brush.horizontalGradient(listOf(ElectricBlueDark, ElectricBlue))
                        else Brush.horizontalGradient(listOf(CardNavyElevated, CardNavyElevated)),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
              ) {
                Column {
                  Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                  )
                  Spacer(Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = timeText,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.72f),
                    )
                    if (isMine && msg.delivered) {
                      Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = "Delivered",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Bottom Message Input Bar
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = messageInput,
          onValueChange = { messageInput = it },
          placeholder = { Text(AppStrings.typeMessage.get(language)) },
          leadingIcon = {
            IconButton(onClick = { messageInput = (messageInput + " 👍").trimStart() }) {
              Icon(Icons.Rounded.Mood, contentDescription = "Quick emoji")
            }
          },
          shape = RoundedCornerShape(50),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ElectricBlue,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("chat_message_input"),
        )

        Box(
          modifier =
            Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(ElectricBlue),
          contentAlignment = Alignment.Center,
        ) {
          IconButton(
            onClick = {
              if (messageInput.isNotBlank()) {
                onSendMessage(messageInput)
                messageInput = ""
              }
            },
            modifier = Modifier.testTag("chat_send_button"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
          }
        }
      }
    }
  }
}
