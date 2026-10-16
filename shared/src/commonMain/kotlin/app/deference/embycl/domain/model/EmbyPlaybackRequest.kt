package app.deference.embycl.domain.model

data class EmbyPlaybackRequest(
	val itemId: String,
	val title: String,
	val urls: ArrayList<String>,
	val selectedIndex: Int,
	val positionMs: Int,
	val accessToken: String,
)
