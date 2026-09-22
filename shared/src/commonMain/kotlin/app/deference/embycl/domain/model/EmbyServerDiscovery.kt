package app.deference.embycl.domain.model

import app.deference.embycl.core.session.Server

data class EmbyServerDiscovery(
	val server: Server,
	val serverInfo: PublicSystemInfo,
	val users: List<EmbyUser>,
	val deviceId: String,
)
