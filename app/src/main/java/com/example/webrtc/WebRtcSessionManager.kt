package com.example.webrtc

import android.content.Context
import android.util.Log
import com.example.domain.model.ConnectionTransportState
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRtcSessionManager(private val context: Context) {

  companion object {
    private const val TAG = "WebRtcSessionManager"
  }

  private var factory: PeerConnectionFactory? = null
  private var peerConnection: PeerConnection? = null
  private var dataChannel: DataChannel? = null
  private var eglBase: EglBase? = null

  private val _iceState = MutableStateFlow(ConnectionTransportState.WAITING)
  val iceState: StateFlow<ConnectionTransportState> = _iceState.asStateFlow()

  var onLocalIceCandidate: ((sdpMid: String, sdpMLineIndex: Int, candidate: String) -> Unit)? = null
  var onDataChannelBinaryMessage: ((ByteArray) -> Unit)? = null

  private fun ensureInitialized() {
    if (factory != null) return
    PeerConnectionFactory.initialize(
      PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
        .setEnableInternalTracer(false)
        .createInitializationOptions()
    )
    eglBase = EglBase.create()
    factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
  }

  fun startPeerConnection(
    stunUrl: String,
    turnUrl: String,
    turnUser: String,
    turnCred: String,
    isOfferer: Boolean,
    onSdpReady: (isOffer: Boolean, sdp: String) -> Unit,
  ) {
    try {
      ensureInitialized()
      close()
      _iceState.value = ConnectionTransportState.CONNECTING

      val iceServers = mutableListOf<PeerConnection.IceServer>()
      if (stunUrl.isNotBlank()) {
        iceServers.add(PeerConnection.IceServer.builder(stunUrl).createIceServer())
      } else {
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
      }
      if (turnUrl.isNotBlank()) {
        val turnBuilder = PeerConnection.IceServer.builder(turnUrl)
        if (turnUser.isNotBlank()) turnBuilder.setUsername(turnUser)
        if (turnCred.isNotBlank()) turnBuilder.setPassword(turnCred)
        iceServers.add(turnBuilder.createIceServer())
      }

      val rtcConfig =
        PeerConnection.RTCConfiguration(iceServers).apply {
          sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
          continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

      peerConnection =
        factory?.createPeerConnection(
          rtcConfig,
          object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
              _iceState.value =
                when (newState) {
                  PeerConnection.IceConnectionState.CONNECTED,
                  PeerConnection.IceConnectionState.COMPLETED -> ConnectionTransportState.CONNECTED
                  PeerConnection.IceConnectionState.CHECKING -> ConnectionTransportState.CONNECTING
                  PeerConnection.IceConnectionState.DISCONNECTED -> ConnectionTransportState.RECONNECTING
                  PeerConnection.IceConnectionState.FAILED -> ConnectionTransportState.FAILED
                  PeerConnection.IceConnectionState.CLOSED -> ConnectionTransportState.DISCONNECTED
                  else -> ConnectionTransportState.WAITING
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
              candidate?.let {
                onLocalIceCandidate?.invoke(it.sdpMid ?: "0", it.sdpMLineIndex, it.sdp)
              }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {
              dc?.let { registerDataChannel(it) }
            }

            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
          },
        )

      if (isOfferer) {
        val init = DataChannel.Init().apply { ordered = true }
        peerConnection?.createDataChannel("sadi-data", init)?.let { registerDataChannel(it) }
        peerConnection?.createOffer(
          object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
              sdp ?: return
              peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
              onSdpReady(true, sdp.description)
            }
          },
          MediaConstraints(),
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "WebRTC init error: ${e.message}", e)
      _iceState.value = ConnectionTransportState.FAILED
    }
  }

  fun setRemoteOfferAndCreateAnswer(offerSdp: String, onAnswerReady: (String) -> Unit) {
    val pc = peerConnection ?: return
    val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
    pc.setRemoteDescription(
      object : SimpleSdpObserver() {
        override fun onSetSuccess() {
          pc.createAnswer(
            object : SimpleSdpObserver() {
              override fun onCreateSuccess(answer: SessionDescription?) {
                answer ?: return
                pc.setLocalDescription(SimpleSdpObserver(), answer)
                onAnswerReady(answer.description)
              }
            },
            MediaConstraints(),
          )
        }
      },
      offer,
    )
  }

  fun setRemoteAnswer(answerSdp: String) {
    val pc = peerConnection ?: return
    val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
    pc.setRemoteDescription(SimpleSdpObserver(), answer)
  }

  fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
    peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
  }

  fun sendBinaryOverDataChannel(bytes: ByteArray): Boolean {
    val dc = dataChannel ?: return false
    if (dc.state() != DataChannel.State.OPEN) return false
    return try {
      dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), true))
    } catch (_: Exception) {
      false
    }
  }

  private fun registerDataChannel(dc: DataChannel) {
    dataChannel = dc
    dc.registerObserver(
      object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) {}
        override fun onStateChange() {}
        override fun onMessage(buffer: DataChannel.Buffer?) {
          buffer ?: return
          val data = ByteArray(buffer.data.remaining())
          buffer.data.get(data)
          onDataChannelBinaryMessage?.invoke(data)
        }
      }
    )
  }

  fun close() {
    try {
      dataChannel?.close()
    } catch (_: Exception) {}
    dataChannel = null
    try {
      peerConnection?.close()
    } catch (_: Exception) {}
    peerConnection = null
    _iceState.value = ConnectionTransportState.DISCONNECTED
  }

  fun dispose() {
    close()
    try {
      factory?.dispose()
    } catch (_: Exception) {}
    factory = null
    try {
      eglBase?.release()
    } catch (_: Exception) {}
    eglBase = null
  }

  private open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {
      Log.w(TAG, "SDP create failure: $error")
    }

    override fun onSetFailure(error: String?) {
      Log.w(TAG, "SDP set failure: $error")
    }
  }
}
