package app.deference.embycl.core.networking

import org.koin.core.annotation.Single

@Single
expect class UserAgentProvider {
	fun getUserAgent(): String
}
