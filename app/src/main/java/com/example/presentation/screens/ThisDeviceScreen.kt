package com.example.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AppPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.UiState
import com.example.domain.model.UserDevice
import com.example.domain.model.UserProfile
import com.example.permissions.PermissionSnapshot
import com.example.permissions.PermissionStatusHelper
import com.example.presentation.components.LanguageTogglePill
import com.example.presentation.components.SadiBrandLogo
import com.example.presentation.components.SadiCard
import com.example.presentation.components.StatusPillBadge
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.WarningAmber
import com.example.utils.AppStrings
import com.example.utils.DeviceCodeUtils
import com.example.utils.QrCodeUtils

@Composable
fun ThisDeviceScreen(
  preferences: AppPreferences,
  profileState: UiState<UserProfile?>,
  devicesState: UiState<List<UserDevice>>,
  permissions: PermissionSnapshot,
  p2pHostActive: Boolean,
  waitingSupportLink: Pair<String, String>?,
  isConnecting: Boolean,
  onLanguageChange: (AppLanguage) -> Unit,
  onLogout: () -> Unit,
  onGenerateSupportLink: () -> Unit,
  onCancelSupportLink: () -> Unit,
  onConnectByInput: (String) -> Unit,
  onRequestScreenCapture: () -> Unit,
  onToggleFavorite: (UserDevice) -> Unit,
  onNavigateToPermissions: () -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit,
  onNavigateToQrScanner: () -> Unit,
  onShowMessage: (String) -> Unit,
  contentPadding: PaddingValues,
) {
  val context = LocalContext.current
  val lang = preferences.language
  val formattedCode = remember(preferences.localDeviceCode) {
    DeviceCodeUtils.formatCode(preferences.localDeviceCode)
  }
  val qrPayload = remember(waitingSupportLink, preferences.localDeviceCode) {
    waitingSupportLink?.second ?: "sadiconnect://connect/code/${preferences.localDeviceCode}"
  }
  val qrBitmap = remember(qrPayload) { QrCodeUtils.generateQrBitmap(qrPayload, 420) }

  var showConnectDialog by remember { mutableStateOf(false) }
  var connectInput by remember { mutableStateOf("") }

  val profile = (profileState as? UiState.Success)?.data
  val displayName = profile?.displayName?.ifBlank { "Sadi User" } ?: "Sadi User"
  val photoUrl = profile?.photoUrl ?: ""

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding =
      PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = contentPadding.calculateTopPadding() + 10.dp,
        bottom = contentPadding.calculateBottomPadding() + 20.dp,
      ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Top Header Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SadiBrandLogo(size = 40.dp)
          Spacer(Modifier.width(10.dp))
          Column {
            Text(
              text = "Sadi Connect",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = AppStrings.tagline.get(lang),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        LanguageTogglePill(current = lang, onSelect = onLanguageChange)
      }
    }

    // Quick Secondary Navigation Row (Permissions, History, Settings, About)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        QuickShortcutChip(
          icon = Icons.Rounded.Security,
          label = if (lang == AppLanguage.BN) "পারমিশন" else "Permissions",
          onClick = onNavigateToPermissions,
          modifier = Modifier.weight(1f).testTag("shortcut_permissions"),
        )
        QuickShortcutChip(
          icon = Icons.Rounded.History,
          label = if (lang == AppLanguage.BN) "ইতিহাস" else "History",
          onClick = onNavigateToHistory,
          modifier = Modifier.weight(1f).testTag("shortcut_history"),
        )
        QuickShortcutChip(
          icon = Icons.Rounded.Settings,
          label = AppStrings.settings.get(lang),
          onClick = onNavigateToSettings,
          modifier = Modifier.weight(1f).testTag("shortcut_settings"),
        )
        QuickShortcutChip(
          icon = Icons.Rounded.Info,
          label = AppStrings.about.get(lang),
          onClick = onNavigateToAbout,
          modifier = Modifier.weight(1f).testTag("shortcut_about"),
        )
      }
    }

    // Profile Card
    item {
      SadiCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, ElectricBlueBright, CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
          ) {
            if (photoUrl.isNotBlank()) {
              AsyncImage(
                model = photoUrl,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = displayName,
                tint = ElectricBlueBright,
                modifier = Modifier.size(30.dp),
              )
            }
          }
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${preferences.localDeviceName} • ID: $formattedCode",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            StatusPillBadge(
              text =
                if (permissions.networkConnected) AppStrings.online.get(lang)
                else AppStrings.offline.get(lang),
              positive = permissions.networkConnected,
            )
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = onLogout,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = DisconnectRed),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("dashboard_logout_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.Logout,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = Color.White,
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = AppStrings.logout.get(lang),
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
          }
        }
      }
    }

    // Generate Support Link Primary CTA
    item {
      Button(
        onClick = onGenerateSupportLink,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("generate_support_link_button"),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                brush = Brush.horizontalGradient(listOf(ElectricBlueDark, ElectricBlue)),
                shape = RoundedCornerShape(16.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AddLink, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
              text = AppStrings.generateSupportLink.get(lang),
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Active Waiting Support Link Banner (if generated)
    if (waitingSupportLink != null) {
      item {
        SadiCard {
          Text(
            text = AppStrings.supportLinkCreated.get(lang),
            style = MaterialTheme.typography.labelLarge,
            color = OnlineGreen,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = waitingSupportLink.second,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
            color = ElectricBlueBright,
          )
          Spacer(Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                copyText(context, "Sadi Support Link", waitingSupportLink.second)
                onShowMessage(AppStrings.copiedToClipboard.get(lang))
              },
              modifier = Modifier.weight(1f).testTag("copy_support_link_button"),
            ) {
              Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(AppStrings.copyLink.get(lang))
            }
            OutlinedButton(
              onClick = onCancelSupportLink,
              border = BorderStroke(1.dp, DisconnectRed),
              modifier = Modifier.testTag("cancel_support_link_button"),
            ) {
              Text(AppStrings.cancelLink.get(lang), color = DisconnectRed)
            }
          }
        }
      }
    }

    // QR Code & 9-Digit Code Two-Column Card
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SadiCard(modifier = Modifier.weight(0.95f)) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (qrBitmap != null) {
              Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Device QR Code",
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }

        SadiCard(modifier = Modifier.weight(1.25f)) {
          Column(
            modifier = Modifier.height(132.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = AppStrings.nineDigitCode.get(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.height(6.dp))
              Text(
                text = formattedCode.ifBlank { "---" },
                style =
                  MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                  ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("device_code_text"),
              )
            }
            OutlinedButton(
              onClick = {
                copyText(context, "Sadi Device Code", formattedCode)
                onShowMessage(AppStrings.copiedToClipboard.get(lang))
              },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
              modifier = Modifier.fillMaxWidth().testTag("copy_device_code_button"),
            ) {
              Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(AppStrings.copyCode.get(lang))
            }
          }
        }
      }
    }

    // Share + Connect + Scan QR Action Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            val shareText =
              if (waitingSupportLink != null) {
                "Sadi Connect\nDevice Code: $formattedCode\nSupport Link: ${waitingSupportLink.second}"
              } else {
                "Sadi Connect\nDevice Code: $formattedCode"
              }
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
            context.startActivity(Intent.createChooser(intent, AppStrings.shareDevice.get(lang)))
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("share_device_button"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(AppStrings.shareDevice.get(lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Button(
          onClick = { showConnectDialog = true },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueDark),
          modifier = Modifier.weight(1.25f).height(48.dp).testTag("open_connect_dialog_button"),
        ) {
          Icon(Icons.Rounded.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            text = if (lang == AppLanguage.BN) "সংযোগ করুন" else "Connect",
            color = Color.White,
            maxLines = 1,
          )
        }

        OutlinedButton(
          onClick = onNavigateToQrScanner,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 12.dp),
          modifier = Modifier.height(48.dp).testTag("scan_qr_button"),
        ) {
          Icon(Icons.Rounded.QrCodeScanner, contentDescription = AppStrings.scanQr.get(lang))
        }
      }
    }

    // System & Permission Status Card
    item {
      SadiCard {
        StatusRowItem(
          icon = Icons.Rounded.WifiTethering,
          title = AppStrings.localP2pHost.get(lang),
          subtitle =
            if (permissions.localIpAddress.isNotBlank()) "LAN ${permissions.localIpAddress}"
            else "Wi-Fi P2P",
          badgeText = if (p2pHostActive) AppStrings.active.get(lang) else AppStrings.offline.get(lang),
          positive = p2pHostActive,
          onClick = onNavigateToSettings,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        StatusRowItem(
          icon = Icons.Rounded.AccessibilityNew,
          title = AppStrings.accessibilityPermission.get(lang),
          subtitle =
            if (permissions.accessibilityGranted) "Gesture service active"
            else if (lang == AppLanguage.BN) "রিমোট টাচের জন্য চালু করুন" else "Tap to enable in Settings",
          badgeText =
            if (permissions.accessibilityGranted) AppStrings.granted.get(lang)
            else AppStrings.required.get(lang),
          positive = permissions.accessibilityGranted,
          onClick = { PermissionStatusHelper.openAccessibilitySettings(context) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        StatusRowItem(
          icon = Icons.Rounded.ScreenShare,
          title = AppStrings.screenCapturePermission.get(lang),
          subtitle =
            if (permissions.screenCaptureActive) "MediaProjection streaming"
            else "MediaProjection API",
          badgeText =
            if (permissions.screenCaptureActive) AppStrings.active.get(lang)
            else AppStrings.ready.get(lang),
          positive = true,
          onClick = onRequestScreenCapture,
        )
      }
    }

    // Connected / Registered Devices Card
    item {
      SadiCard {
        Text(
          text = AppStrings.connectedDevices.get(lang),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        when (devicesState) {
          is UiState.Loading -> {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is UiState.Error -> {
            Text(
              text = devicesState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = DisconnectRed,
            )
          }
          is UiState.Success -> {
            val list = devicesState.data
            if (list.isEmpty()) {
              Text(
                text = AppStrings.noConnectedDevices.get(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              list.forEachIndexed { idx, device ->
                val isThisDevice = device.deviceCode == preferences.localDeviceCode
                Row(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable {
                        if (!isThisDevice) {
                          onConnectByInput(device.deviceCode)
                        }
                      }
                      .padding(vertical = 8.dp, horizontal = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = ElectricBlueBright,
                    modifier = Modifier.size(26.dp),
                  )
                  Spacer(Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text =
                        if (isThisDevice) "${device.deviceName} (${if (lang == AppLanguage.BN) "এই ডিভাইস" else "This"})"
                        else device.deviceName,
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = DeviceCodeUtils.formatCode(device.deviceCode),
                      style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(onClick = { onToggleFavorite(device) }) {
                    Icon(
                      imageVector = Icons.Rounded.Star,
                      contentDescription = "Favorite",
                      tint = if (device.favorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  StatusPillBadge(
                    text = if (device.online) AppStrings.online.get(lang) else AppStrings.offline.get(lang),
                    positive = device.online,
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (idx < list.lastIndex) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showConnectDialog) {
    AlertDialog(
      onDismissRequest = { showConnectDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.connectToDevice.get(lang), fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = connectInput,
            onValueChange = { connectInput = it },
            label = { Text(AppStrings.enter9DigitCode.get(lang)) },
            placeholder = { Text("469-752-988") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("connect_code_input"),
          )
          if (isConnecting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(8.dp))
              Text(AppStrings.waitingApproval.get(lang), style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onConnectByInput(connectInput)
            showConnectDialog = false
          },
          enabled = connectInput.isNotBlank() && !isConnecting,
          modifier = Modifier.testTag("submit_connect_request_button"),
        ) {
          Text(AppStrings.sendRequest.get(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showConnectDialog = false }) {
          Text(AppStrings.reject.get(lang))
        }
      },
    )
  }
}

@Composable
private fun QuickShortcutChip(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(icon, contentDescription = label, tint = ElectricBlueBright, modifier = Modifier.size(18.dp))
      Spacer(Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StatusRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badgeText: String,
  positive: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(ElectricBlue.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = title, tint = if (positive) OnlineGreen else ElectricBlueBright, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
      Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    StatusPillBadge(text = badgeText, positive = positive)
  }
}

private fun copyText(context: Context, label: String, text: String) {
  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
