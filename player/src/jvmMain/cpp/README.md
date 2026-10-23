# Linux Tao player bridge

`linux_readback.cpp` attaches libmpv's software render API to the **existing**
MediaMP player. It owns one worker and two CPU buffers, independently of any Tao
window. A new inlay/PiP surface reads the current frame without loading media again.
Shutdown joins the worker and frees the render context before MediaMP destroys mpv.
JNI calls use a registry so a retired surface cannot access freed native memory.

This bridge is pinned to `org.openani.mediamp:mediamp-mpv-runtime-linux-x64:0.5.2`.
The Linux x64 runtime exports `mediampv::find_handle(long)`,
`mediampv::mpv_handle_t::mpv() const`, and
`mediampv::mpv_handle_t::notify_render_update()`. The bridge resolves these exact
Itanium C++ ABI symbols from the library already loaded by MediaMP, and holds the
registry's `std::shared_ptr`. It never reads the private object's layout and never
loads a second system libmpv. This is an internal ABI dependency, not a supported
upstream extension API. Recheck the symbols, libstdc++ ABI, frame-state packing,
and Java adapter against the tagged source and runtime binary on every upgrade.
Missing symbols fail initialization with a sanitized player error.

The two unmodified ISC-licensed headers in `vendor/mpv` come from mpv v0.41.0:

- https://github.com/mpv-player/mpv/blob/v0.41.0/include/mpv/client.h
- https://github.com/mpv-player/mpv/blob/v0.41.0/include/mpv/render.h

Their copyright/license notices are retained. MediaMP's bundled runtime supplies
the implementation; no additional media library dependency is introduced.

Build on Linux with a C++20 compiler and Java 21 JNI headers. Gradle automatically
invokes `build-linux.sh` for JVM resources, including native-image packaging. Build
release binaries on the oldest supported Linux distribution to avoid accidentally
raising the required glibc/libstdc++ versions. The runtime currently supports x64
Linux, x64/arm64 Windows, and x64/arm64 macOS; Linux arm64 remains unavailable upstream.

Software rendering performs scaling, color conversion and subtitles on the CPU.
It works independently of Tao's X11/Wayland graphics context but does not promise
GPU-path throughput or HDR fidelity. Validate high-resolution/subtitle-heavy media
on target hardware. Windows and macOS use MediaMP's GPU producer with CPU readback.
