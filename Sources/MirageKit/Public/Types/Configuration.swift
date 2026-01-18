import Foundation
import CoreGraphics

// MARK: - Encoder Configuration

/// Configuration for video encoding on the host
public struct MirageEncoderConfiguration: Sendable {
    /// Video codec to use
    public var codec: MirageVideoCodec

    /// Target frame rate
    public var targetFrameRate: Int

    /// Keyframe interval (in frames)
    public var keyFrameInterval: Int

    /// Color space for encoding
    public var colorSpace: MirageColorSpace

    /// Scale factor for retina displays
    public var scaleFactor: CGFloat

    /// Pixel format for capture and encode
    public var pixelFormat: MiragePixelFormat

    /// Quality level for encoded frames (0.0-1.0, where 1.0 is maximum quality)
    /// Lower values reduce frame size significantly with minimal visual impact
    /// Default 0.8 reduces frame size by ~40-50% for better UDP reliability
    public var keyframeQuality: Float

    public init(
        codec: MirageVideoCodec = .hevc,
        targetFrameRate: Int = 60,
        keyFrameInterval: Int = 600,
        colorSpace: MirageColorSpace = .displayP3,
        scaleFactor: CGFloat = 2.0,
        pixelFormat: MiragePixelFormat = .bgr10a2,
        keyframeQuality: Float = 0.8  // Lower quality yields smaller frames for better UDP reliability
    ) {
        self.codec = codec
        self.targetFrameRate = targetFrameRate
        self.keyFrameInterval = keyFrameInterval
        self.colorSpace = colorSpace
        self.scaleFactor = scaleFactor
        self.pixelFormat = pixelFormat
        self.keyframeQuality = keyframeQuality
    }

    /// Default configuration for high-bandwidth local network
    public static let highQuality = MirageEncoderConfiguration(
        targetFrameRate: 120,
        keyFrameInterval: 600,
        pixelFormat: .bgr10a2,
        keyframeQuality: 1.0
    )

    /// Default configuration for lower bandwidth
    public static let balanced = MirageEncoderConfiguration(
        targetFrameRate: 120,
        keyFrameInterval: 600,
        pixelFormat: .bgra8,
        keyframeQuality: 0.75
    )

    /// Configuration optimized for low-latency text applications
    /// Achieves NVIDIA GameStream-competitive latency through:
    /// - Longer keyframe interval (fewer large frames to fragment)
    /// - Lower quality (30-50% smaller frames)
    /// - Aggressive frame skipping (always-latest-frame strategy)
    /// - 8-bit pixel format for faster encode
    /// Best for: IDEs, text editors, terminals - any app where responsiveness matters
    public static let lowLatency = MirageEncoderConfiguration(
        codec: .hevc,
        targetFrameRate: 120,
        keyFrameInterval: 600,
        colorSpace: .displayP3,
        scaleFactor: 2.0,
        pixelFormat: .bgra8,
        keyframeQuality: 0.85
    )

    /// Create a copy with multiple encoder setting overrides
    /// Use this for full client control over encoding parameters
    public func withOverrides(
        keyFrameInterval: Int? = nil,
        keyframeQuality: Float? = nil,
        pixelFormat: MiragePixelFormat? = nil
    ) -> MirageEncoderConfiguration {
        var config = self
        if let interval = keyFrameInterval {
            config.keyFrameInterval = interval
        }
        if let quality = keyframeQuality {
            config.keyframeQuality = quality
        }
        if let pixelFormat {
            config.pixelFormat = pixelFormat
        }
        return config
    }

    /// Create a copy with a different target frame rate
    /// Use this to override the default based on client capability
    public func withTargetFrameRate(_ newFrameRate: Int) -> MirageEncoderConfiguration {
        var config = self
        config.targetFrameRate = newFrameRate
        return config
    }
}

/// Video codec options
public enum MirageVideoCodec: String, Sendable, CaseIterable, Codable {
    case hevc = "hvc1"
    case h264 = "avc1"

    public var displayName: String {
        switch self {
        case .hevc: return "HEVC (H.265)"
        case .h264: return "H.264"
        }
    }
}

/// Color space options
public enum MirageColorSpace: String, Sendable, CaseIterable, Codable {
    case sRGB = "sRGB"
    case displayP3 = "P3"
    // TODO: HDR support - requires proper virtual display EDR configuration
    // case hdr = "HDR"  // Rec. 2020 with PQ transfer function

    public var displayName: String {
        switch self {
        case .sRGB: return "sRGB"
        case .displayP3: return "Display P3"
        // case .hdr: return "HDR (Rec. 2020)"
        }
    }
}

/// Pixel format for stream capture and encoding.
public enum MiragePixelFormat: String, Sendable, CaseIterable, Codable {
    case bgr10a2
    case bgra8

    public var displayName: String {
        switch self {
        case .bgr10a2: return "10-bit"
        case .bgra8: return "8-bit"
        }
    }
}

// MARK: - Network Configuration

/// Configuration for network connections
public struct MirageNetworkConfiguration: Sendable {
    /// Bonjour service type
    public var serviceType: String

    /// Control channel port (TCP) - 0 for auto-assign
    public var controlPort: UInt16

    /// Data channel port (UDP) - 0 for auto-assign
    public var dataPort: UInt16

    /// Whether to enable TLS encryption
    public var enableTLS: Bool

    /// Connection timeout in seconds
    public var connectionTimeout: TimeInterval

    /// Maximum UDP packet size (Mirage header + payload).
    /// Keep <= 1232 to stay under IPv6 minimum MTU once IP/UDP headers are added.
    public var maxPacketSize: Int

    /// Whether to enable peer-to-peer WiFi (AWDL) for discovery and connections.
    /// When enabled, devices can connect directly without needing the same WiFi network.
    public var enablePeerToPeer: Bool

    public init(
        serviceType: String = MirageKit.serviceType,
        controlPort: UInt16 = 0,
        dataPort: UInt16 = 0,
        enableTLS: Bool = true,
        connectionTimeout: TimeInterval = 10,
        maxPacketSize: Int = MirageDefaultMaxPacketSize,
        enablePeerToPeer: Bool = true
    ) {
        self.serviceType = serviceType
        self.controlPort = controlPort
        self.dataPort = dataPort
        self.enableTLS = enableTLS
        self.connectionTimeout = connectionTimeout
        self.maxPacketSize = maxPacketSize
        self.enablePeerToPeer = enablePeerToPeer
    }

    public static let `default` = MirageNetworkConfiguration()
}

// MARK: - Quality Presets

/// Quality preset for quick configuration.
/// Presets define encoder quality defaults that can be overridden by the client.
public enum MirageQualityPreset: String, Sendable, CaseIterable, Codable {
    case ultra       // Highest quality
    case high        // High quality
    case medium      // Balanced quality
    case low         // Low quality
    case lowLatency  // Optimized for text apps - aggressive frame skipping, full quality

    public var displayName: String {
        switch self {
        case .ultra: return "Ultra"
        case .high: return "High"
        case .medium: return "Medium"
        case .low: return "Low"
        case .lowLatency: return "Low Latency"
        }
    }

    public var encoderConfiguration: MirageEncoderConfiguration {
        encoderConfiguration(for: 60)
    }

    public func encoderConfiguration(for frameRate: Int) -> MirageEncoderConfiguration {
        let isHighRefresh = frameRate >= 120
        switch self {
        case .ultra:
            return MirageEncoderConfiguration(
                pixelFormat: .bgr10a2,
                keyframeQuality: 1.0
            )
        case .high:
            return MirageEncoderConfiguration(
                pixelFormat: .bgr10a2,
                keyframeQuality: isHighRefresh ? 0.88 : 0.95
            )
        case .medium:
            return MirageEncoderConfiguration(
                colorSpace: .displayP3,
                pixelFormat: .bgr10a2,
                keyframeQuality: isHighRefresh ? 0.70 : 0.80
            )
        case .low:
            return MirageEncoderConfiguration(
                colorSpace: .sRGB,
                pixelFormat: .bgra8,
                keyframeQuality: isHighRefresh ? 0.18 : 0.24
            )
        case .lowLatency:
            return .lowLatency
        }
    }

}
