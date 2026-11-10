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
- Subtitle font: Nunito (OFL) as a file in `:player` resources, extracted to a cache directory for mpv's `sub-fonts-dir`.

## Checkpoints

1. **Desktop layout and default shortcuts** (`:player` commonMain + jvmMain) — done 2026-11-10, see status below
   - Heading: back button + two-line title (kicker, name) on the surface; chapter label at the end when the panel is closed.
   - Timeline: full-width pill with only the two timers; wavy played segment while playing, flat when paused or with reduced motion; chapter ticks.
   - Dock: three opaque pills. Left: mute + volume, audio, subtitles. Centre: previous, back 10, Play (round when paused, squircle when playing), forward 10, next. Right: speed, more, panel, PiP, fullscreen.
   - Windowed: picture card beside the Chapters panel, nothing over the picture, controls never hide; remove the 32 dp inset presentation. Fullscreen: scrim only behind the heading, controls hide after 3 s. PiP: small dock, Restore, Close.
   - Feedback pill for every shortcut. Default YouTube-style bindings from the brief, all through `RegisterShortcuts`.
   - `EmbeddedPlayer.audioLevel` and `chapters` with the desktop implementation; Android returns null / empty for now.
2. **Queue and Up next** (`:shared` + `:player`) — done 2026-11-10: previous / next episode from `EmbyPlaybackRequest.items`, Up next tab, countdown card in the last 10 s, autoplay switch (persisted), cross-season next via `AdjacentTo`.
3. **Android layout** — done 2026-11-10: portrait (picture, chapters, dock, tiles), landscape (timeline above the row), tablet; sheets restyled; gesture feedback through the shared pill; volume via the existing system-volume path; chapters from mpv.
4. **Subtitle style** — done 2026-11-10: bundled font, size / vertical and horizontal position / text alignment / background page in the Subtitles menu and sheet, persisted, applied through mpv options on both platforms. (Typeface choice was dropped on 2026-11-10 at the user's request; Nunito is the only face.)
5. **Customisable shortcuts**: action ids and binding table in `:coreui`, persistence in `:shared`, Settings → Keyboard shortcuts screen, record dialog with conflict / reserved-key handling, `?` cheat sheet, tooltips that read the live binding.
6. **Errors and closing**: T5 playback error inside the picture area (keeps heading and Back), PiP refused and mpvEx missing as snackbars, non-blocking "Saving your place" with snackbar retry on the details screen.

## Verification per checkpoint

`./gradlew :player:compileKotlinJvm`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:allTests`, `:androidApp:assembleDebug`, `check`. Manual: windowed ⇄ fullscreen ⇄ PiP with F, I, Esc; every shortcut shows its pill; Space on a focused button presses the button; reduced motion; 200 % text on Android.

## Status

### Checkpoint 1 (done 2026-11-10)
- New: `PlayerChapter`, `PlayerAudioLevel`, `PlayerMenu`, `PlayerFeedback`, `PlayerVideoTheme` / `LocalPlayerVideoColorScheme` (provided by `EmbympvTheme`), `RegisterPlayerShortcuts`; components `PlayerPill`, `PlayerPlayButton`, `PlayerTimeline` (rewritten), `PlayerVolume`, `PlayerHeading`, `PlayerDock`, `PlayerDockMenus`, `PlayerMenuButton`, `PlayerChaptersPanel`, `PlayerFeedbackPill`, `PlayerKeyCap`.
- `PlatformPlayerOverlay` takes a `surface` slot so each layout places the video; the windowed 32 dp inset in `PlatformPlayerPresentation` is gone; the PiP window now routes keys to the shortcut manager.
- Removed: `PlayerControls`, `PlayerTrackControls`, `PlayerFullScreenControls`, `PlayerWindowControls`, `PlayerPlaybackOptions`, `PlayerOptionsPage`.
- Desktop volume and chapters come from MediaMP; Android returns null for both until checkpoint 3.
- Deferred on purpose: previous / next episode and the Up next tab (checkpoint 2, need the queue), the frame glow behind the windowed picture (would need a second render of the video), `?` cheat sheet and rebinding (checkpoint 5), `PlayerSource.subtitle` is not filled by `:shared` yet (checkpoint 2).
- Verified: `:coreui:compileKotlinJvm`, `:player:compileKotlinJvm`, `:player:compileAndroidMain`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:jvmTest`, `:androidApp:assembleDebug`, `:desktopApp:test` pass. The bundled desktop libmpv accepts every property and value; frames rendered by mpv 0.41.0 show the face picked by full name without synthetic bold, all three backgrounds, `sub-pos` kept on screen at 0, and `sub-justify` lining up lines. Not run in the app or on a device by me.

### Checkpoint 2 (done 2026-11-10)
- `:player`: `PlayerQueue` / `PlayerQueueEntry` on `PlayerSource.queue`, `PlayerSource.subtitle`, `LocalPlayerArtwork` (thumbnail slot), `EmbeddedPlayerScreen(onNavigate = …)`. The player decides where to go: previous / next buttons and Shift+P / Shift+N, a click in the Up next list, Play now, or the next episode when one ends with autoplay on and Up next not cancelled. Otherwise it closes.
- Panel: `PlayerSidePanel` with Chapters / Up next tabs (tabs only when both exist), queue list with the next entry tagged, "Play next automatically" switch (persisted as `SettingsPreferences.autoplayNextEnabled`, on by default).
- Up next card in the last 10 s (episodes over 1 min): countdown with Play now / Cancel when autoplay is on, Play next episode / Close when off. Enter plays now, Esc cancels. Windowed: bottom-right of the picture; fullscreen: above the dock, also while the controls are hidden.
- `:shared`: `EmbyPlaybackItem.episodeLabel`, `seriesName`, `durationMs`; `RemoteEmbyRepository.play` appends the next season's first episode via `GET /Shows/{SeriesId}/Episodes?AdjacentTo=…` (falls back to the season list on failure); `EmbyPlayerAction.Update.nextItemId`; the ViewModel now advances only to the entry the player names (before, it advanced on every completed episode).
- Not yet: Up next in the PiP mini window, the no-countdown variant for screen readers (needs a platform check for an active screen reader), Android UI for the queue (checkpoint 3; autoplay itself works there because the decision is in common code).
- Verified: `:player:compileKotlinJvm`, `:player:compileAndroidMain`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:jvmTest`, `:androidApp:assembleDebug`, `:desktopApp:test` pass. `:shared:testAndroidHostTest` fails in 4 `GithubUpdateProviderTest` cases (`MissingFieldException: tag_name`), which is updater code this work doesn't touch.

### Checkpoint 3 (done 2026-11-10)
- `AndroidPlayerOverlay` rewritten. Portrait: heading, 16:9 picture (gestures on it only), Chapters / Up next panel, timeline pill, transport pill (previous, back 10, Play, forward 10, next), eight labelled tiles with current values (Subtitles, Audio, Speed, Aspect, Lock, Rotate, PiP, More). Controls never hide in portrait. Landscape and tablet: full-screen picture, short top scrim, heading, timeline pill above the row (lock / rotate / speed · transport · subtitles / audio / panel / more), panel beside the picture (open by default at 1100 dp and wider), controls hide after 3 s.
- Gesture feedback (volume, brightness, drag seek) uses the shared feedback pill; `AndroidPlayerFeedback` and `AndroidPlayerLevel` removed. Double-tap seek ripple, lock and slide-to-unlock unchanged.
- Sheets: new More sheet (Aspect ratio, Open in mpvEx) replaces the More dropdown; labels shared with desktop (`subtitleLabel`, `audioLabel`, `speedLabel`). Removed `AndroidPlayerOptionActions`, `AndroidPlayerQuickActions`, `PlayerTransportControls`, `RegisterPlaybackShortcuts`, `AndroidPlayerTitle`.
- Chapters on Android from mpv `chapter-list` (`AndroidMpvMetadata.chapters`); untitled chapters show as "Chapter N" on every platform.
- Hardware keyboards use `RegisterPlayerShortcuts`; Android has no player volume, so ↑ ↓ M answer "Not available here" (device volume keys still work).
- Verified: `:player:compileAndroidMain`, `:player:compileKotlinJvm`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:jvmTest`, `:androidApp:assembleDebug`, `:desktopApp:test` pass. Not run on a device by me.

### Follow-up to checkpoint 3 (2026-11-10)
- One video surface per player: `PlayerPictureSlot` lets layouts mark the picture's spot with a placeholder while the surface stays at a single call site and follows it in the layout phase. Fixes the cut when closing the panel and when rotating on Android (the `SurfaceView` was being recreated). Desktop uses the same mechanism.
- Animated: Android portrait panel (picture slides between top and centre), landscape and desktop fullscreen picture resize (animated end padding) and panel slide, desktop windowed panel expand/shrink.
- Desktop: a click on the picture shows / hides the controls instead of play / pause. In windowed mode this hides heading, timeline, dock and panel and the picture grows; click or Tab brings them back; windowed still never auto-hides. Double click toggles fullscreen.
- Panel header has a close button; Android portrait shows "Show chapters" at the end of the heading while the panel is closed; the portrait More sheet no longer repeats Aspect ratio.
- The desktop PiP mini window is still a separate window with its own surface; the decoder keeps running, so there is no reload, as before.

### Checkpoint 4 (done 2026-11-10)
- `:player`: `PlayerSubtitleStyle` (`sizePercent` 50–200, `background` `PlayerSubtitleBackground`, `verticalPercent` 0–100 above the bottom, `horizontalPercent` −100–100, `alignment` `PlayerSubtitleAlignment`; sliders move in 5 % steps), `EmbeddedPlayerScreen(subtitleStyle, onSubtitleStyleChange)`, `EmbeddedPlayer.setSubtitleStyle` (no-op by default, iOS). `MpvSubtitleOptions.kt` maps a style to mpv properties shared by both engines (`sub-font` "Nunito Bold", `sub-bold=no`, `sub-font-size` 38 with `sub-scale`, `sub-pos`, `sub-align-x` + `sub-margin-x`, `sub-justify`, `sub-border-style` opaque-box / outline-and-shadow, outline / shadow / back colours from `PlayerColors`). They apply live.
- Font in `composeResources/font`: `subtitle_nunito_bold.ttf` (static wght 700 instance of google/fonts `ofl/nunito/Nunito[wght].ttf` at commit 6049366, made with fontTools 4.60.1 `varLib.instancer --update-name-table --static`; Nunito has no static upstream files and no Reserved Font Name). OFL text in `composeResources/files/licenses`. The same file draws the page's preview.
- mpv gets them through `sub-fonts-dir`: desktop writes them to the user cache (`~/.cache/embyEx/subtitle-fonts`, `~/Library/Caches/embyEx/…`, `%LOCALAPPDATA%\embyEx\…`) before each open; Android to `filesDir/mpv/subtitle-fonts` as an init option. If the desktop cache can't be written, subtitles fall back to a system font.
- UI: `PlayerSubtitleStylePage` (preview placed like the video places it, Size / Vertical position / Horizontal position sliders, Text alignment start / center / end as icon segments, Background Box / Outline / Shadow, note about ASS and picture subtitles, Reset to default which turns into Undo reset). Sliders move the preview while dragged and save once on release (`saveStrings` blocks). The preview is see-through: the page paints the panel itself with a rounded hole over the preview, so the sample line sits on the playing video; the menu (`DropDown` now takes `containerColor` / `shadowElevation`) and the Android sheet (also its scrim) are transparent while this page shows. Desktop: "Subtitle style" under the Subtitles track list and in More (opens under Subtitles when that button exists, otherwise inside More), with a Back row. Android: row in the Subtitles and More sheets, page as its own sheet.
- `:shared`: `SettingsPreferences.subtitleStyle` / `updateSubtitleStyle` (five string keys; unknown values fall back per field, numbers are clamped), passed at both player call sites (streaming and downloads).
- Not covered: ASS/SSA keeps its authored styling (no `sub-ass-override=force`, which would break typesetting); picture subtitles can't be restyled; box corners are square in the video (libass limitation) though the design rounds them. mpv can't move a centred text subtitle sideways (`sub-ass-style-overrides` margins and `sub-ass-styles` don't reach SRT; checked with mpv 0.41.0), so an off-centre horizontal position anchors the subtitle to that side with `sub-margin-x` between 192 and 19 scaled px: leaving 0 jumps from centred to anchored, and the margin, applied to both sides, narrows the wrap width to at least 70 % of a 16:9 picture.
- Verified: `:player:compileKotlinJvm`, `:player:compileAndroidMain`, `:shared:compileKotlinJvm`, `:player:allTests`, `:shared:jvmTest`, `:androidApp:assembleDebug`, `:desktopApp:test` pass. The bundled desktop libmpv accepts every property and value; frames rendered by mpv 0.41.0 with these options show each face picked by full name without synthetic bold and all three backgrounds. Not run in the app or on a device by me.
