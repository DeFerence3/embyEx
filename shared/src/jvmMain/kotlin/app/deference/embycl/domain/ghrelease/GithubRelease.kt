package app.deference.embycl.domain.ghrelease

import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(val body: String? = null)
