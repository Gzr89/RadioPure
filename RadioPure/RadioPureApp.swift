//
//  RadioPureApp.swift
//  RadioPure
//
//  Created by 郭忠仁 on 2026/5/13.
//

import SwiftUI

@main
struct RadioPureApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        // 固定窗口大小，禁止用户调整
        .windowResizability(.contentSize)
    }
}
