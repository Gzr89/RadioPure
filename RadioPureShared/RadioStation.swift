//
//  RadioStation.swift
//  RadioPureShared
//

import Foundation

struct RadioStation: Identifiable, Equatable, Hashable {
    let name: String
    /// 首选地址（常为 ngcdn CDN）
    let url: String
    /// 备用地址（卫星链路 satellitepull；多数电台在 CDN 对部分客户端返回 403 时仍可播放）
    let fallbackURL: String?
    let emoji: String

    /// 使用电台名称作为稳定标识符，与 Android/Windows 端保持一致
    var id: String { name }

    init(name: String, url: String, fallbackURL: String? = nil, emoji: String) {
        self.name = name
        self.url = url
        self.fallbackURL = fallbackURL
        self.emoji = emoji
    }
}
