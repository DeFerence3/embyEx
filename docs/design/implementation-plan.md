# Details screen redesign: implementation plan

Design: `docs/design/design.html` (published: https://claude.ai/artifact/PFGgEXB9HN9ubbHtVyMGTf). Brief and decisions: `docs/design/brief.md`. Tokens: `docs/design/tokens.json` (they match `ui/theme` colors and shapes; type stays the system font).

API facts below were checked against the user's server Swagger spec (Emby Server 4.11.0.11, `/swagger`, Swagger 2.0) on 2026-11-09.

## What already exists

| Need | Where | Status |
|---|---|---|
| Item details | `GET /Users/{UserId}/Items/{Id}` in `RemoteEmbyRepository.item` | Returns full `BaseItemDto` (People, Genres, MediaStreams, UserData) |
| Favorite toggle | `POST`/`DELETE /Users/{UserId}/FavoriteItems/{Id}` in `toggleFavorite` | Exists; response is `UserItemDataDto` (currently ignored) |
| Watched toggle | `POST`/`DELETE /Users/{UserId}/PlayedItems/{Id}` in `togglePlayed` | Exists; same response |
| Cast photos | `EmbyPerson.primaryImageTag`, `id` | Model has them; URL: `/Items/{personId}/Images/Primary?Tag=…&MaxWidth=…` |
| Ratings | `EmbyItem.officialRating`, `communityRating` | Exist |
| Download sources | `DownloadsRepo.download/confirm/remove`, `DownloadSource` (original media sources) | Exists; no inline per-item progress on details |
| Offline state | `OfflineRepository.observeMedia/observeDownloads`, `OfflineUiSession.connected` | Exists |

## Gaps (from the spec)

- **Genres**: `BaseItemDto.Genres: [string]` (also `GenreItems: [{Name, Id}]`). `EmbyItem` has no `genres` field yet, though `ITEM_FIELDS` already requests `Genres`.
- **Person types**: `PersonType` enum is `Actor, Director, Writer, Producer, GuestStar, Composer, Conductor, Lyricist`. The cast & crew rail shows `Actor`, `GuestStar`, then `Director`, `Writer`.
- **Track labels**: `MediaStream` also has `Title`, `ChannelLayout`, `Channels`, `IsHearingImpaired` (for the SDH tag). `EmbyMediaStream` lacks these.
- **Season rail / prev-next**: `GET /Shows/{SeriesId}/Episodes?SeasonId=…&UserId=…&Fields=…` (watched state comes from `UserData`); `AdjacentTo={episodeId}` returns neighbours, which also covers the cross-season Next (S2 E8 → S3 E1).
- **Track choice before play**: playback is a static direct stream (`/Videos/{id}/stream.{ext}?Static=true`), so the server doesn't transcode. Audio/subtitle choice must be passed to the player as preferred stream indexes (mpv `aid`/`sid` on Android, the MediaMP engine on desktop). `PlaybackInfoRequest.AudioStreamIndex/SubtitleStreamIndex` only matter if transcoding is added later.

## Checkpoints

1. **UI and state contract** (`ui/screens/details`)
   - Extend `EmbyDetailsState` with a details UI model: identity (title/logo, kicker for episodes), meta, genres, play mode (play / resume / play again), toggles with pending flags, download status (idle, queued, progress %, paused, done, failed), selected audio/subtitle, people, season episodes (loading / loaded / failed), prev/next.
   - New actions: `ToggleFavorite`, `ToggleWatched`, `UndoWatched`, `SelectAudio(index)`, `SelectSubtitle(index?)`, `PauseDownload`, `CancelDownload`, `RetrySeason`, `DismissPlaybackError`.
   - Replace the `AlertDialog` for errors with the tiered surfaces in the design (snackbar host for T3, inline callouts for T4, `LoadingScaffold` stays for T5, `AlertDialog` only for Remove download).
   - Components: split into `components/` per the design (hero, action tiles, download tile, track chips + sheet/menu, cast rail, season rail, episode nav, media info). The new shared pieces (action tile, track chip, menu with radio items) go in `:coreui` only if `:player` reuses them.
   - Layout: one adaptive layout keyed on window width class (compact < 700, medium, expanded ≥ 1100), replacing the separate portrait/landscape/desktop layouts.
2. **Domain models**
   - `EmbyItem.genres: List<String>`; `EmbyMediaStream.title`, `channelLayout`, `isHearingImpaired`.
   - `EmbyRepository.seasonEpisodes(seriesId, seasonId)` and `adjacentEpisodes(seriesId, episodeId)`.
   - `toggleFavorite`/`togglePlayed` return the updated `EmbyUserData` so the UI reflects the server's state (and reverts on failure).
   - `EmbyPlaybackItem` gains `audioStreamIndex: Int?`, `subtitleStreamIndex: Int?`.
   - A per-item download status flow (derived from `OfflineRepository.observeDownloads` / transfer state).
3. **Data implementation and integration**
   - Map the new fields, add the `/Shows/{Id}/Episodes` calls, person image URLs, the download status flow, and pass track preferences to the player engines (Android mpv-android, desktop MediaMP). Offline: queue favorite/watched changes through the existing pending-sync path if it supports them; otherwise disable the toggles offline with a reason.

## Copy notes

- Downloads are original media sources: the picker lists the server's versions (e.g. 2160p MKV, 1080p MP4); with one source, Download skips the picker.
- Playback failure copy refers to the device not opening the file, not to server conversion.

## Verification

`./gradlew :shared:compileKotlinJvm`, `:player:compileKotlinJvm`, `:shared:allTests`, `:androidApp:assembleDebug`, `check`. Manual checks: phone, tablet and desktop widths; keyboard order (Back → Play → Start over → Favorite → Watched → Download → Audio → Subtitles → Read more → rails); 200% font scale; offline with and without a download.
