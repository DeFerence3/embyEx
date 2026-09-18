package app.deference.embcl.domain.model

import app.deference.embcl.core.session.Server

data class EmbyServerDiscovery(
	val server: Server,
	val serverInfo: PublicSystemInfo,
	val users: List<EmbyUser>,
	val deviceId: String,
)
