package app.deference.embycl.platform

import app.deference.embycl.getPlatform

object AppInfo {
    const val name = "EmbyEx"
    const val version = "1.1"
}

val platformName: String = getPlatform().name
val playerName: String = "Mpv"
