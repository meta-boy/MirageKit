// swift-tools-version: 6.2

import PackageDescription

let package = Package(
    name: "MirageKit",
    platforms: [
        .macOS(.v26),
        .iOS(.v26),
        .visionOS(.v26)
    ],
    products: [
        .library(
            name: "MirageKit",
            targets: ["MirageKit"]
        ),
    ],
    targets: [
        .target(
            name: "MirageKit",
            swiftSettings: [
                .define("MIRAGEKIT_HOST", .when(platforms: [.macOS])),
            ]
        ),
        .testTarget(
            name: "MirageKitTests",
            dependencies: ["MirageKit"]
        ),
    ]
)
