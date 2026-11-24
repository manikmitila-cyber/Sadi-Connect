package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String) {
  BN("bn"),
  EN("en");

  companion object {
    fun fromCode(raw: String?): AppLanguage =
      entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: BN
  }
}

enum class ConnectionTransportState {
  WAITING,
  CONNECTING,
  CONNECTED,
  RECONNECTING,
  DISCONNECTED,
  FAILED,
}

enum class VideoQuality(val wire: String, val maxWidth: Int, val jpegQuality: Int) {
  LOW("low", 480, 45),
  MEDIUM("medium", 720, 65),
  HIGH("high", 1080, 80);

  companion object {
    fun fromWire(raw: String?): VideoQuality =
      entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: MEDIUM
  }
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String = "",
  val preferredLanguage: String = "bn",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserDevice(
  @DocumentId val id: String = "",
  val userId: String = "",
  val deviceName: String = "",
  val deviceModel: String = "",
  val deviceCode: String = "",
  val online: Boolean = true,
  val favorite: Boolean = false,
  val localIp: String = "",
  val p2pPort: Long = 0L,
  val accessibilityReady: Boolean = false,
  val screenCaptureReady: Boolean = false,
  val cameraReady: Boolean = false,
  @ServerTimestamp val lastSeenAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DeviceDirectoryEntry(
  @DocumentId val id: String = "",
  val ownerUid: String = "",
  val deviceId: String = "",
  val deviceCode: String = "",
  val deviceName: String = "",
  val ownerDisplayName: String = "",
  val online: Boolean = false,
  val localIp: String = "",
  val p2pPort: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RemoteSession(
  @DocumentId val id: String = "",
  val hostUid: String = "",
  val hostDeviceId: String = "",
  val hostDeviceName: String = "",
  val hostName: String = "",
  val hostDeviceCode: String = "",
  val hostLocalIp: String = "",
  val hostP2pPort: Long = 0L,
  val controllerUid: String = "",
  val controllerName: String = "",
  val controllerDeviceName: String = "",
  val memberUids: List<String> = emptyList(),
  val joinToken: String = "",
  val status: String = "pending",
  val screenShareAllowed: Boolean = false,
  val remoteTouchAllowed: Boolean = false,
  val remoteCameraAllowed: Boolean = false,
  val voiceCallState: String = "idle",
  val voiceCallerUid: String = "",
  val cameraFacing: String = "back",
  val videoQuality: String = "medium",
  val targetFps: Long = 20L,
  val brightnessPercent: Long = 70L,
  val sdpOffer: String = "",
  val sdpAnswer: String = "",
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val memberUids: List<String> = emptyList(),
  val senderUid: String = "",
  val senderName: String = "",
  val text: String = "",
  val delivered: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IceCandidateSignal(
  @DocumentId val id: String = "",
  val memberUids: List<String> = emptyList(),
  val senderUid: String = "",
  val sdpMid: String = "",
  val sdpMLineIndex: Long = 0L,
  val candidate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RemoteCommand(
  @DocumentId val id: String = "",
  val memberUids: List<String> = emptyList(),
  val senderUid: String = "",
  val type: String = "tap",
  val x: Double = 0.0,
  val y: Double = 0.0,
  val endX: Double = 0.0,
  val endY: Double = 0.0,
  val value: Double = 0.0,
  val executed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ConnectionHistoryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val peerUserId: String = "",
  val peerDeviceId: String = "",
  val peerDeviceName: String = "",
  val peerDeviceCode: String = "",
  val direction: String = "outgoing",
  val status: String = "connected",
  val durationSeconds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
