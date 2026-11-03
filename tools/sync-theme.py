#!/usr/bin/env python3
"""Sync the wh01s17 theme and its fonts from ~/dotfiles into this repository.

The dotfiles are the single source of truth for the theme (plan.md §4.1): the
files copied here are never edited by hand. Gradle generates the design tokens
from `theme/wh01s17/*.toml` (`:core:designsystem:generateTokens`).

Usage:
    python3 tools/sync-theme.py            copy the files and refresh SHA256SUMS
    python3 tools/sync-theme.py --check    verify integrity and warn on divergence
    python3 tools/sync-theme.py --check --strict
                                           same, but divergence is an error

Environment:
    WH01S17_DOTFILES   path to the dotfiles repository (default ~/dotfiles)
"""

from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path

THEME_SRC = Path("desktop/.config/wh01s17/themes/wh01s17")
FONTS_SRC = Path("apps/.local/share/fonts/wh01s17")
SUMS_NAME = "SHA256SUMS"


@dataclass(frozen=True)
class Layout:
    """Where files come from (dotfiles) and where they go (this repo)."""

    repo: Path
    dotfiles: Path

    @property
    def theme_dest(self) -> Path:
        return self.repo / "theme" / "wh01s17"

    @property
    def fonts_dest(self) -> Path:
        return self.repo / "core/designsystem/src/main/res/font"

    @property
    def licenses_dest(self) -> Path:
        return self.repo / "core/designsystem/src/main/assets/licenses"

    @property
    def sums_file(self) -> Path:
        return self.theme_dest / SUMS_NAME


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def plan(layout: Layout) -> list[tuple[Path, Path]]:
    """(source, destination) pairs. Raises FileNotFoundError on a missing source."""
    theme = layout.dotfiles / THEME_SRC
    fonts = layout.dotfiles / FONTS_SRC
    pairs = [(theme / "colors.toml", layout.theme_dest / "colors.toml")]
    pairs += [(f, layout.theme_dest / f.name) for f in sorted(theme.glob("shell.*.toml"))]
    pairs += [
        (theme / "hyprland.lua", layout.theme_dest / "hyprland.lua"),
        (theme / "brand" / "logo.svg", layout.theme_dest / "brand" / "logo.svg"),
        (fonts / "DepartureMono-Regular.otf", layout.fonts_dest / "departure_mono.otf"),
        (fonts / "wh01s17-pixel-icons.ttf", layout.fonts_dest / "wh01s17_pixel_icons.ttf"),
        (fonts / "DepartureMono-LICENSE", layout.licenses_dest / "DepartureMono-LICENSE"),
        (fonts / "Pixelarticons-LICENSE", layout.licenses_dest / "Pixelarticons-LICENSE"),
    ]
    missing = [src for src, _ in pairs if not src.is_file()]
    if missing:
        raise FileNotFoundError("missing in dotfiles: " + ", ".join(str(m) for m in missing))
    return pairs


def stale_files(layout: Layout, pairs: list[tuple[Path, Path]]) -> list[Path]:
    """Shell TOML files kept here that the dotfiles no longer have."""
    wanted = {dest for _, dest in pairs}
    return [p for p in sorted(layout.theme_dest.glob("shell.*.toml")) if p not in wanted]


def write_sums(layout: Layout, pairs: list[tuple[Path, Path]]) -> None:
    lines = sorted(f"{sha256(dest)}  {dest.relative_to(layout.repo).as_posix()}" for _, dest in pairs)
    layout.sums_file.write_text("\n".join(lines) + "\n", encoding="utf-8")


def sync(layout: Layout) -> int:
    pairs = plan(layout)
    changed = 0
    for src, dest in pairs:
        if dest.is_file() and sha256(src) == sha256(dest):
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)
        print(f"updated  {dest.relative_to(layout.repo)}")
        changed += 1
    for old in stale_files(layout, pairs):
        old.unlink()
        print(f"removed  {old.relative_to(layout.repo)}")
        changed += 1
    write_sums(layout, pairs)
    print(f"{changed} file(s) changed; {len(pairs)} tracked in {SUMS_NAME}")
    return 0


def read_sums(layout: Layout) -> dict[str, str]:
    sums: dict[str, str] = {}
    for line in layout.sums_file.read_text(encoding="utf-8").splitlines():
        digest, _, rel = line.partition("  ")
        if digest and rel:
            sums[rel] = digest
    return sums


def check(layout: Layout, strict: bool) -> int:
    """Integrity against SHA256SUMS is always an error; divergence from the dotfiles warns."""
    if not layout.sums_file.is_file():
        print(f"ERROR  {SUMS_NAME} not found; run tools/sync-theme.py", file=sys.stderr)
        return 1
    status = 0
    for rel, digest in read_sums(layout).items():
        path = layout.repo / rel
        if not path.is_file() or sha256(path) != digest:
            print(f"ERROR  {rel} was edited or removed; the theme is synced, not hand-edited", file=sys.stderr)
            status = 1

    if not (layout.dotfiles / THEME_SRC).is_dir():
        print(f"INFO   dotfiles not found at {layout.dotfiles}; divergence check skipped")
        return status
    try:
        pairs = plan(layout)
    except FileNotFoundError as error:
        print(f"WARN   {error}")
        return status
    diverged = [dest for src, dest in pairs if not dest.is_file() or sha256(src) != sha256(dest)]
    diverged += stale_files(layout, pairs)
    for dest in diverged:
        print(f"WARN   {dest.relative_to(layout.repo)} differs from the dotfiles (run tools/sync-theme.py)")
    if diverged and strict:
        status = 1
    return status


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="verify instead of copying")
    parser.add_argument("--strict", action="store_true", help="with --check, divergence is an error")
    parser.add_argument("--dotfiles", type=Path, help="dotfiles repository (default $WH01S17_DOTFILES or ~/dotfiles)")
    args = parser.parse_args(argv)

    dotfiles = args.dotfiles or Path(os.environ.get("WH01S17_DOTFILES", Path.home() / "dotfiles"))
    layout = Layout(repo=Path(__file__).resolve().parent.parent, dotfiles=dotfiles)
    if args.check:
        return check(layout, args.strict)
    try:
        return sync(layout)
    except FileNotFoundError as error:
        print(f"ERROR  {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
