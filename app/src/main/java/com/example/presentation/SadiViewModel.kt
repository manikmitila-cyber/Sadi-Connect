package com.example.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.SadiAccessibilityService
import com.example.data.AppPreferences
import com.example.data.SadiRepository
import com.example.data.SettingsDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConnectionHistoryItem
import com.example.domain.model.ConnectionTransportState
import com.example.domain.model.RemoteCommand
import com.example.domain.model.RemoteSession
import com.example.domain.model.UiState
import com.example.domain.model.UserDevice
import com.example.domain.model.UserProfile
import com.example.domain.model.VideoQuality
import com.example.permissions.PermissionSnapshot
import com.example.permissions.PermissionStatusHelper
import com.example.permissions.SessionRuntimeBridge
import com.example.security.SessionSecurity
import com.example.services.ScreenCaptureService
import com.example.utils.DeviceCodeUtils
import com.example.webrtc.DiscoveredLocalPeer
import com.example.webrtc.LocalP2pTransport
import com.example.webrtc.VoiceAudioEngine
import com.example.webrtc.WebRtcSessionManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class SadiViewModel(
  private val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String,
  private val initialPhotoUrl: String,
  private val repository: SadiRepository,
  private val settingsStore: SettingsDataStore,
  private val localP2p: LocalP2pTransport,
  private val webRtc: WebRtcSessionManager,
  private val voiceEngine: VoiceAudioEngine,
  private val permissionReader: () -> PermissionSnapshot,
  private val appContext: Context,
) : ViewModel() {

  companion object {
    private const val TAG = "SadiVM"
    private const val STOP_TIMEOUT_MS = 5000L
  }

  val preferences: StateFlow<AppPreferences> =
    settingsStore.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = AppPreferences(),
    )

  val userProfile: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { err -> handleStreamError(err, "profile") { emit(UiState.Error(it)) } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val userDevices: StateFlow<UiState<List<UserDevice>>> =
    repository.observeUserDevices(currentUserId)
      .map<List<UserDevice>, UiState<List<UserDevice>>> { UiState.Success(it) }
      .catch { err -> handleStreamError(err, "devices") { emit(UiState.Error(it)) } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val userSessions: StateFlow<List<RemoteSession>> =
    repository.observeUserSessions(currentUserId)
      .catch { err -> handleStreamError(err, "sessions") { emit(emptyList()) } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val connectionHistory: StateFlow<UiState<List<ConnectionHistoryItem>>> =
    repository.observeConnectionHistory(currentUserId)
      .map<List<ConnectionHistoryItem>, UiState<List<ConnectionHistoryItem>>> { UiState.Success(it) }
      .catch { err -> handleStreamError(err, "history") { emit(UiState.Error(it)) } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val _selectedSessionId = MutableStateFlow<String?>(null)
  val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

  private val _activeSession = MutableStateFlow<RemoteSession?>(null)
  val activeSession: StateFlow<RemoteSession?> = _activeSession.asStateFlow()

  private val _incomingRequest = MutableStateFlow<RemoteSession?>(null)
  val incomingRequest: StateFlow<RemoteSession?> = _incomingRequest.asStateFlow()

  private val _waitingSupportLink = MutableStateFlow<Pair<String, String>?>(null)
  val waitingSupportLink: StateFlow<Pair<String, String>?> = _waitingSupportLink.asStateFlow()

  private val _permissions = MutableStateFlow(permissionReader())
  val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

  val p2pHostActive: StateFlow<Boolean> = localP2p.hostActive
  val p2pTransportState: StateFlow<ConnectionTransportState> = localP2p.transportState
  val p2pLatencyMs: StateFlow<Long?> = localP2p.latencyMs
  val discoveredPeers: StateFlow<List<DiscoveredLocalPeer>> = localP2p.discoveredPeers
  val webRtcIceState: StateFlow<ConnectionTransportState> = webRtc.iceState

  val screenCaptureActive: StateFlow<Boolean> = SessionRuntimeBridge.screenCaptureActive
  val remoteScreenFrame: StateFlow<ByteArray?> = SessionRuntimeBridge.remoteScreenFrame
  val localScreenPreviewFrame: StateFlow<ByteArray?> = SessionRuntimeBridge.localScreenPreviewFrame
  val remoteCameraFrame: StateFlow<ByteArray?> = SessionRuntimeBridge.remoteCameraFrame
  val audioAmplitude: StateFlow<Float> = SessionRuntimeBridge.audioAmplitude
  val voiceMuted: StateFlow<Boolean> = voiceEngine.muted
  val voiceSpeakerOn: StateFlow<Boolean> = voiceEngine.speakerOn
  val voiceRunning: StateFlow<Boolean> = voiceEngine.running

  private val _callDurationSeconds = MutableStateFlow(0L)
  val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

  private val _isConnecting = MutableStateFlow(false)
  val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  private val _cameraConsentPromptSession = MutableStateFlow<RemoteSession?>(null)
  val cameraConsentPromptSession: StateFlow<RemoteSession?> = _cameraConsentPromptSession.asStateFlow()

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val chatMessages: StateFlow<List<ChatMessage>> =
    _activeSession
      .flatMapLatest { session ->
        if (session == null || session.id.isBlank()) flowOf(emptyList())
        else repository.observeMessages(session.id, currentUserId)
          .catch { err -> handleStreamError(err, "messages") { emit(emptyList()) } }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  private var commandListenerJob: Job? = null
  private var iceListenerJob: Job? = null
  private var callTimerJob: Job? = null
  private var lastHandledOfferSdp: String = ""
  private var lastHandledAnswerSdp: String = ""
  private val processedCommandIds = mutableSetOf<String>()
  private val processedIceIds = mutableSetOf<String>()
  private var sessionStartMillis: Long = 0L

  init {
    bootstrapDeviceAndTransport()
    observeSessionsLifecycle()
  }

  private fun bootstrapDeviceAndTransport() {
    viewModelScope.launch {
      val (deviceId, deviceCode) = settingsStore.ensureDeviceIdentity()
      val prefs = preferences.value
      repository.ensureUserProfile(
        displayName = initialDisplayName,
        email = initialEmail,
        photoUrl = initialPhotoUrl,
        preferredLanguage = prefs.language.code,
      )
      if (prefs.localP2pEnabled) {
        localP2p.startHost(deviceCode)
        localP2p.startDiscovery()
      }
      delay(300)
      syncDeviceStatusToFirestore(deviceId, deviceCode)
    }

    // Wire runtime capture/transport callbacks
    SessionRuntimeBridge.onLocalScreenFrameCaptured = { jpeg ->
      localP2p.sendPacket(LocalP2pTransport.PACKET_SCREEN_FRAME, jpeg)
      if (jpeg.size <= 60_000) {
        webRtc.sendBinaryOverDataChannel(byteArrayOf(LocalP2pTransport.PACKET_SCREEN_FRAME) + jpeg)
      }
    }
    SessionRuntimeBridge.onStopScreenCaptureRequested = {
      val active = _activeSession.value
      if (active != null && active.hostUid == currentUserId) {
        viewModelScope.launch {
          repository.updateHostPermissions(active.id, screenShareAllowed = false)
        }
      }
    }

    localP2p.onScreenFrameReceived = { SessionRuntimeBridge.publishRemoteScreenFrame(it) }
    localP2p.onCameraFrameReceived = { SessionRuntimeBridge.publishRemoteCameraFrame(it) }
    localP2p.onAudioPcmReceived = { voiceEngine.playIncomingFrame(it) }
    localP2p.onCommandJsonReceived = { json -> executeIncomingJsonCommand(json) }

    voiceEngine.onAudioFrameCaptured = { pcm ->
      localP2p.sendPacket(LocalP2pTransport.PACKET_AUDIO_PCM, pcm)
      webRtc.sendBinaryOverDataChannel(byteArrayOf(LocalP2pTransport.PACKET_AUDIO_PCM) + pcm)
    }

    webRtc.onDataChannelBinaryMessage = { packet ->
      if (packet.size > 1) {
        val type = packet[0]
        val payload = packet.copyOfRange(1, packet.size)
        when (type) {
          LocalP2pTransport.PACKET_SCREEN_FRAME -> SessionRuntimeBridge.publishRemoteScreenFrame(payload)
          LocalP2pTransport.PACKET_CAMERA_FRAME -> SessionRuntimeBridge.publishRemoteCameraFrame(payload)
          LocalP2pTransport.PACKET_AUDIO_PCM -> voiceEngine.playIncomingFrame(payload)
          LocalP2pTransport.PACKET_COMMAND_JSON -> {
            try {
              executeIncomingJsonCommand(JSONObject(String(payload, Charsets.UTF_8)))
            } catch (_: Exception) {}
          }
        }
      }
    }
  }

  private fun observeSessionsLifecycle() {
    viewModelScope.launch {
      userSessions.collectLatest { sessions ->
        val incoming =
          sessions.firstOrNull {
            it.hostUid == currentUserId &&
              it.status == "pending" &&
              it.controllerUid.isNotBlank() &&
              !SessionSecurity.isSessionExpired(it.expiresAtMillis)
          }
        _incomingRequest.value = incoming

        val waitingLink =
          sessions.firstOrNull {
            it.hostUid == currentUserId &&
              it.status == "waiting_link" &&
              !SessionSecurity.isSessionExpired(it.expiresAtMillis)
          }
        _waitingSupportLink.value =
          waitingLink?.let { it.id to SessionSecurity.buildSupportLink(it.id, it.joinToken) }

        val preferredId = _selectedSessionId.value
        val active =
          sessions.firstOrNull { it.id == preferredId && it.status in listOf("pending", "approved") }
            ?: sessions.firstOrNull { it.status == "approved" }
            ?: sessions.firstOrNull { it.status == "pending" && it.controllerUid == currentUserId }

        val prev = _activeSession.value
        _activeSession.value = active

        if (active == null) {
          if (prev != null) {
            teardownSessionRuntime()
          }
        } else {
          val isHost = active.hostUid == currentUserId
          SessionRuntimeBridge.setRemoteTouchApproved(
            isHost && active.status == "approved" && active.remoteTouchAllowed
          )
          if (prev?.id != active.id || (prev.status != "approved" && active.status == "approved")) {
            if (active.status == "approved") {
              onSessionApprovedBecameActive(active)
            }
          }
          handleSignalingUpdates(active)
          handleVoiceCallState(active)
        }
      }
    }
  }

  private fun onSessionApprovedBecameActive(session: RemoteSession) {
    sessionStartMillis = System.currentTimeMillis()
    val isHost = session.hostUid == currentUserId
    val prefs = preferences.value

    if (!isHost && session.hostLocalIp.isNotBlank() && session.hostP2pPort > 0 && prefs.localP2pEnabled) {
      localP2p.connectToPeer(session.hostLocalIp, session.hostP2pPort.toInt())
    }

    webRtc.onLocalIceCandidate = { sdpMid, index, candidate ->
      viewModelScope.launch {
        repository.sendIceCandidate(session.id, session.memberUids, sdpMid, index, candidate)
      }
    }

    // Controller acts as WebRTC Offerer
    if (!isHost && session.sdpOffer.isBlank()) {
      webRtc.startPeerConnection(
        stunUrl = prefs.stunServerUrl,
        turnUrl = prefs.turnServerUrl,
        turnUser = prefs.turnUsername,
        turnCred = prefs.turnCredential,
        isOfferer = true,
      ) { _, sdp ->
        viewModelScope.launch { repository.updateSessionControls(session.id, sdpOffer = sdp) }
      }
    } else if (isHost && session.sdpAnswer.isBlank()) {
      webRtc.startPeerConnection(
        stunUrl = prefs.stunServerUrl,
        turnUrl = prefs.turnServerUrl,
        turnUser = prefs.turnUsername,
        turnCred = prefs.turnCredential,
        isOfferer = false,
      ) { _, _ -> }
    }

    startCommandObserver(session)
    startIceObserver(session)
  }

  private fun handleSignalingUpdates(session: RemoteSession) {
    val isHost = session.hostUid == currentUserId
    if (isHost && session.sdpOffer.isNotBlank() && session.sdpOffer != lastHandledOfferSdp) {
      lastHandledOfferSdp = session.sdpOffer
      webRtc.setRemoteOfferAndCreateAnswer(session.sdpOffer) { answerSdp ->
        viewModelScope.launch { repository.updateSessionControls(session.id, sdpAnswer = answerSdp) }
      }
    } else if (!isHost && session.sdpAnswer.isNotBlank() && session.sdpAnswer != lastHandledAnswerSdp) {
      lastHandledAnswerSdp = session.sdpAnswer
      webRtc.setRemoteAnswer(session.sdpAnswer)
    }
  }

  private fun handleVoiceCallState(session: RemoteSession) {
    when (session.voiceCallState) {
      "active" -> {
        if (!voiceEngine.running.value) {
          voiceEngine.start()
          startCallTimer()
        }
      }
      "idle", "rejected", "ended" -> {
        if (voiceEngine.running.value) {
          voiceEngine.stop()
        }
        stopCallTimer()
      }
    }
  }

  private fun startCallTimer() {
    callTimerJob?.cancel()
    _callDurationSeconds.value = 0L
    callTimerJob =
      viewModelScope.launch {
        while (isActive) {
          delay(1000L)
          _callDurationSeconds.value += 1L
        }
      }
  }

  private fun stopCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = null
    _callDurationSeconds.value = 0L
  }

  private fun startCommandObserver(session: RemoteSession) {
    commandListenerJob?.cancel()
    if (session.hostUid != currentUserId) return
    commandListenerJob =
      viewModelScope.launch {
        repository.observeCommands(session.id, currentUserId)
          .catch { err -> handleStreamError(err, "commands") {} }
          .collect { cmds ->
            cmds.forEach { cmd ->
              if (processedCommandIds.add(cmd.id)) {
                executeRemoteCommand(cmd)
                repository.markCommandExecuted(session.id, cmd.id)
              }
            }
          }
      }
  }

  private fun startIceObserver(session: RemoteSession) {
    iceListenerJob?.cancel()
    iceListenerJob =
      viewModelScope.launch {
        repository.observeIceCandidates(session.id, currentUserId)
          .catch { err -> handleStreamError(err, "ice") {} }
          .collect { candidates ->
            candidates.forEach { ice ->
              if (ice.senderUid != currentUserId && processedIceIds.add(ice.id)) {
                webRtc.addRemoteIceCandidate(ice.sdpMid, ice.sdpMLineIndex.toInt(), ice.candidate)
              }
            }
          }
      }
  }

  private fun executeRemoteCommand(cmd: RemoteCommand) {
    val active = _activeSession.value ?: return
    if (active.hostUid != currentUserId || active.status != "approved") return
    val service = SadiAccessibilityService.instance
    when (cmd.type) {
      "tap" -> if (active.remoteTouchAllowed) service?.executeNormalizedTap(cmd.x, cmd.y)
      "swipe", "scroll" ->
        if (active.remoteTouchAllowed) service?.executeNormalizedSwipe(cmd.x, cmd.y, cmd.endX, cmd.endY)
      "back", "home", "recents", "notifications" ->
        if (active.remoteTouchAllowed) service?.executeGlobalNav(cmd.type)
      "brightness" -> applyLocalBrightness(cmd.value.toInt())
    }
  }

  private fun executeIncomingJsonCommand(json: JSONObject) {
    val type = json.optString("type", "")
    val active = _activeSession.value ?: return
    if (active.hostUid != currentUserId || active.status != "approved") return
    val service = SadiAccessibilityService.instance
    when (type) {
      "tap" -> if (active.remoteTouchAllowed) service?.executeNormalizedTap(json.optDouble("x"), json.optDouble("y"))
      "swipe", "scroll" ->
        if (active.remoteTouchAllowed) {
          service?.executeNormalizedSwipe(
            json.optDouble("x"),
            json.optDouble("y"),
            json.optDouble("endX"),
            json.optDouble("endY"),
          )
        }
      "back", "home", "recents", "notifications" ->
        if (active.remoteTouchAllowed) service?.executeGlobalNav(type)
      "brightness" -> applyLocalBrightness(json.optInt("value", 70))
    }
  }

  private fun applyLocalBrightness(percent: Int) {
    if (Settings.System.canWrite(appContext)) {
      try {
        val brightness255 = (percent.coerceIn(5, 100) * 255) / 100
        Settings.System.putInt(
          appContext.contentResolver,
          Settings.System.SCREEN_BRIGHTNESS_MODE,
          Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        Settings.System.putInt(
          appContext.contentResolver,
          Settings.System.SCREEN_BRIGHTNESS,
          brightness255,
        )
      } catch (_: Exception) {}
    }
  }

  fun refreshPermissions() {
    val snap = permissionReader()
    _permissions.value = snap
    viewModelScope.launch {
      val (devId, devCode) = settingsStore.ensureDeviceIdentity()
      syncDeviceStatusToFirestore(devId, devCode)
    }
  }

  private suspend fun syncDeviceStatusToFirestore(deviceId: String, deviceCode: String) {
    val snap = permissionReader()
    _permissions.value = snap
    val prefs = preferences.value
    val name =
      (userProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
        ?: initialDisplayName
    repository.registerOrUpdateDevice(
      deviceId = deviceId,
      deviceCode = deviceCode,
      deviceName = prefs.localDeviceName,
      ownerDisplayName = name,
      online = snap.networkConnected,
      localIp = snap.localIpAddress,
      p2pPort = localP2p.listenPort.value,
      accessibilityReady = snap.accessibilityGranted,
      screenCaptureReady = snap.screenCaptureActive,
      cameraReady = snap.cameraGranted,
    )
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch {
      settingsStore.setLanguage(language)
      val name =
        (userProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
          ?: initialDisplayName
      repository.ensureUserProfile(name, initialEmail, initialPhotoUrl, language.code)
    }
  }

  fun setDarkMode(enabled: Boolean) {
    viewModelScope.launch { settingsStore.setDarkMode(enabled) }
  }

  fun renameThisDevice(newName: String) {
    viewModelScope.launch {
      settingsStore.setDeviceName(newName)
      val (devId, devCode) = settingsStore.ensureDeviceIdentity()
      syncDeviceStatusToFirestore(devId, devCode)
    }
  }

  fun toggleFavoriteDevice(device: UserDevice) {
    viewModelScope.launch {
      repository.toggleFavoriteDevice(device.id, !device.favorite)
    }
  }

  fun setVideoQuality(quality: VideoQuality, fps: Int) {
    viewModelScope.launch {
      settingsStore.setVideoQuality(quality)
      settingsStore.setTargetFps(fps)
      if (SessionRuntimeBridge.screenCaptureActive.value) {
        val intent =
          Intent(appContext, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_UPDATE_QUALITY
            putExtra(ScreenCaptureService.EXTRA_QUALITY, quality.wire)
            putExtra(ScreenCaptureService.EXTRA_FPS, fps)
          }
        appContext.startService(intent)
      }
      _activeSession.value?.let { session ->
        repository.updateSessionControls(session.id, videoQuality = quality.wire, targetFps = fps)
      }
    }
  }

  fun setLocalP2pEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsStore.setLocalP2pEnabled(enabled)
      val (devId, devCode) = settingsStore.ensureDeviceIdentity()
      if (enabled) {
        localP2p.startHost(devCode)
        localP2p.startDiscovery()
      } else {
        localP2p.shutdown()
      }
      syncDeviceStatusToFirestore(devId, devCode)
    }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
  }

  fun updateIceConfig(stun: String, turn: String, user: String, cred: String) {
    viewModelScope.launch {
      settingsStore.setIceConfig(stun, turn, user, cred)
      emitMessage("ICE / STUN / TURN settings saved")
    }
  }

  fun generateSupportLink() {
    viewModelScope.launch {
      val (devId, devCode) = settingsStore.ensureDeviceIdentity()
      val prefs = preferences.value
      val snap = permissionReader()
      val ownerName =
        (userProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
          ?: initialDisplayName
      val res =
        repository.createSupportLinkSession(
          hostDeviceId = devId,
          hostDeviceName = prefs.localDeviceName,
          hostName = ownerName,
          hostDeviceCode = devCode,
          hostLocalIp = snap.localIpAddress,
          hostP2pPort = localP2p.listenPort.value,
        )
      res.onSuccess { (sessionId, token) ->
        _waitingSupportLink.value = sessionId to SessionSecurity.buildSupportLink(sessionId, token)
      }.onFailure { err ->
        emitMessage(err.localizedMessage ?: "Failed to create support link")
      }
    }
  }

  fun cancelWaitingSupportLink() {
    val waiting = _waitingSupportLink.value ?: return
    viewModelScope.launch {
      repository.updateSessionStatus(waiting.first, "ended")
      _waitingSupportLink.value = null
    }
  }

  fun connectByInput(rawInput: String, onConnectedNav: () -> Unit = {}) {
    val parsed = DeviceCodeUtils.parseConnectInput(rawInput)
    if (parsed == null) {
      emitMessage("Invalid 9-digit code or support link")
      return
    }
    viewModelScope.launch {
      _isConnecting.value = true
      try {
        val prefs = preferences.value
        val myName =
          (userProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
            ?: initialDisplayName
        if (parsed.deviceCode != null) {
          if (parsed.deviceCode == prefs.localDeviceCode) {
            emitMessage("Cannot connect a device to itself")
            return@launch
          }
          val lookup = repository.lookupDeviceByCode(parsed.deviceCode)
          lookup.onSuccess { entry ->
            if (entry == null) {
              emitMessage("Device code not found")
            } else {
              val res =
                repository.requestConnectionByDevice(
                  target = entry,
                  controllerName = myName,
                  controllerDeviceName = prefs.localDeviceName,
                )
              res.onSuccess { sessionId ->
                _selectedSessionId.value = sessionId
                repository.addHistoryEntry(
                  peerUserId = entry.ownerUid,
                  peerDeviceId = entry.deviceId,
                  peerDeviceName = entry.deviceName,
                  peerDeviceCode = entry.deviceCode,
                  direction = "outgoing",
                  status = "connected",
                )
                onConnectedNav()
              }.onFailure { e -> emitMessage(e.localizedMessage ?: "Connection request failed") }
            }
          }.onFailure { e -> emitMessage(e.localizedMessage ?: "Lookup failed") }
        } else if (parsed.sessionId != null) {
          val res =
            repository.claimSupportLinkSession(
              sessionId = parsed.sessionId,
              expectedToken = parsed.token,
              controllerName = myName,
              controllerDeviceName = prefs.localDeviceName,
            )
          res.onSuccess {
            _selectedSessionId.value = parsed.sessionId
            onConnectedNav()
          }.onFailure { e -> emitMessage(e.localizedMessage ?: "Support link failed") }
        }
      } finally {
        _isConnecting.value = false
      }
    }
  }

  fun approveIncomingSession(session: RemoteSession, allowScreen: Boolean, allowTouch: Boolean) {
    viewModelScope.launch {
      val snap = permissionReader()
      val res =
        repository.approveSession(
          sessionId = session.id,
          allowScreenShare = allowScreen,
          allowRemoteTouch = allowTouch,
          hostLocalIp = snap.localIpAddress,
          hostP2pPort = localP2p.listenPort.value,
        )
      res.onSuccess {
        _selectedSessionId.value = session.id
        _incomingRequest.value = null
        repository.addHistoryEntry(
          peerUserId = session.controllerUid,
          peerDeviceId = session.controllerDeviceName,
          peerDeviceName = session.controllerDeviceName.ifBlank { session.controllerName },
          peerDeviceCode = "",
          direction = "incoming",
          status = "connected",
        )
      }.onFailure { err -> emitMessage(err.localizedMessage ?: "Approval failed") }
    }
  }

  fun rejectIncomingSession(session: RemoteSession) {
    viewModelScope.launch {
      repository.updateSessionStatus(session.id, "rejected")
      repository.addHistoryEntry(
        peerUserId = session.controllerUid,
        peerDeviceId = session.controllerDeviceName,
        peerDeviceName = session.controllerDeviceName.ifBlank { session.controllerName },
        peerDeviceCode = "",
        direction = "incoming",
        status = "rejected",
      )
      _incomingRequest.value = null
    }
  }

  fun toggleHostTouchControl(allowed: Boolean) {
    val active = _activeSession.value ?: return
    if (active.hostUid != currentUserId) return
    viewModelScope.launch {
      repository.updateHostPermissions(active.id, remoteTouchAllowed = allowed)
    }
  }

  fun startLocalScreenShareService(resultCode: Int, data: Intent) {
    val prefs = preferences.value
    val serviceIntent =
      Intent(appContext, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_START
        putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
        putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        putExtra(ScreenCaptureService.EXTRA_QUALITY, prefs.videoQuality.wire)
        putExtra(ScreenCaptureService.EXTRA_FPS, prefs.targetFps)
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      appContext.startForegroundService(serviceIntent)
    } else {
      appContext.startService(serviceIntent)
    }
    _activeSession.value?.let { session ->
      if (session.hostUid == currentUserId) {
        viewModelScope.launch {
          repository.updateHostPermissions(session.id, screenShareAllowed = true)
        }
      }
    }
  }

  fun stopLocalScreenShareService() {
    val intent =
      Intent(appContext, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_STOP
      }
    appContext.startService(intent)
  }

  fun sendRemoteGestureOrNav(
    type: String,
    x: Double = 0.0,
    y: Double = 0.0,
    endX: Double = 0.0,
    endY: Double = 0.0,
    value: Double = 0.0,
  ) {
    val session = _activeSession.value ?: return
    if (session.status != "approved") return

    // Fast path over Local P2P socket
    val json =
      JSONObject().apply {
        put("type", type)
        put("x", x)
        put("y", y)
        put("endX", endX)
        put("endY", endY)
        put("value", value)
      }
    val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
    localP2p.sendPacket(LocalP2pTransport.PACKET_COMMAND_JSON, jsonBytes)
    webRtc.sendBinaryOverDataChannel(byteArrayOf(LocalP2pTransport.PACKET_COMMAND_JSON) + jsonBytes)

    // Reliable fallback over Firestore when remoteTouchAllowed is true
    if (session.remoteTouchAllowed || type == "brightness") {
      viewModelScope.launch {
        repository.sendRemoteCommand(
          sessionId = session.id,
          memberUids = session.memberUids,
          type = type,
          x = x,
          y = y,
          endX = endX,
          endY = endY,
          value = value,
        )
      }
    }
  }

  fun setBrightnessPercent(percent: Int) {
    val session = _activeSession.value
    if (session != null) {
      viewModelScope.launch {
        repository.updateSessionControls(session.id, brightnessPercent = percent)
      }
      sendRemoteGestureOrNav(type = "brightness", value = percent.toDouble())
    } else {
      applyLocalBrightness(percent)
    }
  }

  // --- VOICE CALL ---

  fun startOutgoingVoiceCall() {
    val session = _activeSession.value ?: return
    viewModelScope.launch {
      repository.updateSessionControls(
        session.id,
        voiceCallState = "ringing",
        voiceCallerUid = currentUserId,
      )
    }
  }

  fun acceptVoiceCall() {
    val session = _activeSession.value ?: return
    viewModelScope.launch {
      repository.updateSessionControls(session.id, voiceCallState = "active")
    }
  }

  fun rejectOrEndVoiceCall() {
    val session = _activeSession.value ?: return
    voiceEngine.stop()
    stopCallTimer()
    viewModelScope.launch {
      repository.updateSessionControls(session.id, voiceCallState = "ended")
    }
  }

  fun toggleVoiceMute() {
    voiceEngine.setMuted(!voiceEngine.muted.value)
  }

  fun toggleVoiceSpeaker() {
    voiceEngine.setSpeakerEnabled(!voiceEngine.speakerOn.value)
  }

  // --- REMOTE CAMERA ---

  fun toggleRemoteCameraPermission(allow: Boolean) {
    val session = _activeSession.value ?: return
    if (session.hostUid == currentUserId) {
      viewModelScope.launch {
        repository.updateHostPermissions(session.id, remoteCameraAllowed = allow)
      }
    }
    _cameraConsentPromptSession.value = null
  }

  fun switchCameraFacing() {
    val session = _activeSession.value ?: return
    val next = if (session.cameraFacing == "back") "front" else "back"
    viewModelScope.launch {
      repository.updateSessionControls(session.id, cameraFacing = next)
    }
  }

  fun publishCameraFrameFromHost(jpeg: ByteArray) {
    localP2p.sendPacket(LocalP2pTransport.PACKET_CAMERA_FRAME, jpeg)
    if (jpeg.size <= 60_000) {
      webRtc.sendBinaryOverDataChannel(byteArrayOf(LocalP2pTransport.PACKET_CAMERA_FRAME) + jpeg)
    }
  }

  // --- CHAT ---

  fun sendChatMessage(text: String) {
    val session = _activeSession.value ?: return
    val clean = text.trim()
    if (clean.isBlank()) return
    val myName =
      (userProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
        ?: initialDisplayName
    viewModelScope.launch {
      repository.sendMessage(session.id, session.memberUids, myName, clean)
    }
  }

  fun clearChatHistory() {
    val session = _activeSession.value ?: return
    viewModelScope.launch {
      repository.clearSessionMessages(session.id)
    }
  }

  // --- HISTORY & DISCONNECT ---

  fun clearConnectionHistory() {
    viewModelScope.launch {
      repository.clearConnectionHistory()
    }
  }

  fun disconnectActiveSession() {
    val session = _activeSession.value ?: return
    val duration =
      if (sessionStartMillis > 0L) ((System.currentTimeMillis() - sessionStartMillis) / 1000L).coerceAtLeast(0L)
      else 0L
    viewModelScope.launch {
      repository.updateSessionStatus(session.id, "ended")
      val peerUid = if (session.hostUid == currentUserId) session.controllerUid else session.hostUid
      val peerName =
        if (session.hostUid == currentUserId) session.controllerDeviceName.ifBlank { session.controllerName }
        else session.hostDeviceName
      if (peerUid.isNotBlank()) {
        repository.addHistoryEntry(
          peerUserId = peerUid,
          peerDeviceId = session.hostDeviceId,
          peerDeviceName = peerName.ifBlank { "Device" },
          peerDeviceCode = session.hostDeviceCode,
          direction = if (session.hostUid == currentUserId) "incoming" else "outgoing",
          status = "disconnected",
          durationSeconds = duration,
        )
      }
      teardownSessionRuntime()
    }
  }

  fun disconnectAllSessions() {
    viewModelScope.launch {
      userSessions.value
        .filter { it.status in listOf("waiting_link", "pending", "approved") }
        .forEach { repository.updateSessionStatus(it.id, "ended") }
      teardownSessionRuntime()
    }
  }

  suspend fun prepareForSignOut() {
    teardownSessionRuntime()
    localP2p.shutdown()
    webRtc.dispose()
    val (devId, devCode) = settingsStore.ensureDeviceIdentity()
    repository.setDeviceOffline(devId, devCode)
  }

  private fun teardownSessionRuntime() {
    commandListenerJob?.cancel()
    commandListenerJob = null
    iceListenerJob?.cancel()
    iceListenerJob = null
    stopCallTimer()
    voiceEngine.stop()
    stopLocalScreenShareService()
    SessionRuntimeBridge.setRemoteTouchApproved(false)
    SessionRuntimeBridge.clearRemoteStreams()
    localP2p.closeActiveClient()
    webRtc.close()
    processedCommandIds.clear()
    processedIceIds.clear()
    lastHandledOfferSdp = ""
    lastHandledAnswerSdp = ""
    sessionStartMillis = 0L
  }

  fun emitMessage(msg: String) {
    _snackbarEvents.tryEmit(msg)
  }

  private inline fun handleStreamError(
    error: Throwable,
    label: String,
    onUiError: (String) -> Unit,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val root = firestoreError ?: error
    Log.w(TAG, "Error observing $label", root)
    onUiError(root.localizedMessage ?: "Failed to load $label")
  }

  override fun onCleared() {
    teardownSessionRuntime()
    localP2p.shutdown()
    webRtc.dispose()
    super.onCleared()
  }
}
