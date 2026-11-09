# Design brief: EmbyEx — Details screen redesign

Status: approved (2026-11-09) — ready for implementation
Artifacts: options https://claude.ai/artifact/P5yYVe6GdzDs3oea2pNoPz · design https://claude.ai/artifact/PFGgEXB9HN9ubbHtVyMGTf

## Product
- What it is: Unofficial Emby media client (Kotlin/Compose Multiplatform). Browse library, play with built-in player, download for offline.
- Users and context of use: Owners/members of a personal Emby server; phone one-handed, tablet couch, desktop at a desk / big screen.
- Platforms / viewports: Android phone, Android tablet, desktop (Windows/Linux) → phone, tablet, desktop
- Themes: light + dark, follow system (Android dynamic color can override; scrim designed for both)
- Accessibility target: WCAG 2.2 AA + full keyboard/remote navigation (visible focus order on desktop: Play → actions → tracks → cast; Esc = back)
- Implementation target: Compose Multiplatform, Material 3 Expressive (`MaterialExpressiveTheme`, expressive motion)

## Mood and brand
- Feels like: Material 3 Expressive, cinematic, backdrop-led, simple, calm
- Direction: full-bleed artwork fading into surface, title logo, large expressive Play button; text always on a contrast-safe scrim
- Fixed brand assets: existing palette "ink and mint with a warm accent" (primary #006B54 / #76D8B3, tertiary amber), shapes 8/12/20/28/36, system sans type (ui/theme)
- Density: simple, uncluttered

## Content
- Language / locale: English (Compose resources)
- Sample data domain: movies and TV episodes from a personal Emby library

## Current details screen (as found in repo)
- Applies to playable items only (Movie, Episode); folders/series/seasons go to the library screen.
- Shows: backdrop, logo (or title), air date/year, runtime, video resolution, audio languages, subtitle languages, overview, directors, writers, resume progress + minutes left, offline availability.
- Actions: Play/Resume, Download (source picker dialog), Remove download, Play from server (when downloaded), Back.
- Three adaptive layouts today: portrait, landscape, desktop.
- Errors: load failure → LoadingScaffold retry; playback/download errors → AlertDialog.

## Screen inventory
| Feature | Screen | Primary action / question | Notes |
|---|---|---|---|
| Details | Item details (Movie, Episode) | Play / Resume | Adds: community + official rating, genres; cast row with photos; favorite + mark watched toggles; audio/subtitle pickers before play; download/remove; play from server. Episodes: series name (→ library) + S2·E5 label, prev/next episode links, 'More in this season' row with watched/progress markers. Track pickers = chips opening a menu (desktop) / bottom sheet (phone). Download = inline status button: Download → Queued → progress ring % (pause/cancel) → Downloaded ✓ (menu: remove, play from server); inline error + Retry. Series/Season stay on library screen. |

## Key flows to make clickable
- Resume → player handoff; Play from beginning
- Change audio / subtitle via chip → menu/sheet
- Download → source picker → queued → progress → downloaded → remove (T6 confirm)
- Favorite / watched toggles with snackbar undo
- Episode → next episode; season row → other episode
- Load error → retry; offline (cached/downloaded) state

## Decisions log
- 2026-11-09 · Scope: redesign of the details screen only, keep M3 Expressive · user request

- 2026-11-09 · Scope: Movie + Episode only (episodes get series context) · keeps current navigation
- 2026-11-09 · Add rating/age rating/genres, cast with photos, favorite + watched, track pickers · user selection
- 2026-11-09 · Mood: cinematic, backdrop-led · user choice
- 2026-11-09 · A11y beyond AA: keyboard/remote navigation · user choice
- 2026-11-09 · Episodes: series label, prev/next, season row · user choice
- 2026-11-09 · Track pickers: chips → menu/sheet · keeps screen simple, current choice visible
- 2026-11-09 · Downloads: inline status button replaces dialogs for progress · user choice
- 2026-11-09 · Themes: light + dark, follow system · matches app
- 2026-11-09 · Brief approved · user confirmed
- 2026-11-09 · Full design v1 approved · user confirmed
- 2026-11-09 · Type: system font only, no third-party fonts (for now) · app should feel integral to the OS while keeping its own design principles
- 2026-11-09 · Data source for implementation: Emby official docs + the server's /swagger spec · user instruction
- 2026-11-09 · Follow ~/.claude/skills/design workflow and tooling · user instruction

## Options history
- Round 1: A Marquee (safe, Roboto Flex, full-bleed fade), B Poster Sheet (Figtree, art band + rounded sheet + poster), C Billboard (Bricolage Grotesque, full-viewport art, oversized Play + round toggles, deep ink dark). Palette fixed across all three (brand). → A Marquee chosen (closest to current app, labeled actions most accessible)

## Open questions
-

## Phase 3 state plan (details)
Shell page: details top bar over artwork (back only, no ⋮), keyboard focus order map (Back → Play → Start over → Favorite → Watched → Download → Audio → Subtitles → Overview More → Cast → Season row → Prev/Next), offline banner behavior, large-text (200%) reflow, reduced motion.

Movie details
1. Loaded · resuming (default) — Resume + progress, Start over
2. Loaded · not started — Play, no progress
3. Loaded · watched — "Play again", Watched pressed
4. Loading — skeleton mirroring layout, Back usable
5. Load failed — T5 "Couldn't load this title. The server didn't respond. [Try again]"
6. Offline, downloaded — T4 "You're offline. Playing the downloaded copy." Favorite/Watched queue with pending marker; cast shows initials
7. Offline, not downloaded — T5 "You're offline. Connect to your server to see this title. [Open downloads]"
8. Download lifecycle — Download → source picker (sheet phone / dialog desktop) → Queued → 42% (menu: Pause, Cancel) → Paused → Downloaded (menu: Play from server, Remove download)
9. Download failed — T4 inline under actions "Download stopped at 61%. The server stopped responding. [Retry]"; button shows Retry
10. Not enough space — inline in source picker "Needs 18.4 GB, 6.2 GB free. Pick the 1080p version or free up space."
11. Remove download — T6 (re-download costs 18.4 GB): "Remove download? … You can still stream it from the server." [Keep] [Remove download]
12. Mark watched — T3 "Marked as watched · Undo" (restores resume position). Favorite = visible toggle, no snackbar. Toggle sync failure — T3 "Couldn't update favorite · Retry", state reverts
13. Playback failed — T4 dismissible callout above Play (needs time to read, tied to the primary action): "Couldn't start playback. The server couldn't convert this file. [Try again]"
14. Track pickers — Audio menu (desktop) / sheet (phone); Subtitles with 24 tracks: Off first, grouped by language, Forced/SDH tags, scrollable
15. Stress — long title without logo (wraps), no backdrop (tinted fallback + poster), no overview/cast (sections hidden), single audio track (static chip), 200% text

Episode details
16. Loaded — "Northbound" link + S2 · E5, Prev/Next episode, "More in Season 2" row (watched ✓, progress, current highlighted)
17. Season row loading (skeleton) / failed — partial failure inline "Couldn't load Season 2. [Retry]"
18. First / last episode — Prev hidden; Next crosses to "S3 · E1"; series finale hides Next

Decisions: Cast & crew photos are 3:4 rounded rectangles (radius md), not circles: Emby person images are portrait, so circles crop them heavily (user preference, agreed). ⋮ overflow menu dropped (its only job moved into the Download menu) — simpler. Remove download changed from undo to T6 because undo can't restore an 18 GB file cheaply.
