# RadioPure

简洁的网络电台播放器，支持 macOS、iOS、Android 与 Windows。

## 项目结构

| 目录 | 说明 |
|------|------|
| `RadioPure/` | macOS 应用（SwiftUI） |
| `RadioPureIOS/` | iOS 应用（SwiftUI） |
| `RadioPureShared/` | Apple 平台共享代码（播放器、电台模型与清单） |
| `RadioPureAndroid/` | Android 应用（Kotlin + Jetpack Compose + Media3） |
| `RadioPureWindows/` | Windows 应用（WinUI 3 + C# + LibVLCSharp） |
| `RadioPure.xcodeproj/` | Xcode 工程（macOS + iOS 目标） |
| `Tools/` | 工具脚本（如图标生成） |

电台数据统一维护在 `stations.json`。增删频道后运行同步脚本即可更新三个平台：

```bash
# 编辑 stations.json 后执行
python3 Tools/sync_stations.py

# CI 检查是否同步
python3 Tools/sync_stations.py --check
```

自动生成的文件（请勿直接编辑）：

- `RadioPureShared/RadioStationCatalog.swift`
- `RadioPureAndroid/app/src/main/kotlin/.../catalog/RadioStationCatalog.kt`
- `RadioPureWindows/RadioPureWindows/Catalog/RadioStationCatalog.cs`

## macOS / iOS 构建

**环境：** Xcode 15+，macOS

1. 用 Xcode 打开 `RadioPure.xcodeproj`
2. 选择 Scheme：
   - **RadioPure** — macOS
   - **RadioPureIOS** — iOS 模拟器或真机
3. `Product` → `Run`（⌘R）

iOS 后台播放已在 `RadioPureIOS/Info.plist` 中配置 `audio` 模式；允许 HTTP 明文流（与部分卫星链路电台兼容）。

## Android 构建

**环境：** Android Studio（推荐）或命令行，Android SDK（API 35），JDK 17

1. 用 Android Studio 打开 **`RadioPureAndroid/`** 目录（不是仓库根目录的 `.xcodeproj`）
2. 首次打开时按提示同步 Gradle；若缺少 SDK，安装 **Android 15 (API 35)** 平台
3. 连接真机或启动模拟器，点击 Run

**命令行：**

```bash
cd RadioPureAndroid
# 需本机 sdk.dir，可复制 local.properties.example 为 local.properties 并修改路径
./gradlew assembleDebug
```

调试 APK 输出路径：

`RadioPureAndroid/app/build/outputs/apk/debug/app-debug.apk`

安装到已连接设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android 说明：

- `minSdk 26`（Android 8.0+）
- 后台播放通过 `PlaybackService`（Media3 `MediaSessionService`）实现
- Android 13+ 首次运行会请求通知权限（用于媒体通知与锁屏控制）
- 网络配置允许 HTTP，与 iOS 的 `NSAllowsArbitraryLoads` 行为一致

**注意：** `RadioPureAndroid/local.properties` 含本机 SDK 路径，已加入 `.gitignore`，请勿提交。

## Windows 构建

**环境：** Visual Studio 2022（17.10+），工作负载「Windows 应用程序开发」；Windows 10 1809+ 或 Windows 11；.NET 8

1. 用 Visual Studio 打开 **`RadioPureWindows/RadioPureWindows.sln`**
2. 首次打开时还原 NuGet 包（含 LibVLCSharp 与 VideoLAN.LibVLC.Windows 原生库）
3. 选择配置 **Debug | x64**（或 x86 / ARM64），按 F5 运行

**命令行：**

```powershell
cd RadioPureWindows
dotnet restore
dotnet build -c Release
```

Release 可执行文件位于：

`RadioPureWindows\RadioPureWindows\bin\Release\net8.0-windows10.0.19041.0\win-x64\RadioPureWindows.exe`

Windows 说明：

- 固定 **400×640** 窗口，UI 与 macOS 版对齐
- HLS（`.m3u8`）播放使用 **LibVLCSharp**（内置 MediaPlayer 无法可靠播放 HLS）
- 允许 HTTP 明文流（与 iOS / Android 行为一致）
- 需在 **Windows** 主机上构建与运行（macOS 无法编译 WinUI 项目）

### MSIX 打包

项目已配置 **MSIX** 单项目打包（`Package.appxmanifest` + 临时开发证书）。

**Visual Studio：**

1. 右键 `RadioPureWindows` 项目 → **发布**
2. 选择 `win-x64-msix`（或 x86 / ARM64）配置文件
3. 点击 **发布**，生成 `.msix` 安装包

**命令行（x64 示例）：**

```powershell
cd RadioPureWindows
dotnet publish RadioPureWindows\RadioPureWindows.csproj `
  -c Release `
  -p:Platform=x64 `
  -p:PublishProfile=win-x64-msix
```

MSIX 输出目录（示例）：

`RadioPureWindows\RadioPureWindows\bin\Release\net8.0-windows10.0.19041.0\win-x64\publish\`

本地 sideload 安装：

```powershell
Add-AppxPackage -Path .\RadioPureWindows.msix
```

**说明：**

- 首次构建会自动生成 `RadioPureWindows_TemporaryKey.pfx`（已加入 `.gitignore`），仅用于本地测试
- 上架 Microsoft Store 或正式分发前，需替换 `Package.appxmanifest` 中的 `Publisher` 并使用正式代码签名证书
- 包标识：`com.radiopure.app.radiopure`（与 Android `applicationId` 一致）

**CI：** 推送 `RadioPureWindows/` 变更时，GitHub Actions（`windows.yml`）在 `windows-latest` 上编译并产出 MSIX  artifact（使用临时 CI 证书，与清单中 `CN=RadioPure Dev` 一致）。

## 功能概览

- 电台列表滚动选择，点击即播
- 播放 / 暂停、音量调节
- 主 URL 失败时自动尝试 `fallbackURL`（与 Swift 版逻辑一致）
- 后台继续播放（iOS / Android）；Windows 版为前台播放

## 许可证

见仓库内各文件版权声明（如有）。
