# Makepad Library Sample

This PoC showcases how to reuse Rust view components created with [Makepad](https://github.com/makepad/makepad) in native Android and iOS (Kotlin and Swift) projects. For iOS this project depends on my [fork](https://github.com/ajoklar/makepad/tree/ajoklar) of Makepad. You should not use it in production. 

Makepad is a cross-platform app framework written in Rust. It allows to build complete app artifacts. At this stage it is not intended to build reusable native libraries, so I'm doing some experimentation. Other cross-platform frameworks with that feature include:

- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Flutter](https://docs.flutter.dev/add-to-app/android/add-flutter-view)

## Build Instructions

- Make sure to have [cargo make](https://github.com/sagiegurari/cargo-make) and Android NDK 25.2.9519653 installed
- Run `cargo make all` (or `cargo make android` / `cargo make ios` for a single platform) to build the shared Rust libraries
- Open the folder `android` in Android Studio and / or `iosSample` in Xcode. Those are normal native projects.

## Known Issues

- You can't use multiple Makepad views in your native project

### iOS

- When adding SVG resources, the application crashes:
    > -[MTLDebugRenderCommandEncoder setRenderPipelineState:]:1615: failed assertion `Set Render Pipeline State Validation
    >   For depth attachment, the renderPipelineState pixelFormat must be MTLPixelFormatInvalid, as no texture is set.  

### Android

- A hacky _build.rs_ that does not work for x86 builds
