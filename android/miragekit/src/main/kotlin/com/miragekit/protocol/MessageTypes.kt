package com.miragekit.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class ControlMessageType(val value: UByte) {
    // Connection management
    HELLO(0x01u),
    HELLO_RESPONSE(0x02u),
    DISCONNECT(0x03u),
    PING(0x04u),
    PONG(0x05u),

    // Authentication
    AUTH_REQUEST(0x10u),
    AUTH_CHALLENGE(0x11u),
    AUTH_RESPONSE(0x12u),
    AUTH_RESULT(0x13u),

    // Window management
    WINDOW_LIST_REQUEST(0x20u),
    WINDOW_LIST(0x21u),
    WINDOW_UPDATE(0x22u),
    START_STREAM(0x23u),
    STOP_STREAM(0x24u),
    STREAM_STARTED(0x25u),
    STREAM_STOPPED(0x26u),
    STREAM_METRICS_UPDATE(0x27u),

    // Input events
    INPUT_EVENT(0x30u),

    // Keyframe control
    KEYFRAME_REQUEST(0x42u),

    // Cursor updates
    CURSOR_UPDATE(0x50u),
    CURSOR_POSITION_UPDATE(0x51u),

    // Virtual display updates
    CONTENT_BOUNDS_UPDATE(0x60u),
    DISPLAY_RESOLUTION_CHANGE(0x61u),
    STREAM_SCALE_CHANGE(0x62u),
    STREAM_REFRESH_RATE_CHANGE(0x63u),

    // Session state
    SESSION_STATE_UPDATE(0x70u),
    UNLOCK_REQUEST(0x71u),
    UNLOCK_RESPONSE(0x72u),
    LOGIN_DISPLAY_READY(0x73u),
    LOGIN_DISPLAY_STOPPED(0x74u),

    // App-centric streaming
    APP_LIST_REQUEST(0x80u),
    APP_LIST(0x81u),
    SELECT_APP(0x82u),
    APP_STREAM_STARTED(0x83u),
    WINDOW_ADDED_TO_STREAM(0x84u),
    WINDOW_REMOVED_FROM_STREAM(0x85u),
    WINDOW_COOLDOWN_STARTED(0x86u),
    WINDOW_COOLDOWN_CANCELLED(0x87u),
    RETURN_TO_APP_SELECTION(0x88u),
    CLOSE_WINDOW_REQUEST(0x89u),
    STREAM_PAUSED(0x8Au),
    STREAM_RESUMED(0x8Bu),
    CANCEL_COOLDOWN(0x8Cu),
    WINDOW_RESIZABILITY_CHANGED(0x8Du),
    APP_TERMINATED(0x8Eu),

    // Menu bar
    MENU_BAR_UPDATE(0x90u),
    MENU_ACTION_REQUEST(0x91u),
    MENU_ACTION_RESULT(0x92u),

    // Desktop streaming
    START_DESKTOP_STREAM(0xA0u),
    STOP_DESKTOP_STREAM(0xA1u),
    DESKTOP_STREAM_STARTED(0xA2u),
    DESKTOP_STREAM_STOPPED(0xA3u),
    QUALITY_TEST_REQUEST(0xA4u),
    QUALITY_TEST_RESULT(0xA5u),
    QUALITY_PROBE_REQUEST(0xA6u),
    QUALITY_PROBE_RESULT(0xA7u),

    ERROR(0xFFu);

    companion object {
        fun fromValue(value: UByte): ControlMessageType? {
            return entries.find { it.value == value }
        }
    }
}

data class ControlMessage(
    val type: ControlMessageType,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlMessage

        if (type != other.type) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
