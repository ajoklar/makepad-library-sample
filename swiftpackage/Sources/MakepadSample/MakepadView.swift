//
//  MakepadView.swift
//  Makepad
//
//  Created by ajoklar on 08.02.24.
//

import AVKit
import makepadsampleFFI
import MetalKit
import SwiftUI

public struct MakepadView: UIViewRepresentable {
    public init() {}

    public func makeUIView(context _: Context) -> MTKView { init_makepad_view(Bundle.module.resourceURL!.path()) }

    public func updateUIView(_: MTKView, context _: Context) {}
}
