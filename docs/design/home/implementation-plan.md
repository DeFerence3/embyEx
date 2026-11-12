# Home redesign: implementation plan

Design: `docs/design/home/design.html` (published: https://claude.ai/artifact/EpHvctY3sM6rsDGjTop8ok). Brief and decisions: `docs/design/home/brief.md`. Tokens: `docs/design/home/tokens.json` (Marquee, same as `:coreui` theme; system font).

API facts below were checked against the user's server spec on 2026-11-11 (Emby Server 4.11.0.11, `/swagger.json`, Swagger 2.0).

Status: **approved 2026-11-12** — every conclusion below was approved by the user as written (Continue watching: Resume, then Next up; no counts on "New in" headings).

## 0. Adaptivity rules (apply to every checkpoint)

- Layout follows the **window**, never the platform. A desktop window resized to phone size renders the phone layout; a phone driving a desktop-sized display (Samsung DeX) renders the desktop layout. No Home UI in `androidMain` / `jvmMain` / `iosMain` / `jvmAndroidMain`; everything lives in `shared/src/commonMain`.
- Input differences are handled by capability on every platform at once: hover lift (pointer hover), long-press (touch), right-click / Shift+F10 / Menu key (pointer and keyboard), arrow keys (keyboard), pull to refresh (drag) plus the Refresh button and Ctrl+R. No `if (platform == …)`.
- `DeviceConfiguration` stays unchanged (user decision). Window → value examples: phone portrait ≈393×873 → `MOBILE_PORTRAIT`; phone landscape / any window < 480 dp tall → `MOBILE_LANDSCAPE`; half-tiled desktop 768×830 → `TABLET_PORTRAIT`; 1280×800 window, 1536×864 (1080p at 125 %), DeX 1280×720 → `TABLET_LANDSCAPE`; ≥ 840×900 → `DESKTOP`.
- **New `LocalDeviceConfiguration`** (user decision: provided at the app root): `ui/core/adaptive/LocalDeviceConfiguration.kt`. Provided at the top of `EmbyExApp` from `DeviceConfiguration.fromWindowSizeClass(currentWindowAdaptiveInfo().windowSizeClass)`, so it updates on every window resize and every pushed screen (Details, Settings, Downloads) sees the same value. Library and Search keep computing their own for now (user decision).
  - `staticCompositionLocalOf` whose default **throws** ("LocalDeviceConfiguration not provided"), so a missing provider fails loudly instead of silently rendering the phone layout. Previews wrap content in a small `PreviewDeviceConfiguration(configuration) { … }` helper.
- Home maps the five values onto four layouts with one exhaustive `when`, in `ui/screens/home/components/HomeLayout.kt`:

  | DeviceConfiguration | HomeLayout | Design frame |
  |---|---|---|
  | `MOBILE_PORTRAIT` | `Phone` | phone |
  | `MOBILE_LANDSCAPE` | `Short` | landscape (873×393) |
  | `TABLET_PORTRAIT` | `Medium` | tablet |
  | `TABLET_LANDSCAPE`, `DESKTOP` | `Wide` | desktop |

  Components take `layout: HomeLayout` as a parameter (read once in `HomeContent` from the local) so each one has a preview per layout. Card menu, Edit Home and the source status use sheet on `Phone` (card menu also on `Medium`) and menu / dialog on `Short` and `Wide`, exactly as in the design.
- Text scaling: layouts use dp from tokens and `LocalDensity.fontScale` only where the design says so (cards grow with text; bottom-bar labels capped at 1.5×).

## 1. What already exists

| Need | Where | Status |
|---|---|---|
| Home data | `EmbyRepository.home()` → `RemoteEmbyRepository.home()` (`/Users/{id}/Views`, `/Items/Resume` limit 20, `/Items/Latest` limit 24 Movie+Episode) | One `coroutineScope`: any failure fails the whole page |
| Offline Home | `EmbyRepositoryImpl.home()` local branch (catalogue of downloads) | Silent fallback; no source shown on screen |
| Watched toggle | `togglePlayed` — queues offline for downloaded titles via `OfflineRepository.savePlayback` | Reuse |
| Favorite toggle | `toggleFavorite` — throws offline ("Connect to your server…") | Reuse; disabled offline in the menu |
| Connection state | `Session.isConnected`, `Session.browseLocalFiles`, `Session.offlineAccount`, `OfflineUiSession.connected`, `ServerReconnector.reconnectNow()` | Reuse for the source chip |
| Download start | `DownloadsRepo.download/confirm`, `ui/screens/downloads/components/DownloadSourceDialog.kt` | Reuse from the card menu |
| T3 notices | Details pattern: `state.notice` + `SnackbarHostState` + acknowledge action | Reuse the pattern |
| Hover scale | `ui/core/ModifierUtils.kt` `animateWithHover` (1.015 / 0.97) | Extend (see checkpoint 1) |
| Rail header with scroll buttons | `details/components/DetailsRailHeader.kt` (internal) | Promote to `ui/core/components/ShelfHeader.kt` (approved, section 3) |
| Ctrl+R refresh | `shell/components/TopAppBar.kt` via `:coreui` `RegisterShortcuts` | Keep |
| Banner setting | `SettingsPreferences.bannerEnabled` → `PageIntro` | Home stops using `PageIntro`; the setting keeps working for Libraries |

## 2. Gaps (verified against the server spec)

- **Next up:** `GET /Shows/NextUp?UserId&Limit&Fields&EnableImages&ImageTypeLimit&EnableUserData` → `QueryResult_BaseItemDto`.
- **Hide from Continue watching:** `POST /Users/{UserId}/Items/{Id}/HideFromResume?Hide=true|false` → `UserItemDataDto`. Undo = `Hide=false`. Server only.
- **Per-user Home layout:** `GET /DisplayPreferences/{Id}?UserId&Client` → `DisplayPreferences {Id, SortBy, SortOrder, Client, CustomPrefs}`; `POST /DisplayPreferences/{Id}?UserId` with the same body.
- **New in ‹library›:** `GET /Users/{UserId}/Items/Latest?ParentId={viewId}&GroupItems=true&Limit&Fields&EnableUserData` → array of `BaseItemDto` (no total count). One call per library the user enabled.
- **Favorites:** `GET /Users/{UserId}/Items?Filters=IsFavorite&Recursive=true&IncludeItemTypes=Movie,Series,Episode&SortBy=SortName&Limit&Fields` → has `TotalRecordCount`.
- **Watched / favorite responses** return `UserItemDataDto`; today's repository ignores them (return `Unit`). Home doesn't need them changed.

## 3. Approved conclusions

1. **Continue watching order.** Resume items first in the server's order (most recently played first), then Next up episodes in the server's order. A Next up episode is dropped when the same series already has a resumable episode. No guessed interleaving, because Next up items carry no last-played date.
2. **Counts.** Show a count only where the server gives a real total: Continue watching (resume total + Next up shown), Favorites (`TotalRecordCount`), Downloaded (local). "New in" headings show no number (Latest returns no total), just See all. This replaces the "24" in the design.
3. **Layout storage.** DisplayPreferences `Id = "embyex-home"`, `Client = "EmbyEx"`, `CustomPrefs`:
   `top = continue|off`, `cardSize = comfortable|large`, `sections = continue,latest:{viewId},favorites,downloaded` (order), `hidden = …` (comma list), `latestLibraries = {viewId},…`. A local copy, keyed by the offline account key (account isolation), so Home renders before the server answers, and applies offline. Last write wins; a pending local change is pushed on reconnect.
4. **Section failures.** `home()` loads every section independently (`supervisorScope` + per-section result). One failed section shows its inline Retry; the page fails (T5) only when nothing loaded and nothing is cached.
5. **Shared pieces.** Promote the Details rail header (title + count + See all + scroll buttons) to `ui/core/components/ShelfHeader.kt` and use it from both Details and Home, instead of writing a second one. That touches one Details file. Nothing goes to `:coreui`, because `:player` doesn't use these pieces.
6. **Lift.** Change `animateWithHover` to the design values (hover/focus 1.04 + elevation 2, artwork 1.06 inside the clip, pressed 0.97, `motionScheme.fastSpatialSpec()`; reduced motion: no scale). It is already used by the current cards, so Libraries and Search cards get the same lift. That's intended consistency, but it is a visible change outside Home.

## 4. Checkpoints

### Checkpoint 1 — adaptivity foundation, UI and state contract (`ui/screens/home`, `ui/core`)
- `LocalDeviceConfiguration` + provider in `EmbyExApp`; `HomeLayout` mapping; preview helper.
- `HomeState`: `content: HomeUi?` (spotlight items + selected index, sections as a sealed `HomeSectionUi` list: `Shelf`, `Failed(id)`, plus the spotlight), `source: HomeSourceUi` (Connected, Connecting, Reconnecting, Offline, NotResponding, ThisDevice), `refreshing`, `layoutPrefs`, `notice: HomeNotice?`, `fullScreen: HomeFullState?` (Loading, Offline-empty, Unreachable, Device-empty, Server-empty, All-hidden).
- `HomeAction`: `SelectSlide(index)`, `NextSlide`, `PreviousSlide`, `Refresh`, `RetrySection(id)`, `MarkWatched(id)`, `RemoveFromContinue(id)`, `ToggleFavorite(id)`, `Download(id)`, `Undo`, `AcknowledgeNotice`, plus Edit Home actions (`SetTop`, `SetCardSize`, `ToggleSection`, `MoveSection(up/down)`, `ToggleLatestLibrary`, `ResetLayout`). Navigation (open item, See all, Edit Home, Downloads) stays as callbacks from the route.
- Replace `HomeEvent.Error` with state (`notice`, `fullScreen`) so nothing is lost when no collector is attached.
- Components (`ui/screens/home/components/`, one per file, `internal`, `modifier` first optional param, previews per `HomeLayout` and state): `HomeTopBar` (+ `SourceChip`), `SourceStatus` (sheet/dialog), `Spotlight`, `Filmstrip` (tab list semantics, arrows), `HomeShelf`, `PosterCard`, `WideCard`, `CardMenu` (sheet/menu), `HomeSkeleton`, `HomeFullState`, `SectionError`, `EditHomeContent` (sheet/dialog). Bottom-bar fixes go straight into `shell/components/EmbyExBottomBar.kt` (icon on every tab, `contentDescription` only once, account label "Account and settings, ‹user›", 56/64 dp targets, label scale cap).
- Strings: all new copy in `composeResources/values/strings.xml` with a `home_` prefix (the design's wording).
- Colors: new theme roles for text and controls on artwork (`onScrim`, `onScrimVariant`, `onScrimTrack`) in `:coreui` theme, if the theme doesn't already expose equivalents; no hex in feature code.

### Checkpoint 2 — domain contracts and models
- `EmbyHome` → `continueWatching: HomeSection<EmbyItem>`, `latestByLibrary: List<HomeLibrarySection>`, `favorites`, `downloaded`, each with its own success/failure; `nextUp` merged per conclusion 1.
- `EmbyRepository`: `hideFromResume(itemId, hide)`, `homeSection(id)` for retry and See all paging; `HomeLayoutRepository` (new contract in `domain/repository`): `observe(): Flow<HomeLayoutPrefs>`, `update(change)`, `reset()`.
- `HomeLayoutPrefs` data class (top, cardSize, ordered sections, hidden set, latest libraries) with a default.

### Checkpoint 3 — data implementation and integration
- `RemoteEmbyRepository`: Next up, Latest per library with `GroupItems=true`, Favorites, HideFromResume, DisplayPreferences get/post. Same `ITEM_FIELDS`, `safeApiCall`, no change to the base Ktor setup or serialization config.
- `EmbyRepositoryImpl`: per-section offline fallback (downloads catalogue), "This device" path, account check as today.
- `HomeLayoutRepositoryImpl`: local cache keyed by account + server sync per conclusion 3; Koin registration following existing conventions.
- Shell: top bar uses `HomeTopBar` on the Home tab; account menu gains Edit Home; See all routes (`HomeSectionScreen(sectionId)`, `ContinueWatchingScreen`) as new `Screen`s using the existing navigator; Settings gets an "Edit Home" entry.
- Remove `PageIntro` from Home (the setting still controls Libraries).

## 5. Verification

- `./gradlew :shared:compileKotlinJvm`, `:player:compileKotlinJvm`, `:shared:allTests`, `:androidApp:assembleDebug`, `:desktopApp:test`, `check`. iOS framework linking needs a macOS host (not available here).
- Desktop (`:desktopApp:run`): resize the window through 390×844 (phone), 873×393 (short), 768×830 (tablet), 1280×800 and maximised (wide). Each must match its design frame; hover lift, right-click menu, arrow keys, Ctrl+R, Esc.
- Android phone (adb): portrait, landscape, 200 % font, TalkBack labels on cards, the filmstrip ("slide 1 of 8"), the source chip. Desktop-sized display on the phone: `adb shell wm size 1920x1080` + `wm density 160`, checked for the Wide layout, then reset with `wm size reset` / `wm density reset`. I'll ask before changing the phone's display settings.
- States to walk on both platforms: loaded, refreshing, section failed, offline with downloads, server not responding, browsing this device (the state seen on the phone), Edit Home save and offline.

## Progress

### Checkpoint 1 — done 2026-11-12 (uncommitted)
- `LocalDeviceConfiguration` (`ui/core/adaptive/LocalDeviceConfiguration.kt`) provided at the top of `EmbyExApp`; `HomeLayout` mapping in `ui/screens/home/components/HomeLayout.kt`.
- `ShelfHeader` promoted to `ui/core/components` (Details' People and Season rails use it; their scroll buttons are now tonal and name the shelf). `DetailsRailHeader.kt` and the `details_scroll_*` strings removed.
- `animateWithHover` now 1.04 / 0.97; new `artworkZoom` (1.06). Applies to Library and Search cards too.
- Home: new state/actions/ViewModel (no `HomeEvent`; notices live in state), UI models in `home/model/`, components in `home/components/`, previews in `HomePreviews.kt`. Spotlight + filmstrip, Recently added shelf (one mixed row until checkpoint 3 adds per-library rows), card menu (sheet or anchored menu by layout) with Mark watched / unwatched (+ Undo), Favorites, Go to series; source chip + status sheet/dialog; T4 offline / this-device callout; full-screen states; skeleton; pull to refresh; snackbar notices.
- Shell: Home top bar with source chip and busy Refresh; bottom bar shows an icon on every tab, stacked on phone windows, labels capped at 1.5×, Tab semantics, account button labelled "Account and settings, ‹user›".
- Removed dead code: `MediaRow`, `LibraryRow`, `WideMediaCard` (Home-only).
- Not in checkpoint 1 (needs checkpoint 2–3 data): Next up, Remove from Continue watching, Download from the card menu, per-library "New in" rows, Favorites and Downloaded rows, See all screens, Edit Home. No scrim colour roles were needed (Home never puts text on artwork).

Verification: `:shared:compileKotlinJvm`, `:shared:jvmTest`, `:player:compileKotlinJvm`, `:androidApp:assembleDebug` pass. Pre-existing failures, also on unmodified HEAD: `:shared:testAndroidHostTest` (4 × `GithubUpdateProviderTest`) and `:shared:compileKotlinIosSimulatorArm64` (`MainViewController.kt` calls a missing `App()`).

Device checks (2026-11-12):
- Phone M2101K6P (portrait, dark, dynamic colour, real library): spotlight, filmstrip selection, card menu sheet, status sheet.
- Desktop window resized to 390×844 → phone layout; 873×393 → Short; 768×830 → Medium (shelf scroll buttons appear when a row overflows); 1280×800 and maximised 1536×864 → Wide.
- Test phone (Redmi Note 5 Pro, Android 11): portrait ↔ landscape switches Phone ↔ Short; `wm density 160` (≈2160×1080 dp, DeX-like) → the desktop layout.

Fixes found on device:
- `ProvideDeviceConfiguration` now measures the app's root area (`BoxWithConstraints` + `WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass`). With `currentWindowAdaptiveInfo()`, `LocalWindowInfo.containerSize` lagged one rotation behind on Android (the activity handles `configChanges`), so landscape rendered the portrait layout and vice versa. Library and Search still call `currentWindowAdaptiveInfo()` themselves and have the same lag.
- The source chip showed "· Connecting…" without a server name during the first load; the name is now seeded at start.

### Checkpoint 2 — done 2026-11-12 (uncommitted)
- Domain (`domain/model/home/`): `HomeSectionId` (stable keys `continue`, `latest:{id}`, `favorites`, `downloaded`), `HomeSectionResult` (`Loaded(items, total)` | `Failed`), `HomeTop`, `HomeCardSize`, `HomeSectionPref`, `HomeLayoutPrefs` (+ `resolvedFor(libraries)`: first-run defaults, saved order kept, removed libraries dropped, new libraries appended — shown for movies/TV), `HomeLayoutSync`. `EmbyHome` is now `libraries` + `sections: Map<HomeSectionId, HomeSectionResult>`.
- Contracts: `EmbyRepository.home(layout)`, `homeSection(id, startIndex)`, `hideFromResume(itemId, hide)`; new `HomeLayoutRepository` (implementation in checkpoint 3).
- Minimal data adapters so it runs: sections load independently (`supervisorScope`); Continue watching = Resume (Next up merge in checkpoint 3); "New in" = Latest per library with `GroupItems=true`; Favorites = `Filters=IsFavorite`; HideFromResume POST; Downloaded and offline sections from this device.
- UI: "New in ‹library›", Favorites, Downloaded rows (16:9 cards for Continue watching / Downloaded), per-section Retry, counts only with a real total, Next up derived for episodes only, shared `focusRing` on Home/shell controls, Library and Search read `LocalDeviceConfiguration`.
- Design: Edit Home's separate library checkboxes removed (one control per "New in" row).
- Found: Tab order goes top bar → bottom bar → page content, because `Scaffold` composes the bottom bar before the body (pre-existing; fix needs a shell change — pending approval).

### Checkpoint 3 — done 2026-11-12 (uncommitted)
- Continue watching = Resume, then `/Shows/NextUp` (Next up dropped when its series has a resumable episode; Next up failure doesn't fail the row).
- `HomeLayoutRepositoryImpl` (`data/repository/home/`): local copy per account (`home_layout:{serverId}:{userId}` in DataStore via new `EmbyPreference.observeString/setString`), synced with `/DisplayPreferences/embyex-home` (Client `EmbyEx`, CustomPrefs `top`, `cardSize`, `sections` = `key=1|key=0`); changes apply locally, are marked pending, pushed on start/reconnect; `HomeLayoutCodec` drops unknown keys. Browsing this device keeps a local-only layout.
- Edit Home (sheet on Phone, dialog otherwise): top of Home, card size, section switches + Move up/down (Continue watching pinned while it's the top), Reset with Undo, sync status line (shows "Saved on this device" whenever Home isn't connected). Opened from the end of Home and from the account menu. Edits made offline keep the server libraries' rows (`resolvedFor(keepMissingLibraries = true)`).
- See all: `HomeSectionScreen(key, title)` + `HomeSectionViewModel`, grid like Library, pages via `homeSection(startIndex)` near the end of the grid.
- Card menu: Remove from Continue watching (HideFromResume, Undo), Download (Details' source picker + permission flow). Large cards scale card widths 1.35×.
- "Sign in again": when the saved sign-in expired (`isLoggedInState` false, offline account kept) the chip, status, banner and full state say so and link to sign-in, instead of "offline".
- Shell: bottom bar drawn after the tab content (Tab order: top bar → page → bottom bar); account menu has Edit Home.

Device checks (test phone, offline): Tab order top bar → content → bottom bar; account menu → Edit Home; Large saved locally and kept across restart; offline status line. Not yet checked with a live server: Next up merge, server sync of the layout, See all, Remove from Continue watching, Download from the menu, Sign in again.
