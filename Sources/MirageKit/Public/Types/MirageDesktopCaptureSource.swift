//
//  MirageDesktopCaptureSource.swift
//  MirageKit
//
//  Created by Ethan Lipnik on 1/21/26.
//

import Foundation

public enum MirageDesktopCaptureSource: String, Sendable, CaseIterable, Codable {
    case virtualDisplay
    case mainDisplay

    public var displayName: String {
        switch self {
        case .virtualDisplay:
            "Virtual Display"
        case .mainDisplay:
            "Main Display"
        }
    }
}
