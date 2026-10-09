package app.deference.embycl.domain.model.update

import app.deference.embycl.BuildConfig

/** Release information always refers to the installed build, not the latest update. */
object CurrentBuildRelease {
    val tag = "v${BuildConfig.VERSION_NAME.removePrefix("v")}"
    val pageUrl = "https://github.com/DeFerence3/embyEx/releases/tag/$tag"
    val apiUrl = "https://api.github.com/repos/DeFerence3/embyEx/releases/tags/$tag"
}
