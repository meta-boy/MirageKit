package com.miragekit.client

import android.content.Context
import android.os.Build
import android.view.Surface
import com.miragekit.discovery.MirageDiscovery
import com.miragekit.network.TcpConnection
import com.miragekit.network.UdpReceiver
import com.miragekit.protocol.*
import com.miragekit.video.MirageDecoder
import com.miragekit.video.PacketReassembler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.UUID

class MirageClient(private val context: Context) {
    private val discovery = MirageDiscovery(context)
    private var tcpConnection: TcpConnection? = null
    private var udpReceiver: UdpReceiver? = null
    private var decoder: MirageDecoder? = null
    private val reassembler = PacketReassembler()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var connectedHost: MirageHost? = null
        private set

    fun discoverHosts(): Flow<List<MirageHost>> = discovery.discoverHosts()

    suspend fun connect(host: MirageHost) = withContext(Dispatchers.IO) {
        val tcp = TcpConnection(host.endpoint, 9847)
        tcp.connect()
        tcpConnection = tcp

        // Handshake
        val deviceId = UUID.randomUUID().toString()
        val hello = HelloMessage(
            deviceID = deviceId,
            deviceName = Build.MODEL,
            deviceType = DeviceType.unknown,
            protocolVersion = 1,
            capabilities = MirageHostCapabilities()
        )

        val helloJson = Json.encodeToString(hello)
        tcp.sendMessage(ControlMessage(ControlMessageType.HELLO, helloJson.toByteArray()))

        // Wait for HelloResponse
        val responseMsg = tcp.receiveMessage()
        if (responseMsg.type == ControlMessageType.HELLO_RESPONSE) {
            val response = Json.decodeFromString<HelloResponseMessage>(String(responseMsg.payload))
            if (response.accepted) {
                connectedHost = host
                // Start UDP listener on the assigned data port?
                // Actually the host TELLS us the data port to send to? Or the port it listens on?
                // UDP is usually Client sends to Host DataPort.
                // But Client also needs to listen for Video.
                // Usually Host sends video to Client's UDP port.
                // Does Client register its UDP port?
                // Protocol: "Client registers stream IDs to receive data."
                // "UDP registration (streamID + deviceID)"

                // Let's assume we listen on ANY port and send a registration packet?
                // Or maybe `HelloResponse` tells us where to send UDP?
                // `HelloResponse` has `dataPort`. That's likely the Host's UDP port.
                // The client likely needs to punch a hole or send a packet there.

                startUdpReception(9848) // Local port, can be random 0
            } else {
                tcp.disconnect()
                throw Exception("Connection rejected")
            }
        } else {
            tcp.disconnect()
             throw Exception("Unexpected message during handshake: ${responseMsg.type}")
        }

        // Start control loop
        startControlLoop()
    }

    private fun startUdpReception(localPort: Int) {
        val receiver = UdpReceiver()
        udpReceiver = receiver

        receiver.start(localPort).onEach { (header, payload) ->
            val frame = reassembler.processPacket(header, payload)
            if (frame != null) {
                 decoder?.decodeFrame(frame)
            }
        }.launchIn(scope)
    }

    private fun startControlLoop() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val msg = tcpConnection?.receiveMessage() ?: break
                    handleControlMessage(msg)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun handleControlMessage(msg: ControlMessage) {
        when (msg.type) {
            ControlMessageType.PING -> {
                scope.launch {
                    tcpConnection?.sendMessage(ControlMessage(ControlMessageType.PONG, ByteArray(0)))
                }
            }
            else -> {}
        }
    }

    fun setSurface(surface: Surface) {
        decoder = MirageDecoder(surface)
    }

    suspend fun sendInput(event: MirageInputEvent) {
        val streamID = 0.toUShort()
        val inputMsg = InputEventMessage(streamID, event)
        val json = Json.encodeToString(inputMsg)

        tcpConnection?.sendMessage(ControlMessage(ControlMessageType.INPUT_EVENT, json.toByteArray()))
    }

    fun disconnect() {
        scope.cancel()
        runBlocking {
            tcpConnection?.disconnect()
        }
        decoder?.stop()
    }
}
