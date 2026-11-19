package app.deference.embycl.platform

import app.deference.embycl.core.utils.normalizeReleaseBody
import app.deference.embycl.core.utils.resolveLinks
import app.deference.embycl.domain.appupdate.UpdateInfo
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.ReleaseVersion
import app.deference.embycl.domain.model.update.UpdateRelease

object GithubUpdateProvider {
    fun findUpdate(
        release: GithubRelease,
        installedVersionCode: Long,
    ): UpdateInfo? {
        val version = ReleaseVersion.parse(text = release.tag) ?: error("The release ${release.tag} is not tagged with an app version.")
        if (version.versionCode <= installedVersionCode) return null

        // The release pipeline publishes one universal APK, not split APKs.
        val asset = release.assets.singleOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: error("This release does not contain a single Android APK.")
        require(asset.size > 0) { "The update APK is empty." }
        require(asset.browserDownloadUrl.startsWith("https://")) { "Update URLs must use HTTPS." }
        val updateRelease = UpdateRelease(
            version = release.tag.removePrefix("v"),
            tag = release.tag,
            versionCode = version.versionCode,
            changeLogMarkDown = release.body.orEmpty().normalizeReleaseBody().resolveLinks(),
        )
        return UpdateInfo(
            release = updateRelease,
            url = asset.browserDownloadUrl,
            digest = asset.digest,
            size = asset.size,
        )
    }
}
