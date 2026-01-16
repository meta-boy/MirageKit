import Foundation
import CoreGraphics

// MARK: - Encoder Configuration

/// Configuration for video encoding on the host
public struct MirageEncoderConfiguration: Sendable {
    /// Video codec to use
    public var codec: MirageVideoCodec

    /// Maximum bitrate in bits per second
    public var maxBitrate: Int

    /// Minimum bitrate in bits per second
    public var minBitrate: Int

    /// Target frame rate
    public var targetFrameRate: Int

    /// Keyframe interval (in frames)
    public var keyFrameInterval: Int

    /// Color space for encoding
    public var colorSpace: MirageColorSpace

    /// Scale factor for retina displays
    public var scaleFactor: CGFloat

    /// Whether to enable adaptive bitrate
    public var enableAdaptiveBitrate: Bool

    /// Quality level for keyframes (0.0-1.0, where 1.0 is maximum quality)
    /// Lower values reduce keyframe size significantly with minimal visual impact
    /// Default 0.8 reduces keyframe size by ~40-50% for better UDP reliability
    public var keyframeQuality: Float

    public init(
        codec: MirageVideoCodec = .hevc,
        maxBitrate: Int = 100_000_000,
        minBitrate: Int = 5_000_000,
        targetFrameRate: Int = 60,  // 60fps is sufficient for productivity apps
        keyFrameInterval: Int = 600,  // 10 seconds at 60fps - minimizes periodic lag spikes
        colorSpace: MirageColorSpace = .displayP3,
        scaleFactor: CGFloat = 2.0,
        enableAdaptiveBitrate: Bool = true,
        keyframeQuality: Float = 0.8  // Lower quality yields smaller keyframes for better UDP reliability
    ) {
        self.codec = codec
        self.maxBitrate = maxBitrate
        self.minBitrate = minBitrate
        self.targetFrameRate = targetFrameRate
        self.keyFrameInterval = keyFrameInterval
        self.colorSpace = colorSpace
        self.scaleFactor = scaleFactor
        self.enableAdaptiveBitrate = enableAdaptiveBitrate
        self.keyframeQuality = keyframeQuality
    }

    /// Default configuration for high-bandwidth local network
    public static let highQuality = MirageEncoderConfiguration(
        maxBitrate: 200_000_000,
        minBitrate: 50_000_000,
        targetFrameRate: 120,
        keyFrameInterval: 600  // 10 seconds - minimizes lag spikes
    )

    /// Default configuration for lower bandwidth
    public static let balanced = MirageEncoderConfiguration(
        maxBitrate: 50_000_000,
        minBitrate: 10_000_000,
        targetFrameRate: 60,
        keyFrameInterval: 600  // 10 seconds - minimizes lag spikes
    )

    /// Configuration optimized for low-latency text applications
    /// Achieves NVIDIA GameStream-competitive latency through:
    /// - Longer keyframe interval (fewer large frames to fragment)
    /// - Lower keyframe quality (30-50% smaller keyframes)
    /// - Aggressive frame skipping (always-latest-frame strategy)
    /// Best for: IDEs, text editors, terminals - any app where responsiveness matters
    public static let lowLatency = MirageEncoderConfiguration(
        codec: .hevc,
        maxBitrate: 150_000_000,        // High bitrate for quality
        minBitrate: 20_000_000,         // Higher floor to maintain sharpness
        targetFrameRate: 120,
        keyFrameInterval: 600,          // 10 seconds - minimizes lag spikes
        colorSpace: .displayP3,         // Full P3 color
        scaleFactor: 2.0,
        enableAdaptiveBitrate: true,
        keyframeQuality: 0.85           // Reduce keyframe size by ~30-50% (visually lossless)
    )

    /// Create a copy with a different max bitrate
    /// Use this to override the default bitrate based on client network capabilities
    public func withMaxBitrate(_ newMaxBitrate: Int) -> MirageEncoderConfiguration {
        var config = self
        config.maxBitrate = newMaxBitrate
        // Also update minBitrate to be proportional (10% of max as a floor)
        config.minBitrate = max(5_000_000, newMaxBitrate / 10)
        return config
    }

    /// Create a copy with multiple encoder setting overrides
    /// Use this for full client control over encoding parameters
    public func withOverrides(
        maxBitrate: Int? = nil,
        keyFrameInterval: Int? = nil,
        keyframeQuality: Float? = nil
    ) -> MirageEncoderConfiguration {
        var config = self
        if let bitrate = maxBitrate {
            config.maxBitrate = bitrate
            config.minBitrate = max(5_000_000, bitrate / 10)
        }
        if let interval = keyFrameInterval {
            config.keyFrameInterval = interval
        }
        if let quality = keyframeQuality {
            config.keyframeQuality = quality
        }
        return config
    }

    /// Create a copy with a different target frame rate
    /// Use this to enable 120fps streaming on P2P connections with capable displays
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
/// Presets define maximums; streams still adapt down for motion and feedback.
public enum MirageQualityPreset: String, Sendable, CaseIterable, Codable {
    case ultra       // 120fps cap, highest bitrate
    case high        // 120fps cap, high bitrate
    case medium      // 60fps cap
    case low         // 30fps cap
    case adaptive    // Balanced caps with adaptive bitrate
    case lowLatency  // Optimized for text apps - aggressive frame skipping, full quality

    public var displayName: String {
        switch self {
        case .ultra: return "Ultra (120fps)"
        case .high: return "High (120fps)"
        case .medium: return "Medium (60fps)"
        case .low: return "Low (30fps)"
        case .adaptive: return "Adaptive"
        case .lowLatency: return "Low Latency"
        }
    }

    public var encoderConfiguration: MirageEncoderConfiguration {
        switch self {
        case .ultra:
            return MirageEncoderConfiguration(
                maxBitrate: 200_000_000,
                targetFrameRate: 120,
                enableAdaptiveBitrate: true
            )
        case .high:
            return MirageEncoderConfiguration(
                maxBitrate: 100_000_000,
                targetFrameRate: 120,
                enableAdaptiveBitrate: true
            )
        case .medium:
            return MirageEncoderConfiguration(
                maxBitrate: 50_000_000,
                targetFrameRate: 60,
                enableAdaptiveBitrate: true
            )
        case .low:
            return MirageEncoderConfiguration(
                maxBitrate: 20_000_000,
                targetFrameRate: 30,
                enableAdaptiveBitrate: true
            )
        case .adaptive:
            return MirageEncoderConfiguration(
                maxBitrate: 150_000_000,
                targetFrameRate: 120,
                enableAdaptiveBitrate: true
            )
        case .lowLatency:
            return .lowLatency
        }
    }
}
