package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SadiRepository
import com.example.data.SettingsDataStore
import com.example.firebase.FirebaseAuthHelper
import com.example.navigation.SadiMainShell
import com.example.permissions.PermissionStatusHelper
import com.example.permissions.SessionRuntimeBridge
import com.example.presentation.SadiViewModel
import com.example.presentation.screens.AuthScreen
import com.example.presentation.screens.SplashScreen
import com.example.ui.theme.SadiConnectTheme
import com.example.webrtc.LocalP2pTransport
import com.example.webrtc.VoiceAudioEngine
import com.example.webrtc.WebRtcSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val pendingDeepLinkState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    extractDeepLink(intent)

    setContent {
      val settingsStore = remember { SettingsDataStore(applicationContext) }
      val prefs by
        settingsStore.preferencesFlow.collectAsStateWithLifecycle(
          initialValue = com.example.data.AppPreferences()
        )
      val scope = rememberCoroutineScope()

      SadiConnectTheme(darkTheme = prefs.darkMode) {
        SadiAppRoot(
          settingsStore = settingsStore,
          pendingDeepLink = pendingDeepLinkState.value,
          onDeepLinkConsumed = { pendingDeepLinkState.value = null },
          onLanguageChange = { lang -> scope.launch { settingsStore.setLanguage(lang) } },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    extractDeepLink(intent)
  }

  private fun extractDeepLink(intent: Intent?) {
    val data = intent?.dataString
    if (!data.isNullOrBlank()) {
      pendingDeepLinkState.value = data
    }
  }
}

@Composable
fun SadiAppRoot(
  settingsStore: SettingsDataStore,
  pendingDeepLink: String?,
  onDeepLinkConsumed: () -> Unit,
  onLanguageChange: (com.example.domain.model.AppLanguage) -> Unit,
) {
  var showSplash by remember { mutableStateOf(true) }
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val prefs by
    settingsStore.preferencesFlow.collectAsStateWithLifecycle(
      initialValue = com.example.data.AppPreferences()
    )

  if (showSplash) {
    SplashScreen(language = prefs.language, onFinished = { showSplash = false })
    return
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      language = prefs.language,
      onLanguageChange = onLanguageChange,
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
    )
  } else {
    val uid = user.uid
    key(uid) {
      AuthenticatedSadiApp(
        user = user,
        settingsStore = settingsStore,
        pendingDeepLink = pendingDeepLink,
        onDeepLinkConsumed = onDeepLinkConsumed,
      )
    }
  }
}

@Composable
private fun AuthenticatedSadiApp(
  user: FirebaseUser,
  settingsStore: SettingsDataStore,
  pendingDeepLink: String?,
  onDeepLinkConsumed: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val sadiViewModel: SadiViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val transportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            SadiViewModel(
              currentUserId = user.uid,
              initialDisplayName = user.displayName ?: "Sadi User",
              initialEmail = user.email ?: "",
              initialPhotoUrl = user.photoUrl?.toString() ?: "",
              repository = SadiRepository(db),
              settingsStore = settingsStore,
              localP2p = LocalP2pTransport(app, transportScope),
              webRtc = WebRtcSessionManager(app),
              voiceEngine = VoiceAudioEngine(app, transportScope),
              permissionReader = {
                PermissionStatusHelper.readSnapshot(
                  app,
                  SessionRuntimeBridge.screenCaptureActive.value,
                )
              },
              appContext = app,
            )
          }
        },
    )

  // Refresh permissions whenever user returns from Android System Settings
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        sadiViewModel.refreshPermissions()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val screenCaptureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data
      if (result.resultCode == Activity.RESULT_OK && data != null) {
        sadiViewModel.startLocalScreenShareService(result.resultCode, data)
      } else {
        sadiViewModel.emitMessage("Screen capture permission was cancelled")
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      sadiViewModel.refreshPermissions()
      if (!granted) sadiViewModel.emitMessage("Camera permission denied")
    }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      sadiViewModel.refreshPermissions()
      if (!granted) sadiViewModel.emitMessage("Microphone permission denied")
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      sadiViewModel.refreshPermissions()
    }

  LaunchedEffect(Unit) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !PermissionStatusHelper.hasNotificationPermission(context)
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  SadiMainShell(
    currentUserId = user.uid,
    viewModel = sadiViewModel,
    pendingDeepLinkInput = pendingDeepLink,
    onDeepLinkConsumed = onDeepLinkConsumed,
    onRequestScreenCapture = {
      val mgr =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      screenCaptureLauncher.launch(mgr.createScreenCaptureIntent())
    },
    onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
    onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
    onRequestNotificationPermission = {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    },
    onSignOut = {
      FirebaseAuthHelper.signOut(
        credentialManager = credentialManager,
        scope = scope,
        preSignOutCleanup = { sadiViewModel.prepareForSignOut() },
      )
    },
  )
}
