# RadioPure 代码重构记录

**日期：** 2026-06-30
**范围：** Apple 共享层（macOS + iOS）为主，兼及 Android / Windows 电台数据同步

---

## 一、重构背景

对全量代码（macOS / iOS / Android / Windows 四个平台）进行评估后，发现以下主要问题：

1. **macOS 与 iOS 的 ContentView 约 95% 重复**（各 ~240 行），仅 hover 效果和窗口尺寸有差异
2. **电台数据在三个平台各维护一份**（Swift / Kotlin / C#），增删电台需手动同步，容易遗漏
3. **Swift `RadioStation.id` 使用 `UUID()`**，每次创建实例随机生成，与 Android/Windows 用 `name` 做 ID 不一致
4. **颜色、字号、间距等魔法数字**散落在 UI 代码各处，不利于全局主题调整
5. **iOS 端缺少音频中断处理**和锁屏 NowPlaying 控制

## 二、重构内容与方式

### 2.1 修复 RadioStation ID 设计

**文件：** `RadioPureShared/RadioStation.swift`

**问题：**
```swift
// 旧代码：每个实例生成随机 UUID
struct RadioStation: Identifiable, Equatable {
    let id = UUID()
    ...
}
```
`UUID()` 每次创建实例都不同，如果 catalog 被重新构建（如从 JSON 解析），相同电台的 `id` 会不同，导致 `==` 比较失败。

**方案：** 改为基于电台名称的稳定标识符，与 Android（`val id: String get() = name`）和 Windows（`public string Id => Name`）保持一致：
```swift
struct RadioStation: Identifiable, Equatable, Hashable {
    ...
    var id: String { name }
}
```

### 2.2 提取设计常量

**新建文件：** `RadioPureShared/Theme.swift`

将 20+ 个散落的颜色值和关键尺寸提取为语义化常量：

| 旧写法 | 新写法 |
|--------|--------|
| `Color(red: 0.08, green: 0.08, blue: 0.10)` | `RadioTheme.background` |
| `Color.white.opacity(0.4)` | `RadioTheme.mutedText` |
| `Color.green` | `RadioTheme.playingColor` |
| `Color.red.opacity(0.8)` | `RadioTheme.errorTextColor` |

### 2.3 合并 iOS/macOS ContentView（最大收益）

**新建：** `RadioPureShared/ContentView.swift`
**删除：** `RadioPure/ContentView.swift`、`RadioPureIOS/ContentView.swift`

利用 Xcode 的 `PBXFileSystemSynchronizedRootGroup` 特性——`RadioPureShared/` 目录下的文件自动编译到两个 target，无需修改 `project.pbxproj`。

平台差异通过 `#if os(macOS)` 条件编译处理：
- macOS：固定窗口尺寸（400×640）+ `StationRow` hover 效果
- iOS：自适应布局 + 选中行背景高亮

**消除约 400 行重复代码。**

### 2.4 电台数据单一数据源

**新建文件：**
- `stations.json` — 32 个电台的唯一数据源
- `Tools/sync_stations.py` — 从 JSON 生成三个平台的 `RadioStationCatalog`

**工作流：**
```bash
# 编辑电台数据
vim stations.json

# 一键同步到三个平台
python3 Tools/sync_stations.py

# CI 检查是否已同步
python3 Tools/sync_stations.py --check
```

生成的文件头部标注 `AUTO-GENERATED from stations.json — do not edit manually.`。

### 2.5 增强 RadioPlayer

**文件：** `RadioPureShared/RadioPlayer.swift`

新增功能：

| 功能 | 平台 | 说明 |
|------|------|------|
| NowPlaying 信息 | macOS + iOS | 锁屏/控制中心显示电台名称和播放状态 |
| 远程命令 | macOS + iOS | 蓝牙耳机/锁屏的播放/暂停按钮生效 |
| 音频中断处理 | iOS | 来电时自动暂停，通话结束后自动恢复 |

关键实现：
- 使用 `MPNowPlayingInfoCenter` 和 `MPRemoteCommandCenter`（`MediaPlayer` 框架）
- iOS 音频中断通过 `AVAudioSession.interruptionNotification` 监听
- 使用 `@ObservationIgnored` 避免 `@Observable` 宏与中断监听器存储属性的冲突
- 远程命令回调通过 `Task { @MainActor }` 确保线程安全

## 三、验证结果

| 验证项 | 结果 |
|--------|------|
| macOS target 编译（`xcodebuild -scheme RadioPure`） | 成功，零 Swift 错误/警告 |
| iOS target 编译（`xcodebuild -scheme RadioPureIOS`） | 成功，零 Swift 错误/警告 |
| 电台同步检查（`sync_stations.py --check`） | 三平台完全一致 |
| Lint 检查 | 无错误 |

## 四、文件变更清单

| 操作 | 文件 |
|------|------|
| 修改 | `RadioPureShared/RadioStation.swift` |
| 修改 | `RadioPureShared/RadioPlayer.swift` |
| 修改 | `RadioPureShared/RadioStationCatalog.swift`（自动生成） |
| 新增 | `RadioPureShared/Theme.swift` |
| 新增 | `RadioPureShared/ContentView.swift` |
| 新增 | `stations.json` |
| 新增 | `Tools/sync_stations.py` |
| 新增 | `docs/refactoring-2026-06-30.md`（本文档） |
| 删除 | `RadioPure/ContentView.swift` |
| 删除 | `RadioPureIOS/ContentView.swift` |
| 重新生成 | `RadioPureAndroid/.../RadioStationCatalog.kt` |
| 重新生成 | `RadioPureWindows/.../RadioStationCatalog.cs` |

## 五、未来可优化方向

以下项目在本次重构中未涉及，可作为后续改进：

1. **Android Service 绑定**：将 `PlaybackService` 的轮询等待改为 `bindService` + `ServiceConnection`
2. **Windows 列表刷新优化**：`RefreshStationRows` 每次全量重建改为 `ObservableCollection` 增量更新
3. **状态持久化**：记住上次播放的电台和音量（UserDefaults / SharedPreferences / LocalSettings）
4. **网络状态监听**：断网恢复后自动重连
5. **错误重试机制**：fallback 失败后添加 exponential backoff 重试
