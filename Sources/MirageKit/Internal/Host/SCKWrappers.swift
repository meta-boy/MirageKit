import Foundation
import CoreMedia

#if os(macOS)
import ScreenCaptureKit

/// Wrapper to send SCWindow across actor boundaries safely
/// SCWindow is a ScreenCaptureKit type that's internally thread-safe
struct SCWindowWrapper: @unchecked Sendable {
    let window: SCWindow
}

/// Wrapper to send SCRunningApplication across actor boundaries safely
struct SCApplicationWrapper: @unchecked Sendable {
    let application: SCRunningApplication
}

/// Wrapper to send SCDisplay across actor boundaries safely
struct SCDisplayWrapper: @unchecked Sendable {
    let display: SCDisplay
}

/// Wrapper to send CMSampleBuffer across actor boundaries safely
/// CMSampleBuffer is a Core Media type that's internally thread-safe
struct SampleBufferWrapper: @unchecked Sendable {
    let buffer: CMSampleBuffer
}

#endif
