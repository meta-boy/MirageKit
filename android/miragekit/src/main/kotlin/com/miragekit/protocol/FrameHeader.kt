package com.miragekit.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val MIRAGE_PROTOCOL_MAGIC = 0x4D49_5247.toUInt() // "MIRG"
const val MIRAGE_PROTOCOL_VERSION: UByte = 1u
const val MIRAGE_HEADER_SIZE = 61

data class FrameHeader(
    var magic: UInt = MIRAGE_PROTOCOL_MAGIC,
    var version: UByte = MIRAGE_PROTOCOL_VERSION,
    var flags: UShort,
    var streamID: UShort,
    var sequenceNumber: UInt,
    var timestamp: ULong,
    var frameNumber: UInt,
    var fragmentIndex: UShort,
    var fragmentCount: UShort,
    var payloadLength: UInt,
    var frameByteCount: UInt,
    var checksum: UInt,
    var contentRectX: Float = 0f,
    var contentRectY: Float = 0f,
    var contentRectWidth: Float = 0f,
    var contentRectHeight: Float = 0f,
    var dimensionToken: UShort = 0u,
    var epoch: UShort = 0u
) {
    fun serialize(): ByteArray {
        val buffer = ByteBuffer.allocate(MIRAGE_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(magic.toInt())
        buffer.put(version.toByte())
        buffer.putShort(flags.toShort())
        buffer.putShort(streamID.toShort())
        buffer.putInt(sequenceNumber.toInt())
        buffer.putLong(timestamp.toLong())
        buffer.putInt(frameNumber.toInt())
        buffer.putShort(fragmentIndex.toShort())
        buffer.putShort(fragmentCount.toShort())
        buffer.putInt(payloadLength.toInt())
        buffer.putInt(frameByteCount.toInt())
        buffer.putInt(checksum.toInt())
        buffer.putFloat(contentRectX)
        buffer.putFloat(contentRectY)
        buffer.putFloat(contentRectWidth)
        buffer.putFloat(contentRectHeight)
        buffer.putShort(dimensionToken.toShort())
        buffer.putShort(epoch.toShort())
        return buffer.array()
    }

    companion object {
        fun deserialize(data: ByteArray): FrameHeader? {
            if (data.size < MIRAGE_HEADER_SIZE) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val magic = buffer.int.toUInt()
            if (magic != MIRAGE_PROTOCOL_MAGIC) return null

            val version = buffer.get().toUByte()
            if (version != MIRAGE_PROTOCOL_VERSION) return null

            return FrameHeader(
                magic = magic,
                version = version,
                flags = buffer.short.toUShort(),
                streamID = buffer.short.toUShort(),
                sequenceNumber = buffer.int.toUInt(),
                timestamp = buffer.long.toULong(),
                frameNumber = buffer.int.toUInt(),
                fragmentIndex = buffer.short.toUShort(),
                fragmentCount = buffer.short.toUShort(),
                payloadLength = buffer.int.toUInt(),
                frameByteCount = buffer.int.toUInt(),
                checksum = buffer.int.toUInt(),
                contentRectX = buffer.float,
                contentRectY = buffer.float,
                contentRectWidth = buffer.float,
                contentRectHeight = buffer.float,
                dimensionToken = buffer.short.toUShort(),
                epoch = buffer.short.toUShort()
            )
        }
    }
}

object FrameFlags {
    const val KEYFRAME: UShort = 1u
    const val END_OF_FRAME: UShort = 2u // 1 << 1
    const val PARAMETER_SET: UShort = 4u // 1 << 2
    const val DISCONTINUITY: UShort = 8u // 1 << 3
    const val PRIORITY: UShort = 16u // 1 << 4
    const val LOGIN_DISPLAY: UShort = 128u // 1 << 7
    const val DESKTOP_STREAM: UShort = 256u // 1 << 8
    const val REPEATED_FRAME: UShort = 512u // 1 << 9
    const val FEC_PARITY: UShort = 1024u // 1 << 10
}
