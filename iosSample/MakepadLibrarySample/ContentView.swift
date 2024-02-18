//
//  ContentView.swift
//  MakepadLibrarySample
//
//  Created by ajoklar on 08.02.24.
//

import MakepadSample
import SwiftUI

struct ContentView: View {
    @State var buttonTapped = false
    
    var body: some View {
        VStack {
            if buttonTapped {
                Text("Native SwiftUI Text")
                MakepadView()
                    .ignoresSafeArea()
            } else {
                Button("Show the Makepad Example") { buttonTapped = true }
            }
        }
    }
}

#Preview {
    ContentView()
}
