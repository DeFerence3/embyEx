<h1 align="center">EmbyEx</h1>

<p align="center"> A modern, unofficial <a href="https://emby.media/">Emby</a> client for <b>Android, Windows, and Linux</b>, built with Kotlin Multiplatform and Compose Multiplatform.</p>

<p align="center">
  <a><img alt="Android" src="https://img.shields.io/badge/Android-7.0+(sdk.24)-3DDC84?logo=android&logoColor=white" /></a>
  <a><img alt="Desktop" src="https://img.shields.io/badge/Desktop-Windows%20%7C%20Linux-475569" /></a>
  <a><img alt="KMP" src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white" /></a>
</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Latest Stable" src="https://img.shields.io/github/v/release/DeFerence3/embyEx?label=release&logo=github"/></a>
  <a href="../../releases"><img alt="Latest Beta" src="https://img.shields.io/github/v/release/DeFerence3/embyEx?include_prereleases&label=beta&color=orange"/></a>
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/DeFerence3/embyEx/total?label=downloads"/></a>
  <a href="../../actions/workflows/ci.yml" ><img alt="Build and tests" src="https://img.shields.io/github/actions/workflow/status/DeFerence3/embyEx/ci.yml?branch=dev&label=build%20%26%20tests&logo=githubactions&logoColor=white"/></a>
</p>

<p align="center">
  <img alt="Licence" src="https://img.shields.io/badge/License-GPL--3.0-blue"/>
</p>

<p align="center">
  <a href="#-get-started">⬇️ Install EmbyEx</a> | <a href="../../issues">🐛Report an issue</a>
</p>



> [!WARNING]
> #### *This is a vibe coded project!!*
>
> I tried my best to inspect and verify the code my self, but there can be slop included.
> De-sloping is going on, even though caution referencing the repository
>

## ✨ Highlights

- **Explore your library** — Browse movies and shows, search your collection, and view rich media details.
- **Watch your way** — Enjoy a built-in player with seeking, audio and subtitle selection, playback speed, and picture-in-picture on supported devices.
- **Take it offline** — Download movies and episodes in their original format and watch without a server connection.
- **Pick up where you left off** — Resume playback and sync saved watch progress when you reconnect.
- **Stay in control** — Pause, resume, retry, and remove downloads in one place.
- **Enjoy a modern interface** — Material 3 Expressive styling across Android and desktop.

## 🚀 Get started

### 🖥️ Desktop

Install with one command. It downloads the latest release and installs it for your system.

**Linux** (Arch, Debian, Ubuntu and derivatives, x86_64):

```sh
curl -fsSL https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.sh | sh
```

**Windows** (PowerShell):

```powershell
irm https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.ps1 | iex
```

Run the same command again to update.

<details>
<summary>Want beta?</summary>

**Beta builds:** to install the newest pre-release instead, add the beta option:

```sh
curl -fsSL https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.sh | sh -s -- --beta
```

```powershell
& ([scriptblock]::Create((irm https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.ps1))) -Beta
```
</details>

<details>
<summary>Prefer a manual install?</summary>

Download the package for your system from [GitHub Releases](../../releases/latest): `.msi` for Windows, `.deb` for Debian/Ubuntu, or `.pkg.tar.zst` for Arch Linux.

</details>

### 📱 Android

Download the APK from [GitHub Releases](../../releases/latest) and open it to install.

### ▶️ First launch

Connect to your Emby server and start watching. Already downloaded something? Choose **Browse local files** on the sign-in screen to access media saved by EmbyEx without connecting to a server.

## 📱 Platform support

| Platform | Playback | Offline downloads |
| --- | --- | --- |
| Android | ✅ | ✅ |
| Windows / Linux | ✅ | ✅ |
| macOS | On the way | On the way |
| iOS | On the way | On the way |

Available installers may vary by release. Video format support depends on the device and playback engine.

## ℹ️ Good to know

- Downloads keep the **original media file**, without transcoding. Offline files are stored in app-managed storage; **uninstalling the Android app deletes its downloads**.
- Want new features early? Turn on **Settings → Updates → Beta updates** to receive pre-release versions in the app. They may be less stable.
- EmbyEx is an **independent, unofficial client** and is not affiliated with or endorsed by Emby. Online streaming requires access to an Emby server.
- This project has been developed with substantial AI assistance and is still evolving. Bug reports and contributions are welcome.

## ❤️ Credits

Inspired by [Emby](https://emby.media/) and the interface of [mpvEx](https://github.com/marlboro-advance/mpvEx). Built with Compose Multiplatform, MediaMP, ExoPlayer, and libmpv.

Found a bug or have an idea? [Open an issue](../../issues).

## 📄 License

EmbyEx is free software, licensed under the [GNU General Public License v3.0](LICENSE).
