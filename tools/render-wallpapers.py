#!/usr/bin/env python3
"""Render original W and moon portraits; stdlib only. Run from any directory."""
import argparse
from pathlib import Path
import struct
import tomllib
import zlib
import wallpaper_moon as moon

ROOT = Path(__file__).resolve().parents[1]
OLED_BACKGROUND = (0, 0, 0)  # Hardware black, independent of the theme surface palette.
SIZES = ((1220, 2712), (1080, 2400), (1440, 3200))


def rgb(value):
    return tuple(bytes.fromhex(value.lstrip("#"))[:3])


def png(width, height, pixels):
    def chunk(kind, data):
        return struct.pack("!I", len(data)) + kind + data + struct.pack("!I", zlib.crc32(kind + data))
    rows = b"".join(b"\0" + pixels[y * width * 3:(y + 1) * width * 3] for y in range(height))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack("!2I5B", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b""))


def artwork(kind, palette):
    accent = rgb(palette["accent"])
    if kind == "mark":
        # Brand/logo.svg, on the same pixel grid as the lunar artwork.
        boxes = ((0, 0, 10, 30), (16, 0, 26, 30), (32, 0, 42, 30),
                 (8, 30, 18, 38), (24, 30, 34, 38))
        return {(x, y): accent for x0, y0, x1, y1 in boxes for y in range(y0, y1) for x in range(x0, x1)}
    tones = (rgb(palette["background"]), tuple(round(c * 0.6) for c in accent), accent,
             tuple(round(c + (255 - c) * 0.55) for c in accent), rgb(palette["bright_foreground"]))
    pixels = {pos: tones[tone] for pos, tone in moon.render_moon(full=kind == "moon").items()}
    if kind == "moon-wordmark":
        for i, ch in enumerate("wh01s17"):
            color = accent if ch.isdigit() else rgb(palette["foreground"])
            for y, row in enumerate(moon.GLYPHS[ch]):
                for x, bit in enumerate(row):
                    if bit == "#":
                        pixels[(i * 6 + x + 7, y + 65)] = color
    return pixels


def render(kind, width, height, palette):
    art = artwork(kind, palette)
    xs, ys = zip(*art)
    aw, ah = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
    scale = max(1, width // 110)
    left, top = (width - aw * scale) // 2, (height - ah * scale) // 2
    pixels = bytearray(bytes(OLED_BACKGROUND) * width * height)
    for (x, y), color in art.items():
        line = bytes(color) * scale
        for dy in range(scale):
            offset = ((top + (y - min(ys)) * scale + dy) * width + left + (x - min(xs)) * scale) * 3
            pixels[offset:offset + len(line)] = line
    return png(width, height, pixels)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "core/designsystem/src/main/assets/wallpapers")
    args = parser.parse_args()
    palette = tomllib.loads((ROOT / "theme/wh01s17/colors.toml").read_text())
    for width, height in SIZES:
        folder = args.output / f"{width}x{height}"
        folder.mkdir(parents=True, exist_ok=True)
        for kind in ("mark", "moon", "moon-wordmark"):
            (folder / f"{kind}.png").write_bytes(render(kind, width, height, palette))


if __name__ == "__main__":
    main()
