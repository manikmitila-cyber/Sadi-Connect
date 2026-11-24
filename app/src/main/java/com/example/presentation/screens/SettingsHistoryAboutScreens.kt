package com.example.presentation.screens

import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.ConnectionHistoryItem
import com.example.domain.model.UiState
import com.example.presentation.components.SadiBrandLogo
import com.example.presentation.components.SadiCard
import com.example.presentation.components.SadiWaveBackground
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.utils.AppStrings
import com.example.utils.QrCodeUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun SettingsScreen(
  preferences: AppPreferences,
  onDarkModeChange: (Boolean) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onRenameDevice: (String) -> Unit,
  onNotificationsToggle: (Boolean) -> Unit,
  onLocalP2pToggle: (Boolean) -> Unit,
  onSaveIceConfig: (String, String, String, String) -> Unit,
  onDisconnectAll: () -> Unit,
  onLogout: () -> Unit,
  onShowMessage: (String) -> Unit,
  onBack: () -> Unit,
) {
  val lang = preferences.language
  var showRenameDialog by remember { mutableStateOf(false) }
  var showIceDialog by remember { mutableStateOf(false) }
  var showUpdateDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = AppStrings.settings.get(lang),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    item {
      SadiCard {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = ElectricBlueBright)
          Spacer(Modifier.width(12.dp))
          Text(AppStrings.darkMode.get(lang), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
          Switch(
            checked = preferences.darkMode,
            onCheckedChange = onDarkModeChange,
            modifier = Modifier.testTag("dark_mode_switch"),
          )
        }
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Language,
          title = AppStrings.languageSelection.get(lang),
          trailingText = if (lang == AppLanguage.BN) "বাংলা" else "English",
          onClick = {
            onLanguageChange(if (lang == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
          },
          modifier = Modifier.testTag("settings_language_row"),
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Devices,
          title = AppStrings.deviceManagement.get(lang),
          trailingText = preferences.localDeviceName,
          onClick = { showRenameDialog = true },
          modifier = Modifier.testTag("settings_device_rename_row"),
        )
        SettingsDivider()
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Notifications, contentDescription = null, tint = ElectricBlueBright)
          Spacer(Modifier.width(12.dp))
          Text(AppStrings.notifications.get(lang), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
          Switch(
            checked = preferences.notificationsEnabled,
            onCheckedChange = onNotificationsToggle,
            modifier = Modifier.testTag("notifications_switch"),
          )
        }
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Rounded.WifiTethering,
          title = AppStrings.connectionSettings.get(lang),
          trailingText = if (preferences.localP2pEnabled) "P2P + WebRTC" else "WebRTC",
          onClick = { showIceDialog = true },
          modifier = Modifier.testTag("settings_connection_row"),
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Rounded.SystemUpdate,
          title = AppStrings.appUpdate.get(lang),
          trailingText = "v1.0.0",
          onClick = { showUpdateDialog = true },
          modifier = Modifier.testTag("settings_update_row"),
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.AutoMirrored.Rounded.Logout,
          title = AppStrings.logout.get(lang),
          trailingText = "",
          tint = DisconnectRed,
          onClick = onLogout,
          modifier = Modifier.testTag("settings_logout_row"),
        )
      }
    }

    item {
      OutlinedButton(
        onClick = {
          onDisconnectAll()
          onShowMessage(if (lang == AppLanguage.BN) "সব সেশন বিচ্ছিন্ন করা হয়েছে" else "All sessions disconnected")
        },
        border = BorderStroke(1.dp, DisconnectRed.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("disconnect_all_button"),
      ) {
        Text(
          text = if (lang == AppLanguage.BN) "সব সংযোগ বিচ্ছিন্ন করুন" else "Disconnect All Sessions",
          color = DisconnectRed,
        )
      }
    }
  }

  if (showRenameDialog) {
    var nameInput by remember { mutableStateOf(preferences.localDeviceName) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.deviceManagement.get(lang)) },
      text = {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text(if (lang == AppLanguage.BN) "ডিভাইসের নাম" else "Device Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(onClick = {
          onRenameDevice(nameInput)
          showRenameDialog = false
        }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
    )
  }

  if (showIceDialog) {
    var p2p by remember { mutableStateOf(preferences.localP2pEnabled) }
    var stun by remember { mutableStateOf(preferences.stunServerUrl) }
    var turn by remember { mutableStateOf(preferences.turnServerUrl) }
    var turnUser by remember { mutableStateOf(preferences.turnUsername) }
    var turnCred by remember { mutableStateOf(preferences.turnCredential) }
    AlertDialog(
      onDismissRequest = { showIceDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.connectionSettings.get(lang)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Local Wi-Fi P2P", modifier = Modifier.weight(1f))
            Switch(checked = p2p, onCheckedChange = { p2p = it })
          }
          OutlinedTextField(value = stun, onValueChange = { stun = it }, label = { Text("STUN Server") }, singleLine = true)
          OutlinedTextField(value = turn, onValueChange = { turn = it }, label = { Text("TURN Server (Optional)") }, singleLine = true)
          OutlinedTextField(value = turnUser, onValueChange = { turnUser = it }, label = { Text("TURN Username") }, singleLine = true)
          OutlinedTextField(value = turnCred, onValueChange = { turnCred = it }, label = { Text("TURN Credential") }, singleLine = true)
        }
      },
      confirmButton = {
        Button(onClick = {
          onLocalP2pToggle(p2p)
          onSaveIceConfig(stun, turn, turnUser, turnCred)
          showIceDialog = false
        }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showIceDialog = false }) { Text("Cancel") } },
    )
  }

  if (showUpdateDialog) {
    AlertDialog(
      onDismissRequest = { showUpdateDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text("Sadi Connect v1.0.0") },
      text = {
        Text(
          if (lang == AppLanguage.BN) "আপনার Sadi Connect অ্যাপটি সর্বশেষ ভার্সনে রয়েছে।"
          else "You are running the latest version of Sadi Connect (v1.0.0).",
        )
      },
      confirmButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("OK") } },
    )
  }
}

@Composable
private fun SettingsDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
  )
}

@Composable
private fun SettingsActionRow(
  icon: ImageVector,
  title: String,
  trailingText: String,
  tint: Color = ElectricBlueBright,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = title, tint = tint)
    Spacer(Modifier.width(12.dp))
    Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    if (trailingText.isNotBlank()) {
      Text(text = trailingText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.width(4.dp))
    }
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun ConnectionHistoryScreen(
  language: AppLanguage,
  historyState: UiState<List<ConnectionHistoryItem>>,
  onClearAll: () -> Unit,
  onBack: () -> Unit,
) {
  val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

  Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
      }
      Text(
        text = AppStrings.connectionHistory.get(language),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    }
    Spacer(Modifier.height(10.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (historyState) {
        is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        is UiState.Error -> Text(historyState.message, color = DisconnectRed, modifier = Modifier.align(Alignment.Center))
        is UiState.Success -> {
          val list = historyState.data
          if (list.isEmpty()) {
            SadiCard(modifier = Modifier.align(Alignment.TopCenter)) {
              Text(
                text = if (language == AppLanguage.BN) "কোনো সংযোগ ইতিহাস পাওয়া যায়নি।" else "No connection history recorded yet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
              )
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              items(list, key = { it.id }) { item ->
                SadiCard {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ElectricBlue.copy(alpha = 0.16f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = ElectricBlueBright)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(text = item.peerDeviceName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                      val ts = item.createdAt?.toDate()?.let { dateFormat.format(it) } ?: ""
                      Text(
                        text = "$ts • ${item.direction.uppercase()} (${item.durationSeconds}s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Box(
                      modifier =
                        Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(if (item.status == "connected" || item.status == "disconnected") OnlineGreen else DisconnectRed),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    Button(
      onClick = onClearAll,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("clear_history_button"),
    ) {
      Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ElectricBlueBright)
      Spacer(Modifier.width(8.dp))
      Text(AppStrings.clearAll.get(language))
    }
  }
}

@Composable
fun AboutScreen(
  language: AppLanguage,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }

  SadiWaveBackground(darkTheme = true) {
    Column(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = AppStrings.about.get(language),
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(Modifier.height(24.dp))
      SadiBrandLogo(size = 92.dp)
      Spacer(Modifier.height(16.dp))
      Text(
        text =
          buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("Sadi ") }
            withStyle(SpanStyle(color = ElectricBlueBright, fontWeight = FontWeight.Bold)) { append("Connect") }
          },
        style = MaterialTheme.typography.headlineMedium,
      )
      Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
      Spacer(Modifier.height(6.dp))
      Text(
        text = AppStrings.tagline.get(language).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFCBD5E1),
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = AppStrings.aboutDescription.get(language),
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFFCBD5E1),
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))

      SadiCard {
        SettingsActionRow(
          icon = Icons.Rounded.PrivacyTip,
          title = AppStrings.privacyPolicy.get(language),
          trailingText = "",
          onClick = { showPrivacyDialog = true },
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.AutoMirrored.Rounded.HelpOutline,
          title = AppStrings.helpAndSupport.get(language),
          trailingText = "",
          onClick = { showHelpDialog = true },
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Share,
          title = AppStrings.shareApp.get(language),
          trailingText = "",
          onClick = {
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Sadi Connect — Secure Remote Screen Sharing, Control, Voice Call & Chat")
              }
            context.startActivity(Intent.createChooser(intent, AppStrings.shareApp.get(language)))
          },
        )
      }
      Spacer(Modifier.weight(1f))
      Text("© 2026 Sadi Connect. All rights reserved.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
    }
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.privacyPolicy.get(language)) },
      text = {
        Text(
          if (language == AppLanguage.BN) {
            "Sadi Connect কখনো ব্যবহারকারীর অজান্তে স্ক্রিন, ক্যামেরা বা মাইক্রোফোন চালু করে না। প্রতিটি রিমোট সেশন শুধুমাত্র ডিভাইস মালিকের স্পষ্ট অনুমোদনের পর শুরু হয় এবং যেকোনো সময় বন্ধ করা যায়।"
          } else {
            "Sadi Connect never captures your screen, camera, or audio without explicit consent. Every remote session requires owner approval and can be terminated anytime."
          },
        )
      },
      confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") } },
    )
  }

  if (showHelpDialog) {
    AlertDialog(
      onDismissRequest = { showHelpDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(AppStrings.helpAndSupport.get(language)) },
      text = { Text(AppStrings.permissionGuideBody.get(language)) },
      confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("OK") } },
    )
  }
}

@Composable
fun QrScannerScreen(
  language: AppLanguage,
  hasCameraPermission: Boolean,
  onRequestCameraPermission: () -> Unit,
  onQrDecoded: (String) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember { PreviewView(context) }
  val executor = remember { Executors.newSingleThreadExecutor() }
  var handled by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("qr_scanner_back")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
      }
      Text(AppStrings.scanQr.get(language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(12.dp))

    if (!hasCameraPermission) {
      SadiCard {
        Text(
          text = if (language == AppLanguage.BN) "QR স্ক্যান করতে ক্যামেরা পারমিশন দিন।" else "Grant camera permission to scan a Sadi Connect QR code.",
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRequestCameraPermission) {
          Text(if (language == AppLanguage.BN) "ক্যামেরা অনুমতি দিন" else "Grant Camera Permission")
        }
      }
    } else {
      DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
          try {
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis =
              ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                  analyzer.setAnalyzer(executor) { proxy ->
                    if (!handled) {
                      try {
                        val buffer = proxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val decoded = QrCodeUtils.decodeYuvQrCode(bytes, proxy.width, proxy.height)
                        if (!decoded.isNullOrBlank()) {
                          handled = true
                          ContextCompat.getMainExecutor(context).execute { onQrDecoded(decoded) }
                        }
                      } catch (_: Exception) {}
                    }
                    proxy.close()
                  }
                }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
          } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
        onDispose {
          try {
            future.get().unbindAll()
          } catch (_: Exception) {}
        }
      }

      Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
      }
    }
  }
}
