package com.miragekit.protocol

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FrameHeaderTest {

    @Test
    fun testSerialization() {
        val header = FrameHeader(
            flags = FrameFlags.KEYFRAME,
            streamID = 1u,
            sequenceNumber = 100u,
            timestamp = 123456789u,
            frameNumber = 50u,
            fragmentIndex = 0u,
            fragmentCount = 1u,
            payloadLength = 1024u,
            frameByteCount = 1024u,
            checksum = 0xDEADBEEFu,
            contentRectX = 0f,
            contentRectY = 0f,
            contentRectWidth = 1920f,
            contentRectHeight = 1080f,
            dimensionToken = 5u,
            epoch = 1u
        )

        val data = header.serialize()

        assertEquals(MIRAGE_HEADER_SIZE, data.size)

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(MIRAGE_PROTOCOL_MAGIC.toInt(), buffer.int)
        assertEquals(MIRAGE_PROTOCOL_VERSION.toByte(), buffer.get())
        assertEquals(FrameFlags.KEYFRAME.toShort(), buffer.short)
        assertEquals(1.toShort(), buffer.short) // streamID
        assertEquals(100, buffer.int) // sequenceNumber
        assertEquals(123456789L, buffer.long) // timestamp
        assertEquals(50, buffer.int) // frameNumber
        assertEquals(0.toShort(), buffer.short) // fragmentIndex
        assertEquals(1.toShort(), buffer.short) // fragmentCount
        assertEquals(1024, buffer.int) // payloadLength
        assertEquals(1024, buffer.int) // frameByteCount
        assertEquals(0xDEADBEEF.toInt(), buffer.int) // checksum
        assertEquals(0f, buffer.float, 0.0f)
        assertEquals(0f, buffer.float, 0.0f)
        assertEquals(1920f, buffer.float, 0.0f)
        assertEquals(1080f, buffer.float, 0.0f)
        assertEquals(5.toShort(), buffer.short) // dimensionToken
        assertEquals(1.toShort(), buffer.short) // epoch
    }

    @Test
    fun testDeserialization() {
        val buffer = ByteBuffer.allocate(MIRAGE_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(MIRAGE_PROTOCOL_MAGIC.toInt())
        buffer.put(MIRAGE_PROTOCOL_VERSION.toByte())
        buffer.putShort(FrameFlags.END_OF_FRAME.toShort())
        buffer.putShort(2) // streamID
        buffer.putInt(200) // sequenceNumber
        buffer.putLong(987654321L) // timestamp
        buffer.putInt(75) // frameNumber
        buffer.putShort(1) // fragmentIndex
        buffer.putShort(2) // fragmentCount
        buffer.putInt(512) // payloadLength
        buffer.putInt(1024) // frameByteCount
        buffer.putInt(0xCAFEBABE.toInt()) // checksum
        buffer.putFloat(10f)
        buffer.putFloat(20f)
        buffer.putFloat(800f)
        buffer.putFloat(600f)
        buffer.putShort(10) // dimensionToken
        buffer.putShort(2) // epoch

        val data = buffer.array()
        val header = FrameHeader.deserialize(data)

        assertNotNull(header)
        assertEquals(MIRAGE_PROTOCOL_MAGIC, header?.magic)
        assertEquals(MIRAGE_PROTOCOL_VERSION, header?.version)
        assertEquals(FrameFlags.END_OF_FRAME, header?.flags)
        assertEquals(2u.toUShort(), header?.streamID)
        assertEquals(200u, header?.sequenceNumber)
        assertEquals(987654321u.toULong(), header?.timestamp)
        assertEquals(75u, header?.frameNumber)
        assertEquals(1u.toUShort(), header?.fragmentIndex)
        assertEquals(2u.toUShort(), header?.fragmentCount)
        assertEquals(512u, header?.payloadLength)
        assertEquals(1024u, header?.frameByteCount)
        assertEquals(0xCAFEBABE.toUInt(), header?.checksum)
        assertEquals(10f, header?.contentRectX)
        assertEquals(20f, header?.contentRectY)
        assertEquals(800f, header?.contentRectWidth)
        assertEquals(600f, header?.contentRectHeight)
        assertEquals(10u.toUShort(), header?.dimensionToken)
        assertEquals(2u.toUShort(), header?.epoch)
    }

    @Test
    fun testInvalidMagic() {
        val buffer = ByteBuffer.allocate(MIRAGE_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x12345678) // Wrong magic
        // ... fill rest to avoid underflow if logic checks size first

        val header = FrameHeader.deserialize(buffer.array())
        assertNull(header)
    }
}
