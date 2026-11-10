# Player redesign (Dockside): implementation plan

Design: `docs/design/player/design.html` (published: https://claude.ai/artifact/WuXMZJYe2KRRnrAAar8cPi). Brief, decisions and state plan: `docs/design/player/brief.md`. Tokens: `docs/design/player/tokens.json` (Marquee colours; they match `ui/theme`).

Library facts below were checked against the pinned jars on 2026-11-10 (MediaMP 0.5.3, `mediamp-api-desktop`, `mediamp-mpv-desktop`).

## What already exists

| Need | Where | Status |
|---|---|---|
| Play/pause, seek, skip | `EmbeddedPlayer` (`:player` commonMain) | Exists |
| Audio / subtitle tracks, speed, aspect | `EmbeddedPlayer.audioTracks`, `subtitleTracks`, `playbackSpeed`, `aspectRatio` | Exist on desktop and Android |
| Volume and mute, desktop | MediaMP `AudioLevelController` (`volume`, `isMute`, `setVolume`, `setMute`, `volumeUp/Down`) | In the engine, not exposed by `EmbeddedPlayer` |
| Chapters, desktop | MediaMP `MediaMetadata.chapters: Flow<List<Chapter(name, durationMillis, offsetMillis)>>` | In the engine, not exposed |
| Chapters, Android | mpv `chapter-list` property through mpv-android | Not read yet |
| Subtitle font | mpv options `sub-font`, `sub-fonts-dir`, `sub-font-size`, `sub-border-style`, `sub-ass-override` (`MPVHandle.setPropertyString` on desktop, `MPVLib` on Android) | Not used yet |
| Episode queue | `EmbyPlaybackRequest.items` + `selectedIndex` (season siblings, from `RemoteEmbyRepository.play`) | Exists; the player only ever opens `item` |
| Shortcuts | `:coreui` `KeyShortcut` / `KeyShortcutManager` / `RegisterShortcuts` (key-down, modifiers, priority groups) | Exists; bindings are hard-coded and untyped |
| Preferences | `EmbyPreference` (DataStore) in `:shared`, `SettingsPreferences` object | Exists; `:player` can't depend on `:shared` |

## Gaps

- `EmbeddedPlayer` needs `audioLevel: PlayerAudioLevel?` and `chapters: StateFlow<List<PlayerChapter>>` (app-owned types; MediaMP stays inside jvmMain).
- `PlayerSource` needs queue context: `kicker` (series · S2 · E5), `previous` / `next` (title, S·E, artwork) and a callback to switch item. The queue stays in `:shared`; `:player` only gets what it displays and an `onNavigate(direction)` callback.
- Cross-season Next (S2 E8 → S3 E1) needs `GET /Shows/{SeriesId}/Episodes?AdjacentTo={id}`; the current queue is one season.
- Shortcut actions need stable ids (`PlayerAction.TogglePlay`, …) and a binding table that can be overridden. Bindings live in `:coreui` (used by `:player` and `:shared`), persistence in `:shared`, injected at the app boundary.
- Subtitle fonts: three OFL typefaces (Nunito, Atkinson Hyperlegible, Barlow Semi Condensed) as files in `:player` resources, extracted to a cache directory for mpv's `sub-fonts-dir`. Exact files and licence texts must be taken from the upstream repositories before adding them.

## Checkpoints

1. **Desktop layout and default shortcuts** (`:player` commonMain + jvmMain) — done 2026-11-10, see status below
   - Heading: back button + two-line title (kicker, name) on the surface; chapter label at the end when the panel is closed.
   - Timeline: full-width pill with only the two timers; wavy played segment while playing, flat when paused or with reduced motion; chapter ticks.
   - Dock: three opaque pills. Left: mute + volume, audio, subtitles. Centre: previous, back 10, Play (round when paused, squircle when playing), forward 10, next. Right: speed, more, panel, PiP, fullscreen.
   - Windowed: picture card beside the Chapters panel, nothing over the picture, controls never hide; remove the 32 dp inset presentation. Fullscreen: scrim only behind the heading, controls hide after 3 s. PiP: small dock, Restore, Close.
   - Feedback pill for every shortcut. Default YouTube-style bindings from the brief, all through `RegisterShortcuts`.
   - `EmbeddedPlayer.audioLevel` and `chapters` with the desktop implementation; Android returns null / empty for now.
2. **Queue and Up next** (`:shared` + `:player`): previous / next episode from `EmbyPlaybackRequest.items`, Up next tab, countdown card in the last 10 s, autoplay switch (persisted), cross-season next via `AdjacentTo`.
3. **Android layout**: portrait (picture, chapters, dock, tiles), landscape (timeline above the row), tablet; sheets restyled; gesture feedback through the shared pill; volume via the existing system-volume path; chapters from mpv.
4. **Subtitle style**: bundled fonts, typeface / size / background page in the Subtitles menu and sheet, persisted, applied through mpv options on both platforms.
5. **Customisable shortcuts**: action ids and binding table in `:coreui`, persistence in `:shared`, Settings → Keyboard shortcuts screen, record dialog with conflict / reserved-key handling, `?` cheat sheet, tooltips that read the live binding.
6. **Errors and closing**: T5 playback error inside the picture area (keeps heading and Back), PiP refused and mpvEx missing as snackbars, non-blocking "Saving your place" with snackbar retry on the details screen.

## Verification per checkpoint

`./gradlew :player:compileKotlinJvm`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:allTests`, `:androidApp:assembleDebug`, `check`. Manual: windowed ⇄ fullscreen ⇄ PiP with F, I, Esc; every shortcut shows its pill; Space on a focused button presses the button; reduced motion; 200 % text on Android.

## Status

### Checkpoint 1 (done 2026-11-10)
- New: `PlayerChapter`, `PlayerAudioLevel`, `PlayerMenu`, `PlayerFeedback`, `PlayerVideoTheme` / `LocalPlayerVideoColorScheme` (provided by `EmbympvTheme`), `RegisterPlayerShortcuts`; components `PlayerPill`, `PlayerPlayButton`, `PlayerTimeline` (rewritten), `PlayerVolume`, `PlayerHeading`, `PlayerDock`, `PlayerDockMenus`, `PlayerMenuButton`, `PlayerChaptersPanel`, `PlayerFeedbackPill`, `PlayerKeyCap`.
- `PlatformPlayerOverlay` takes a `surface` slot so each layout places the video; the windowed 32 dp inset in `TaoPlayerPresentation` is gone; the PiP window now routes keys to the shortcut manager.
- Removed: `PlayerControls`, `PlayerTrackControls`, `PlayerFullScreenControls`, `PlayerWindowControls`, `PlayerPlaybackOptions`, `PlayerOptionsPage`.
- Desktop volume and chapters come from MediaMP; Android returns null for both until checkpoint 3.
- Deferred on purpose: previous / next episode and the Up next tab (checkpoint 2, need the queue), the frame glow behind the windowed picture (would need a second render of the video), `?` cheat sheet and rebinding (checkpoint 5), `PlayerSource.subtitle` is not filled by `:shared` yet (checkpoint 2).
- Verified: `:player:compileKotlinJvm`, `:shared:compileKotlinJvm`, `:player:compileAndroidMain`, `:player:allTests`, `:shared:jvmTest`, `:androidApp:assembleDebug`, `:desktopApp:test` pass. `check` fails on two existing problems unrelated to this work: `shared/src/iosMain/.../MainViewController.kt` references a missing `App`, and `androidApp` unit tests have no JUnit dependency.
