//
//  ContentView.swift
//  RadioPureShared
//
//  macOS / iOS 统一视图，通过条件编译处理平台差异。
//

import SwiftUI

// MARK: - 主视图
struct ContentView: View {
    private let stations = RadioStationCatalog.all

    @State private var radioPlayer = RadioPlayer()

    var body: some View {
        ZStack {
            RadioTheme.background
                .ignoresSafeArea()

            VStack(spacing: 0) {
                headerView

                Divider()
                    .background(RadioTheme.dividerColor)

                stationListView

                Divider()
                    .background(RadioTheme.dividerColor)

                controlsView
            }
        }
        #if os(macOS)
        .frame(width: RadioTheme.windowWidth, height: RadioTheme.windowHeight)
        .fixedSize()
        #endif
    }

    // MARK: - 顶部标题区域
    private var headerView: some View {
        VStack(spacing: 6) {
            Text("RadioPure")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(RadioTheme.mutedText)
                .tracking(3)

            if let station = radioPlayer.currentStation {
                HStack(spacing: 6) {
                    if radioPlayer.isError {
                        Circle()
                            .fill(RadioTheme.errorColor)
                            .frame(width: RadioTheme.statusDotSize, height: RadioTheme.statusDotSize)
                    } else if radioPlayer.isPlaying {
                        Circle()
                            .fill(RadioTheme.playingColor)
                            .frame(width: RadioTheme.statusDotSize, height: RadioTheme.statusDotSize)
                    }
                    Text(station.name)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(radioPlayer.isError ? .white.opacity(0.6) : RadioTheme.primaryText)
                }
                .transition(.opacity)
                .animation(.easeInOut, value: station.name)
            } else {
                Text("选择一个电台开始收听")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(RadioTheme.tertiaryText)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 20)
    }

    // MARK: - 电台列表
    private var stationListView: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                ForEach(stations) { station in
                    StationRow(
                        station: station,
                        isSelected: radioPlayer.currentStation == station,
                        isPlaying: radioPlayer.isPlaying && radioPlayer.currentStation == station,
                        isError: radioPlayer.isError && radioPlayer.currentStation == station
                    )
                    .onTapGesture {
                        radioPlayer.play(station: station)
                    }

                    if station != stations.last {
                        Divider()
                            .background(RadioTheme.subtleDividerColor)
                            .padding(.leading, 60)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - 底部播放控制
    private var controlsView: some View {
        VStack(spacing: 16) {
            Button(action: {
                radioPlayer.togglePlayPause()
            }) {
                ZStack {
                    Circle()
                        .fill(radioPlayer.currentStation != nil ? RadioTheme.activeButton : RadioTheme.inactiveButton)
                        .frame(width: RadioTheme.playButtonSize, height: RadioTheme.playButtonSize)

                    Image(systemName: radioPlayer.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(radioPlayer.currentStation != nil
                            ? RadioTheme.background
                            : RadioTheme.mutedText)
                        .offset(x: radioPlayer.isPlaying ? 0 : 2)
                }
            }
            .buttonStyle(.plain)
            .disabled(radioPlayer.currentStation == nil)

            HStack(spacing: 10) {
                Image(systemName: "speaker.fill")
                    .font(.system(size: 12))
                    .foregroundColor(RadioTheme.mutedText)
                    .frame(width: 18)

                Slider(value: $radioPlayer.volume, in: 0...1)
                    .tint(RadioTheme.sliderTint)
                    .frame(maxWidth: .infinity)

                Image(systemName: "speaker.wave.3.fill")
                    .font(.system(size: 12))
                    .foregroundColor(RadioTheme.mutedText)
                    .frame(width: 18)
            }
            .padding(.horizontal, 32)
        }
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 电台列表行组件
struct StationRow: View {
    let station: RadioStation
    let isSelected: Bool
    let isPlaying: Bool
    let isError: Bool

    #if os(macOS)
    @State private var isHovered = false
    #endif

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: RadioTheme.iconCornerRadius)
                    .fill(isSelected ? RadioTheme.selectedIconBg : RadioTheme.defaultIconBg)
                    .frame(width: RadioTheme.iconSize, height: RadioTheme.iconSize)

                Text(station.emoji)
                    .font(.system(size: 18))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(station.name)
                    .font(.system(size: 16, weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? RadioTheme.primaryText : RadioTheme.secondaryText)

                Group {
                    if isError {
                        Text("加载失败，请重试")
                            .foregroundColor(RadioTheme.errorTextColor)
                    } else if isPlaying {
                        Text("正在播放")
                            .foregroundColor(RadioTheme.playingColor)
                    } else {
                        Text("点击收听")
                            .foregroundColor(RadioTheme.subtleText)
                    }
                }
                .font(.system(size: 12))
            }

            Spacer()

            if isSelected {
                if isError {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(RadioTheme.errorIconColor)
                } else if isPlaying {
                    Image(systemName: "waveform")
                        .font(.system(size: 16))
                        .foregroundColor(RadioTheme.playingColor)
                } else {
                    Image(systemName: "pause.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(RadioTheme.mutedText)
                }
            } else {
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(RadioTheme.faintText)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(rowBackground)
        .contentShape(Rectangle())
        #if os(macOS)
        .onHover { hovering in
            withAnimation(.easeInOut(duration: 0.15)) {
                isHovered = hovering
            }
        }
        #endif
    }

    private var rowBackground: Color {
        #if os(macOS)
        isHovered ? RadioTheme.hoverRowBg : Color.clear
        #else
        isSelected ? RadioTheme.selectedRowBg : Color.clear
        #endif
    }
}

#Preview {
    ContentView()
}
