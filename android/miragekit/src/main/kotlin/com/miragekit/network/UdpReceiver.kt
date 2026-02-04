package com.miragekit.network

import com.miragekit.protocol.FrameHeader
import com.miragekit.protocol.MIRAGE_HEADER_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.Arrays

class UdpReceiver {
    private var socket: DatagramSocket? = null

    // Default max packet size from Swift is 1200.
    private val bufferSize = 2048

    // Send a packet to the destination
    suspend fun send(data: ByteArray, host: String, port: Int) = withContext(Dispatchers.IO) {
        val socket = socket ?: return@withContext
        val address = java.net.InetAddress.getByName(host)
        val packet = DatagramPacket(data, data.size, address, port)
        socket.send(packet)
    }

    fun start(port: Int): Flow<Pair<FrameHeader, ByteArray>> = callbackFlow {
        val socket = DatagramSocket(port)
        this@UdpReceiver.socket = socket

        val buffer = ByteArray(bufferSize)
        val packet = DatagramPacket(buffer, buffer.size)

        withContext(Dispatchers.IO) {
            while (isActive) {
                try {
                    socket.receive(packet)
                    // We must copy the data because the buffer is reused
                    val data = Arrays.copyOf(packet.data, packet.length)

                    val header = FrameHeader.deserialize(data)
                    if (header != null) {
                        // Extract payload
                        if (data.size >= MIRAGE_HEADER_SIZE) {
                            val payload = Arrays.copyOfRange(data, MIRAGE_HEADER_SIZE, data.size)
                            trySend(header to payload)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        // socket closed or error
                    }
                }
            }
        }

        awaitClose {
            socket.close()
        }
    }
}
