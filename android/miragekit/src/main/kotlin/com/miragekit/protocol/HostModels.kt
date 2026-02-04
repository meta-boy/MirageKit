package com.miragekit.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType {
    mac,
    iPad,
    iPhone,
    vision,
    unknown
}

@Serializable
data class MirageHostCapabilities(
    val maxStreams: Int = 4,
    val supportsHEVC: Boolean = true,
    val supportsP3ColorSpace: Boolean = true,
    val maxFrameRate: Int = 120,
    val protocolVersion: Int = 1,
    val deviceID: String? = null // UUID string
) {
    companion object {
        fun fromTxtRecord(txtRecord: Map<String, String>): MirageHostCapabilities {
            return MirageHostCapabilities(
                maxStreams = txtRecord["maxStreams"]?.toIntOrNull() ?: 4,
                supportsHEVC = txtRecord["hevc"] == "1",
                supportsP3ColorSpace = txtRecord["p3"] == "1",
                maxFrameRate = txtRecord["maxFps"]?.toIntOrNull() ?: 120,
                protocolVersion = txtRecord["proto"]?.toIntOrNull() ?: 1,
                deviceID = txtRecord["did"]
            )
        }
    }
}

data class MirageHost(
    val id: String, // UUID
    val name: String,
    val deviceType: DeviceType,
    val endpoint: String, // IP or Hostname
    val port: Int,
    val capabilities: MirageHostCapabilities
)

@Serializable
data class MirageApplication(
    val id: Int,
    val bundleIdentifier: String?,
    val name: String,
    val iconData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MirageApplication

        if (id != other.id) return false
        if (bundleIdentifier != other.bundleIdentifier) return false
        if (name != other.name) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (bundleIdentifier?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class MirageWindow(
    val id: UInt, // WindowID = UInt32
    val title: String?,
    val application: MirageApplication?,
    val frame: List<Double>, // [x, y, width, height]
    val isOnScreen: Boolean,
    val windowLayer: Int,
    val tabCount: Int = 1
)
