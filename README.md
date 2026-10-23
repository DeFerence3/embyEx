# EmbyEx

An Emby client with an integrated player on Android and desktop.

> [!WARNING] 
> ### Vibe Coded!!
> This project is almost entirely vibe coded, caution referencing this repository
>

- [Emby](https://emby.media/)
- [MediaMP](https://github.com/open-ani/mediamp)

## Offline downloads

Download an individual movie or episode from its details screen, selecting a media
version when offered. Downloads keep the original file; they do not transcode it.
Android uses MediaMP with ExoPlayer; desktop bundles MediaMP with libmpv.
No external player installation or executable setting is required.
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

The embedded player resumes saved positions and checkpoints progress every ten
seconds, on pause/seek, and when closed. Server reports are ordered and the final
save completes before refreshing details. Failed server saves offer Retry. Local
checkpoints use the existing revision checks and offline synchronization queue.
If media never opens, the previous position is preserved. Offline playback does
not increment server play counts. Downloaded subtitles are limited to supported
text formats.

MediaMP is pinned to 0.5.2, verified against Maven Central and the upstream tagged
source. The user explicitly authorized this version and its native runtime
without requiring klibs.io indexing. Only the host desktop OS/architecture runtime
is packaged. The app and player use Nucleus's default Tao backend on Windows,
macOS, and Linux. A version-pinned adapter draws MediaMP's Windows/macOS readback
frames into the app; Linux uses a small JNI bridge to libmpv's software renderer.
Linux rendering is CPU-based and can be slower for high-resolution video/subtitles.
The adapter must be reviewed when upgrading MediaMP's internal APIs. GraalVM native-image
packaging is required and remains enabled; JVM compilation alone does not verify
native playback. Android codec support follows ExoPlayer and the device decoders.
iOS playback remains unsupported.

Normal playback is an inlay covering the app content, with transparent Material icon controls.
Controls hide after three seconds of playback inactivity and reappear on pointer,
touch, or keyboard input; they stay visible while paused, seeking, or choosing tracks.
The desktop PiP action opens a title-bar-free, resizable, always-on-top Tao window.
Restore returns playback to the app. The same decoder, render producer, and progress
reporter continue through both transitions; only the view changes. The outgoing view
stays until the incoming host draws. Resize PiP using its edges and drag the video
area to move it; F toggles desktop full screen,
Space toggles playback, and Escape restores PiP/exits full screen before closing.
Android uses native PiP on supported Android 8+ devices, with system expand/close controls.
Its player is attached to the activity window, and the manifest handles PiP layout changes
without recreating playback, following [Android's PiP setup](https://developer.android.com/develop/ui/compose/system/pip-setup).
Playback controls include pause/resume, seeking, audio tracks, and subtitles.
Streaming episodes advance to the next item after the completed item's final
report is accepted. Android continues playing in PiP and pauses when the activity
stops (including dismissing PiP); other configuration recreation retains the resume position.

See the [execution plan](docs/offline-download-execution-plan.md),
[playback limitations](docs/offline-download-playback.md), and
[synchronization behavior](docs/offline-download-sync.md) for implementation details.

The earlier external-mpvEx readiness findings do not validate the new embedded
player. Verify streaming, offline resume, seeking, completion, and lifecycle
behavior on physical devices before release.

Linux desktop builds also require a C++20 compiler (`CXX`, default `c++`) and the
Java 21 toolchain's JNI headers. Gradle builds `libembyex-player.so` into generated
JVM resources; GraalVM includes it using the native-image resource metadata.
See [the renderer ABI notes](player/src/jvmMain/cpp/README.md) for the native boundary.

## Player module

`:shared` depends on the multiplatform `:player` library. The player owns its
controls, resources, playback position model, Android ExoPlayer implementation,
desktop MediaMP/libmpv adapters, Linux JNI build, and native-image metadata.
It targets Android, JVM, `iosArm64`, and `iosSimulatorArm64`; the iOS implementation
still reports unsupported playback.

The integration API is `PlayerSource`, `EmbeddedPlayerDialog`, `PlayerInlayHost`,
and `PlaybackPosition`. Existing package names are retained, including the JNI
callback names. `:shared` keeps Emby requests, authentication, progress saving,
offline file leases, and the app-level `OfflinePlaybackHost`; `:player` has no
dependency on `:shared` or Koin. The existing app theme is inherited by the player.

Compile the player with `./gradlew :player:compileKotlinJvm`. For integration,
run `./gradlew :shared:compileKotlinJvm :player:allTests :shared:allTests check`
and `./gradlew :androidApp:assembleDebug`. On Windows use `.\gradlew.bat`.
iOS framework linking and runtime verification require macOS/Xcode.
