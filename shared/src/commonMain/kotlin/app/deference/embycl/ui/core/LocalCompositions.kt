package app.deference.embycl.ui.core

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.nav.Navigator
import com.deference.koasty.KoastManager

val LocalNavigator = staticCompositionLocalOf<Navigator> {
	error("No backstack provided")
}

val LocalKoasty = staticCompositionLocalOf {
	KoastManager()
}

val LocalDeviceConfiguration: ProvidableCompositionLocal<DeviceConfiguration> = staticCompositionLocalOf {
	error("LocalDeviceConfiguration is not provided. Wrap the content in ProvideDeviceConfiguration.")
}
