package app.deference.embycl.ui.screens.home.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration

internal enum class HomeLayout {
    Phone,
    Short,
    Medium,
    Wide;

    val isSideBySide: Boolean get() = this != Phone
    val gutter: Dp get() = if (this == Phone) 16.dp else 24.dp
    val posterWidth: Dp get() = when (this) {
        Phone, Short -> 144.dp
        Medium -> 160.dp
        Wide -> 168.dp
    }
    val wideWidth: Dp get() = when (this) {
        Phone, Short -> 272.dp
        Medium, Wide -> 300.dp
    }
    val usesSheets: Boolean get() = this == Phone || this == Medium

    companion object {
        fun from(configuration: DeviceConfiguration): HomeLayout = when (configuration) {
            DeviceConfiguration.MOBILE_PORTRAIT -> Phone
            DeviceConfiguration.MOBILE_LANDSCAPE -> Short
            DeviceConfiguration.TABLET_PORTRAIT -> Medium
            DeviceConfiguration.TABLET_LANDSCAPE,
            DeviceConfiguration.DESKTOP -> Wide
        }
    }
}
