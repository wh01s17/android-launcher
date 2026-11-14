"""CHANGELOG sections and tag checks used by the release workflow."""
import importlib.util
from pathlib import Path
import unittest

TOOLS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("release_notes", TOOLS / "release-notes.py")
notes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notes)

CHANGELOG = """# Historial

## [Sin publicar]

## [1.0.0] — 2026-11-14

### Añadido

- Firma de release.

## [0.5.0] — 2026-11-05

- Estilo.

[1.0.0]: https://example.invalid/v1.0.0
"""


class ReleaseNotesTest(unittest.TestCase):
    def test_section_stops_at_the_next_version(self):
        self.assertEqual(notes.section(CHANGELOG, "1.0.0"), "### Añadido\n\n- Firma de release.\n")

    def test_last_section_drops_link_references(self):
        self.assertEqual(notes.section(CHANGELOG, "0.5.0"), "- Estilo.\n")

    def test_missing_or_empty_sections_fail(self):
        with self.assertRaises(ValueError):
            notes.section(CHANGELOG, "2.0.0")
        with self.assertRaises(ValueError):
            notes.section(CHANGELOG, "Sin publicar")

    def test_tag_must_match_version_properties(self):
        self.assertEqual(notes.check("v1.0.0", "version=1.0.0\n", CHANGELOG), "1.0.0")
        with self.assertRaises(ValueError):
            notes.check("v1.0.1", "version=1.0.0\n", CHANGELOG)

    def test_repository_changelog_has_the_current_version(self):
        root = TOOLS.parent
        version = notes.project_version((root / "version.properties").read_text())
        self.assertTrue(notes.section((root / "CHANGELOG.md").read_text(), version))


if __name__ == "__main__":
    unittest.main()
