//
//  InputCapturingView+Gestures.swift
//  MirageKit
//
//  Created by Ethan Lipnik on 1/23/26.
//

#if os(iOS) || os(visionOS)
import UIKit

extension InputCapturingView {
    func setupGestureRecognizers() {
        // Long press gesture for immediate click detection
        // minimumPressDuration=0 fires immediately on touch down
        // allowableMovement is ignored after recognition, so .changed fires for all movement
        // This replaces both tap and pan gestures for unified mouse handling
        longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        longPressGesture.minimumPressDuration = 0
        longPressGesture.allowedTouchTypes = [
            NSNumber(value: UITouch.TouchType.direct.rawValue),
            NSNumber(value: UITouch.TouchType.indirectPointer.rawValue)
        ]
        longPressGesture.delegate = self
        addGestureRecognizer(longPressGesture)

        // Right-click gesture (secondary click with pointer)
        rightClickGesture = UITapGestureRecognizer(target: self, action: #selector(handleRightClick(_:)))
        rightClickGesture.buttonMaskRequired = .secondary
        rightClickGesture.allowedTouchTypes = [NSNumber(value: UITouch.TouchType.indirectPointer.rawValue)]
        addGestureRecognizer(rightClickGesture)

        // Scroll gesture - ONLY for direct touch (2-finger pan on screen)
        // Trackpad scrolling uses ScrollPhysicsCapturingView for native momentum/bounce
        scrollGesture = UIPanGestureRecognizer(target: self, action: #selector(handleScroll(_:)))
        scrollGesture.allowedScrollTypesMask = []  // Disable trackpad scroll handling
        scrollGesture.allowedTouchTypes = [NSNumber(value: UITouch.TouchType.direct.rawValue)]
        scrollGesture.minimumNumberOfTouches = 2
        scrollGesture.maximumNumberOfTouches = 2
        scrollGesture.delegate = self
        addGestureRecognizer(scrollGesture)

        // Hover gesture for pointer movement tracking
        hoverGesture = UIHoverGestureRecognizer(target: self, action: #selector(handleHover(_:)))
        addGestureRecognizer(hoverGesture)

        // Rotation gesture for direct touch
        directRotationGesture = UIRotationGestureRecognizer(target: self, action: #selector(handleDirectRotation(_:)))
        directRotationGesture.allowedTouchTypes = [NSNumber(value: UITouch.TouchType.direct.rawValue)]
        directRotationGesture.delegate = self
        addGestureRecognizer(directRotationGesture)
    }

    // MARK: - Coordinate Helpers

    /// Normalize a point to 0-1 range relative to view bounds
    /// The gesture location is in self's coordinate space, so normalize against self.bounds
    /// This ensures correct mapping regardless of nested view hierarchy offsets
    func normalizedLocation(_ point: CGPoint) -> CGPoint {
        // Normalize directly against our bounds - the view receiving the gesture
        // Scale factors cancel out: (point * scale) / (bounds * scale) = point / bounds
        guard bounds.width > 0 && bounds.height > 0 else {
            return CGPoint(x: 0.5, y: 0.5)  // Default to center if bounds not ready
        }

        var normalized = CGPoint(
            x: point.x / bounds.width,
            y: point.y / bounds.height
        )

        if dockSnapEnabled {
            // Snap cursor to bottom edge when in dock trigger zone (bottom 1%)
            // This allows users to easily open the iPad dock without precise edge targeting
            if normalized.y >= 0.99 {
                normalized.y = 1.0
            }
        }

        return normalized
    }

    /// Get combined modifiers from a gesture (at event time) and keyboard state
    /// Polls hardware keyboard for accurate modifier state to avoid stuck modifiers
    func modifiers(from gesture: UIGestureRecognizer) -> MirageModifierFlags {
        let hardwareAvailable = refreshModifiersForInput()
        if hardwareAvailable {
            let snapshot = keyboardModifiers
            sendModifierSnapshotIfNeeded(snapshot)
            return snapshot
        }

        let gestureModifiers = MirageModifierFlags(uiKeyModifierFlags: gesture.modifierFlags)
        resyncModifierState(from: gesture.modifierFlags)
        let snapshot = gestureModifiers.union(keyboardModifiers)
        sendModifierSnapshotIfNeeded(snapshot)
        return snapshot
    }

    // MARK: - Gesture Handlers

    @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        let rawLocation = gesture.location(in: self)
        let location = normalizedLocation(rawLocation)
        let eventModifiers = modifiers(from: gesture)

        switch gesture.state {
        case .began:
            // Detect multi-click timing
            let now = CACurrentMediaTime()
            let timeSinceLastTap = now - lastTapTime
            let distance = hypot(location.x - lastTapLocation.x, location.y - lastTapLocation.y)

            if timeSinceLastTap < Self.multiClickTimeThreshold && distance < Self.multiClickDistanceThreshold {
                currentClickCount += 1
            } else {
                currentClickCount = 1
            }

            lastTapTime = now
            lastTapLocation = location
            isDragging = false
            lastPanLocation = location

            MirageLogger.client("PRESS: normalized=(\(String(format: "%.3f", location.x)), \(String(format: "%.3f", location.y))), clickCount=\(currentClickCount)")

            let mouseEvent = MirageMouseEvent(
                button: .left,
                location: location,
                clickCount: currentClickCount,
                modifiers: eventModifiers
            )
            onInputEvent?(.mouseDown(mouseEvent))

        case .changed:
            // Track all movement - no threshold, pixel-perfect dragging
            let distance = hypot(location.x - lastPanLocation.x, location.y - lastPanLocation.y)
            if distance > 0.0001 {  // Any actual movement
                isDragging = true
                let mouseEvent = MirageMouseEvent(button: .left, location: location, modifiers: eventModifiers)
                onInputEvent?(.mouseDragged(mouseEvent))
                lastPanLocation = location
            }

        case .ended:
            let mouseEvent = MirageMouseEvent(
                button: .left,
                location: location,
                clickCount: currentClickCount,
                modifiers: eventModifiers
            )
            onInputEvent?(.mouseUp(mouseEvent))
            isDragging = false

        case .cancelled:
            // Send mouseUp on cancel to avoid stuck mouse state
            let mouseEvent = MirageMouseEvent(
                button: .left,
                location: location,
                clickCount: currentClickCount,
                modifiers: eventModifiers
            )
            onInputEvent?(.mouseUp(mouseEvent))
            isDragging = false

        default:
            break
        }
    }

    @objc func handleRightClick(_ gesture: UITapGestureRecognizer) {
        let location = normalizedLocation(gesture.location(in: self))
        let now = CACurrentMediaTime()

        // Detect multi-click for right button
        let timeSinceLastTap = now - lastRightTapTime
        let distance = hypot(location.x - lastRightTapLocation.x, location.y - lastRightTapLocation.y)

        if timeSinceLastTap < Self.multiClickTimeThreshold && distance < Self.multiClickDistanceThreshold {
            currentRightClickCount += 1
        } else {
            currentRightClickCount = 1
        }

        lastRightTapTime = now
        lastRightTapLocation = location

        let eventModifiers = modifiers(from: gesture)
        let mouseEvent = MirageMouseEvent(
            button: .right,
            location: location,
            clickCount: currentRightClickCount,
            modifiers: eventModifiers
        )

        onInputEvent?(.rightMouseDown(mouseEvent))
        onInputEvent?(.rightMouseUp(mouseEvent))
    }

    @objc func handleScroll(_ gesture: UIPanGestureRecognizer) {
        let translation = gesture.translation(in: self)
        // For touch scrolling, use the gesture location (center of two fingers)
        let location = normalizedLocation(gesture.location(in: self))

        // Reset translation to get incremental deltas
        gesture.setTranslation(.zero, in: self)

        let eventModifiers = modifiers(from: gesture)
        let scrollEvent = MirageScrollEvent(
            deltaX: translation.x,
            deltaY: translation.y,
            location: location,
            phase: MirageScrollPhase(gestureState: gesture.state),
            modifiers: eventModifiers,
            isPrecise: true  // Trackpad/touch scrolling is precise
        )

        onInputEvent?(.scrollWheel(scrollEvent))
    }

    @objc func handleHover(_ gesture: UIHoverGestureRecognizer) {
        let location = normalizedLocation(gesture.location(in: self))

        switch gesture.state {
        case .began, .changed:
            // Track cursor position for scroll events
            lastCursorPosition = location

            // Only send mouse moved if not dragging (pan gesture handles that)
            if !isDragging {
                let eventModifiers = modifiers(from: gesture)
                let mouseEvent = MirageMouseEvent(button: .left, location: location, modifiers: eventModifiers)
                onInputEvent?(.mouseMoved(mouseEvent))
            }
        default:
            break
        }
    }

    // MARK: - Direct Touch Gesture Handlers

    @objc func handleDirectPinch(_ gesture: UIPinchGestureRecognizer) {
        let phase = MirageScrollPhase(gestureState: gesture.state)
        refreshModifiersForInput()

        switch gesture.state {
        case .began:
            lastDirectPinchScale = 1.0
            let event = MirageMagnifyEvent(magnification: 0, phase: phase)
            onInputEvent?(.magnify(event))

        case .changed:
            let magnification = gesture.scale - lastDirectPinchScale
            lastDirectPinchScale = gesture.scale
            let event = MirageMagnifyEvent(magnification: magnification, phase: phase)
            onInputEvent?(.magnify(event))

        case .ended, .cancelled:
            let event = MirageMagnifyEvent(magnification: 0, phase: phase)
            onInputEvent?(.magnify(event))
            lastDirectPinchScale = 1.0

        default:
            break
        }
    }

    @objc func handleDirectRotation(_ gesture: UIRotationGestureRecognizer) {
        let phase = MirageScrollPhase(gestureState: gesture.state)
        refreshModifiersForInput()

        switch gesture.state {
        case .began:
            lastDirectRotationAngle = 0
            let event = MirageRotateEvent(rotation: 0, phase: phase)
            onInputEvent?(.rotate(event))

        case .changed:
            // Convert radians to degrees for the delta
            let rotationDelta = (gesture.rotation - lastDirectRotationAngle) * (180.0 / .pi)
            lastDirectRotationAngle = gesture.rotation
            let event = MirageRotateEvent(rotation: rotationDelta, phase: phase)
            onInputEvent?(.rotate(event))

        case .ended, .cancelled:
            let event = MirageRotateEvent(rotation: 0, phase: phase)
            onInputEvent?(.rotate(event))
            lastDirectRotationAngle = 0

        default:
            break
        }
    }
}

// MARK: - UIGestureRecognizerDelegate

extension InputCapturingView: UIGestureRecognizerDelegate {
    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        // Allow hover to work with other gestures
        if gestureRecognizer is UIHoverGestureRecognizer || otherGestureRecognizer is UIHoverGestureRecognizer {
            return true
        }

        // Allow pinch and rotation to work simultaneously (map-style interaction)
        if (gestureRecognizer is UIPinchGestureRecognizer && otherGestureRecognizer is UIRotationGestureRecognizer) ||
           (gestureRecognizer is UIRotationGestureRecognizer && otherGestureRecognizer is UIPinchGestureRecognizer) {
            return true
        }

        return false
    }
}
#endif
