package com.example.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.SadiViewModel
import com.example.presentation.components.IncomingSessionApprovalDialog
import com.example.presentation.components.SadiWaveBackground
import com.example.presentation.screens.AboutScreen
import com.example.presentation.screens.ChatScreen
import com.example.presentation.screens.ConnectionHistoryScreen
import com.example.presentation.screens.PermissionsScreen
import com.example.presentation.screens.QrScannerScreen
import com.example.presentation.screens.RemoteCameraScreen
import com.example.presentation.screens.RemoteControlScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.ThisDeviceScreen
import com.example.presentation.screens.VoiceCallScreen
import com.example.ui.theme.CardNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.utils.AppStrings
import kotlinx.coroutines.flow.collectLatest

object SadiRoutes {
  const val THIS_DEVICE = "this_device"
  const val REMOTE_CONTROL = "remote_control"
  const val CHAT = "chat"
  const val VOICE_CALL = "voice_call"
  const val REMOTE_CAMERA = "remote_camera"
  const val PERMISSIONS = "permissions"
  const val SETTINGS = "settings"
  const val HISTORY = "history"
  const val ABOUT = "about"
  const val QR_SCANNER = "qr_scanner"
}

@Composable
fun SadiMainShell(
  currentUserId: String,
  viewModel: SadiViewModel,
  pendingDeepLinkInput: String?,
  onDeepLinkConsumed: () -> Unit,
  onRequestScreenCapture: () -> Unit,
  onRequestCameraPermission: () -> Unit,
  onRequestMicPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onSignOut: () -> Unit,
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  val prefs by viewModel.preferences.collectAsStateWithLifecycle()
  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val devicesState by viewModel.userDevices.collectAsStateWithLifecycle()
  val historyState by viewModel.connectionHistory.collectAsStateWithLifecycle()
  val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
  val incomingRequest by viewModel.incomingRequest.collectAsStateWithLifecycle()
  val waitingSupportLink by viewModel.waitingSupportLink.collectAsStateWithLifecycle()
  val permissions by viewModel.permissions.collectAsStateWithLifecycle()
  val p2pHostActive by viewModel.p2pHostActive.collectAsStateWithLifecycle()
  val p2pTransportState by viewModel.p2pTransportState.collectAsStateWithLifecycle()
  val webRtcIceState by viewModel.webRtcIceState.collectAsStateWithLifecycle()
  val p2pLatencyMs by viewModel.p2pLatencyMs.collectAsStateWithLifecycle()
  val screenCaptureActive by viewModel.screenCaptureActive.collectAsStateWithLifecycle()
  val remoteScreenFrame by viewModel.remoteScreenFrame.collectAsStateWithLifecycle()
  val localScreenPreviewFrame by viewModel.localScreenPreviewFrame.collectAsStateWithLifecycle()
  val remoteCameraFrame by viewModel.remoteCameraFrame.collectAsStateWithLifecycle()
  val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
  val callDurationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
  val voiceMuted by viewModel.voiceMuted.collectAsStateWithLifecycle()
  val voiceSpeakerOn by viewModel.voiceSpeakerOn.collectAsStateWithLifecycle()
  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()

  val lang = prefs.language

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collectLatest { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  LaunchedEffect(pendingDeepLinkInput) {
    if (!pendingDeepLinkInput.isNullOrBlank()) {
      viewModel.connectByInput(pendingDeepLinkInput) {
        navController.navigate(SadiRoutes.REMOTE_CONTROL) {
          launchSingleTop = true
        }
      }
      onDeepLinkConsumed()
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val mainTabRoutes = setOf(SadiRoutes.THIS_DEVICE, SadiRoutes.REMOTE_CONTROL, SadiRoutes.CHAT)
  val showPrimaryNav = currentRoute in mainTabRoutes
  val isWideScreen = LocalConfiguration.current.screenWidthDp >= 720

  val tabs =
    listOf(
      Triple(SadiRoutes.THIS_DEVICE, AppStrings.navThisDevice.get(lang), Icons.Rounded.Devices to Icons.Outlined.Devices),
      Triple(SadiRoutes.REMOTE_CONTROL, AppStrings.navRemoteControl.get(lang), Icons.Rounded.ScreenShare to Icons.Outlined.ScreenShare),
      Triple(SadiRoutes.CHAT, AppStrings.navChat.get(lang), Icons.Rounded.ChatBubble to Icons.Outlined.ChatBubbleOutline),
    )

  SadiWaveBackground(darkTheme = prefs.darkMode) {
    Scaffold(
      containerColor = Color.Transparent,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (showPrimaryNav && !isWideScreen) {
          NavigationBar(
            containerColor = if (prefs.darkMode) CardNavy else MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            tabs.forEach { (route, label, icons) ->
              val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = { Icon(if (selected) icons.first else icons.second, contentDescription = label) },
                label = { Text(label) },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricBlueBright,
                    selectedTextColor = ElectricBlueBright,
                    indicatorColor = ElectricBlue.copy(alpha = 0.2f),
                  ),
                modifier = Modifier.testTag("bottom_tab_$route"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize()) {
        if (showPrimaryNav && isWideScreen) {
          NavigationRail(
            containerColor = if (prefs.darkMode) CardNavy else MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
          ) {
            tabs.forEach { (route, label, icons) ->
              val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = { Icon(if (selected) icons.first else icons.second, contentDescription = label) },
                label = { Text(label) },
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = SadiRoutes.THIS_DEVICE,
          ) {
            composable(SadiRoutes.THIS_DEVICE) {
              ThisDeviceScreen(
                preferences = prefs,
                profileState = profileState,
                devicesState = devicesState,
                permissions = permissions,
                p2pHostActive = p2pHostActive,
                waitingSupportLink = waitingSupportLink,
                isConnecting = isConnecting,
                onLanguageChange = viewModel::setLanguage,
                onLogout = onSignOut,
                onGenerateSupportLink = viewModel::generateSupportLink,
                onCancelSupportLink = viewModel::cancelWaitingSupportLink,
                onConnectByInput = { input ->
                  viewModel.connectByInput(input) {
                    navController.navigate(SadiRoutes.REMOTE_CONTROL) { launchSingleTop = true }
                  }
                },
                onRequestScreenCapture = onRequestScreenCapture,
                onToggleFavorite = viewModel::toggleFavoriteDevice,
                onNavigateToPermissions = { navController.navigate(SadiRoutes.PERMISSIONS) },
                onNavigateToHistory = { navController.navigate(SadiRoutes.HISTORY) },
                onNavigateToSettings = { navController.navigate(SadiRoutes.SETTINGS) },
                onNavigateToAbout = { navController.navigate(SadiRoutes.ABOUT) },
                onNavigateToQrScanner = { navController.navigate(SadiRoutes.QR_SCANNER) },
                onShowMessage = viewModel::emitMessage,
                contentPadding = innerPadding,
              )
            }

            composable(SadiRoutes.REMOTE_CONTROL) {
              RemoteControlScreen(
                currentUserId = currentUserId,
                preferences = prefs,
                activeSession = activeSession,
                p2pTransportState = p2pTransportState,
                webRtcIceState = webRtcIceState,
                p2pLatencyMs = p2pLatencyMs,
                screenCaptureActive = screenCaptureActive,
                remoteScreenFrame = remoteScreenFrame,
                localScreenPreviewFrame = localScreenPreviewFrame,
                onBackClick = { navController.navigate(SadiRoutes.THIS_DEVICE) },
                onConnectByInput = { viewModel.connectByInput(it) },
                onDisconnect = viewModel::disconnectActiveSession,
                onSendCommand = viewModel::sendRemoteGestureOrNav,
                onBrightnessChange = viewModel::setBrightnessPercent,
                onQualityChange = viewModel::setVideoQuality,
                onStartScreenShare = onRequestScreenCapture,
                onStopScreenShare = viewModel::stopLocalScreenShareService,
                onToggleHostTouch = viewModel::toggleHostTouchControl,
                onOpenVoiceCall = { navController.navigate(SadiRoutes.VOICE_CALL) },
                onOpenRemoteCamera = { navController.navigate(SadiRoutes.REMOTE_CAMERA) },
                contentPadding = innerPadding,
              )
            }

            composable(SadiRoutes.CHAT) {
              ChatScreen(
                currentUserId = currentUserId,
                language = lang,
                activeSession = activeSession,
                messages = chatMessages,
                onSendMessage = viewModel::sendChatMessage,
                onClearChat = viewModel::clearChatHistory,
                onOpenVoiceCall = { navController.navigate(SadiRoutes.VOICE_CALL) },
                onOpenRemoteCamera = { navController.navigate(SadiRoutes.REMOTE_CAMERA) },
                onBack = { navController.navigate(SadiRoutes.THIS_DEVICE) },
                contentPadding = innerPadding,
              )
            }

            composable(SadiRoutes.VOICE_CALL) {
              VoiceCallScreen(
                currentUserId = currentUserId,
                language = lang,
                activeSession = activeSession,
                callDurationSeconds = callDurationSeconds,
                audioAmplitude = audioAmplitude,
                muted = voiceMuted,
                speakerOn = voiceSpeakerOn,
                hasMicPermission = permissions.microphoneGranted,
                onRequestMicPermission = onRequestMicPermission,
                onStartCall = viewModel::startOutgoingVoiceCall,
                onAcceptCall = viewModel::acceptVoiceCall,
                onEndOrRejectCall = viewModel::rejectOrEndVoiceCall,
                onToggleMute = viewModel::toggleVoiceMute,
                onToggleSpeaker = viewModel::toggleVoiceSpeaker,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.REMOTE_CAMERA) {
              RemoteCameraScreen(
                currentUserId = currentUserId,
                language = lang,
                activeSession = activeSession,
                hasCameraPermission = permissions.cameraGranted,
                remoteCameraFrame = remoteCameraFrame,
                onRequestCameraPermission = onRequestCameraPermission,
                onToggleCameraApproval = viewModel::toggleRemoteCameraPermission,
                onSwitchCameraFacing = viewModel::switchCameraFacing,
                onPublishHostCameraFrame = viewModel::publishCameraFrameFromHost,
                onShowMessage = viewModel::emitMessage,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.PERMISSIONS) {
              PermissionsScreen(
                language = lang,
                permissions = permissions,
                onRequestScreenCapture = onRequestScreenCapture,
                onRequestCamera = onRequestCameraPermission,
                onRequestMic = onRequestMicPermission,
                onRequestNotifications = onRequestNotificationPermission,
                onRefresh = viewModel::refreshPermissions,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.SETTINGS) {
              SettingsScreen(
                preferences = prefs,
                onDarkModeChange = viewModel::setDarkMode,
                onLanguageChange = viewModel::setLanguage,
                onRenameDevice = viewModel::renameThisDevice,
                onNotificationsToggle = viewModel::setNotificationsEnabled,
                onLocalP2pToggle = viewModel::setLocalP2pEnabled,
                onSaveIceConfig = viewModel::updateIceConfig,
                onDisconnectAll = viewModel::disconnectAllSessions,
                onLogout = onSignOut,
                onShowMessage = viewModel::emitMessage,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.HISTORY) {
              ConnectionHistoryScreen(
                language = lang,
                historyState = historyState,
                onClearAll = viewModel::clearConnectionHistory,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.ABOUT) {
              AboutScreen(
                language = lang,
                onBack = { navController.popBackStack() },
              )
            }

            composable(SadiRoutes.QR_SCANNER) {
              QrScannerScreen(
                language = lang,
                hasCameraPermission = permissions.cameraGranted,
                onRequestCameraPermission = onRequestCameraPermission,
                onQrDecoded = { payload ->
                  navController.popBackStack()
                  viewModel.connectByInput(payload) {
                    navController.navigate(SadiRoutes.REMOTE_CONTROL) { launchSingleTop = true }
                  }
                },
                onBack = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }

    // Explicit Owner Approval Dialog whenever another device requests connection
    val pendingIncoming = incomingRequest
    if (pendingIncoming != null) {
      IncomingSessionApprovalDialog(
        session = pendingIncoming,
        language = lang,
        onAccept = { allowScreen, allowTouch ->
          viewModel.approveIncomingSession(pendingIncoming, allowScreen, allowTouch)
          if (allowScreen) onRequestScreenCapture()
          navController.navigate(SadiRoutes.REMOTE_CONTROL) { launchSingleTop = true }
        },
        onReject = { viewModel.rejectIncomingSession(pendingIncoming) },
      )
    }
  }
}
