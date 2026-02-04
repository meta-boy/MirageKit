package com.miragekit.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import com.miragekit.protocol.FrameFlags
import com.miragekit.protocol.FrameHeader
import java.nio.ByteBuffer

class MirageDecoder(private val surface: Surface) {
    private var codec: MediaCodec? = null
    private var isConfigured = false
    private var currentWidth = 1920
    private var currentHeight = 1080

    fun configure(width: Int, height: Int) {
        if (codec != null) {
            stop()
        }

        currentWidth = width
        currentHeight = height

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
        // Try to enable low latency if possible (this key is hidden or API dependent, skipping for safety)

        try {
            codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            codec?.configure(format, surface, null, 0)
            codec?.start()
            isConfigured = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            codec?.stop()
            codec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        codec = null
        isConfigured = false
    }

    fun decodeFrame(frame: ReassembledFrame) {
        // Check for resolution change or initial configuration
        val frameWidth = frame.header.contentRectWidth.toInt()
        val frameHeight = frame.header.contentRectHeight.toInt()

        // If the frame has valid dimensions and they differ from current config, reconfigure
        if (frameWidth > 0 && frameHeight > 0 && (frameWidth != currentWidth || frameHeight != currentHeight || !isConfigured)) {
            configure(frameWidth, frameHeight)
        } else if (!isConfigured) {
             // Fallback if first frame has no valid rect (unlikely for I-frame)
             configure(1920, 1080)
        }

        val codec = codec ?: return

        try {
            val index = codec.dequeueInputBuffer(10000)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index)
                if (buffer != null) {
                    buffer.clear()
                    buffer.put(frame.data)

                    var flags = 0
                    if ((frame.header.flags.toUInt() and FrameFlags.KEYFRAME.toUInt()) != 0u) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    if ((frame.header.flags.toUInt() and FrameFlags.PARAMETER_SET.toUInt()) != 0u) {
                        flags = flags or MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                    }

                    // Timestamp in FrameHeader is nanoseconds. MediaCodec takes microseconds.
                    val presentationTimeUs = frame.header.timestamp.toLong() / 1000

                    codec.queueInputBuffer(index, 0, frame.data.size, presentationTimeUs, flags)
                }
            }

            val info = MediaCodec.BufferInfo()
            var outputIndex = codec.dequeueOutputBuffer(info, 0)
            while (outputIndex >= 0) {
                codec.releaseOutputBuffer(outputIndex, true)
                outputIndex = codec.dequeueOutputBuffer(info, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
