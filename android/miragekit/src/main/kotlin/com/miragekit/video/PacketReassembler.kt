package com.miragekit.video

import com.miragekit.protocol.FrameHeader
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

data class ReassembledFrame(
    val header: FrameHeader,
    val data: ByteArray
)

class PacketReassembler {
    // Map of FrameNumber -> (Map of FragmentIndex -> Data)
    private val pendingFrames = ConcurrentHashMap<UInt, MutableMap<UShort, ByteArray>>()
    // Map of FrameNumber -> Expected Fragment Count
    private val frameFragmentCounts = ConcurrentHashMap<UInt, UShort>()

    private var maxFrameNumberSeen: UInt = 0u

    fun processPacket(header: FrameHeader, payload: ByteArray): ReassembledFrame? {
        val frameNum = header.frameNumber

        // Update max frame seen
        if (frameNum > maxFrameNumberSeen) {
            maxFrameNumberSeen = frameNum

            // Prune old frames (simple heuristic: remove anything older than 60 frames)
            // Note: This doesn't handle UInt wrap-around logic perfectly but works for short sessions
            val threshold = if (maxFrameNumberSeen > 60u) maxFrameNumberSeen - 60u else 0u

            val iterator = pendingFrames.keys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (key < threshold) {
                    iterator.remove()
                    frameFragmentCounts.remove(key)
                }
            }
        }

        val fragments = pendingFrames.computeIfAbsent(frameNum) { ConcurrentHashMap() }
        frameFragmentCounts.putIfAbsent(frameNum, header.fragmentCount)

        fragments[header.fragmentIndex] = payload

        val expectedCount = frameFragmentCounts[frameNum]!!.toInt()

        if (fragments.size == expectedCount) {
            // Assemble
            val outputStream = ByteArrayOutputStream()
            var complete = true
            for (i in 0 until expectedCount) {
                val frag = fragments[i.toUShort()]
                if (frag == null) {
                    complete = false
                    break
                }
                outputStream.write(frag)
            }

            if (complete) {
                val fullFrameData = outputStream.toByteArray()
                pendingFrames.remove(frameNum)
                frameFragmentCounts.remove(frameNum)
                return ReassembledFrame(header, fullFrameData)
            }
        }

        return null
    }
}
