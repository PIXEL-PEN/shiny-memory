# Markor Plus — Changelog (branch: markor-plus-v1.2.6)

This changelog is specific to the **Markor Plus** module on this branch.
For the full module history, see: [docs/markor-plus/CHANGELOG.md](docs/markor-plus/CHANGELOG.md)

## [1.2.6] - 2025-08-30
### Added
- Doc icon badges for long-title notes (>120 chars):
  - hollow dot on emoji doc = title-only (no body)
  - filled red dot on emoji doc = has body content
- README and module changelog documenting Calendar Input + Folder Tree.

### Changed
- Date-stamp emphasized at the top of new notes (non-US style).

### Fixed
- Creation-time chronology preserved (editing a note does not bump its position).

### Performance
- Image-import optimization groundwork (candidate for separate upstream PR).

### Known limitations
- Rename/move/copy handled via Markor’s native browser.
- Very large archives may still impact scanning; `.nomedia` or `.skip-scan` can mitigate.
- Root paths vary across devices; no automated migration yet.
- Accessibility/i18n and small-screen polish ongoing.
- CalendarView quirk: browsing to other months in the Folder Tree reflects correctly only after a day is selected. Future updates will evaluate third-party calendars to remove this quirk.
