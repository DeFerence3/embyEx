# EmbyEx

A simple emby client, opens media in mpvEx

> [!WARNING] 
> ### Vibe Coded!!
> This project is almost entirely vibe coded, caution referencing this repository
>

- [Emby](https://emby.media/)
- [mpvEx](https://github.com/marlboro-advance/mpvEx)

## Offline downloads

Download an individual movie or episode from its details screen, selecting a media
version when offered. Downloads keep the original file; they do not transcode it.
Android uses mpvEx for playback and desktop uses your configured mpv installation.
iOS offline playback is not supported in this implementation.

Saved media lives in persistent app-managed storage, hidden from Gallery. It is
not disposable cache. Supported external subtitles and available artwork are
saved alongside it; failures of optional assets do not invalidate the media file.

- Use **Home**, **Libraries**, **Search**, and the normal details screen for both
  server media and saved media. A small check/download icon marks media available
  offline. **Browse local files** on sign-in opens this same shell with saved media
  from this device, without contacting a server.
- Open **Downloads** from the account menu to monitor, pause, resume, retry or remove a download. On
  Android, **Resume queue** starts a new background job when a visible action is
  required. The unmetered-only setting can prevent downloads on metered networks.
- Completed downloads, saved details and local search remain available when the
  server is unavailable, including after restarting the app. Incomplete or missing
  files cannot be played. Available local copies are preferred while online too.
- Reconnection refreshes metadata and syncs saved watch progress while the app is
  in the foreground. It keeps downloaded files and leaves manually paused jobs
  paused. **Watch progress** lets you resolve conflicting device/server positions.
- Missing or inaccessible server items keep their local copies. Automatic sync
  pauses until you explicitly retry. Signing out hides that account's downloads until you sign in
  or choose **Browse local files**; it does not delete them. **Remove** deletes the local payload. Uninstalling the
  Android app removes its private downloads; media payloads are excluded from backup.

Android progress capture depends on the external player returning a usable result.
If mpvEx returns no position, close playback and use **Finish playback** when shown;
the previously saved position is preserved. Offline playback does not increment
server play counts. Downloaded subtitles are limited to supported text formats.

See the [execution plan](docs/offline-download-execution-plan.md),
[playback limitations](docs/offline-download-playback.md), and
[synchronization behavior](docs/offline-download-sync.md) for implementation details.

Current verification found a native crash in mpvEx 1.2.9 on the Android test
device when opening downloaded media. Android playback is not yet release
verified; see the [readiness report](docs/offline-download-readiness.md).
