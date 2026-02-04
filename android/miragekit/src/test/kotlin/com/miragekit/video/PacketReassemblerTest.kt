package com.miragekit.video

import com.miragekit.protocol.FrameFlags
import com.miragekit.protocol.FrameHeader
import org.junit.Assert.*
import org.junit.Test

class PacketReassemblerTest {

    @Test
    fun testSingleFragmentFrame() {
        val reassembler = PacketReassembler()

        val header = createHeader(frameNum = 1u, fragIdx = 0u, fragCount = 1u)
        val payload = ByteArray(10) { it.toByte() }

        val frame = reassembler.processPacket(header, payload)

        assertNotNull(frame)
        assertEquals(header, frame?.header)
        assertArrayEquals(payload, frame?.data)
    }

    @Test
    fun testMultiFragmentFrame() {
        val reassembler = PacketReassembler()
        val frameNum = 2u

        val header1 = createHeader(frameNum, 0u, 2u)
        val payload1 = ByteArray(5) { 1 }

        val header2 = createHeader(frameNum, 1u, 2u)
        val payload2 = ByteArray(5) { 2 }

        // First fragment - should return null
        val frame1 = reassembler.processPacket(header1, payload1)
        assertNull(frame1)

        // Second fragment - should return complete frame
        val frame2 = reassembler.processPacket(header2, payload2)
        assertNotNull(frame2)

        assertEquals(10, frame2?.data?.size)
        // Verify concatenation order
        val expected = payload1 + payload2
        assertArrayEquals(expected, frame2?.data)
    }

    @Test
    fun testOutOfOrderFragments() {
        val reassembler = PacketReassembler()
        val frameNum = 3u

        val header1 = createHeader(frameNum, 0u, 3u)
        val payload1 = ByteArray(1) { 1 }

        val header2 = createHeader(frameNum, 1u, 3u)
        val payload2 = ByteArray(1) { 2 }

        val header3 = createHeader(frameNum, 2u, 3u)
        val payload3 = ByteArray(1) { 3 }

        // Receive 3, then 1, then 2
        assertNull(reassembler.processPacket(header3, payload3))
        assertNull(reassembler.processPacket(header1, payload1))

        val frame = reassembler.processPacket(header2, payload2)
        assertNotNull(frame)

        val expected = byteArrayOf(1, 2, 3)
        assertArrayEquals(expected, frame?.data)
    }

    @Test
    fun testPruning() {
        val reassembler = PacketReassembler()

        // Simulate receiving a very new frame (e.g. 100)
        // This should prune anything older than 40 (100 - 60)

        // Add a partial frame 10
        reassembler.processPacket(createHeader(10u, 0u, 2u), ByteArray(1))

        // Add frame 100
        val frame100 = reassembler.processPacket(createHeader(100u, 0u, 1u), ByteArray(1))
        assertNotNull(frame100)

        // Now try to complete frame 10 - it should have been pruned
        // Note: The implementation returns null if not complete.
        // If it was pruned, the previous fragments are gone.
        // If we send the remaining fragment, it will think it's a new incomplete frame with just that fragment.
        // So we can't easily distinguish "pruned" from "just started" via return value alone without inspecting internal state.
        // But functionally, it shouldn't return a complete frame if we just send the 2nd part of 10.

        val frame10 = reassembler.processPacket(createHeader(10u, 1u, 2u), ByteArray(1))
        assertNull(frame10)
    }

    private fun createHeader(frameNum: UInt, fragIdx: UInt, fragCount: UInt): FrameHeader {
        return FrameHeader(
            flags = 0u,
            streamID = 0u,
            sequenceNumber = 0u,
            timestamp = 0u,
            frameNumber = frameNum,
            fragmentIndex = fragIdx.toUShort(),
            fragmentCount = fragCount.toUShort(),
            payloadLength = 0u,
            frameByteCount = 0u,
            checksum = 0u
        )
    }
}
