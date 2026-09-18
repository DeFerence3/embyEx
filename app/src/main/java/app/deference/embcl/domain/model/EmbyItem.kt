package app.deference.embcl.domain.model

import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.buildUrl
import app.deference.embcl.core.utils.or
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class EmbyItem(
	@SerialName("Id")
	val id: String,
	@SerialName("Name")
	val name: String = "Untitled",
	@SerialName("Type")
	val type: String = "",
	@SerialName("MediaType")
	val mediaType: String? = null,
	@SerialName("Container")
	val container: String? = null,
	@SerialName("CollectionType")
	val collectionType: String? = null,
	@SerialName("IsFolder")
	val isFolder: Boolean = false,
	@SerialName("Overview")
	val overview: String? = null,
	@SerialName("ProductionYear")
	val productionYear: Int? = null,
	@SerialName("OfficialRating")
	val officialRating: String? = null,
	@SerialName("CommunityRating")
	val communityRating: Float? = null,
	@SerialName("RunTimeTicks")
	val runTimeTicks: Long? = null,
	@SerialName("SeriesName")
	val seriesName: String? = null,
	@SerialName("SeriesId")
	val seriesId: String? = null,
	@SerialName("SeasonName")
	val seasonName: String? = null,
	@SerialName("SeasonId")
	val seasonId: String? = null,
	@SerialName("ParentId")
	val parentId: String? = null,
	@SerialName("IndexNumber")
	val indexNumber: Int? = null,
	@SerialName("ParentIndexNumber")
	val parentIndexNumber: Int? = null,
	@SerialName("ImageTags")
	val imageTags: Map<String, String> = emptyMap(),
	@SerialName("BackdropImageTags")
	val backdropImageTags: List<String> = emptyList(),
	@SerialName("ParentBackdropItemId")
	val parentBackdropItemId: String? = null,
	@SerialName("ParentBackdropImageTags")
	val parentBackdropImageTags: List<String> = emptyList(),
	@SerialName("ParentLogoItemId")
	val parentLogoItemId: String? = null,
	@SerialName("ParentLogoImageTag")
	val parentLogoImageTag: String? = null,
	@SerialName("SeriesPrimaryImageTag")
	val seriesPrimaryImageTag: String? = null,
	@SerialName("PremiereDate")
	val premiereDate: Instant? = null,
	@SerialName("MediaStreams")
	val mediaStreams: List<EmbyMediaStream> = emptyList(),
	@SerialName("People")
	val people: List<EmbyPerson> = emptyList(),
	@SerialName("UserData")
	val userData: EmbyUserData? = null,
) {
	
	fun isEpisode() = type == "Episode"
	
	fun subtitle(): String? = when {
		isEpisode() -> "S${parentIndexNumber.or { "Unknown" }} Ep $indexNumber"
		type == "Season" -> "Season $indexNumber"
		productionYear != null -> productionYear.toString()
		collectionType != null -> collectionType.replaceFirstChar { it.uppercase() }
		else -> type.takeIf { it.isNotBlank() }
	}
	
	fun imageUrl(type: String = "Primary", maxWidth: Int = 600): String?{
		val imageItemId = when (type) {
			"Backdrop" if backdropImageTags.isNotEmpty() -> id
			"Backdrop" if parentBackdropImageTags.isNotEmpty() -> parentBackdropItemId
			"Logo" if imageTags.containsKey("Logo") -> id
			"Logo" if ! parentLogoItemId.isNullOrBlank() -> parentLogoItemId
			"Primary" if imageTags.containsKey("Primary") -> id
			else -> null
		} ?: return null
		val tag = when (type) {
			"Backdrop" -> backdropImageTags.firstOrNull() ?: parentBackdropImageTags.firstOrNull()
			"Logo" -> imageTags["Logo"] ?: parentLogoImageTag
			else -> imageTags[type]
		}
		return buildUrl(
			Session.serverUrl,
			"/Items/$imageItemId/Images/$type",
			mapOf(
				"MaxWidth" to maxWidth.toString(),
				"Quality" to "90",
				"Tag" to tag,
				"api_key" to Session.accessToken,
			),
		).toString()
	}
}
