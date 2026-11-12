package app.deference.embycl.data.repository.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
internal data class DisplayPreferencesDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Client")
    val client: String? = null,
    @SerialName("SortBy")
    val sortBy: String? = null,
    @SerialName("SortOrder")
    val sortOrder: String? = null,
    @SerialName("CustomPrefs")
    val customPrefs: Map<String, String?> = emptyMap(),
)
