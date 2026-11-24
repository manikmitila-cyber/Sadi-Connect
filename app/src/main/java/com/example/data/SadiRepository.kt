package com.example.data

import android.content.Context
import android.os.Build
import com.example.R
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConnectionHistoryItem
import com.example.domain.model.DeviceDirectoryEntry
import com.example.domain.model.IceCandidateSignal
import com.example.domain.model.RemoteCommand
import com.example.domain.model.RemoteSession
import com.example.domain.model.UserDevice
import com.example.domain.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.security.SessionSecurity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SadiRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- USER PROFILE ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user profile listen." }
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err -> handleFlowError(err, OperationType.GET, docRef.path, uid) }
    )
  }

  suspend fun ensureUserProfile(
    displayName: String,
    email: String,
    photoUrl: String,
    preferredLanguage: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      val safeName = displayName.trim().ifBlank { "Sadi User" }.take(60)
      val safeEmail = email.trim().take(120)
      val safePhoto = photoUrl.trim().take(500)
      val lang = if (preferredLanguage == "en") "en" else "bn"
      if (!snap.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "email" to safeEmail.ifBlank { null },
            "photoUrl" to safePhoto.ifBlank { null },
            "preferredLanguage" to lang,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      } else {
        val updates =
          mapOf(
            "displayName" to safeName,
            "email" to safeEmail.ifBlank { null },
            "photoUrl" to safePhoto.ifBlank { null },
            "preferredLanguage" to lang,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // --- USER DEVICES & 9-DIGIT DIRECTORY ---

  fun observeUserDevices(userId: String): Flow<List<UserDevice>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user device list." }
    val colRef = db.collection("users").document(uid).collection("devices")
    emitAll(
      colRef.snapshots()
        .map { snap -> snap.toObjects(UserDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err -> handleFlowError(err, OperationType.LIST, colRef.path, uid) }
    )
  }

  suspend fun registerOrUpdateDevice(
    deviceId: String,
    deviceCode: String,
    deviceName: String,
    ownerDisplayName: String,
    online: Boolean,
    localIp: String,
    p2pPort: Int,
    accessibilityReady: Boolean,
    screenCaptureReady: Boolean,
    cameraReady: Boolean,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val devPath = "users/$uid/devices/$deviceId"
    return try {
      require(deviceId.isNotBlank())
      require(deviceCode.matches(Regex("^[0-9]{9}$")))
      val safeDeviceName = deviceName.trim().ifBlank { "Android Device" }.take(60)
      val safeOwnerName = ownerDisplayName.trim().ifBlank { "Sadi User" }.take(60)
      val safeModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80).ifBlank { "Android" }
      val safeIp = localIp.trim().take(64)
      val safePort = p2pPort.coerceIn(0, 65535)

      val devRef = db.collection("users").document(uid).collection("devices").document(deviceId)
      val existingDev = devRef.get().await()
      if (!existingDev.exists()) {
        val devPayload =
          mapOf(
            "userId" to uid,
            "deviceName" to safeDeviceName,
            "deviceModel" to safeModel,
            "deviceCode" to deviceCode,
            "online" to online,
            "favorite" to false,
            "localIp" to safeIp.ifBlank { null },
            "p2pPort" to safePort,
            "accessibilityReady" to accessibilityReady,
            "screenCaptureReady" to screenCaptureReady,
            "cameraReady" to cameraReady,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        devRef.set(devPayload).await()
      } else {
        val devUpdates =
          mapOf(
            "deviceName" to safeDeviceName,
            "deviceModel" to safeModel,
            "online" to online,
            "localIp" to safeIp.ifBlank { null },
            "p2pPort" to safePort,
            "accessibilityReady" to accessibilityReady,
            "screenCaptureReady" to screenCaptureReady,
            "cameraReady" to cameraReady,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        devRef.update(devUpdates).await()
      }

      val dirRef = db.collection("device_directory").document(deviceCode)
      val existingDir = dirRef.get().await()
      if (!existingDir.exists()) {
        val dirPayload =
          mapOf(
            "ownerUid" to uid,
            "deviceId" to deviceId,
            "deviceCode" to deviceCode,
            "deviceName" to safeDeviceName,
            "ownerDisplayName" to safeOwnerName,
            "online" to online,
            "localIp" to safeIp.ifBlank { null },
            "p2pPort" to safePort,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        dirRef.set(dirPayload).await()
      } else if (existingDir.getString("ownerUid") == uid) {
        val dirUpdates =
          mapOf(
            "deviceName" to safeDeviceName,
            "ownerDisplayName" to safeOwnerName,
            "online" to online,
            "localIp" to safeIp.ifBlank { null },
            "p2pPort" to safePort,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        dirRef.update(dirUpdates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, devPath)
      Result.failure(e)
    }
  }

  suspend fun toggleFavoriteDevice(deviceId: String, favorite: Boolean): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "users/$uid/devices/$deviceId"
    return try {
      db.document(path)
        .update(mapOf("favorite" to favorite, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun setDeviceOffline(deviceId: String, deviceCode: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.success(Unit)
    val path = "users/$uid/devices/$deviceId"
    return try {
      db.document(path)
        .update(mapOf("online" to false, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      if (deviceCode.matches(Regex("^[0-9]{9}$"))) {
        val dirRef = db.collection("device_directory").document(deviceCode)
        val snap = dirRef.get().await()
        if (snap.exists() && snap.getString("ownerUid") == uid) {
          dirRef.update(mapOf("online" to false, "updatedAt" to FieldValue.serverTimestamp())).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun lookupDeviceByCode(code: String): Result<DeviceDirectoryEntry?> {
    val cleanCode = code.filter { it.isDigit() }
    val path = "device_directory/$cleanCode"
    return try {
      requireUserId()
      require(cleanCode.matches(Regex("^[0-9]{9}$"))) { "Invalid 9-digit device code" }
      val snap = db.document(path).get().await()
      Result.success(
        snap.toObject(DeviceDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  // --- SESSIONS & SUPPORT LINKS ---

  fun observeUserSessions(userId: String): Flow<List<RemoteSession>> = flow {
    val uid = requireUserId()
    check(uid == userId)
    val query = db.collection("sessions").whereArrayContains("memberUids", uid)
    emitAll(
      query.snapshots()
        .map { snap -> snap.toObjects(RemoteSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err -> handleFlowError(err, OperationType.LIST, "sessions", uid) }
    )
  }

  suspend fun createSupportLinkSession(
    hostDeviceId: String,
    hostDeviceName: String,
    hostName: String,
    hostDeviceCode: String,
    hostLocalIp: String,
    hostP2pPort: Int,
  ): Result<Pair<String, String>> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("sessions").document()
    return try {
      val token = SessionSecurity.generateJoinToken()
      val expiresAt = System.currentTimeMillis() + SessionSecurity.SUPPORT_LINK_TTL_MILLIS
      val payload =
        mapOf(
          "hostUid" to uid,
          "hostDeviceId" to hostDeviceId,
          "hostDeviceName" to hostDeviceName.take(60).ifBlank { "Android Device" },
          "hostName" to hostName.take(60).ifBlank { "Sadi User" },
          "hostDeviceCode" to hostDeviceCode,
          "hostLocalIp" to hostLocalIp.take(64).ifBlank { null },
          "hostP2pPort" to hostP2pPort.coerceIn(0, 65535),
          "controllerUid" to "",
          "controllerName" to "",
          "controllerDeviceName" to "",
          "memberUids" to listOf(uid),
          "joinToken" to token,
          "status" to "waiting_link",
          "screenShareAllowed" to false,
          "remoteTouchAllowed" to false,
          "remoteCameraAllowed" to false,
          "voiceCallState" to "idle",
          "voiceCallerUid" to "",
          "cameraFacing" to "back",
          "videoQuality" to "medium",
          "targetFps" to 20,
          "brightnessPercent" to 70,
          "expiresAtMillis" to expiresAt,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(payload).await()
      Result.success(docRef.id to token)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun requestConnectionByDevice(
    target: DeviceDirectoryEntry,
    controllerName: String,
    controllerDeviceName: String,
  ): Result<String> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("sessions").document()
    return try {
      val token = SessionSecurity.generateJoinToken()
      val expiresAt = System.currentTimeMillis() + SessionSecurity.SUPPORT_LINK_TTL_MILLIS
      val members = if (target.ownerUid == uid) listOf(uid) else listOf(target.ownerUid, uid)
      // Note: rules require memberUids size == 2 for controller request; if connecting to own second device,
      // wait: rules allow memberUids == [target.ownerUid, uid] when target.ownerUid != uid,
      // OR if same user's two devices, hostUid == uid so let's handle both cleanly!
      val normalizedMembers = listOf(target.ownerUid, uid)
      val payload =
        mapOf(
          "hostUid" to target.ownerUid,
          "hostDeviceId" to target.deviceId,
          "hostDeviceName" to target.deviceName.take(60).ifBlank { "Remote Device" },
          "hostName" to target.ownerDisplayName.take(60).ifBlank { "Sadi User" },
          "hostDeviceCode" to target.deviceCode,
          "hostLocalIp" to target.localIp.take(64).ifBlank { null },
          "hostP2pPort" to target.p2pPort.toInt().coerceIn(0, 65535),
          "controllerUid" to uid,
          "controllerName" to controllerName.take(60).ifBlank { "Controller" },
          "controllerDeviceName" to controllerDeviceName.take(60).ifBlank { "Android" },
          "memberUids" to normalizedMembers,
          "joinToken" to token,
          "status" to "pending",
          "screenShareAllowed" to false,
          "remoteTouchAllowed" to false,
          "remoteCameraAllowed" to false,
          "voiceCallState" to "idle",
          "voiceCallerUid" to "",
          "cameraFacing" to "back",
          "videoQuality" to "medium",
          "targetFps" to 20,
          "brightnessPercent" to 70,
          "expiresAtMillis" to expiresAt,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun claimSupportLinkSession(
    sessionId: String,
    expectedToken: String?,
    controllerName: String,
    controllerDeviceName: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "sessions/$sessionId"
    return try {
      val docRef = db.document(path)
      val snap = docRef.get().await()
      val session =
        snap.toObject(RemoteSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: return Result.failure(IllegalArgumentException("Support session not found"))
      if (SessionSecurity.isSessionExpired(session.expiresAtMillis)) {
        return Result.failure(IllegalStateException("Support link has expired"))
      }
      if (!expectedToken.isNullOrBlank() && session.joinToken != expectedToken) {
        return Result.failure(SecurityException("Invalid support link token"))
      }
      if (session.status != "waiting_link") {
        return Result.failure(IllegalStateException("Support link is no longer waiting"))
      }
      docRef.update(
        mapOf(
          "controllerUid" to uid,
          "controllerName" to controllerName.take(60).ifBlank { "Controller" },
          "controllerDeviceName" to controllerDeviceName.take(60).ifBlank { "Android" },
          "memberUids" to listOf(session.hostUid, uid),
          "status" to "pending",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun approveSession(
    sessionId: String,
    allowScreenShare: Boolean,
    allowRemoteTouch: Boolean,
    hostLocalIp: String,
    hostP2pPort: Int,
  ): Result<Unit> {
    val path = "sessions/$sessionId"
    return try {
      requireUserId()
      val updates =
        mapOf(
          "status" to "approved",
          "screenShareAllowed" to allowScreenShare,
          "remoteTouchAllowed" to allowRemoteTouch,
          "hostLocalIp" to hostLocalIp.take(64).ifBlank { null },
          "hostP2pPort" to hostP2pPort.coerceIn(0, 65535),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      db.document(path).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateHostPermissions(
    sessionId: String,
    screenShareAllowed: Boolean? = null,
    remoteTouchAllowed: Boolean? = null,
    remoteCameraAllowed: Boolean? = null,
  ): Result<Unit> {
    val path = "sessions/$sessionId"
    return try {
      requireUserId()
      val map = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
      if (screenShareAllowed != null) map["screenShareAllowed"] = screenShareAllowed
      if (remoteTouchAllowed != null) map["remoteTouchAllowed"] = remoteTouchAllowed
      if (remoteCameraAllowed != null) map["remoteCameraAllowed"] = remoteCameraAllowed
      db.document(path).update(map).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateSessionStatus(sessionId: String, newStatus: String): Result<Unit> {
    val path = "sessions/$sessionId"
    return try {
      requireUserId()
      db.document(path)
        .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateSessionControls(
    sessionId: String,
    videoQuality: String? = null,
    targetFps: Int? = null,
    brightnessPercent: Int? = null,
    cameraFacing: String? = null,
    voiceCallState: String? = null,
    voiceCallerUid: String? = null,
    sdpOffer: String? = null,
    sdpAnswer: String? = null,
  ): Result<Unit> {
    val path = "sessions/$sessionId"
    return try {
      requireUserId()
      val map = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
      if (videoQuality != null) map["videoQuality"] = videoQuality
      if (targetFps != null) map["targetFps"] = targetFps.coerceIn(5, 60)
      if (brightnessPercent != null) map["brightnessPercent"] = brightnessPercent.coerceIn(0, 100)
      if (cameraFacing != null) map["cameraFacing"] = cameraFacing
      if (voiceCallState != null) map["voiceCallState"] = voiceCallState
      if (voiceCallerUid != null) map["voiceCallerUid"] = voiceCallerUid
      if (sdpOffer != null) map["sdpOffer"] = sdpOffer.take(32768)
      if (sdpAnswer != null) map["sdpAnswer"] = sdpAnswer.take(32768)
      db.document(path).update(map).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- WEBRTC ICE CANDIDATES ---

  fun observeIceCandidates(sessionId: String, userId: String): Flow<List<IceCandidateSignal>> = flow {
    val uid = requireUserId()
    check(uid == userId)
    val query =
      db.collection("sessions")
        .document(sessionId)
        .collection("ice_candidates")
        .whereArrayContains("memberUids", uid)
    emitAll(
      query.snapshots()
        .map { snap -> snap.toObjects(IceCandidateSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err -> handleFlowError(err, OperationType.LIST, "sessions/$sessionId/ice_candidates", uid) }
    )
  }

  suspend fun sendIceCandidate(
    sessionId: String,
    memberUids: List<String>,
    sdpMid: String,
    sdpMLineIndex: Int,
    candidate: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("sessions").document(sessionId).collection("ice_candidates").document()
    return try {
      val payload =
        mapOf(
          "memberUids" to memberUids.distinct().take(2),
          "senderUid" to uid,
          "sdpMid" to sdpMid.take(64),
          "sdpMLineIndex" to sdpMLineIndex.coerceIn(0, 32),
          "candidate" to candidate.take(2048),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- REAL-TIME CHAT ---

  fun observeMessages(sessionId: String, userId: String): Flow<List<ChatMessage>> = flow {
    val uid = requireUserId()
    check(uid == userId)
    val query =
      db.collection("sessions")
        .document(sessionId)
        .collection("messages")
        .whereArrayContains("memberUids", uid)
    emitAll(
      query.snapshots()
        .map { snap ->
          snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { err -> handleFlowError(err, OperationType.LIST, "sessions/$sessionId/messages", uid) }
    )
  }

  suspend fun sendMessage(
    sessionId: String,
    memberUids: List<String>,
    senderName: String,
    text: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("sessions").document(sessionId).collection("messages").document()
    return try {
      val cleanText = text.trim().take(1000)
      require(cleanText.isNotEmpty())
      val payload =
        mapOf(
          "memberUids" to memberUids.distinct().take(2),
          "senderUid" to uid,
          "senderName" to senderName.trim().ifBlank { "User" }.take(60),
          "text" to cleanText,
          "delivered" to true,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun clearSessionMessages(sessionId: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "sessions/$sessionId/messages"
    return try {
      val snap =
        db.collection("sessions")
          .document(sessionId)
          .collection("messages")
          .whereArrayContains("memberUids", uid)
          .get()
          .await()
      val batch = db.batch()
      snap.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- REMOTE COMMANDS ---

  fun observeCommands(sessionId: String, userId: String): Flow<List<RemoteCommand>> = flow {
    val uid = requireUserId()
    check(uid == userId)
    val query =
      db.collection("sessions")
        .document(sessionId)
        .collection("commands")
        .whereArrayContains("memberUids", uid)
    emitAll(
      query.snapshots()
        .map { snap ->
          snap.toObjects(RemoteCommand::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { !it.executed && it.senderUid != uid }
        }
        .catch { err -> handleFlowError(err, OperationType.LIST, "sessions/$sessionId/commands", uid) }
    )
  }

  suspend fun sendRemoteCommand(
    sessionId: String,
    memberUids: List<String>,
    type: String,
    x: Double = 0.0,
    y: Double = 0.0,
    endX: Double = 0.0,
    endY: Double = 0.0,
    value: Double = 0.0,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("sessions").document(sessionId).collection("commands").document()
    return try {
      val payload =
        mapOf(
          "memberUids" to memberUids.distinct().take(2),
          "senderUid" to uid,
          "type" to type,
          "x" to x.coerceIn(0.0, 1.0),
          "y" to y.coerceIn(0.0, 1.0),
          "endX" to endX.coerceIn(0.0, 1.0),
          "endY" to endY.coerceIn(0.0, 1.0),
          "value" to value.coerceIn(0.0, 100.0),
          "executed" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markCommandExecuted(sessionId: String, commandId: String): Result<Unit> {
    val path = "sessions/$sessionId/commands/$commandId"
    return try {
      requireUserId()
      db.document(path).update("executed", true).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- CONNECTION HISTORY ---

  fun observeConnectionHistory(userId: String): Flow<List<ConnectionHistoryItem>> = flow {
    val uid = requireUserId()
    check(uid == userId)
    val colRef = db.collection("users").document(uid).collection("history")
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.DESCENDING).snapshots()
        .map { snap -> snap.toObjects(ConnectionHistoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err -> handleFlowError(err, OperationType.LIST, colRef.path, uid) }
    )
  }

  suspend fun addHistoryEntry(
    peerUserId: String,
    peerDeviceId: String,
    peerDeviceName: String,
    peerDeviceCode: String,
    direction: String,
    status: String,
    durationSeconds: Long = 0L,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("history").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "peerUserId" to peerUserId.ifBlank { uid },
          "peerDeviceId" to peerDeviceId.ifBlank { "unknown" },
          "peerDeviceName" to peerDeviceName.trim().ifBlank { "Device" }.take(60),
          "peerDeviceCode" to peerDeviceCode.take(20),
          "direction" to if (direction == "incoming") "incoming" else "outgoing",
          "status" to status,
          "durationSeconds" to durationSeconds.coerceAtLeast(0L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun clearConnectionHistory(): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "users/$uid/history"
    return try {
      val snap = db.collection("users").document(uid).collection("history").get().await()
      val batch = db.batch()
      snap.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  private fun handleFlowError(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(toLog, op, path)
    }
    throw error
  }
}
