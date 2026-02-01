//
//  MessageTypes+Cursor.swift
//  MirageKit
//
//  Created by Ethan Lipnik on 1/24/26.
//
//  Message type definitions.
//

import Foundation

// MARK: - Cursor Messages

/// Cursor state update sent from host to client when cursor appearance changes
struct CursorUpdateMessage: Codable {
    /// The stream this cursor update applies to
    let streamID: StreamID
    /// The current cursor type on the host
    let cursorType: MirageCursorType
    /// Whether the cursor is currently within the streamed window bounds
    let isVisible: Bool
}

/// Cursor position update sent from host to client for secondary display sync
struct CursorPositionUpdateMessage: Codable {
    /// The stream this cursor position applies to
    let streamID: StreamID
    /// Normalized cursor position (0-1, top-left origin)
    let normalizedX: Float
    let normalizedY: Float
    /// Whether the cursor is currently within the streamed window bounds
    let isVisible: Bool
}
