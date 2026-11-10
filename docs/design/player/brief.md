# Design brief: EmbyEx — Player redesign

Status: phase 3 full design v1 published — iterating
Artifacts: options round 1 https://claude.ai/artifact/FfbaRwAxhCUhLbFUw96ps3 · round 2 (remix) https://claude.ai/artifact/5LYn9QtYmRMsMD6hHrAXAc · design https://claude.ai/artifact/WuXMZJYe2KRRnrAAar8cPi

Separate from the Details design in `docs/design/` (user choice, 2026-11-10). Reuses its Marquee tokens and brand.

## Product
- What it is: The built-in video player of EmbyEx (unofficial Emby client, Kotlin/Compose Multiplatform). Plays movies and episodes from a personal Emby server or from downloads.
- Users and context of use: Desktop at a desk with keyboard and mouse (windowed next to other work, or fullscreen); Android phone in hand (portrait and landscape), tablet on the couch, sometimes with a hardware keyboard.
- Platforms / viewports: Desktop (Linux/Windows) windowed, fullscreen, PiP mini window; Android phone portrait + landscape, tablet → desktop, phone, tablet
- Themes: light + dark, follow system. Chrome drawn over video is always light-on-dark (same contrast in both themes); everything outside the picture follows the app theme.
- Accessibility target: WCAG 2.2 AA + complete keyboard operation (every player action reachable by shortcut and by Tab), visible feedback for every shortcut, no reliance on hover
- Implementation target: Compose Multiplatform, Material 3 Expressive

## Mood and brand
- Feels like: Material 3 Expressive, cinematic, calm, the same player as today but tidier
- Must not feel like: a different app; a control panel covering the picture
- Fixed brand assets: Marquee tokens from `docs/design/tokens.json` (ink and mint, amber accent, radii 12/20/28/36, system font only)
- Density: uncluttered; picture first

## Content
- Language / locale: English (Compose resources)
- Voice: short, plain, says what happened and what to do (matches the Details design)
- Sample data domain: movies and TV episodes from a personal Emby library

## Current player (as found in repo, 2026-11-10)
- Desktop overlay (`PlayerOverlay`): top row = Back, title (2 lines max), Subtitles menu, Audio menu, More (Playback speed 0.5–2×, Aspect ratio Fit/Crop/Stretch). Centre = Back 10 s, Play/Pause (80 dp, shape morphs), Forward 10 s; buffering replaces Play with a spinner. Bottom row = elapsed, slider, duration (click toggles remaining) with PiP and Fullscreen buttons inline to the right of the slider.
- Desktop windowed: same overlay, video inset 32 dp with rounded corners on the app background. Fullscreen: edge to edge.
- Desktop PiP: separate always-on-top window 480×270 (min 320×180), slider + Back 10 s / Play / Forward 10 s / Restore; Esc restores.
- Android fullscreen: portrait = title top, timeline, transport, scrolling quick actions (Rotate, Audio, Subtitles, Speed, Lock, PiP, Aspect, More → Open in mpvEx). Landscape = title + tracks top, transport centre, utility row (Lock, Rotate, Speed … PiP, Aspect) above the timeline at the very bottom. Gestures: tap toggles controls, double-tap seek, vertical drag volume/brightness, slide to unlock. Options open as bottom sheets. System PiP has no in-app controls.
- Controls auto-hide after 3 s (longer with accessibility timeouts); stay while paused, failed, menu open, seeking, or keyboard focus is inside.
- Shortcuts today: Space and media Play/Pause key = play/pause; F = fullscreen; Esc = leave PiP → leave fullscreen → close. App-wide manager also holds Ctrl+R (refresh Home).
- Not available today: volume/mute control on desktop, chapters, next/previous episode, seek preview, Up next.
- Around the player (host screen): "Getting ready to play…", "Saving your watch progress…", save failure dialog (Retry / Dismiss).
- Errors: playback failure = centred surface "Could not play this media. Check your connection and media format, then retry." + Retry. PiP refusal = text line above the timeline.
- Settings today: one scrolling page with sections (account card, UI elements, Updates, server info).

## User requirements (2026-11-10)
- Almost the same UI, Material 3 Expressive.
- Timeline is full width with only the two timers left and right; every other control sits in a row below it (today PiP/Fullscreen are inline with the timeline on desktop).
- Fullscreen and shrunk states must be clearly different; the shrunk design (windowed and PiP) needs improving. In windowed mode today the controls cover the picture and the margin around the video is wasted.
- Main goal: better UX and accessibility through deep keyboard shortcut integration.

## Additions agreed in the interview
- Volume + mute on desktop (button + slider in the control row; Android keeps the drag gesture).
- Next / previous episode (episodes only).
- Chapter markers on the timeline, chapter name in the seek bubble, chapter jump keys.
- Seek preview bubble (time + chapter, no thumbnail) for pointer and keyboard.
- Up next card at the end of an episode: 10 s countdown, Play now / Cancel; no auto-advance with reduced motion or a screen reader; movies and finales close as today.
- Customisable shortcuts: Settings → Keyboard shortcuts, all app shortcuts grouped.
- Subtitle style: a page inside the Subtitles menu/sheet with font (a few bundled subtitle typefaces), size, background/outline and a live preview line. App UI stays on the system font; subtitles are the one place a bundled typeface is used.

## Default shortcut map (YouTube-style, all rebindable)
| Group | Action | Default |
|---|---|---|
| Playback | Play / Pause | Space, K, Media Play/Pause |
| Playback | Next / previous episode | Shift+N / Shift+P |
| Playback | Speed down / up (0.25× steps) | < / > |
| Seeking | Back / forward 5 s | ← / → |
| Seeking | Back / forward 10 s | J / L |
| Seeking | Previous / next chapter | Ctrl+← / Ctrl+→ |
| Seeking | Jump to 0–90 % | 0–9 |
| Seeking | Jump to start | Home |
| Volume | Volume up / down 5 % | ↑ / ↓ |
| Volume | Mute | M |
| Tracks & display | Subtitles on / off | C |
| Tracks & display | Subtitles menu / Audio menu | S / A |
| Tracks & display | Cycle aspect ratio | Z |
| Window | Fullscreen | F |
| Window | Picture in picture | I |
| Window | Back one level (menu → PiP → fullscreen → close) | Esc (fixed) |
| Help | Shortcut cheat sheet | ? |
| General | Refresh Home | Ctrl+R |

Tab / Shift+Tab move focus through visible controls; Enter or Space activates the focused control (Space is play/pause only when no control has focus). Esc, Tab and Enter are reserved and can't be reassigned.

## Screen inventory
| Feature | Screen | Primary action / question | Notes |
|---|---|---|---|
| Player | Desktop · Windowed | Play / pause while seeing the whole picture | Controls must not cover the picture; uses the space around the video. Top: Back, title (+ series · S2 · E5). Timeline full width, timers left/right. Control row below: prev, back 10, play, forward 10, next, volume · subtitles, audio, speed, more, PiP, fullscreen |
| Player | Desktop · Fullscreen | Play / pause | Edge to edge, auto-hiding overlay, same order of controls as windowed so muscle memory carries over |
| Player | Desktop · PiP mini window | Play / pause; restore | 480×270 down to 320×180; compact: thin timeline, play, back/forward, restore, close; keyboard still works |
| Player | Android phone · Portrait | Play / pause | Timeline, then transport, then quick actions (rotate, audio, subtitles, speed, lock, PiP, aspect, more). Gestures and sheets stay |
| Player | Android phone · Landscape | Play / pause | Timeline moves above the control row (today it is below) |
| Player | Android tablet | Play / pause | Landscape layout with roomier row; hardware keyboard shortcuts |
| Player | Menus / sheets | Pick a track or option | Subtitles (Off + tracks), Audio, Speed, Aspect, More (Open in mpvEx on Android), Volume popover (desktop). Menu on desktop, bottom sheet on Android |
| Player | Subtitle style | Make subtitles readable | Inside Subtitles menu/sheet: font, size, background/outline, live preview; applies immediately |
| Player | Shortcut feedback | "Did my key do something?" | Brief on-screen bezel for every shortcut: play/pause, ±5 s, ±10 s, volume %, mute, speed, subtitles on/off, chapter name |
| Player | Shortcut cheat sheet | Learn the keys | Opens with ?, grouped list, link to Settings → Keyboard shortcuts; shortcut hints also in tooltips |
| Player | Up next | Continue to next episode or stop | Card with thumbnail, S·E, title, countdown ring; Play now / Cancel |
| Player | Global states | — | Getting ready, opening/buffering, paused, controls hidden, seeking with bubble + chapters, playback failed, PiP refused, locked (Android), saving progress, save failed, long title, no chapters, movie (no prev/next), single audio track, no subtitles |
| Settings | Keyboard shortcuts | Change a shortcut | Grouped list with search; row = action, current key chips, edit; reset one / reset all |
| Settings | Record shortcut | Press the new key | Press-to-record dialog; conflict ("Already used by Mute. Replace?"), reserved key, unsupported key; cancel with Esc |

## Key flows to make clickable
- Windowed ↔ fullscreen ↔ PiP and back (Esc ladder)
- Play / pause and seek by key with on-screen feedback
- Open Subtitles menu → pick a track; volume popover → mute
- Seek on the timeline with chapter bubble
- ? → cheat sheet → Customise → Settings → record a new key → conflict → replace; reset all
- Episode end → Up next countdown → Play now / Cancel
- Playback failed → Retry

## Decisions log
- 2026-11-10 · Separate player design in `docs/design/player/`, reusing Marquee tokens · user choice
- 2026-11-10 · Platforms: desktop + Android (phone portrait/landscape, tablet) · user choice; timeline and transport are shared code
- 2026-11-10 · "Shrunk" = desktop windowed in-app player and the PiP mini window, both redesigned · user choice
- 2026-11-10 · Problems to fix in windowed state: controls cover the picture; margin around the video is wasted space · user answer
- 2026-11-10 · Shortcut convention: YouTube-style · user choice (most widely known)
- 2026-11-10 · Shortcuts customisable in Settings, covering all app shortcuts in groups · user choice
- 2026-11-10 · Add volume + mute, next/previous episode, chapter markers, seek preview bubble · user selection
- 2026-11-10 · Episode end: Up next card that auto-plays after 10 s (not with reduced motion / screen reader) · user choice
- 2026-11-10 · Layout chosen through a round of 3 directions; palette, type, shapes fixed by Marquee · user choice
- 2026-11-10 · Brief approved · user confirmed
- 2026-11-10 · Direction: remix of B (Dock) with C's heading and side panel · user: "design wise B is superior", heading and panel from C are "awesome"; other elements to be redesigned for coherence
- 2026-11-10 · Round 2 confirmed: D Dockside is the direction; tokens copied to `tokens.json` · user confirmed
- 2026-11-10 · Dock grouping kept: volume, audio, subtitles left; speed, more, panel, PiP, fullscreen right · user choice (transport stays centred beside the panel)
- 2026-11-10 · Default subtitle typeface: Nunito; Atkinson Hyperlegible and Barlow Semi Condensed offered on the Subtitle style page · user choice
- 2026-11-10 · State plan approved · user confirmed
- 2026-11-10 · Volume and brightness drags reuse the keyboard feedback pill instead of today's vertical meter · one feedback language for touch and keys
- 2026-11-10 · New shortcut P toggles the Chapters / Up next panel · follows from the closable panel
- 2026-11-10 · Subtitles use bundled typefaces (app UI stays system font); Subtitle style options page; candidate typefaces compared in the directions round · user note + choice

## Options history
- Round 1 (palette, shapes and UI font fixed by Marquee; options differ in layout, control shape, timeline and subtitle face). Generated by `scripts/gen_options.py`.
  - A Centre Stage: today's overlay; fullscreen keeps the 80 px centre Play, other controls in a row under the timeline; windowed puts timeline + one row under the picture; white M3 slider; subtitles Atkinson Hyperlegible.
  - B Dock: all controls in opaque floating pills (no scrim), same dock windowed and fullscreen, glow from the frame in the windowed margin, wavy progress while playing, labelled tiles on phone portrait; subtitles Nunito.
  - C Console: opaque console with labelled values (subtitle, audio, speed), Play at the left, chapter-segmented timeline, Chapters / Up next side panel when windowed, control strip under the PiP picture; subtitles Barlow Semi Condensed.
  - → Remix requested (2026-11-10): B is the stronger design overall; take C's heading (transparent, leading back button, no pill) and C's Chapters / Up next side panel, and redesign the surrounding elements so the result is coherent rather than a paste-up.
- Round 2 (remix, single option). Generated by `scripts/gen_round2.py`.
  - Local: `options/round-2/options.html`. Fullscreen with the panel open resizes the picture into the free area so subtitles are never covered.
  - D Dockside = B's pills, wavy timeline, glow and phone tiles + C's heading and side panel. Redesigned to fit: dock regrouped (sound and language left, playback and window right) so the transport stays centred beside the panel; panel restyled with pill tabs and pill rows, can be closed (P), floats in fullscreen, sits between picture and dock on phone portrait; B's chapter pill becomes a quiet heading label when the panel is closed; menus rounded to match the pills.

## Phase 3 state plan (Dockside)

Frames are fixed per screen (desktop 1280×800, PiP 480×270, phone 390×844 and 844×390, tablet 1180×820): windowed, fullscreen, PiP, portrait and landscape are different layouts, not responsive variants, so there is no global viewport switch. Theme switch applies to everything outside the picture.

### Foundations
- Design system: tokens via renderers; components with all states: icon button, Play, pill (dock, line, tag), timeline, volume, tooltip + key cap, feedback pill, menu, sheet, panel (tabs, chapter row, episode row), tile, Up next card, snackbar, callout, dialog, switch, segmented, text field (search), list row (settings), slide to unlock, level meter. Error framework specimens T1–T6 with this app's copy.
- Shell & behaviour: mode map (windowed ⇄ fullscreen ⇄ PiP, Esc ladder), focus order, when controls hide, getting ready, saving your place, reduced motion, text at 200 %, narrow window rules.

### Desktop player
1. Windowed — Playing · Paused · Panel closed · Up next tab · Seeking by keyboard (bubble + feedback) · Opening · Buffering · Up next countdown · Up next without countdown (reduced motion / screen reader) · Movie (no prev/next, Chapters only) · No chapters, no queue (no panel) · Long title · Playback failed (T5) · Offline (T5) · Narrow window 760 px (panel as overlay, dock collapsed)
2. Fullscreen — Controls visible · Controls hidden · Shortcut feedback while hidden · Paused · Panel open (picture resizes) · Buffering · Up next countdown · Playback failed (T5)
3. PiP — Resting · Hover / focus · Paused · Minimum size 320×180 · Buffering · Up next · Playback failed

### Android player
4. Phone portrait — Playing · Paused · Up next tab · Double-tap seek · Volume / brightness drag · Locked · Buffering · Up next countdown · Movie without chapters · Long title · PiP refused (T3) · mpvEx missing (T3) · Playback failed (T5, adds Open in mpvEx)
5. Phone landscape — Controls visible · Controls hidden · Panel open · Locked (slide to unlock) · Gesture feedback · Paused · Buffering · Up next · Playback failed
6. Tablet — Landscape playing with panel · Landscape controls hidden · Portrait playing

### Menus and panel
7. Subtitles (menu on desktop / tablet, sheet on phone) — Track selected · Off · 24 tracks grouped by language · No subtitles in this video · Track failed to load (T3, selection reverts)
8. Subtitle style — Default · Large + outline · Each typeface · Reset
9. Audio — Tracks with language and layout · Single track (button disabled with reason)
10. Speed — 0.5× to 2×
11. More — Aspect ratio · Subtitle style · Keyboard shortcuts · Open in mpvEx (Android)
12. Volume — Default · Muted · Narrow (popover)
13. Panel — Chapters · Up next · Loading · Up next failed (inline, Retry) · Autoplay off · Last episode of season · Series finale · 40 chapters with long names

### Keyboard
14. Shortcut feedback — catalogue of every pill: play, pause, ±5 s, ±10 s, chapter, jump to %, volume, mute, speed, subtitles on/off, aspect, next / previous episode, "not available here"
15. Cheat sheet (?) — Default · With customised keys · Single column (phone / tablet with keyboard)
16. Settings → Keyboard shortcuts — Default · Search results · No results · Customised rows · No keyboard connected (Android, T4) · Shortcut reset (T3 Undo) · All reset (T3 Undo) · phone layout
17. Record shortcut — Listening · Captured · Conflict (inline: Replace / Try another) · Reserved key · Modifier only

### Tier decisions and judgment calls
- Playback failed = T5 inside the picture area; heading and Back stay so nobody is trapped. One message, because the engine can't tell a network failure from an unsupported format: "Couldn't play this video. Check your connection, or the file may be in a format this device can't play. Your place at 24:16 is saved." [Try again] [Close player]; Android adds [Open in mpvEx].
- Offline when starting a stream = T5: "You're offline. Connect to your server, or play something you downloaded." [Open downloads]
- PiP refused (Android) = T3 with action: "Couldn't start picture in picture. Allow it for EmbyEx in system settings. · Settings". Today it is a text line that never leaves.
- mpvEx missing = T3: "Couldn't open mpvEx. Check that it's installed." (today a toast)
- Saving your place on close: not a blocking screen. Return at once; a small "Saving your place…" pill only if it takes longer than 300 ms; failure = T3 on the screen you return to: "Couldn't save your place at 24:16. · Retry". Today: full-screen spinner and a dialog.
- Up next failed to load = partial failure, inline in the panel with Retry; the rest of the player is unaffected.
- Subtitle track failed = T3 "Couldn't load Deutsch subtitles. · Retry", selection goes back to the previous track.
- Reset one / reset all shortcuts = done at once with T3 Undo, no confirm dialog (restoring is cheap).
- Shortcut conflict = inline in the record dialog (it belongs to that one input): "Ctrl+R is used by Refresh Home." [Replace] [Try another]. Esc, Tab, Enter are reserved.
- Controls never hide in windowed and portrait (they don't cover the picture). They hide after 3 s in fullscreen and landscape, and stay while paused, a menu is open, focus is inside, or the pointer rests on them.
- Space activates a focused control; K and the media key always play / pause. Focus starts on the player, not on a control, so Space is play / pause until Tab is pressed.
- Narrow windows: under 1100 px the panel is closed by default and opens over the picture; under 700 px the volume slider folds into its button and Speed moves into More.
- Fullscreen with the panel open resizes the picture so subtitles are never covered.
- With reduced motion or a screen reader: no countdown auto-advance, flat timeline, no shape animation, static glow.

### Flows to make clickable
- Play ⇄ Pause; Windowed ⇄ Fullscreen ⇄ PiP; panel open ⇄ closed; Chapters ⇄ Up next
- Subtitles menu → Subtitle style; More → Keyboard shortcuts → cheat sheet → Customise → Settings → Record → Conflict → Replace
- Playback failed → Try again → Opening → Playing; Up next → Play now / Cancel
- Reset all → Undo snackbar

## Open questions
- Feedback on full design v1

## Build
- Sources: `src/` (generated pages in `src/pages/`), generator `scripts/gen_design.py` + `scripts/design/*.py`. Edit the generator, not the HTML.
- `python3 scripts/gen_design.py && python3 ~/.claude/skills/design/scripts/design_tools.py build src/index.html design.html`
