package com.example.webrtc

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.domain.model.ConnectionTransportState
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class DiscoveredLocalPeer(
  val serviceName: String,
  val host: String,
  val port: Int,
)

class LocalP2pTransport(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val TAG = "LocalP2pTransport"
    private const val SERVICE_TYPE = "_sadiconnect._tcp."

    const val PACKET_SCREEN_FRAME: Byte = 0x01
    const val PACKET_CAMERA_FRAME: Byte = 0x02
    const val PACKET_AUDIO_PCM: Byte = 0x03
    const val PACKET_COMMAND_JSON: Byte = 0x04
    const val PACKET_PING: Byte = 0x05
    const val PACKET_PONG: Byte = 0x06
  }

  private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
  private var serverSocket: ServerSocket? = null
  private var activeSocket: Socket? = null
  private var outStream: DataOutputStream? = null
  private var acceptJob: Job? = null
  private var readJob: Job? = null
  private var registrationListener: NsdManager.RegistrationListener? = null
  private var discoveryListener: NsdManager.DiscoveryListener? = null
  private val writeLock = Any()

  private val _hostActive = MutableStateFlow(false)
  val hostActive: StateFlow<Boolean> = _hostActive.asStateFlow()

  private val _listenPort = MutableStateFlow(0)
  val listenPort: StateFlow<Int> = _listenPort.asStateFlow()

  private val _transportState = MutableStateFlow(ConnectionTransportState.DISCONNECTED)
  val transportState: StateFlow<ConnectionTransportState> = _transportState.asStateFlow()

  private val _latencyMs = MutableStateFlow<Long?>(null)
  val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

  private val _discoveredPeers = MutableStateFlow<List<DiscoveredLocalPeer>>(emptyList())
  val discoveredPeers: StateFlow<List<DiscoveredLocalPeer>> = _discoveredPeers.asStateFlow()

  var onScreenFrameReceived: ((ByteArray) -> Unit)? = null
  var onCameraFrameReceived: ((ByteArray) -> Unit)? = null
  var onAudioPcmReceived: ((ByteArray) -> Unit)? = null
  var onCommandJsonReceived: ((JSONObject) -> Unit)? = null

  fun startHost(deviceCode: String) {
    if (_hostActive.value && serverSocket?.isClosed == false) return
    acceptJob?.cancel()
    acceptJob =
      scope.launch(Dispatchers.IO) {
        try {
          val server = ServerSocket(0)
          serverSocket = server
          val port = server.localPort
          _listenPort.value = port
          _hostActive.value = true
          registerNsdService(deviceCode, port)

          while (isActive && !server.isClosed) {
            val client = server.accept()
            Log.i(TAG, "Accepted local P2P client ${client.inetAddress.hostAddress}")
            attachSocket(client)
          }
        } catch (e: Exception) {
          Log.w(TAG, "P2P host stopped: ${e.message}")
          _hostActive.value = false
        }
      }
  }

  fun connectToPeer(hostIp: String, port: Int) {
    if (hostIp.isBlank() || port <= 0) {
      _transportState.value = ConnectionTransportState.FAILED
      return
    }
    scope.launch(Dispatchers.IO) {
      _transportState.value = ConnectionTransportState.CONNECTING
      try {
        val socket = Socket()
        socket.connect(InetSocketAddress(hostIp, port), 4500)
        socket.tcpNoDelay = true
        attachSocket(socket)
      } catch (e: Exception) {
        Log.w(TAG, "Failed local P2P connect to $hostIp:$port - ${e.message}")
        _transportState.value = ConnectionTransportState.DISCONNECTED
      }
    }
  }

  fun startDiscovery() {
    if (nsdManager == null || discoveryListener != null) return
    val listener =
      object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
          if (service.serviceType != SERVICE_TYPE) return
          @Suppress("DEPRECATION")
          nsdManager.resolveService(
            service,
            object : NsdManager.ResolveListener {
              override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
              override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val hostAddr = serviceInfo.host?.hostAddress ?: return
                val peer = DiscoveredLocalPeer(serviceInfo.serviceName, hostAddr, serviceInfo.port)
                _discoveredPeers.value =
                  (_discoveredPeers.value.filterNot { it.serviceName == peer.serviceName } + peer)
              }
            },
          )
        }

        override fun onServiceLost(service: NsdServiceInfo) {
          _discoveredPeers.value =
            _discoveredPeers.value.filterNot { it.serviceName == service.serviceName }
        }

        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
      }
    discoveryListener = listener
    try {
      nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    } catch (e: Exception) {
      Log.w(TAG, "NSD discovery failed: ${e.message}")
    }
  }

  private fun registerNsdService(deviceCode: String, port: Int) {
    if (nsdManager == null) return
    unregisterNsd()
    val info =
      NsdServiceInfo().apply {
        serviceName = "SadiConnect-$deviceCode"
        serviceType = SERVICE_TYPE
        setPort(port)
      }
    val listener =
      object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {}
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
      }
    registrationListener = listener
    try {
      nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    } catch (e: Exception) {
      Log.w(TAG, "NSD register failed: ${e.message}")
    }
  }

  private fun attachSocket(socket: Socket) {
    closeActiveClient()
    activeSocket = socket
    outStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
    val inStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
    _transportState.value = ConnectionTransportState.CONNECTED

    readJob =
      scope.launch(Dispatchers.IO) {
        try {
          sendPing()
          while (isActive && !socket.isClosed) {
            val type = inStream.readByte()
            val length = inStream.readInt()
            if (length < 0 || length > 8 * 1024 * 1024) break
            val payload = ByteArray(length)
            inStream.readFully(payload)
            when (type) {
              PACKET_SCREEN_FRAME -> onScreenFrameReceived?.invoke(payload)
              PACKET_CAMERA_FRAME -> onCameraFrameReceived?.invoke(payload)
              PACKET_AUDIO_PCM -> onAudioPcmReceived?.invoke(payload)
              PACKET_COMMAND_JSON -> {
                val json = JSONObject(String(payload, Charsets.UTF_8))
                onCommandJsonReceived?.invoke(json)
              }
              PACKET_PING -> sendPacket(PACKET_PONG, payload)
              PACKET_PONG -> {
                val sentAt = String(payload, Charsets.UTF_8).toLongOrNull()
                if (sentAt != null) {
                  _latencyMs.value = (System.currentTimeMillis() - sentAt).coerceAtLeast(1L)
                }
              }
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Local P2P read ended: ${e.message}")
        } finally {
          _transportState.value = ConnectionTransportState.DISCONNECTED
          _latencyMs.value = null
        }
      }
  }

  fun sendPing() {
    sendPacket(PACKET_PING, System.currentTimeMillis().toString().toByteArray(Charsets.UTF_8))
  }

  fun sendPacket(type: Byte, payload: ByteArray) {
    val stream = outStream ?: return
    scope.launch(Dispatchers.IO) {
      try {
        synchronized(writeLock) {
          stream.writeByte(type.toInt())
          stream.writeInt(payload.size)
          stream.write(payload)
          stream.flush()
        }
      } catch (_: Exception) {
        _transportState.value = ConnectionTransportState.DISCONNECTED
      }
    }
  }

  fun closeActiveClient() {
    readJob?.cancel()
    readJob = null
    try {
      activeSocket?.close()
    } catch (_: Exception) {}
    activeSocket = null
    outStream = null
    _transportState.value = ConnectionTransportState.DISCONNECTED
    _latencyMs.value = null
  }

  private fun unregisterNsd() {
    registrationListener?.let {
      try {
        nsdManager?.unregisterService(it)
      } catch (_: Exception) {}
    }
    registrationListener = null
  }

  fun shutdown() {
    closeActiveClient()
    acceptJob?.cancel()
    acceptJob = null
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverSocket = null
    _hostActive.value = false
    unregisterNsd()
    discoveryListener?.let {
      try {
        nsdManager?.stopServiceDiscovery(it)
      } catch (_: Exception) {}
    }
    discoveryListener = null
  }
}
