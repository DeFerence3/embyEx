# Design brief: EmbyEx — Home redesign

Status: design final + implementation plan approved (2026-11-12)
Artifacts: options round 1 https://claude.ai/artifact/53twwGDrhYiPsj8Rbjhqr4 · round 2 https://claude.ai/artifact/5JzfC5wrCXq6GJLcsJvMHS · design https://claude.ai/artifact/EpHvctY3sM6rsDGjTop8ok

Separate from the Details design in `docs/design/` and the Player design in `docs/design/player/`. Reuses the Marquee tokens and brand (`docs/design/tokens.json`).

## Product
- What it is: Unofficial Emby media client (Kotlin/Compose Multiplatform). Browse library, play with the built-in player, download for offline.
- Users and context of use: owners/members of a personal Emby server; phone one-handed, tablet on the couch, desktop at a desk with keyboard and mouse.
- Platforms / viewports: Android phone, Android tablet, desktop (Windows/Linux) → phone, tablet, desktop
- Themes: light + dark, follow system (Android dynamic color can override)
- Accessibility target: WCAG 2.2 AA + full keyboard navigation on desktop (Tab between sections, arrow keys inside rows, visible focus), screen-reader friendly rows (headings, item count, position), 200% text, reduced motion, no gesture-only actions
- Implementation target: Compose Multiplatform, Material 3 Expressive

## Mood and brand
- Feels like: Material 3 Expressive, cinematic, calm, the app's own identity (ink and mint, amber accent, asymmetric expressive shapes)
- Must not feel like: a streaming-service clone; a wall of identical cards
- Fixed brand assets: Marquee tokens (`docs/design/tokens.json`): primary #006B54 / #76D8B3, tertiary amber, radii 8/12/20/28/36, system font only
- Density: simple, uncluttered; redesign only what improves accessibility or clarity

## Content
- Language / locale: English (Compose resources)
- Voice: short, plain, says what happened and what to do (matches Details and Player)
- Sample data domain: movies and TV episodes from a personal Emby library

## Current Home (as found in repo, 2026-11-11)
- Top bar: "EmbyEx" + server-name chip; tonal Refresh button (Ctrl+R).
- Intro banner `PageIntro` ("YOUR COLLECTION, YOUR TIME / Find your next great watch."), hideable in Settings ("banners"). No action.
- Rows: Continue watching (`/Items/Resume`, 20, wide 16:9 cards with % watched), Recently added (`/Items/Latest`, 24, Movie+Episode mixed, posters; episodes render as wide list cards), Your libraries (`/Views`, 16:9 cards, asymmetric shape).
- Empty: "No media available". Load error: full-page "A little interruption" + Try again. Loading: centred expressive spinner.
- Offline: repository falls back to the local catalogue (downloads) silently; no visible offline indicator on Home.
- Bottom bar: pill tabs Home / Libraries / Search; only the selected tab shows an icon; account avatar opens menu (Downloads, Settings); avatar button has label "Account".
- Cards: tap opens Details (playable) or Library (folders). No other actions.
- Observed on the phone (2026-11-11, dark, dynamic color): chip shows "M2101K6P" (device model, not a server), Home says "No media available / Media from your library or saved on this device will appear here." Refresh doesn't change it. The page never says whether it's showing the server or this device, why it's empty, or what to do. → Home needs a visible source indicator and distinct empty states (server library empty, nothing downloaded while offline / browsing this device, server unreachable).

## Screen inventory
| Feature | Screen | Primary action / question | Notes |
|---|---|---|---|
| Home | Home | Resume something (Continue watching carousel at the top) | Top = Continue watching carousel (resume + Next up), user-moved only, no auto-advance, "1 of 8" + prev/next + See all. Sections below (default order): New in ‹library› (one row per library, episodes grouped by series), Favorites, Downloaded on this device. No Libraries section (it's a tab). Each section heading has a count + "See all". Pull to refresh (phone), Refresh + Ctrl+R. "Edit Home" at the end. Shared hover/focus lift on every card. |
| Home | Card actions | Act on a title without opening it | ⋮ button on cards + long-press / right-click. Mark watched / unwatched, Remove from Continue watching, Go to series, Add to / remove from favorites, Download. Sheet on phone, menu on desktop. |
| Home | Edit Home | Make Home your own | Per user, saved on the server (Emby display preferences), follows the user to every device; last saved copy used offline. Controls: top of Home (Continue watching carousel / Off = Continue watching as a normal shelf); card size (Comfortable / Large); sections on/off + Move up / Move down; which libraries get a "New in" row; Reset to default. Its own page in Settings (Settings › Home › Edit Home), one centred column at every window size; the Home footer and the account menu open the same page. A small schematic preview of Home sits at the top and follows every change; it keeps one height so the controls below never move. States: saving, saved, save failed (kept on device, retries), offline (changes apply locally, sync later). |
| Home | See all (section grid) | Browse a full section | Vertical grid of one section; keyboard/screen-reader alternative to horizontal rows. |
| Shell | Bottom bar + account menu | Switch tab | Keep pill bar; icon on every tab, labelled account button ("Account and settings, ‹user›"), 48 dp targets. |
| Shell | Top bar source chip | Where is this from? | Chip names the source: server name + status (connected / reconnecting / offline) or "On this device". Tapping explains and offers Reconnect / Downloads. |

## Decisions log
- 2026-11-11 · Scope: Home redesign, separate folder `docs/design/home/`, reuse Marquee tokens · consistent with Player redesign
- 2026-11-11 · Sections: Continue watching incl. Next up; Latest per library; Libraries row; Favorites; Downloaded · user selection
- 2026-11-11 · Top: Resume hero replaces the decorative intro banner · user choice (one clear primary action)
- 2026-11-11 · Hero shows one title, not a carousel · user choice (calm; no auto-rotation)
- 2026-11-11 · Card menu (⋮ + long-press / right-click) · user choice (no gesture-only actions)
- 2026-11-11 · Navigation: keep pill bottom bar, fix a11y only · user choice
- 2026-11-11 · Edit Home sheet: show/hide + reorder with buttons · user choice
- 2026-11-11 · "See all" on every section · my call: grid alternative to horizontal scrolling for keyboard, switch and screen-reader users

- 2026-11-11 · Brief approved · user confirmed
- 2026-11-11 · Customisation: hero choice, card size, library rows; not card style per section · user selection
- 2026-11-11 · Home layout saved per user on the server (Emby display preferences) · user choice (separate preferences per account, same on every device)
- 2026-11-11 · Round 1 feedback: remix A Shelf + B Spotlight · user choice
- 2026-11-11 · Libraries section removed from Home (the Libraries tab already covers it) · user call; "pin libraries" option dropped with it
- 2026-11-11 · Top of Home becomes a Continue watching carousel; the separate Continue watching row is removed (it duplicated the hero). Supersedes "one title" hero · user call. A11y conditions (mine): never auto-advances, visible prev/next buttons + "1 of 8", arrow keys, each slide announced "slide 1 of 8", reduced motion = no slide animation
- 2026-11-11 · Hover: one shared lift animation for every card (scale + shadow + art zoom inside the frame; focus gets the same plus the ring), no per-card shape morph · user call (morphing shapes costs a change to every card). Extends today's animateWithHover (1.015 → stronger)
- 2026-11-11 · Direction: E Spotlight + filmstrip · user choice; tokens copied to docs/design/home/tokens.json
- 2026-11-11 · State plan approved · user "go"
- 2026-11-11 · Offline card actions (corrected after reading EmbyRepositoryImpl): Mark as watched queues for downloaded titles (savePlayback) and syncs later; favorites, Remove from Continue watching (HideFromResume) and Go to series need the server, so they are disabled with the reason
- 2026-11-11 · Edit Home: Continue watching row is pinned (move buttons disabled) while Top of Home = Continue watching · my call
- 2026-11-11 · Tablet/desktop spotlight: art starts at 40 % and the text column sits on solid page colour (light-theme contrast) · my call after review
- 2026-11-11 · Bottom-bar labels stay visible at 200 % text, scaled to 1.5× at most · my call (hiding them hurt accessibility)
- 2026-11-11 · Account menu gains Edit Home · my call (findable without scrolling to the end)
- 2026-11-11 · Adaptivity (user rule): layout follows the window via DeviceConfiguration only, never the platform; no UI in platform source sets. Mapping approved: MOBILE_PORTRAIT → phone · MOBILE_LANDSCAPE → compact side-by-side (new 'landscape' frame, 873×393) · TABLET_PORTRAIT → tablet · TABLET_LANDSCAPE + DESKTOP → desktop. DeviceConfiguration thresholds unchanged. LocalDeviceConfiguration provided at the app root; Library/Search not migrated now
- 2026-11-11 · Short windows use the menu / dialog variants (a bottom sheet doesn't fit 393 dp of height) · my call
- 2026-11-11 · API checked against the user's server /swagger (Emby 4.11.0.11): /Shows/NextUp, POST /Users/{UserId}/Items/{Id}/HideFromResume?Hide=, GET/POST /DisplayPreferences/{Id}?UserId&Client (CustomPrefs), /Users/{UserId}/Items/Latest?ParentId&GroupItems
- 2026-11-12 · Implementation plan approved (docs/design/home/implementation-plan.md): Continue watching = Resume then Next up; no counts on New in headings (Latest has no total); layout in DisplayPreferences 'embyex-home' + per-account local copy; independent sections; shared ShelfHeader; stronger lift everywhere · user approval
- 2026-11-12 · Edit Home: each library's "New in" row is a normal section (switch + Move up/down); the separate library checkbox list is dropped · user approval
- 2026-11-12 · Edit Home moves from the Home sheet/dialog to a separate Settings page · user request
- 2026-11-12 · Edit Home page gets a small live preview of Home at the top (schematic, fixed height, one spoken summary) · user request
- 2026-11-12 · Next up is derived: an episode in Continue watching without a saved position · user approval (episodes only, after a movie with no position was tagged on device)
- 2026-11-12 · Section results use a dedicated HomeSectionResult (Loaded(items, total) | Failed) · user choice
- 2026-11-12 · New libraries append at the end of the saved layout; shown for movies/TV, hidden otherwise · user approval
- 2026-11-12 · LocalDeviceConfiguration measures the app's root area (BoxWithConstraints); currentWindowAdaptiveInfo lagged one rotation on Android · user approval. Library and Search migrated to it · user approval
- 2026-11-12 · Visible keyboard focus: shared focusRing (3 dp, primary; filmstrip uses onSurface so focus differs from selection) on Home, shelf header and shell controls · design requirement, found missing on device
- Sources: generated by docs/design/home/scripts/gen_design.py (pages) + src/app.css; rebuild with design_tools.py build

## Options history
- Round 1 (2026-11-11): A Shelf (today's shelves tidied, hero art card with scrim), B Spotlight (full-bleed hero like Details, 64 px Resume, library button grid, morphing card corners), C Index (no text on art, Continue watching as a list, library tiles). Palette, shapes and system font fixed (Marquee). → user: remix A + B
- Round 2 (2026-11-11): A+B remix, Libraries section removed, Continue watching moved into a top carousel, shared hover lift. D Marquee carousel (M3 hero carousel: large card with scrim + peek, 1 of 8 + prev/next) vs E Spotlight + filmstrip (full-bleed hero + thumbnail tab strip). → E chosen

## Open questions
-

## Phase 3 state plan (Home, direction E)

Pages: Design system · Shell & global states · Home · Card actions · Edit Home · See all.

Home (phone, tablet, desktop; light + dark)
1. Loaded · connected (default): spotlight 1 of 8 (Resume), filmstrip, New in Movies, New in TV Shows, Favorites, Downloaded, Edit Home
2. Spotlight on a Next up title (slide 3): Play instead of Resume, "Next up" tag, no progress
3. Loading (first load): skeleton mirroring spotlight + strip + two shelves; chip "Harbour · Connecting…"; nav usable
4. Refreshing: content stays, thin progress under the top bar (pull-to-refresh indicator on phone), Refresh button busy
5. Nothing in progress: spotlight falls back to "Just added" (newest titles, Play); strip shows them
6. Top of Home off (Edit Home): Continue watching becomes the first normal shelf
7. Large cards (Edit Home): bigger posters and type, fewer per row
8. Partial failure: "Couldn't load New in TV Shows. [Retry]" inline in that section only
9. Background refresh failed: T3 "Couldn't refresh Home · Retry", content kept
10. Reconnecting: chip "Harbour · Reconnecting…", content kept; after ~10 s becomes state 11
11. Offline with downloads: T4 "You're offline. Showing the 2 titles on this device. Home updates when Harbour is back. [Try again]"; only on-device titles; network actions disabled with reason
12. Offline, nothing downloaded: T5 "You're offline and nothing is downloaded yet. Connect to Harbour to browse your library. [Try again]"
13. Server unreachable, nothing cached: T5 "Couldn't reach Harbour. The server didn't respond. Check that it's running, then try again. [Try again] [Server settings]"
14. Browsing this device (the state seen on the phone): chip "This device"; T4 "Showing videos on this device. Connect to a server to see your library. [Connect to a server]"
15. This device, nothing there: empty "No videos on this device yet. Downloads and videos you add show up here. [Connect to a server]"
16. Server library empty: "Harbour has no movies or shows yet. Add media folders in the Emby dashboard, then refresh. [Refresh]"
17. All sections hidden: "Your Home is empty because every section is hidden. [Edit Home]"
18. Stress: 1 title in progress (no prev/next, no strip), long titles (2 lines, then ellipsis; full name in label), missing artwork (tinted fallback), 240 in a count, 200 % text

Card actions
19. Menu (desktop, anchored to ⋮ / right-click) and bottom sheet (phone, title header): Mark as watched, Remove from Continue watching, Go to ‹series›, Add to favorites, Download
20. Marked as watched: title leaves the carousel, T3 "Marked 'The Long Way Round' as watched · Undo"
21. Removed from Continue watching: T3 "Removed from Continue watching · Undo"
22. Action failed: T3 "Couldn't update 'Kestrel'. Check your connection · Retry", state reverts
23. Offline menu: network actions disabled with "Needs a connection to Harbour"
24. Download from the menu: one source → T3 "Downloading 'Kestrel' · View"; several → the source picker from the Details design

Edit Home (sheet on phone, dialog on desktop; changes save automatically)
25. Default: Top of Home (Carousel / Off), Card size (Comfortable / Large), Sections (switch + Move up / Move down), "New in" libraries (Movies, TV Shows, Kids, Concerts), Reset to default; status "Saved to your account"
26. Saving…
27. Save failed: T4 in the sheet "Couldn't save to Harbour. Your layout is kept on this device and syncs when it can. [Retry]"
28. Offline: T4 "Changes apply here now and sync to your account when you're back online."
29. Reset to default: reversible, T3 "Home reset to default · Undo"

See all (section grid)
30. Loaded (New in Movies · 24, grid), Continue watching as a list
31. Loading more / end of list
32. Empty (Favorites 0): "No favorites yet. Use ⋮ › Add to favorites on any title."
33. Failed: T5 with Back kept

Shell & global
- Bottom bar (phone: icon above label; desktop: icon beside label), account menu (Downloads, Settings, header with user + server)
- Source chip sheet/popover per status: Connected (server, user, last refreshed, [Refresh]), Reconnecting, Offline ([Try again] [Open downloads]), This device ([Connect to a server])
- Keyboard map: chip → Refresh → spotlight title → Resume → ⋮ → filmstrip (← →) → See all → each shelf (heading See all, then ← → inside, Tab leaves) → Edit Home → bottom bar. Ctrl+R refresh, Shift+F10 / Menu key opens the card menu, Esc closes
- 200 % text reflow, reduced motion

Clickable flows: filmstrip → spotlight; Resume → player handoff; ⋮ → Mark watched → Undo; Edit Home → toggle → Saved; See all → grid → Back; source chip → status sheet; offline → Try again → loaded; partial failure → Retry.
