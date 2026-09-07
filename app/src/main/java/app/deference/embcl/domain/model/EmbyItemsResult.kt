package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyItemsResult(
	@SerialName("Items")
	val items: List<EmbyItem> = emptyList(),
	@SerialName("TotalRecordCount")
	val totalRecordCount: Int = 0,
)
