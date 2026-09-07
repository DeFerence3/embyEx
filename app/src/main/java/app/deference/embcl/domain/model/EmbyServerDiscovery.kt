package app.deference.embcl.domain.model

data class EmbyServerDiscovery(
	val serverUrl: String,
	val serverInfo: PublicSystemInfo,
	val users: List<EmbyUser>,
	val deviceId: String,
)
