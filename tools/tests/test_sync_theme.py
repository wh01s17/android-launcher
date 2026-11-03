"""Tests for tools/sync-theme.py. Run: python3 -m unittest discover -s tools/tests"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import sys
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("sync_theme", Path(__file__).resolve().parent.parent / "sync-theme.py")
sync_theme = importlib.util.module_from_spec(SPEC)
sys.modules["sync_theme"] = sync_theme  # dataclasses resolves its module here
SPEC.loader.exec_module(sync_theme)


def make_dotfiles(root: Path, color: str = "#0a0e0f") -> None:
    theme = root / sync_theme.THEME_SRC
    (theme / "brand").mkdir(parents=True)
    (theme / "colors.toml").write_text(f'background = "{color}"\n')
    (theme / "shell.bar.toml").write_text("[bar]\n")
    (theme / "shell.tooltip.toml").write_text("[tooltip]\n")
    (theme / "hyprland.lua").write_text("rounding = 10\n")
    (theme / "brand" / "logo.svg").write_text("<svg/>")
    fonts = root / sync_theme.FONTS_SRC
    fonts.mkdir(parents=True)
    for name in ("DepartureMono-Regular.otf", "wh01s17-pixel-icons.ttf", "DepartureMono-LICENSE", "Pixelarticons-LICENSE"):
        (fonts / name).write_bytes(name.encode())


class SyncThemeTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        # Las funciones imprimen avisos y errores a propósito: aquí se comprueba su resultado.
        quiet = contextlib.ExitStack()
        quiet.enter_context(contextlib.redirect_stdout(io.StringIO()))
        quiet.enter_context(contextlib.redirect_stderr(io.StringIO()))
        self.addCleanup(quiet.close)
        base = Path(self._tmp.name)
        self.layout = sync_theme.Layout(repo=base / "repo", dotfiles=base / "dotfiles")
        self.layout.repo.mkdir()
        make_dotfiles(self.layout.dotfiles)

    def test_sync_copies_files_and_writes_sums(self) -> None:
        self.assertEqual(0, sync_theme.sync(self.layout))
        self.assertTrue((self.layout.theme_dest / "colors.toml").is_file())
        self.assertTrue((self.layout.fonts_dest / "departure_mono.otf").is_file())
        self.assertTrue((self.layout.licenses_dest / "Pixelarticons-LICENSE").is_file())
        sums = sync_theme.read_sums(self.layout)
        self.assertIn("theme/wh01s17/colors.toml", sums)
        self.assertIn("core/designsystem/src/main/res/font/wh01s17_pixel_icons.ttf", sums)

    def test_check_passes_right_after_sync(self) -> None:
        sync_theme.sync(self.layout)
        self.assertEqual(0, sync_theme.check(self.layout, strict=True))

    def test_hand_edit_is_an_error(self) -> None:
        sync_theme.sync(self.layout)
        (self.layout.theme_dest / "colors.toml").write_text('background = "#ffffff"\n')
        self.assertEqual(1, sync_theme.check(self.layout, strict=False))

    def test_divergence_warns_but_only_fails_when_strict(self) -> None:
        sync_theme.sync(self.layout)
        make_dotfiles_again = self.layout.dotfiles / sync_theme.THEME_SRC / "colors.toml"
        make_dotfiles_again.write_text('background = "#000000"\n')
        self.assertEqual(0, sync_theme.check(self.layout, strict=False))
        self.assertEqual(1, sync_theme.check(self.layout, strict=True))

    def test_missing_dotfiles_skips_divergence_check(self) -> None:
        sync_theme.sync(self.layout)
        missing = sync_theme.Layout(repo=self.layout.repo, dotfiles=self.layout.dotfiles / "nope")
        self.assertEqual(0, sync_theme.check(missing, strict=True))

    def test_removed_upstream_shell_file_is_removed_here(self) -> None:
        sync_theme.sync(self.layout)
        (self.layout.dotfiles / sync_theme.THEME_SRC / "shell.tooltip.toml").unlink()
        self.assertEqual(1, sync_theme.check(self.layout, strict=True))
        sync_theme.sync(self.layout)
        self.assertFalse((self.layout.theme_dest / "shell.tooltip.toml").exists())
        self.assertEqual(0, sync_theme.check(self.layout, strict=True))

    def test_missing_source_file_is_reported(self) -> None:
        (self.layout.dotfiles / sync_theme.THEME_SRC / "hyprland.lua").unlink()
        with self.assertRaises(FileNotFoundError):
            sync_theme.plan(self.layout)

    def test_missing_sums_is_an_error(self) -> None:
        self.assertEqual(1, sync_theme.check(self.layout, strict=False))


if __name__ == "__main__":
    unittest.main()
