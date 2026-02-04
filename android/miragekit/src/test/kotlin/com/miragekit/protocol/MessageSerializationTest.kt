package com.miragekit.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class MessageSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testHelloMessage() {
        val deviceID = "550e8400-e29b-41d4-a716-446655440000"
        val msg = HelloMessage(
            deviceID = deviceID,
            deviceName = "Android Device",
            deviceType = DeviceType.unknown,
            protocolVersion = 1,
            capabilities = MirageHostCapabilities()
        )

        val encoded = json.encodeToString(msg)
        // Basic check to ensure it encodes without error and contains key fields
        assert(encoded.contains("\"deviceID\":\"$deviceID\""))
        assert(encoded.contains("\"deviceName\":\"Android Device\""))
        assert(encoded.contains("\"deviceType\":\"unknown\""))
    }

    @Test
    fun testInputEventSerialization() {
        val ts = 123456789.0
        val event = MirageInputEvent.KeyDown(
            MirageKeyEvent(
                keyCode = 0u,
                characters = "a",
                timestamp = ts
            )
        )

        val inputMsg = InputEventMessage(0u, event)
        val encoded = json.encodeToString(inputMsg)

        // Verify structure matches Swift expected format: {"streamID":0,"event":{"keyDown":{"keyCode":0,"characters":"a",...}}}
        assert(encoded.contains("\"streamID\":0"))
        assert(encoded.contains("\"keyDown\""))
        assert(encoded.contains("\"keyCode\":0"))
        assert(encoded.contains("\"characters\":\"a\""))
    }
}
