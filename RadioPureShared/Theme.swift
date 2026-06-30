//
//  Theme.swift
//  RadioPureShared
//
//  设计常量：颜色、尺寸集中管理，消除魔法数字。
//

import SwiftUI

enum RadioTheme {

    // MARK: - 背景

    static let background = Color(red: 0.08, green: 0.08, blue: 0.10)

    // MARK: - 文字颜色

    static let primaryText = Color.white
    static let secondaryText = Color.white.opacity(0.75)
    static let tertiaryText = Color.white.opacity(0.5)
    static let mutedText = Color.white.opacity(0.4)
    static let subtleText = Color.white.opacity(0.3)
    static let faintText = Color.white.opacity(0.2)

    // MARK: - 状态颜色

    static let playingColor = Color.green
    static let errorColor = Color.red
    static let errorTextColor = Color.red.opacity(0.8)
    static let errorIconColor = Color.red.opacity(0.7)

    // MARK: - 分隔线

    static let dividerColor = Color.white.opacity(0.1)
    static let subtleDividerColor = Color.white.opacity(0.07)

    // MARK: - 列表行

    static let selectedIconBg = Color.white.opacity(0.15)
    static let defaultIconBg = Color.white.opacity(0.06)
    static let hoverRowBg = Color.white.opacity(0.05)
    static let selectedRowBg = Color.white.opacity(0.04)

    // MARK: - 控件

    static let sliderTint = Color.white.opacity(0.7)
    static let activeButton = Color.white
    static let inactiveButton = Color.white.opacity(0.2)

    // MARK: - 尺寸

    static let statusDotSize: CGFloat = 6
    static let iconSize: CGFloat = 40
    static let iconCornerRadius: CGFloat = 10
    static let playButtonSize: CGFloat = 52

    #if os(macOS)
    static let windowWidth: CGFloat = 400
    static let windowHeight: CGFloat = 640
    #endif
}
