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
                // Start UDP listener on dynamic port (0)
                // The host expects us to send a registration packet to its dataPort.
                startUdpReception(0, host.endpoint, response.dataPort.toInt(), deviceId)
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

    private fun startUdpReception(localPort: Int, hostAddress: String, hostPort: Int, deviceId: String) {
        val receiver = UdpReceiver()
        udpReceiver = receiver

        // Launch the receiver flow
        receiver.start(localPort).onEach { (header, payload) ->
            val frame = reassembler.processPacket(header, payload)
            if (frame != null) {
                 decoder?.decodeFrame(frame)
            }
        }.launchIn(scope)

        // Send registration/punch packet (FrameHeader with no payload)
        // Protocol: "Client registers stream IDs to receive data"
        // We construct a dummy header or specific registration format.
        // Assuming implicit registration by sending any packet to the host data port.
        // Or we might need to send the deviceID or streamID.
        // Based on Swift code reading, the client sends "UDP registration (streamID + deviceID)".
        // However, we don't have the exact struct for that.
        // Let's send a FrameHeader with empty payload and streamID 0 to punch the hole.
        scope.launch(Dispatchers.IO) {
            val punchHeader = FrameHeader(
                flags = 0u,
                streamID = 0u,
                sequenceNumber = 0u,
                timestamp = 0u,
                frameNumber = 0u,
                fragmentIndex = 0u,
                fragmentCount = 1u,
                payloadLength = 0u,
                frameByteCount = 0u,
                checksum = 0u
            )
            val data = punchHeader.serialize()
            try {
                // Repeat a few times to ensure arrival
                repeat(3) {
                    receiver.send(data, hostAddress, hostPort)
                    delay(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
