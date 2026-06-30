import Cocoa

struct IconSpec {
    let width: Int
    let height: Int
}

func makeColor(_ hex: UInt32, alpha: CGFloat = 1.0) -> NSColor {
    let r = CGFloat((hex >> 16) & 0xFF) / 255.0
    let g = CGFloat((hex >> 8) & 0xFF) / 255.0
    let b = CGFloat(hex & 0xFF) / 255.0
    return NSColor(calibratedRed: r, green: g, blue: b, alpha: alpha)
}

func drawCenteredText(
    _ text: String,
    in rect: NSRect,
    font: NSFont,
    color: NSColor,
    yOffset: CGFloat = 0
) {
    let para = NSMutableParagraphStyle()
    para.alignment = .center

    let attrs: [NSAttributedString.Key: Any] = [
        .font: font,
        .foregroundColor: color,
        .paragraphStyle: para
    ]

    let string = NSAttributedString(string: text, attributes: attrs)
    let textSize = string.size()
    let textRect = NSRect(
        x: rect.midX - textSize.width / 2.0,
        y: rect.midY - textSize.height / 2.0 + yOffset,
        width: textSize.width,
        height: textSize.height
    )
    string.draw(in: textRect)
}

func drawIcon(size: CGSize) -> NSImage {
    let image = NSImage(size: size)
    image.lockFocusFlipped(false)
    defer { image.unlockFocus() }

    let rect = NSRect(origin: .zero, size: size)
    let unit = min(rect.width, rect.height)

    let bgInner = makeColor(0xF8F7F4)
    let bgOuter = makeColor(0xEEECE8)
    if let gradient = NSGradient(colors: [bgInner, bgOuter]) {
        gradient.draw(in: rect, relativeCenterPosition: NSPoint(x: 0.0, y: 0.0))
    } else {
        bgInner.setFill()
        rect.fill()
    }

    let charcoal = makeColor(0x1F1F1F)
    let teal = makeColor(0x2AA6A1)

    let gFontSize = unit * 0.30
    let radioFontSize = unit * 0.30
    let gFont = NSFont.systemFont(ofSize: gFontSize, weight: .bold)
    let radioFont = NSFont.systemFont(ofSize: radioFontSize, weight: .bold)

    let gRect = NSRect(
        x: rect.minX,
        y: rect.midY + unit * 0.08,
        width: rect.width,
        height: unit * 0.34
    )
    drawCenteredText("G", in: gRect, font: gFont, color: teal)

    let radioTextRect = NSRect(
        x: rect.minX,
        y: rect.midY - unit * 0.34,
        width: rect.width,
        height: unit * 0.34
    )
    drawCenteredText("Radio", in: radioTextRect, font: radioFont, color: charcoal)

    return image
}

func writePNG(image: NSImage, to url: URL) throws {
    guard
        let tiff = image.tiffRepresentation,
        let rep = NSBitmapImageRep(data: tiff),
        let png = rep.representation(using: .png, properties: [:])
    else {
        throw NSError(domain: "GenerateAppIcon", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode PNG"])
    }
    try png.write(to: url, options: .atomic)
}

func usageAndExit() -> Never {
    FileHandle.standardError.write(Data("""
Usage:
  swift Tools/GenerateAppIcon.swift <output_png_path> [size]

Examples:
  swift Tools/GenerateAppIcon.swift RadioPure/Assets.xcassets/AppIcon.appiconset/icon_512@2x.png 1024

""".utf8))
    exit(2)
}

let args = CommandLine.arguments
guard args.count >= 2 else { usageAndExit() }

let outputPath = args[1]
let size = (args.count >= 3 ? Int(args[2]) : 1024) ?? 1024
let spec = IconSpec(width: size, height: size)

let outputURL = URL(fileURLWithPath: outputPath)
let folderURL = outputURL.deletingLastPathComponent()
try FileManager.default.createDirectory(at: folderURL, withIntermediateDirectories: true)

let image = drawIcon(size: CGSize(width: spec.width, height: spec.height))
try writePNG(image: image, to: outputURL)
print("Wrote \(spec.width)x\(spec.height) PNG to \(outputURL.path)")
