package app.deference.embycl.domain.repository

import app.deference.embycl.domain.ghrelease.GithubRelease

interface GithubRepo {
    suspend fun getLatestRelease(prerelease: Boolean = false): GithubRelease
}
