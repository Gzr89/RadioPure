#!/usr/bin/env python3
"""
从 stations.json 生成 Swift / Kotlin / C# 电台列表。

用法：
    python3 Tools/sync_stations.py          # 从项目根目录执行
    python3 Tools/sync_stations.py --check  # 检查是否已同步（CI 用）
"""

import json
import os
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
STATIONS_JSON = PROJECT_ROOT / "stations.json"

SWIFT_OUT = PROJECT_ROOT / "RadioPureShared" / "RadioStationCatalog.swift"
KOTLIN_OUT = (
    PROJECT_ROOT
    / "RadioPureAndroid"
    / "app"
    / "src"
    / "main"
    / "kotlin"
    / "com"
    / "radiopure"
    / "app"
    / "radiopure"
    / "catalog"
    / "RadioStationCatalog.kt"
)
CSHARP_OUT = (
    PROJECT_ROOT
    / "RadioPureWindows"
    / "RadioPureWindows"
    / "Catalog"
    / "RadioStationCatalog.cs"
)

HEADER = "// AUTO-GENERATED from stations.json — do not edit manually.\n// Run: python3 Tools/sync_stations.py\n"


def load_stations():
    with open(STATIONS_JSON, encoding="utf-8") as f:
        return json.load(f)


def escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"')


# ─── Swift ──────────────────────────────────────────────────────────────────────

def gen_swift(stations):
    lines = [
        HEADER,
        "import Foundation\n",
        "enum RadioStationCatalog {",
        "    static let all: [RadioStation] = [",
    ]
    for s in stations:
        fb = f'"{escape(s["fallbackURL"])}"' if s.get("fallbackURL") else "nil"
        lines.append(
            f'        RadioStation(\n'
            f'            name: "{escape(s["name"])}",\n'
            f'            url: "{escape(s["url"])}",\n'
            f'            fallbackURL: {fb},\n'
            f'            emoji: "{s["emoji"]}"),',
        )
    lines.append("    ]")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


# ─── Kotlin ─────────────────────────────────────────────────────────────────────

def gen_kotlin(stations):
    lines = [
        "package com.radiopure.app.radiopure.catalog\n",
        HEADER,
        "import com.radiopure.app.radiopure.model.RadioStation\n",
        "object RadioStationCatalog {",
        "    val all: List<RadioStation> = listOf(",
    ]
    for s in stations:
        fb_arg = (
            f'\n            fallbackURL = "{escape(s["fallbackURL"])}",'
            if s.get("fallbackURL")
            else ""
        )
        lines.append(
            f"        RadioStation(\n"
            f'            name = "{escape(s["name"])}",\n'
            f'            url = "{escape(s["url"])}",{fb_arg}\n'
            f'            emoji = "{s["emoji"]}",\n'
            f"        ),"
        )
    lines.append("    )")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


# ─── C# ─────────────────────────────────────────────────────────────────────────

def gen_csharp(stations):
    lines = [
        HEADER,
        "using RadioPureWindows.Models;\n",
        "namespace RadioPureWindows.Catalog;\n",
        "public static class RadioStationCatalog",
        "{",
        "    public static IReadOnlyList<RadioStation> All { get; } =",
        "    [",
    ]
    for s in stations:
        fb = f'"{escape(s["fallbackURL"])}"' if s.get("fallbackURL") else "null"
        lines.append(
            f'        new("{escape(s["name"])}", '
            f'"{escape(s["url"])}", '
            f'{fb}, '
            f'"{s["emoji"]}"),'
        )
    lines.append("    ];")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


# ─── Main ───────────────────────────────────────────────────────────────────────

def write_if_changed(path: Path, content: str) -> bool:
    if path.exists():
        existing = path.read_text(encoding="utf-8")
        if existing == content:
            return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def main():
    check_only = "--check" in sys.argv

    stations = load_stations()
    print(f"Loaded {len(stations)} stations from {STATIONS_JSON.name}")

    generators = [
        ("Swift", SWIFT_OUT, gen_swift),
        ("Kotlin", KOTLIN_OUT, gen_kotlin),
        ("C#", CSHARP_OUT, gen_csharp),
    ]

    any_changed = False
    for lang, path, gen_fn in generators:
        content = gen_fn(stations)
        if check_only:
            if not path.exists():
                print(f"  MISSING: {path.relative_to(PROJECT_ROOT)}")
                any_changed = True
            elif path.read_text(encoding="utf-8") != content:
                print(f"  OUT OF SYNC: {path.relative_to(PROJECT_ROOT)}")
                any_changed = True
            else:
                print(f"  OK: {path.relative_to(PROJECT_ROOT)}")
        else:
            changed = write_if_changed(path, content)
            status = "updated" if changed else "unchanged"
            print(f"  {lang}: {path.relative_to(PROJECT_ROOT)} [{status}]")
            any_changed = any_changed or changed

    if check_only and any_changed:
        print("\nStation catalogs are out of sync. Run: python3 Tools/sync_stations.py")
        sys.exit(1)

    if not check_only:
        print("\nDone." if any_changed else "\nAll files already up to date.")


if __name__ == "__main__":
    main()
