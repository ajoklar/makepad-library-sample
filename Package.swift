// swift-tools-version: 5.8
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let basePath = "swiftpackage"

let package = Package(
    name: "MakepadSample",
    platforms: [.iOS(.v16)],
    products: [
        .library(
            name: "MakepadSample",
            targets: ["MakepadSample"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "MakepadSample",
            dependencies: ["makepadsampleFFI"],
            path: "\(basePath)/Sources/MakepadSample",
            resources: [.copy("Resources/makepad")]
        ),
        .binaryTarget(
            name: "makepadsampleFFI",
            path: "\(basePath)/Sources/makepadsampleFFI.xcframework"
        ),
        .testTarget(
            name: "MakepadSampleTests",
            dependencies: ["MakepadSample"],
            path: "\(basePath)/Tests/MakepadSampleTests"
        ),
    ]
)
