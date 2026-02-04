package com.miragekit.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.json.*

@Serializable
enum class MirageMouseButton(val value: Int) {
    Left(0), Right(1), Middle(2), Button3(3), Button4(4);
}

@Serializable
enum class MirageScrollPhase(val value: Int) {
    None(0), Began(1), Changed(2), Ended(3), Cancelled(4), MayBegin(5);
}

@Serializable
data class MirageKeyEvent(
    val keyCode: UShort,
    val characters: String? = null,
    val charactersIgnoringModifiers: String? = null,
    val modifiers: UInt = 0u,
    val isRepeat: Boolean = false,
    val timestamp: Double
)

@Serializable
data class MirageMouseEvent(
    val button: MirageMouseButton = MirageMouseButton.Left,
    val location: List<Double>, // [x, y]
    val clickCount: Int = 1,
    val modifiers: UInt = 0u,
    val pressure: Double = 1.0,
    val timestamp: Double
)

@Serializable
data class MirageScrollEvent(
    val deltaX: Double,
    val deltaY: Double,
    val location: List<Double>? = null,
    val phase: MirageScrollPhase = MirageScrollPhase.None,
    val momentumPhase: MirageScrollPhase = MirageScrollPhase.None,
    val modifiers: UInt = 0u,
    val isPrecise: Boolean = false,
    val timestamp: Double
)

@Serializable
data class MirageMagnifyEvent(
    val magnification: Double,
    val phase: MirageScrollPhase = MirageScrollPhase.None,
    val timestamp: Double
)

@Serializable
data class MirageRotateEvent(
    val rotation: Double,
    val phase: MirageScrollPhase = MirageScrollPhase.None,
    val timestamp: Double
)

@Serializable
data class MirageResizeEvent(
    val windowID: UInt,
    val newSize: List<Double>, // [width, height]
    val scaleFactor: Double,
    val pixelSize: List<Double>,
    val timestamp: Double
)

@Serializable
data class MirageRelativeResizeEvent(
    val windowID: UInt,
    val aspectRatio: Double,
    val relativeScale: Double,
    val clientScreenSize: List<Double>,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val timestamp: Double
)

@Serializable
data class MiragePixelResizeEvent(
    val windowID: UInt,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val timestamp: Double
)

// Wrapper for input events to match Swift enum encoding
@Serializable(with = MirageInputEventSerializer::class)
sealed class MirageInputEvent {
    data class KeyDown(val event: MirageKeyEvent) : MirageInputEvent()
    data class KeyUp(val event: MirageKeyEvent) : MirageInputEvent()
    data class FlagsChanged(val modifiers: UInt) : MirageInputEvent()
    data class MouseDown(val event: MirageMouseEvent) : MirageInputEvent()
    data class MouseUp(val event: MirageMouseEvent) : MirageInputEvent()
    data class MouseMoved(val event: MirageMouseEvent) : MirageInputEvent()
    data class MouseDragged(val event: MirageMouseEvent) : MirageInputEvent()
    data class RightMouseDown(val event: MirageMouseEvent) : MirageInputEvent()
    data class RightMouseUp(val event: MirageMouseEvent) : MirageInputEvent()
    data class RightMouseDragged(val event: MirageMouseEvent) : MirageInputEvent()
    data class OtherMouseDown(val event: MirageMouseEvent) : MirageInputEvent()
    data class OtherMouseUp(val event: MirageMouseEvent) : MirageInputEvent()
    data class OtherMouseDragged(val event: MirageMouseEvent) : MirageInputEvent()
    data class ScrollWheel(val event: MirageScrollEvent) : MirageInputEvent()
    data class Magnify(val event: MirageMagnifyEvent) : MirageInputEvent()
    data class Rotate(val event: MirageRotateEvent) : MirageInputEvent()
    data class WindowResize(val event: MirageResizeEvent) : MirageInputEvent()
    data class RelativeResize(val event: MirageRelativeResizeEvent) : MirageInputEvent()
    data class PixelResize(val event: MiragePixelResizeEvent) : MirageInputEvent()
    object WindowFocus : MirageInputEvent()
}

object MirageInputEventSerializer : KSerializer<MirageInputEvent> {
    override val descriptor = buildClassSerialDescriptor("MirageInputEvent")

    override fun serialize(encoder: Encoder, value: MirageInputEvent) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val element = when (value) {
            is MirageInputEvent.KeyDown -> JsonObject(mapOf("keyDown" to Json.encodeToJsonElement(MirageKeyEvent.serializer(), value.event)))
            is MirageInputEvent.KeyUp -> JsonObject(mapOf("keyUp" to Json.encodeToJsonElement(MirageKeyEvent.serializer(), value.event)))
            is MirageInputEvent.FlagsChanged -> JsonObject(mapOf("flagsChanged" to JsonPrimitive(value.modifiers.toLong())))
            is MirageInputEvent.MouseDown -> JsonObject(mapOf("mouseDown" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.MouseUp -> JsonObject(mapOf("mouseUp" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.MouseMoved -> JsonObject(mapOf("mouseMoved" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.MouseDragged -> JsonObject(mapOf("mouseDragged" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.RightMouseDown -> JsonObject(mapOf("rightMouseDown" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.RightMouseUp -> JsonObject(mapOf("rightMouseUp" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.RightMouseDragged -> JsonObject(mapOf("rightMouseDragged" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.OtherMouseDown -> JsonObject(mapOf("otherMouseDown" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.OtherMouseUp -> JsonObject(mapOf("otherMouseUp" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.OtherMouseDragged -> JsonObject(mapOf("otherMouseDragged" to Json.encodeToJsonElement(MirageMouseEvent.serializer(), value.event)))
            is MirageInputEvent.ScrollWheel -> JsonObject(mapOf("scrollWheel" to Json.encodeToJsonElement(MirageScrollEvent.serializer(), value.event)))
            is MirageInputEvent.Magnify -> JsonObject(mapOf("magnify" to Json.encodeToJsonElement(MirageMagnifyEvent.serializer(), value.event)))
            is MirageInputEvent.Rotate -> JsonObject(mapOf("rotate" to Json.encodeToJsonElement(MirageRotateEvent.serializer(), value.event)))
            is MirageInputEvent.WindowResize -> JsonObject(mapOf("windowResize" to Json.encodeToJsonElement(MirageResizeEvent.serializer(), value.event)))
            is MirageInputEvent.RelativeResize -> JsonObject(mapOf("relativeResize" to Json.encodeToJsonElement(MirageRelativeResizeEvent.serializer(), value.event)))
            is MirageInputEvent.PixelResize -> JsonObject(mapOf("pixelResize" to Json.encodeToJsonElement(MiragePixelResizeEvent.serializer(), value.event)))
            is MirageInputEvent.WindowFocus -> JsonObject(mapOf("windowFocus" to JsonObject(emptyMap())))
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): MirageInputEvent {
        throw SerializationException("Deserialization of MirageInputEvent is not supported (Client only sends input)")
    }
}

@Serializable
data class InputEventMessage(
    val streamID: UShort,
    val event: MirageInputEvent
)
