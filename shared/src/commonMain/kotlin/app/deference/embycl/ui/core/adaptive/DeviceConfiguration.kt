package app.deference.embycl.ui.core.adaptive

import androidx.window.core.layout.WindowSizeClass

enum class DeviceConfiguration {
	MOBILE_PORTRAIT,
	MOBILE_LANDSCAPE,
	TABLET_PORTRAIT,
	TABLET_LANDSCAPE,
	DESKTOP;
	
	companion object {
		
		fun fromWindowSizeClass(
			windowSizeClass: WindowSizeClass,
		): DeviceConfiguration {
			
			val widthAtLeastMedium =
				windowSizeClass.isWidthAtLeastBreakpoint(
					WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
				)
			
			val widthAtLeastExpanded =
				windowSizeClass.isWidthAtLeastBreakpoint(
					WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
				)
			
			val heightAtLeastMedium =
				windowSizeClass.isHeightAtLeastBreakpoint(
					WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
				)
			
			val heightAtLeastExpanded =
				windowSizeClass.isHeightAtLeastBreakpoint(
					WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
				)
			
			return when {
				
				// Narrow + enough vertical room
				!widthAtLeastMedium && heightAtLeastMedium ->
					MOBILE_PORTRAIT
				
				// Short windows: phone landscape / compact desktop window
				!heightAtLeastMedium ->
					MOBILE_LANDSCAPE
				
				// Medium width with generous height
				!widthAtLeastExpanded && heightAtLeastExpanded ->
					TABLET_PORTRAIT
				
				// Expanded width but not enough height for desktop treatment
				widthAtLeastExpanded && !heightAtLeastExpanded ->
					TABLET_LANDSCAPE
				
				// Medium x medium windows
				!widthAtLeastExpanded ->
					TABLET_PORTRAIT
				
				// Expanded x expanded
				else ->
					DESKTOP
			}
		}
	}
}
