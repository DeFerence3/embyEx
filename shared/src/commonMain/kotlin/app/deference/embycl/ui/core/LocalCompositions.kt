package app.deference.embycl.ui.core

import androidx.compose.runtime.staticCompositionLocalOf
import app.deference.embycl.ui.core.nav.Navigator
import com.deference.koasty.KoastManager

val LocalNavigator = staticCompositionLocalOf<Navigator> {
	error("No backstack provided")
}

val LocalKoasty = staticCompositionLocalOf {
	KoastManager()
}

