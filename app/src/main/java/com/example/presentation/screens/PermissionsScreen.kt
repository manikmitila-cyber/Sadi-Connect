package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.permissions.PermissionSnapshot
import com.example.permissions.PermissionStatusHelper
import com.example.presentation.components.SadiCard
import com.example.presentation.components.StatusPillBadge
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.WarningAmber
import com.example.utils.AppStrings

@Composable
fun PermissionsScreen(
  language: AppLanguage,
  permissions: PermissionSnapshot,
  onRequestScreenCapture: () -> Unit,
  onRequestCamera: () -> Unit,
  onRequestMic: () -> Unit,
  onRequestNotifications: () -> Unit,
  onRefresh: () -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  var showGuideDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("permissions_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = AppStrings.permissionsAndSettings.get(language),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    item {
      SadiCard {
        PermissionDashboardRow(
          icon = Icons.Rounded.AccessibilityNew,
          iconTint = WarningAmber,
          title = "Accessibility Permission",
          statusText =
            if (permissions.accessibilityGranted) AppStrings.active.get(language)
            else AppStrings.required.get(language),
          granted = permissions.accessibilityGranted,
          onClick = { PermissionStatusHelper.openAccessibilitySettings(context) },
        )
        RowDivider()
        PermissionDashboardRow(
          icon = Icons.Rounded.ScreenShare,
          iconTint = ElectricBlueBright,
          title = "Screen Capture Permission",
          statusText =
            if (permissions.screenCaptureActive) AppStrings.active.get(language)
            else AppStrings.ready.get(language),
          granted = true,
          onClick = onRequestScreenCapture,
        )
        RowDivider()
        PermissionDashboardRow(
          icon = Icons.Rounded.Mic,
          iconTint = ElectricBlue,
          title = "Microphone Permission",
          statusText =
            if (permissions.microphoneGranted) AppStrings.active.get(language)
            else AppStrings.required.get(language),
          granted = permissions.microphoneGranted,
          onClick = onRequestMic,
        )
        RowDivider()
        PermissionDashboardRow(
          icon = Icons.Rounded.PhotoCamera,
          iconTint = WarningAmber,
          title = "Camera Permission",
          statusText =
            if (permissions.cameraGranted) AppStrings.active.get(language)
            else AppStrings.required.get(language),
          granted = permissions.cameraGranted,
          onClick = onRequestCamera,
        )
        RowDivider()
        PermissionDashboardRow(
          icon = Icons.Rounded.Notifications,
          iconTint = OnlineGreen,
          title = "Notification Permission",
          statusText =
            if (permissions.notificationGranted) AppStrings.active.get(language)
            else AppStrings.required.get(language),
          granted = permissions.notificationGranted,
          onClick = onRequestNotifications,
        )
        RowDivider()
        PermissionDashboardRow(
          icon = Icons.Rounded.Wifi,
          iconTint = ElectricBlueBright,
          title = "Network Permission",
          statusText =
            if (permissions.networkConnected) AppStrings.active.get(language)
            else AppStrings.offline.get(language),
          granted = permissions.networkConnected,
          onClick = onRefresh,
        )
      }
    }

    item {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showGuideDialog = true }
            .testTag("permission_guide_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(ElectricBlue),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = Color.White)
          }
          Spacer(Modifier.width(12.dp))
          Text(
            text = AppStrings.permissionGuideHint.get(language),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }

  if (showGuideDialog) {
    AlertDialog(
      onDismissRequest = { showGuideDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.permissionsAndSettings.get(language), fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = AppStrings.permissionGuideBody.get(language),
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showGuideDialog = false }) {
          Text("OK")
        }
      },
    )
  }
}

@Composable
private fun RowDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
  )
}

@Composable
private fun PermissionDashboardRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  statusText: String,
  granted: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(iconTint.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f),
    )
    StatusPillBadge(text = statusText, positive = granted)
  }
}
