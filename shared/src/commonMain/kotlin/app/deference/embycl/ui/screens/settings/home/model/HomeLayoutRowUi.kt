package app.deference.embycl.ui.screens.settings.home.model

import androidx.compose.runtime.Immutable
import app.deference.embycl.ui.screens.home.model.HomeSectionKind

/** One Home section in the Edit Home list. [pinned]: Continue watching held at the top by "Top of Home". */
@Immutable
data class HomeLayoutRowUi(
    val key: String,
    val kind: HomeSectionKind,
    val visible: Boolean,
    val pinned: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)
