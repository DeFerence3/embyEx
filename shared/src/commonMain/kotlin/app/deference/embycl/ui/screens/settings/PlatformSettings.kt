package app.deference.embycl.ui.screens.settings

import androidx.compose.runtime.Composable

internal expect val hasPlayerSettings: Boolean

/** Whether the platform draws its own window title bar, which users can turn off. */
internal expect val hasTitleBarSetting: Boolean

/** Whether the platform has an in-app updater that can follow beta releases. */
internal expect val hasUpdateChannelSetting: Boolean

@Composable
internal expect fun PlayerSettings()
