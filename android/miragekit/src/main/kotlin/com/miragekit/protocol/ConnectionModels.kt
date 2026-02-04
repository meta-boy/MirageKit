package com.miragekit.protocol

import kotlinx.serialization.Serializable

@Serializable
data class HelloMessage(
    val deviceID: String, // UUID string
    val deviceName: String,
    val deviceType: DeviceType,
    val protocolVersion: Int,
    val capabilities: MirageHostCapabilities,
    val iCloudUserID: String? = null
)

@Serializable
data class HelloResponseMessage(
    val accepted: Boolean,
    val hostID: String, // UUID string
    val hostName: String,
    val requiresAuth: Boolean,
    val dataPort: UShort
)

@Serializable
enum class DisconnectReason {
    userRequested,
    timeout,
    error,
    hostShutdown,
    authFailed
}

@Serializable
data class DisconnectMessage(
    val reason: DisconnectReason,
    val message: String? = null
)
