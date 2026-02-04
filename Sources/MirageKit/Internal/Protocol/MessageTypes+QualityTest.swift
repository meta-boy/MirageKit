//
//  MessageTypes+QualityTest.swift
//  MirageKit
//
//  Created by Ethan Lipnik on 2/2/26.
//
//  Control messages for quality testing.
//

import Foundation

package struct QualityTestRequestMessage: Codable {
    package let testID: UUID
    package let plan: MirageQualityTestPlan
    package let payloadBytes: Int

    package init(testID: UUID, plan: MirageQualityTestPlan, payloadBytes: Int) {
        self.testID = testID
        self.plan = plan
        self.payloadBytes = payloadBytes
    }
}

package struct QualityTestResultMessage: Codable {
    package let testID: UUID
    package let benchmarkWidth: Int
    package let benchmarkHeight: Int
    package let benchmarkFrameRate: Int
    package let encodeMs: Double?
    package let benchmarkVersion: Int

    package init(
        testID: UUID,
        benchmarkWidth: Int,
        benchmarkHeight: Int,
        benchmarkFrameRate: Int,
        encodeMs: Double?,
        benchmarkVersion: Int
    ) {
        self.testID = testID
        self.benchmarkWidth = benchmarkWidth
        self.benchmarkHeight = benchmarkHeight
        self.benchmarkFrameRate = benchmarkFrameRate
        self.encodeMs = encodeMs
        self.benchmarkVersion = benchmarkVersion
    }
}

package struct QualityProbeRequestMessage: Codable {
    package let probeID: UUID
    package let width: Int
    package let height: Int
    package let frameRate: Int
    package let pixelFormat: MiragePixelFormat

    package init(
        probeID: UUID,
        width: Int,
        height: Int,
        frameRate: Int,
        pixelFormat: MiragePixelFormat
    ) {
        self.probeID = probeID
        self.width = width
        self.height = height
        self.frameRate = frameRate
        self.pixelFormat = pixelFormat
    }
}

package struct QualityProbeResultMessage: Codable {
    package let probeID: UUID
    package let width: Int
    package let height: Int
    package let frameRate: Int
    package let pixelFormat: MiragePixelFormat
    package let encodeMs: Double?

    package init(
        probeID: UUID,
        width: Int,
        height: Int,
        frameRate: Int,
        pixelFormat: MiragePixelFormat,
        encodeMs: Double?
    ) {
        self.probeID = probeID
        self.width = width
        self.height = height
        self.frameRate = frameRate
        self.pixelFormat = pixelFormat
        self.encodeMs = encodeMs
    }
}
