package com.example.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.VideoQuality
import com.example.utils.DeviceCodeUtils
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sadiDataStore: DataStore<Preferences> by preferencesDataStore(name = "sadi_connect_prefs")

data class AppPreferences(
  val darkMode: Boolean = true,
  val language: AppLanguage = AppLanguage.BN,
  val localDeviceId: String = "",
  val localDeviceCode: String = "",
  val localDeviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
  val videoQuality: VideoQuality = VideoQuality.MEDIUM,
  val targetFps: Int = 20,
  val localP2pEnabled: Boolean = true,
  val notificationsEnabled: Boolean = true,
  val stunServerUrl: String = "stun:stun.l.google.com:19302",
  val turnServerUrl: String = "",
  val turnUsername: String = "",
  val turnCredential: String = "",
)

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val LOCAL_DEVICE_ID = stringPreferencesKey("local_device_id")
    val LOCAL_DEVICE_CODE = stringPreferencesKey("local_device_code")
    val LOCAL_DEVICE_NAME = stringPreferencesKey("local_device_name")
    val VIDEO_QUALITY = stringPreferencesKey("video_quality")
    val TARGET_FPS = intPreferencesKey("target_fps")
    val LOCAL_P2P_ENABLED = booleanPreferencesKey("local_p2p_enabled")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val STUN_URL = stringPreferencesKey("stun_url")
    val TURN_URL = stringPreferencesKey("turn_url")
    val TURN_USER = stringPreferencesKey("turn_user")
    val TURN_CRED = stringPreferencesKey("turn_cred")
  }

  val preferencesFlow: Flow<AppPreferences> =
    context.sadiDataStore.data.map { prefs ->
      val defaultName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
      AppPreferences(
        darkMode = prefs[Keys.DARK_MODE] ?: true,
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
        localDeviceId = prefs[Keys.LOCAL_DEVICE_ID] ?: "",
        localDeviceCode = prefs[Keys.LOCAL_DEVICE_CODE] ?: "",
        localDeviceName = prefs[Keys.LOCAL_DEVICE_NAME] ?: defaultName,
        videoQuality = VideoQuality.fromWire(prefs[Keys.VIDEO_QUALITY]),
        targetFps = prefs[Keys.TARGET_FPS] ?: 20,
        localP2pEnabled = prefs[Keys.LOCAL_P2P_ENABLED] ?: true,
        notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        stunServerUrl = prefs[Keys.STUN_URL] ?: "stun:stun.l.google.com:19302",
        turnServerUrl = prefs[Keys.TURN_URL] ?: "",
        turnUsername = prefs[Keys.TURN_USER] ?: "",
        turnCredential = prefs[Keys.TURN_CRED] ?: "",
      )
    }

  suspend fun ensureDeviceIdentity(): Pair<String, String> {
    val current = context.sadiDataStore.data.first()
    var devId = current[Keys.LOCAL_DEVICE_ID]
    var devCode = current[Keys.LOCAL_DEVICE_CODE]
    if (devId.isNullOrBlank() || devCode.isNullOrBlank()) {
      devId = devId?.takeIf { it.isNotBlank() } ?: "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      devCode = devCode?.takeIf { it.length == 9 } ?: DeviceCodeUtils.generateNineDigitCode()
      context.sadiDataStore.edit { prefs ->
        prefs[Keys.LOCAL_DEVICE_ID] = devId
        prefs[Keys.LOCAL_DEVICE_CODE] = devCode
      }
    }
    return devId to devCode
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.sadiDataStore.edit { it[Keys.DARK_MODE] = enabled }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.sadiDataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setDeviceName(name: String) {
    val sanitized = name.trim().take(60)
    if (sanitized.isNotBlank()) {
      context.sadiDataStore.edit { it[Keys.LOCAL_DEVICE_NAME] = sanitized }
    }
  }

  suspend fun setVideoQuality(quality: VideoQuality) {
    context.sadiDataStore.edit { it[Keys.VIDEO_QUALITY] = quality.wire }
  }

  suspend fun setTargetFps(fps: Int) {
    context.sadiDataStore.edit { it[Keys.TARGET_FPS] = fps.coerceIn(5, 60) }
  }

  suspend fun setLocalP2pEnabled(enabled: Boolean) {
    context.sadiDataStore.edit { it[Keys.LOCAL_P2P_ENABLED] = enabled }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    context.sadiDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
  }

  suspend fun setIceConfig(stun: String, turn: String, turnUser: String, turnCred: String) {
    context.sadiDataStore.edit {
      it[Keys.STUN_URL] = stun.trim().ifBlank { "stun:stun.l.google.com:19302" }
      it[Keys.TURN_URL] = turn.trim()
      it[Keys.TURN_USER] = turnUser.trim()
      it[Keys.TURN_CRED] = turnCred.trim()
    }
  }
}
