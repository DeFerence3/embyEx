package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbySession(
	val serverUrl: String,
	val serverName: String,
	val serverId: String,
	val userId: String,
	val userName: String,
	val accessToken: String,
	val deviceId: String,
)

@Serializable
data class PublicSystemInfo(
	@SerialName("ServerName") val serverName: String = "Emby",
	@SerialName("Id") val id: String = "",
	@SerialName("Version") val version: String = "",
)

@Serializable
data class AuthenticateRequest(
	@SerialName("Username") val username: String,
	@SerialName("Password") val password: String,
	@SerialName("Pw") val pw: String = password,
)

@Serializable
data class AuthenticateUserRequest(
	@SerialName("Password") val password: String,
	@SerialName("Pw") val pw: String = password,
)

@Serializable
data class AuthenticationResult(
	@SerialName("User") val user: EmbyUser,
	@SerialName("AccessToken") val accessToken: String,
	@SerialName("ServerId") val serverId: String = "",
)

@Serializable
data class EmbyUser(
	@SerialName("Id")
	val id: String,
	@SerialName("Name")
	val name: String,
	@SerialName("ServerName")
	val serverName: String? = null,
	@SerialName("PrimaryImageTag")
	val primaryImageTag: String? = null,
	@SerialName("HasPassword")
	val hasPassword: Boolean = false,
	@SerialName("HasConfiguredPassword") val hasConfiguredPassword: Boolean = false,
)

data class EmbyServerDiscovery(
	val serverUrl: String,
	val serverInfo: PublicSystemInfo,
	val users: List<EmbyUser>,
	val deviceId: String,
)

@Serializable
data class EmbyItemsResult(
	@SerialName("Items") val items: List<EmbyItem> = emptyList(),
	@SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
)

@Serializable
data class EmbyItem(
	@SerialName("Id") val id: String,
	@SerialName("Name") val name: String = "Untitled",
	@SerialName("Type") val type: String = "",
	@SerialName("MediaType") val mediaType: String? = null,
	@SerialName("Container") val container: String? = null,
	@SerialName("CollectionType") val collectionType: String? = null,
	@SerialName("IsFolder") val isFolder: Boolean = false,
	@SerialName("Overview") val overview: String? = null,
	@SerialName("ProductionYear") val productionYear: Int? = null,
	@SerialName("OfficialRating") val officialRating: String? = null,
	@SerialName("CommunityRating") val communityRating: Float? = null,
	@SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
	@SerialName("SeriesName") val seriesName: String? = null,
	@SerialName("SeasonName") val seasonName: String? = null,
	@SerialName("IndexNumber") val indexNumber: Int? = null,
	@SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
	@SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
	@SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
	@SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
	@SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String> = emptyList(),
	@SerialName("UserData") val userData: EmbyUserData? = null,
)

@Serializable
data class EmbyUserData(
	@SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
	@SerialName("PlayedPercentage") val playedPercentage: Double? = null,
	@SerialName("Played") val played: Boolean = false,
	@SerialName("IsFavorite") val isFavorite: Boolean = false,
)

@Serializable
data class EmbyPlaybackReport(
	@SerialName("ItemId") val itemId: String,
	@SerialName("PositionTicks") val positionTicks: Long,
	@SerialName("IsPaused") val isPaused: Boolean = false,
	@SerialName("CanSeek") val canSeek: Boolean = true,
	@SerialName("PlayMethod") val playMethod: String = "DirectPlay",
)

enum class EmbyPlaybackEvent { Started, Progress, Stopped }

data class EmbyHome(
	val views: List<EmbyItem> = emptyList(),
	val resume: List<EmbyItem> = emptyList(),
	val latest: List<EmbyItem> = emptyList(),
)

fun EmbyItem.subtitle(): String? = when {
	type == "Episode" -> listOfNotNull(
		seriesName,
		parentIndexNumber?.let { season ->
			indexNumber?.let { episode -> "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}" }
		},
	).joinToString(" · ").ifBlank { null }
	
	productionYear != null -> productionYear.toString()
	collectionType != null -> collectionType.replaceFirstChar { it.uppercase() }
	else -> type.takeIf { it.isNotBlank() }
}

fun Long.asRuntime(): String {
	val totalMinutes = this / 10_000_000L / 60L
	val hours = totalMinutes / 60
	val minutes = totalMinutes % 60
	return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
