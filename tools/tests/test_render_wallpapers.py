"""Portable rendering and original-only asset checks, without requiring the dotfiles."""
import importlib.util
from pathlib import Path
import struct
import sys
import tomllib
import unittest
import zlib

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
spec = importlib.util.spec_from_file_location("wallpapers", TOOLS / "render-wallpapers.py")
wallpapers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(wallpapers)


class WallpaperTest(unittest.TestCase):
    def test_png_has_exact_dimensions_and_decompresses(self):
        palette = tomllib.loads((TOOLS.parent / "theme/wh01s17/colors.toml").read_text())
        for kind in ("mark", "moon", "moon-wordmark"):
            image = wallpapers.render(kind, 110, 240, palette)
            self.assertEqual(image[:8], b"\x89PNG\r\n\x1a\n")
            self.assertEqual(struct.unpack("!II", image[16:24]), (110, 240))
            compressed = image[41:-16]
            raw = zlib.decompress(compressed)
            self.assertEqual(raw[1:4], b"\0\0\0")
            self.assertEqual(len(raw), 240 * (1 + 110 * 3))
            self.assertNotEqual(raw[1:331], raw[120 * 331 + 1:121 * 331])

    def test_packaged_assets_only_include_original_art_and_all_sizes(self):
        root = TOOLS.parent / "core/designsystem/src/main/assets/wallpapers"
        for width, height in wallpapers.SIZES:
            folder = root / f"{width}x{height}"
            self.assertEqual({p.stem for p in folder.glob("*.png")}, {"mark", "moon", "moon-wordmark"})
            for path in folder.glob("*.png"):
                image = path.read_bytes()
                self.assertEqual(struct.unpack("!II", image[16:24]), (width, height))
                self.assertEqual(zlib.decompress(image[41:-16])[:4], b"\0\0\0\0")
