import CoreGraphics
import CoreMedia
import CoreVideo
import VideoToolbox

#if os(macOS)

final class PixelBufferScaler {
    private var transferSession: VTPixelTransferSession?
    private var pixelBufferPool: CVPixelBufferPool?
    private var outputSize: CGSize = .zero
    private var outputFormatDescription: CMVideoFormatDescription?
    private let pixelFormat: OSType

    init(pixelFormat: OSType = kCVPixelFormatType_ARGB2101010LEPacked) {
        self.pixelFormat = pixelFormat
    }

    func updateOutputSize(_ size: CGSize) {
        guard size.width > 0, size.height > 0 else {
            outputSize = .zero
            pixelBufferPool = nil
            outputFormatDescription = nil
            return
        }

        guard size != outputSize else { return }
        outputSize = size
        outputFormatDescription = nil

        let poolAttributes: [CFString: Any] = [
            kCVPixelBufferPoolMinimumBufferCountKey: 3
        ]
        let bufferAttributes: [CFString: Any] = [
            kCVPixelBufferPixelFormatTypeKey: pixelFormat,
            kCVPixelBufferWidthKey: Int(size.width),
            kCVPixelBufferHeightKey: Int(size.height),
            kCVPixelBufferIOSurfacePropertiesKey: [:] as CFDictionary,
            kCVPixelBufferMetalCompatibilityKey: true
        ]

        var pool: CVPixelBufferPool?
        CVPixelBufferPoolCreate(
            kCFAllocatorDefault,
            poolAttributes as CFDictionary,
            bufferAttributes as CFDictionary,
            &pool
        )
        pixelBufferPool = pool
    }

    func scale(pixelBuffer: CVPixelBuffer, outputSize: CGSize) -> CVPixelBuffer? {
        updateOutputSize(outputSize)
        guard let pool = pixelBufferPool else { return nil }

        var outputBuffer: CVPixelBuffer?
        let status = CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, pool, &outputBuffer)
        guard status == kCVReturnSuccess, let outputBuffer else { return nil }
        guard let session = ensureTransferSession() else { return nil }

        let transferStatus = VTPixelTransferSessionTransferImage(session, from: pixelBuffer, to: outputBuffer)
        guard transferStatus == noErr else { return nil }

        return outputBuffer
    }

    func sampleBuffer(from pixelBuffer: CVPixelBuffer, timingInfo: CMSampleTimingInfo) -> CMSampleBuffer? {
        if outputFormatDescription == nil {
            var formatDescription: CMVideoFormatDescription?
            let status = CMVideoFormatDescriptionCreateForImageBuffer(
                allocator: kCFAllocatorDefault,
                imageBuffer: pixelBuffer,
                formatDescriptionOut: &formatDescription
            )
            guard status == noErr else { return nil }
            outputFormatDescription = formatDescription
        }

        guard let formatDescription = outputFormatDescription else { return nil }

        var sampleBuffer: CMSampleBuffer?
        var timing = timingInfo
        let status = CMSampleBufferCreateReadyWithImageBuffer(
            allocator: kCFAllocatorDefault,
            imageBuffer: pixelBuffer,
            formatDescription: formatDescription,
            sampleTiming: &timing,
            sampleBufferOut: &sampleBuffer
        )
        guard status == noErr else { return nil }
        return sampleBuffer
    }

    private func ensureTransferSession() -> VTPixelTransferSession? {
        if let transferSession {
            return transferSession
        }

        var session: VTPixelTransferSession?
        let status = VTPixelTransferSessionCreate(
            allocator: kCFAllocatorDefault,
            pixelTransferSessionOut: &session
        )
        guard status == noErr, let session else { return nil }

        let scalingMode = kVTScalingMode_Normal
        VTSessionSetProperty(session, key: kVTPixelTransferPropertyKey_ScalingMode, value: scalingMode)
        transferSession = session
        return session
    }
}

#endif
