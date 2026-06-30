//
//  RadioPureIOSApp.swift
//  RadioPureIOS
//

import SwiftUI
import AVFoundation

@main
struct RadioPureIOSApp: App {
    init() {
        configureAudioSession()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true)
        } catch {
            // 配置失败时仍可尝试播放，仅可能受静音开关影响
        }
    }
}
