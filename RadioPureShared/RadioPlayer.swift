//
//  RadioPlayer.swift
//  RadioPureShared
//

import AVFoundation
import MediaPlayer
import Observation

@Observable
class RadioPlayer {
    private var player: AVPlayer?
    private var statusObservation: NSKeyValueObservation?

    private var resolvedStreamURLString: String?
    private var didTryFallbackForCurrentStation = false

    var isPlaying = false
    var isError = false
    var volume: Float = 0.7 {
        didSet { player?.volume = volume }
    }
    var currentStation: RadioStation?

    #if os(iOS)
    @ObservationIgnored private var interruptionObserver: (any NSObjectProtocol)?
    #endif

    init() {
        setupRemoteCommands()
        #if os(iOS)
        observeAudioInterruption()
        #endif
    }

    // MARK: - 播放控制

    func play(station: RadioStation) {
        if currentStation == station {
            if isPlaying { pause() } else { resume() }
            return
        }

        stopAndRelease(resettingFallback: true)
        currentStation = station
        isError = false

        guard let chosen = streamingURL(for: station, primary: true),
              let url = URL(string: chosen) else { return }

        resolvedStreamURLString = chosen
        attachPlayer(for: station, streamURL: url)
    }

    func pause() {
        player?.pause()
        isPlaying = false
        updateNowPlayingInfo()
    }

    func resume() {
        player?.play()
        isPlaying = true
        updateNowPlayingInfo()
    }

    func togglePlayPause() {
        if isPlaying { pause() } else { resume() }
    }

    // MARK: - 内部播放逻辑

    private func streamingURL(for station: RadioStation, primary: Bool) -> String? {
        if primary { return station.url }
        if let fb = station.fallbackURL { return fb }
        return nil
    }

    private func attachPlayer(for station: RadioStation, streamURL: URL) {
        let playerItem = AVPlayerItem(url: streamURL)
        let newPlayer = AVPlayer(playerItem: playerItem)
        newPlayer.volume = volume

        statusObservation = playerItem.observe(\.status, options: [.new]) { [weak self] item, _ in
            Task { @MainActor [weak self] in
                guard let self else { return }
                guard item.status == .failed else { return }
                guard self.currentStation == station else { return }

                if let fb = station.fallbackURL,
                   !self.didTryFallbackForCurrentStation,
                   self.resolvedStreamURLString != fb,
                   let fbURL = URL(string: fb)
                {
                    self.didTryFallbackForCurrentStation = true
                    self.isError = false
                    self.resolvedStreamURLString = fb
                    self.statusObservation?.invalidate()
                    self.statusObservation = nil
                    self.player?.pause()
                    self.player = nil
                    self.attachPlayer(for: station, streamURL: fbURL)
                    self.player?.play()
                    self.isPlaying = true
                    self.updateNowPlayingInfo()
                    return
                }

                self.isPlaying = false
                self.isError = true
                self.updateNowPlayingInfo()
            }
        }

        player = newPlayer
        player?.play()
        isPlaying = true
        updateNowPlayingInfo()
    }

    private func stopAndRelease(resettingFallback: Bool = false) {
        statusObservation?.invalidate()
        statusObservation = nil
        player?.pause()
        player = nil
        isPlaying = false
        isError = false
        resolvedStreamURLString = nil
        if resettingFallback {
            didTryFallbackForCurrentStation = false
        }
    }

    // MARK: - NowPlaying 锁屏 / 控制中心信息

    private func updateNowPlayingInfo() {
        guard let station = currentStation else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: station.name,
            MPMediaItemPropertyArtist: "RadioPure",
            MPNowPlayingInfoPropertyIsLiveStream: true,
            MPNowPlayingInfoPropertyPlaybackRate: NSNumber(value: isPlaying ? 1.0 : 0.0),
        ]
    }

    private func setupRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.resume() }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.pause() }
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.togglePlayPause() }
            return .success
        }
        center.nextTrackCommand.isEnabled = false
        center.previousTrackCommand.isEnabled = false
    }

    // MARK: - iOS 音频中断处理

    #if os(iOS)
    private func observeAudioInterruption() {
        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] notification in
            let typeValue = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let optionsValue = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt
            Task { @MainActor [weak self] in
                self?.handleInterruption(typeValue: typeValue, optionsValue: optionsValue)
            }
        }
    }

    private func handleInterruption(typeValue: UInt?, optionsValue: UInt?) {
        guard let typeValue,
              let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }

        switch type {
        case .began:
            player?.pause()
            isPlaying = false
            updateNowPlayingInfo()
        case .ended:
            if let optionsValue {
                let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                if options.contains(.shouldResume) {
                    resume()
                }
            }
        @unknown default:
            break
        }
    }
    #endif
}
