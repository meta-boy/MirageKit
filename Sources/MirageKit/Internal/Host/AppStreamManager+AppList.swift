//
//  AppStreamManager+AppList.swift
//  MirageKit
//
//  Created by Ethan Lipnik on 1/24/26.
//
//  App stream manager extensions.
//

#if os(macOS)
import Foundation
import AppKit

extension AppStreamManager {
    // MARK: - App List

    /// Get list of installed apps with streaming status
    public func getInstalledApps(includeIcons: Bool = true) async -> [MirageInstalledApp] {
        let now = Date()
        let statusSnapshot = snapshotStatus()

        if includeIcons {
            if let task = appScanTaskWithIcons {
                let apps = await task.value
                let refreshed = await refreshStatuses(for: apps)
                cachedAppsWithIcons = refreshed
                lastAppsScanWithIconsAt = now
                return refreshed
            }

            if isCacheValid(lastAppsScanWithIconsAt, ttl: appScanWithIconsTTL, now: now),
               !cachedAppsWithIcons.isEmpty {
                return await refreshStatuses(for: cachedAppsWithIcons)
            }

            let task = Task(priority: .utility) { [applicationScanner] in
                await applicationScanner.scanInstalledApps(
                    includeIcons: true,
                    runningApps: statusSnapshot.runningApps,
                    streamingApps: statusSnapshot.streamingApps
                )
            }
            appScanTaskWithIcons = task
            let apps = await task.value
            appScanTaskWithIcons = nil

            let refreshed = await refreshStatuses(for: apps)
            cachedAppsWithIcons = refreshed
            lastAppsScanWithIconsAt = now
            return refreshed
        }

        if let task = appScanTaskWithoutIcons {
            let apps = await task.value
            let refreshed = await refreshStatuses(for: apps)
            cachedAppsWithoutIcons = refreshed
            lastAppsScanWithoutIconsAt = now
            return refreshed
        }

        if isCacheValid(lastAppsScanWithoutIconsAt, ttl: appScanWithoutIconsTTL, now: now),
           !cachedAppsWithoutIcons.isEmpty {
            return await refreshStatuses(for: cachedAppsWithoutIcons)
        }

        let task = Task(priority: .utility) { [applicationScanner] in
            await applicationScanner.scanInstalledApps(
                includeIcons: false,
                runningApps: statusSnapshot.runningApps,
                streamingApps: statusSnapshot.streamingApps
            )
        }
        appScanTaskWithoutIcons = task
        let apps = await task.value
        appScanTaskWithoutIcons = nil

        let refreshed = await refreshStatuses(for: apps)
        cachedAppsWithoutIcons = refreshed
        lastAppsScanWithoutIconsAt = now
        return refreshed
    }

    func invalidateAppListCache() {
        cachedAppsWithIcons.removeAll()
        cachedAppsWithoutIcons.removeAll()
        lastAppsScanWithIconsAt = nil
        lastAppsScanWithoutIconsAt = nil
    }

    private func snapshotStatus() -> (runningApps: Set<String>, streamingApps: Set<String>) {
        let runningApps = Set(
            NSWorkspace.shared.runningApplications
                .compactMap { $0.bundleIdentifier?.lowercased() }
        )
        let streamingApps = Set(sessions.keys.map { $0.lowercased() })
        return (runningApps, streamingApps)
    }

    private func refreshStatuses(for apps: [MirageInstalledApp]) async -> [MirageInstalledApp] {
        let statusSnapshot = snapshotStatus()
        return await applicationScanner.updateStatus(
            for: apps,
            runningApps: statusSnapshot.runningApps,
            streamingApps: statusSnapshot.streamingApps
        )
    }

    private func isCacheValid(_ lastScan: Date?, ttl: TimeInterval, now: Date) -> Bool {
        guard let lastScan else { return false }
        return now.timeIntervalSince(lastScan) <= ttl
    }

    /// Check if an app is available for streaming (not already being streamed)
    public func isAppAvailableForStreaming(_ bundleIdentifier: String) -> Bool {
        let key = bundleIdentifier.lowercased()

        guard let session = sessions[key] else {
            return true // Not being streamed
        }

        // Check if reservation has expired
        if session.reservationExpired {
            return true
        }

        return false
    }

    /// Get the client ID that has exclusive access to an app (if any)
    public func clientStreamingApp(_ bundleIdentifier: String) -> UUID? {
        let key = bundleIdentifier.lowercased()
        guard let session = sessions[key], !session.reservationExpired else {
            return nil
        }
        return session.clientID
    }

}

#endif
