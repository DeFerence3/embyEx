package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
	@SerialName("SeasonName")
	val seasonName: String? = null,
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
	@SerialName("UserData")
	val userData: EmbyUserData? = null,
) {
	fun subtitle(): String? = when {
		type == "Episode" -> listOfNotNull(
			seriesName,
			parentIndexNumber?.let { season ->
				indexNumber?.let { episode -> "SE" }
			},
		).joinToString(" · ").ifBlank { null }
		
		productionYear != null -> productionYear.toString()
		collectionType != null -> collectionType.replaceFirstChar { it.uppercase() }
		else -> type.takeIf { it.isNotBlank() }
	}
}
