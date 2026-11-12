package app.deference.embycl.data.repository.home

import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionPref
import app.deference.embycl.domain.model.home.HomeTop

internal object HomeLayoutCodec {
    private const val TOP = "top"
    private const val CARD_SIZE = "cardSize"
    private const val SECTIONS = "sections"

    fun encode(layout: HomeLayoutPrefs): Map<String, String> = mapOf(
        TOP to when (layout.top) {
            HomeTop.ContinueWatching -> "continue"
            HomeTop.Off -> "off"
        },
        CARD_SIZE to when (layout.cardSize) {
            HomeCardSize.Comfortable -> "comfortable"
            HomeCardSize.Large -> "large"
        },
        SECTIONS to layout.sections.joinToString(separator = "|") { "${it.id.key}=${if (it.visible) 1 else 0}" },
    )

    fun decode(values: Map<String, String?>): HomeLayoutPrefs? {
        if (values[SECTIONS] == null && values[TOP] == null && values[CARD_SIZE] == null) return null
        val sections = values[SECTIONS]
            .orEmpty()
            .split("|")
            .mapNotNull { entry ->
                val key = entry.substringBeforeLast("=", missingDelimiterValue = "")
                val id = HomeSectionId.fromKey(key = key) ?: return@mapNotNull null
                HomeSectionPref(
                    id = id,
                    visible = entry.substringAfterLast("=") != "0",
                )
            }
        return HomeLayoutPrefs(
            top = if (values[TOP] == "off") HomeTop.Off else HomeTop.ContinueWatching,
            cardSize = if (values[CARD_SIZE] == "large") HomeCardSize.Large else HomeCardSize.Comfortable,
            sections = sections,
        )
    }
}
