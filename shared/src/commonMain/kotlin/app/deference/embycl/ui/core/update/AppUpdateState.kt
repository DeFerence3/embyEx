package app.deference.embycl.ui.core.update

import app.deference.embycl.domain.model.update.AppUpdate

data class AppUpdateState(
    val update: AppUpdate = AppUpdate.Idle,
    val dialogVisible: Boolean = false,
)
