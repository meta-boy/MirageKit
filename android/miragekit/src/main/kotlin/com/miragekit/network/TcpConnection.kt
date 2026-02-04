package com.miragekit.network

import com.miragekit.protocol.ControlMessage
import com.miragekit.protocol.ControlMessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TcpConnection(private val host: String, private val port: Int) {
    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    suspend fun connect() = withContext(Dispatchers.IO) {
        socket = Socket(host, port)
        inputStream = DataInputStream(socket!!.getInputStream())
        outputStream = DataOutputStream(socket!!.getOutputStream())
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
    }

    suspend fun sendMessage(message: ControlMessage) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext

        // Header
        out.writeByte(message.type.value.toInt())

        // Length (Little Endian)
        val lengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        lengthBuffer.putInt(message.payload.size)
        out.write(lengthBuffer.array())

        // Payload
        out.write(message.payload)
        out.flush()
    }

    suspend fun receiveMessage(): ControlMessage = withContext(Dispatchers.IO) {
        val `in` = inputStream ?: throw IllegalStateException("Not connected")

        val typeByte = `in`.readByte().toUByte()
        val type = ControlMessageType.fromValue(typeByte) ?: ControlMessageType.ERROR

        val lengthBytes = ByteArray(4)
        `in`.readFully(lengthBytes)
        val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).int

        val payload = ByteArray(length)
        `in`.readFully(payload)

        ControlMessage(type, payload)
    }
}
