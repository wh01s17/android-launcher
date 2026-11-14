#!/usr/bin/env python3
"""Release notes and version checks for a tagged release (plan.md §8.1–8.2, docs/release.md).

    python3 tools/release-notes.py notes 1.0.0      # prints the CHANGELOG section of 1.0.0
    python3 tools/release-notes.py check v1.0.0     # tag == version.properties and CHANGELOG has it

CI uses both before publishing the GitHub Release.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HEADING = re.compile(r"^## \[(?P<version>[^\]]+)\]")


def section(changelog: str, version: str) -> str:
    """Body of `## [version]` up to the next `## ` heading, without the heading itself."""
    lines = changelog.splitlines()
    for start, line in enumerate(lines):
        match = HEADING.match(line)
        if match and match.group("version") == version:
            end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
            body = "\n".join(lines[start + 1:end]).strip()
            # Drop trailing link references ([0.5.0]: https://...) that belong to the whole file.
            body = re.sub(r"(\n\[[^\]]+\]: \S+)+\s*$", "", body).strip()
            if not body:
                raise ValueError(f"CHANGELOG section {version} is empty")
            return body + "\n"
    raise ValueError(f"CHANGELOG has no section for {version}")


def project_version(properties: str) -> str:
    match = re.search(r"^version=(\d+\.\d+\.\d+)\s*$", properties, re.MULTILINE)
    if not match:
        raise ValueError("version.properties needs version=MAJOR.MINOR.PATCH")
    return match.group(1)


def check(tag: str, properties: str, changelog: str) -> str:
    version = tag.removeprefix("v")
    expected = project_version(properties)
    if version != expected:
        raise ValueError(f"tag {tag} does not match version.properties ({expected})")
    section(changelog, version)
    return version


def main(argv: list[str]) -> int:
    if len(argv) != 3 or argv[1] not in ("notes", "check"):
        print(__doc__.strip(), file=sys.stderr)
        return 2
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    try:
        if argv[1] == "notes":
            sys.stdout.write(section(changelog, argv[2].removeprefix("v")))
        else:
            properties = (ROOT / "version.properties").read_text(encoding="utf-8")
            print(check(argv[2], properties, changelog))
    except ValueError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
