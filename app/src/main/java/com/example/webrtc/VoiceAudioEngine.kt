package com.example.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.permissions.PermissionStatusHelper
import com.example.permissions.SessionRuntimeBridge
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceAudioEngine(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val TAG = "VoiceAudioEngine"
    private const val SAMPLE_RATE = 16000
    private const val FRAME_BYTES = 640 // 20ms of 16-bit mono PCM @ 16kHz
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var recordJob: Job? = null
  private var audioRecord: AudioRecord? = null
  private var audioTrack: AudioTrack? = null

  private val _muted = MutableStateFlow(false)
  val muted: StateFlow<Boolean> = _muted.asStateFlow()

  private val _speakerOn = MutableStateFlow(false)
  val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

  private val _running = MutableStateFlow(false)
  val running: StateFlow<Boolean> = _running.asStateFlow()

  var onAudioFrameCaptured: ((ByteArray) -> Unit)? = null

  @SuppressLint("MissingPermission")
  fun start() {
    if (_running.value) return
    startPlaybackTrack()

    if (!PermissionStatusHelper.hasMicrophonePermission(context)) {
      Log.w(TAG, "RECORD_AUDIO permission not granted; starting receive-only audio")
      _running.value = true
      return
    }

    val minBuf =
      AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(FRAME_BYTES * 4)

    try {
      val recorder =
        AudioRecord(
          MediaRecorder.AudioSource.VOICE_COMMUNICATION,
          SAMPLE_RATE,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          minBuf,
        )
      if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        _running.value = true
        return
      }
      audioRecord = recorder
      recorder.startRecording()
      _running.value = true
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

      recordJob =
        scope.launch(Dispatchers.IO) {
          val buf = ByteArray(FRAME_BYTES)
          while (isActive && _running.value) {
            val read = recorder.read(buf, 0, buf.size)
            if (read > 0) {
              if (!_muted.value) {
                val frame = buf.copyOf(read)
                SessionRuntimeBridge.setAudioAmplitude(computeNormalizedRms(frame))
                onAudioFrameCaptured?.invoke(frame)
              } else {
                SessionRuntimeBridge.setAudioAmplitude(0.02f)
              }
            }
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start AudioRecord: ${e.message}", e)
    }
  }

  private fun startPlaybackTrack() {
    val minOut =
      AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(FRAME_BYTES * 4)
    try {
      val track =
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(SAMPLE_RATE)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(minOut)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      track.play()
      audioTrack = track
    } catch (e: Exception) {
      Log.w(TAG, "AudioTrack init failed: ${e.message}")
    }
  }

  fun playIncomingFrame(pcm: ByteArray) {
    try {
      audioTrack?.write(pcm, 0, pcm.size)
      val level = computeNormalizedRms(pcm)
      if (level > SessionRuntimeBridge.audioAmplitude.value) {
        SessionRuntimeBridge.setAudioAmplitude(level)
      }
    } catch (_: Exception) {}
  }

  fun setMuted(muted: Boolean) {
    _muted.value = muted
    if (muted) SessionRuntimeBridge.setAudioAmplitude(0f)
  }

  fun setSpeakerEnabled(enabled: Boolean) {
    _speakerOn.value = enabled
    try {
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = enabled
    } catch (_: Exception) {}
  }

  fun stop() {
    _running.value = false
    recordJob?.cancel()
    recordJob = null
    try {
      audioRecord?.stop()
      audioRecord?.release()
    } catch (_: Exception) {}
    audioRecord = null
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (_: Exception) {}
    audioTrack = null
    try {
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = false
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (_: Exception) {}
    SessionRuntimeBridge.setAudioAmplitude(0f)
  }

  private fun computeNormalizedRms(pcm16: ByteArray): Float {
    if (pcm16.size < 2) return 0f
    var sumSq = 0.0
    val samples = pcm16.size / 2
    for (i in 0 until samples) {
      val lo = pcm16[i * 2].toInt() and 0xFF
      val hi = pcm16[i * 2 + 1].toInt()
      val sample = (hi shl 8) or lo
      val norm = sample / 32768.0
      sumSq += norm * norm
    }
    val rms = sqrt(sumSq / samples.coerceAtLeast(1))
    return (rms * 3.5).toFloat().coerceIn(0f, 1f)
  }
}
