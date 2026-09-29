package app.deference.embycl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.nucleusframework.core.runtime.LinuxDesktopEnvironment
import dev.nucleusframework.window.DecoratedWindowScope
import dev.nucleusframework.window.DecoratedWindowState
import dev.nucleusframework.window.LocalContentColor
import dev.nucleusframework.window.LocalIsDarkTheme
import dev.nucleusframework.window.LocalTitleBarInfo
import dev.nucleusframework.window.TitleBarScope
import dev.nucleusframework.window.TitleBarScopeImpl
import dev.nucleusframework.window.WindowControlType
import dev.nucleusframework.window.icons.windows.Close
import dev.nucleusframework.window.icons.windows.CloseDark
import dev.nucleusframework.window.icons.windows.CloseFullscreen
import dev.nucleusframework.window.icons.windows.CloseFullscreenDark
import dev.nucleusframework.window.icons.windows.CloseFullscreenInactive
import dev.nucleusframework.window.icons.windows.CloseFullscreenInactiveDark
import dev.nucleusframework.window.icons.windows.CloseHover
import dev.nucleusframework.window.icons.windows.CloseInactive
import dev.nucleusframework.window.icons.windows.CloseInactiveDark
import dev.nucleusframework.window.icons.windows.Maximize
import dev.nucleusframework.window.icons.windows.MaximizeDark
import dev.nucleusframework.window.icons.windows.MaximizeInactive
import dev.nucleusframework.window.icons.windows.MaximizeInactiveDark
import dev.nucleusframework.window.icons.windows.Minimize
import dev.nucleusframework.window.icons.windows.MinimizeDark
import dev.nucleusframework.window.icons.windows.MinimizeInactive
import dev.nucleusframework.window.icons.windows.MinimizeInactiveDark
import dev.nucleusframework.window.icons.windows.Restore
import dev.nucleusframework.window.icons.windows.RestoreDark
import dev.nucleusframework.window.icons.windows.RestoreInactive
import dev.nucleusframework.window.icons.windows.RestoreInactiveDark
import dev.nucleusframework.window.icons.windows.WindowsControlButtonIcons
import dev.nucleusframework.window.internal.WindowsCaptionButtonStyle
import dev.nucleusframework.window.internal.animateWindowsCaptionColor
import dev.nucleusframework.window.internal.windowsCaptionButtonBackground
import dev.nucleusframework.window.styling.TitleBarColors
import dev.nucleusframework.window.styling.TitleBarMetrics
import dev.nucleusframework.window.styling.TitleBarStyle
import dev.nucleusframework.window.tao.TaoDecoratedWindowScope
import dev.nucleusframework.window.tao.TaoWindow
import dev.nucleusframework.window.utils.linux.LinuxButtonLayout
import dev.nucleusframework.window.utils.linux.LinuxTitleBarButton
import dev.nucleusframework.window.utils.linux.linuxTitleBarIcons
import dev.nucleusframework.window.utils.linux.rememberLinuxButtonLayout

private val WINDOWS_BUTTON_WIDTH = 46.dp
private const val CLOSE_HOVER_ALPHA_EPSILON = 0.02f

internal enum class WindowControlSlot { Minimize, Maximize, Close }

@Composable
fun DecoratedWindowScope.EmbyExTitleBar(
) {
	val state = this.state
	val colors = MaterialTheme.colorScheme
	val style = TitleBarStyle(
		TitleBarColors(
			background = colors.background,
			inactiveBackground = colors.background,
			content = colors.onBackground,
			border = colors.primary,
			iconButtonHoveredBackground = Color.LightGray.copy(alpha = 0.25f),
			iconButtonPressedBackground = Color.LightGray
		),
		TitleBarMetrics(height = 35.dp)
	)
	
	val taoScope = this as TaoDecoratedWindowScope
	val taoWindow = taoScope.window
	val isFullscreen = state.isFullscreen
	val onExitFullscreen = { taoWindow.setFullscreen(false) }
	val titleBarInfo = LocalTitleBarInfo.current
	val scope = TitleBarScopeImpl(titleBarInfo.title, titleBarInfo.icon)
	
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(35.dp)
			.background(colors.surface),
		contentAlignment = Alignment.Center
	) {
		val textStyle = TextStyle(color = colors.onSurface)
		CompositionLocalProvider(
			LocalTextStyle provides textStyle,
			LocalContentColor provides colors.onSurface
		){
			Text("embyEx")
		}
		val linuxLayout = if (dev.nucleusframework.core.runtime.Platform.Current == dev.nucleusframework.core.runtime.Platform.Linux) rememberLinuxButtonLayout() else null
		when (dev.nucleusframework.core.runtime.Platform.Current) {
			dev.nucleusframework.core.runtime.Platform.Linux -> if (linuxLayout != null) {
				scope.WindowControlsLinux(
					win = taoWindow,
					state = state,
					isResizable = taoWindow.isResizable,
					style = style,
					layout = linuxLayout,
					isFullscreen = state.isFullscreen,
					onExitFullscreen = { taoWindow.setFullscreen(false) },
				)
			}
			dev.nucleusframework.core.runtime.Platform.Windows -> Row(
				modifier = Modifier
					.align(Alignment.CenterEnd)
			) {
				for (slot in WindowControlSlot.entries) {
					val action = resolveWindowControl(slot, taoWindow, state, isFullscreen, onExitFullscreen)
					if (action != null) WindowsWindowControl(action.type, state, style, action.onClick)
				}
			}
			else -> Unit
		}
	}
}

@Suppress("FunctionNaming", "LoopWithTooManyJumpStatements")
@Composable
internal fun TitleBarScope.WindowControlsLinux(
	win: TaoWindow,
	state: DecoratedWindowState,
	isResizable: Boolean,
	style: TitleBarStyle,
	layout: LinuxButtonLayout = rememberLinuxButtonLayout(),
	isFullscreen: Boolean = false,
	onExitFullscreen: (() -> Unit)? = null,
) {
	val icons = linuxTitleBarIcons()
	val buttonAlignment = if (layout.controlsOnRight) Alignment.End else Alignment.Start
	
	// Iterate over `layout.buttons` in natural order — `layout.buttons[0]` is
	// "closest to the edge". Core's `TitleBarMeasurePolicy` places End items
	// first-declared = rightmost (controls-on-right) and Start items
	// first-declared = leftmost. Mirrors `decorated-window-jni`'s
	// `WindowControlArea.kt` exactly.
	for (button in layout.buttons) {
		when (button) {
			LinuxTitleBarButton.CLOSE -> {
				val closeHover = if (state.isActive) icons.closeHoverFocused else icons.closeHover
				val closePressed = if (state.isActive) icons.closePressedFocused else icons.closePressed
				LinuxControlButton(
					onClick = { win.requestUserClose() },
					icon = icons.close,
					iconHover = closeHover,
					iconPressed = closePressed,
					contentDescription = "Close",
					style = style,
					modifier = Modifier.align(buttonAlignment),
					isCloseButton = true,
				)
			}
			LinuxTitleBarButton.MAXIMIZE -> {
				if (isFullscreen && onExitFullscreen != null) {
					LinuxControlButton(
						onClick = onExitFullscreen,
						icon = icons.maximize,
						iconHover = icons.maximizeHover,
						iconPressed = icons.maximizePressed,
						contentDescription = "Exit fullscreen",
						style = style,
						modifier = Modifier.align(buttonAlignment),
					)
					continue
				}
				if (!isResizable) continue
				if (state.isMaximized) {
					LinuxControlButton(
						onClick = { win.setMaximized(false) },
						icon = icons.restore,
						iconHover = icons.restoreHover,
						iconPressed = icons.restorePressed,
						contentDescription = "Restore",
						style = style,
						modifier = Modifier.align(buttonAlignment),
					)
				} else {
					LinuxControlButton(
						onClick = { win.setMaximized(true) },
						icon = icons.maximize,
						iconHover = icons.maximizeHover,
						iconPressed = icons.maximizePressed,
						contentDescription = "Maximize",
						style = style,
						modifier = Modifier.align(buttonAlignment),
					)
				}
			}
			LinuxTitleBarButton.MINIMIZE -> {
				LinuxControlButton(
					onClick = { win.minimize() },
					icon = icons.minimize,
					iconHover = icons.minimizeHover,
					iconPressed = icons.minimizePressed,
					contentDescription = "Minimize",
					style = style,
					modifier = Modifier.align(buttonAlignment),
				)
			}
		}
	}
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WindowsCaptionButton(
	onClick: () -> Unit,
	isDark: Boolean,
	style: TitleBarStyle,
	icon: ImageVector,
	contentDescription: String,
	iconHover: ImageVector? = null,
	isCloseButton: Boolean = false,
	modifier: Modifier = Modifier,
) {
	var hovered by remember { mutableStateOf(false) }
	var pressed by remember { mutableStateOf(false) }
	val appearing = hovered || pressed
	
	val targetBackground =
		windowsCaptionButtonBackground(
			hovered = hovered,
			pressed = pressed,
			isCloseButton = isCloseButton,
			isDark = isDark,
			customHover = style.colors.iconButtonHoveredBackground,
			customPressed = style.colors.iconButtonPressedBackground,
		)
	val backgroundColor =
		animateWindowsCaptionColor(
			targetBackground,
			appearing = appearing,
			durationMillis = WindowsCaptionButtonStyle.BACKGROUND_FADE_OUT_MILLIS,
		)
	
	// Keep the white close glyph while the red fill is still fading out so
	// the X does not snap back to gray on a still-red background.
	val isCloseHovered =
		isCloseButton &&
				(appearing || backgroundColor.alpha > CLOSE_HOVER_ALPHA_EPSILON)
	val currentIcon: Painter =
		rememberVectorPainter(
			if (isCloseHovered && iconHover != null) iconHover else icon,
		)
	
	val colorFilter =
		captionButtonColorFilter(
			hovered = hovered,
			pressed = pressed,
			isCloseHovered = isCloseHovered,
			style = style,
		)
	
	Box(
		modifier =
			modifier
				.focusable(false)
				.fillMaxHeight()
				.width(WINDOWS_BUTTON_WIDTH)
				.background(backgroundColor)
				.onPointerEvent(PointerEventType.Enter) { hovered = true }
				.onPointerEvent(PointerEventType.Exit) {
					hovered = false
					pressed = false
				}.onPointerEvent(PointerEventType.Press) { pressed = true }
				.onPointerEvent(PointerEventType.Release) { pressed = false }
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = onClick,
				),
		contentAlignment = Alignment.Center,
	) {
		Image(painter = currentIcon, contentDescription = contentDescription, colorFilter = colorFilter)
	}
}

private fun captionButtonColorFilter(
	hovered: Boolean,
	pressed: Boolean,
	isCloseHovered: Boolean,
	style: TitleBarStyle,
): ColorFilter? {
	val iconTint = style.colors.controlButtonIconColor
	val iconHoverTint = style.colors.controlButtonIconHoverColor
	return when {
		// Close hover swaps to the baked-red close artwork; don't tint it.
		isCloseHovered -> null
		(hovered || pressed) && iconHoverTint != Color.Unspecified ->
			ColorFilter.tint(iconHoverTint)
		iconTint != Color.Unspecified -> ColorFilter.tint(iconTint)
		else -> null
	}
}

internal class WindowControlAction(
	val type: WindowControlType,
	val onClick: () -> Unit,
)

internal fun resolveWindowControl(
	slot: WindowControlSlot,
	window: TaoWindow,
	state: DecoratedWindowState,
	isFullscreen: Boolean,
	onExitFullscreen: (() -> Unit)?,
): WindowControlAction? =
	when (slot) {
		WindowControlSlot.Minimize ->
			WindowControlAction(WindowControlType.Minimize) { window.minimize() }
		
		WindowControlSlot.Maximize ->
			when {
				isFullscreen && onExitFullscreen != null ->
					WindowControlAction(WindowControlType.ExitFullscreen, onExitFullscreen)
				
				!window.isResizable -> null
				
				state.isMaximized ->
					WindowControlAction(WindowControlType.Restore) { window.setMaximized(false) }
				
				else ->
					WindowControlAction(WindowControlType.Maximize) { window.setMaximized(true) }
			}
		
		// Fire the user's onCloseRequest (mirrors AWT's WINDOW_CLOSING
		// dispatch). Calling `requestClose()` directly would destroy the
		// window without giving the app a chance to exit the Tao event loop.
		WindowControlSlot.Close ->
			WindowControlAction(WindowControlType.Close) { window.requestUserClose() }
	}

@Composable
internal fun WindowsWindowControl(
	type: WindowControlType,
	state: DecoratedWindowState,
	style: TitleBarStyle,
	onClick: () -> Unit,
) {
	val isDark = LocalIsDarkTheme.current
	val isCloseButton = type == WindowControlType.Close
	
	WindowsCaptionButton(
		onClick = onClick,
		isDark = isDark,
		style = style,
		icon = windowsControlIcon(type, active = state.isActive, isDark = isDark),
		contentDescription = windowsControlDescription(type),
		iconHover = if (isCloseButton) WindowsControlButtonIcons.CloseHover else null,
		isCloseButton = isCloseButton,
		modifier = Modifier,
	)
}

private fun windowsControlIcon(
	type: WindowControlType,
	active: Boolean,
	isDark: Boolean,
): ImageVector =
	when (type) {
		WindowControlType.Minimize ->
			pickVariant(
				active,
				isDark,
				WindowsControlButtonIcons.Minimize,
				WindowsControlButtonIcons.MinimizeDark,
				WindowsControlButtonIcons.MinimizeInactive,
				WindowsControlButtonIcons.MinimizeInactiveDark,
			)
		
		WindowControlType.Maximize ->
			pickVariant(
				active,
				isDark,
				WindowsControlButtonIcons.Maximize,
				WindowsControlButtonIcons.MaximizeDark,
				WindowsControlButtonIcons.MaximizeInactive,
				WindowsControlButtonIcons.MaximizeInactiveDark,
			)
		
		WindowControlType.Restore ->
			pickVariant(
				active,
				isDark,
				WindowsControlButtonIcons.Restore,
				WindowsControlButtonIcons.RestoreDark,
				WindowsControlButtonIcons.RestoreInactive,
				WindowsControlButtonIcons.RestoreInactiveDark,
			)
		
		WindowControlType.ExitFullscreen ->
			pickVariant(
				active,
				isDark,
				WindowsControlButtonIcons.CloseFullscreen,
				WindowsControlButtonIcons.CloseFullscreenDark,
				WindowsControlButtonIcons.CloseFullscreenInactive,
				WindowsControlButtonIcons.CloseFullscreenInactiveDark,
			)
		
		WindowControlType.Close ->
			pickVariant(
				active,
				isDark,
				WindowsControlButtonIcons.Close,
				WindowsControlButtonIcons.CloseDark,
				WindowsControlButtonIcons.CloseInactive,
				WindowsControlButtonIcons.CloseInactiveDark,
			)
	}

@Suppress("LongParameterList")
private fun pickVariant(
	active: Boolean,
	isDark: Boolean,
	light: ImageVector,
	dark: ImageVector,
	inactiveLight: ImageVector,
	inactiveDark: ImageVector,
): ImageVector =
	if (active) {
		if (isDark) dark else light
	} else {
		if (isDark) inactiveDark else inactiveLight
	}

private fun windowsControlDescription(type: WindowControlType): String =
	when (type) {
		WindowControlType.Minimize -> "Minimize"
		WindowControlType.Maximize -> "Maximize"
		WindowControlType.Restore -> "Restore"
		WindowControlType.Close -> "Close"
		WindowControlType.ExitFullscreen -> "Exit fullscreen"
	}
@Suppress("FunctionNaming", "LongParameterList")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LinuxControlButton(
	onClick: () -> Unit,
	icon: Painter,
	iconHover: Painter,
	iconPressed: Painter,
	contentDescription: String,
	style: TitleBarStyle,
	modifier: Modifier = Modifier,
	isCloseButton: Boolean = false,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isKde = LinuxDesktopEnvironment.Current == LinuxDesktopEnvironment.KDE
	val dpSize: DpSize = if (isKde) DpSize(28.dp, 28.dp) else DpSize(40.dp, 40.dp)
	Box(
		modifier =
			modifier
				.focusable(false)
				.let { if (isKde) it.offset(y = (-2).dp) else it }
				.size(dpSize)
				.clickable(
					interactionSource = interactionSource,
					indication = null,
					onClick = onClick,
				),
		contentAlignment = Alignment.Center,
	) {
		var hovered by remember { mutableStateOf(false) }
		var pressed by remember { mutableStateOf(false) }
		
		// Show hover/pressed feedback regardless of window focus — native
		// GNOME and KDE both highlight title-bar controls on hover even when
		// the window is inactive. The focused/unfocused icon *variant* is
		// already chosen at the call site (e.g. closeHover vs
		// closeHoverFocused), so the correct artwork is used either way.
		val currentIcon =
			when {
				pressed -> iconPressed
				hovered -> iconHover
				else -> icon
			}
		
		// Apply the custom icon tint when set, but skip the close button while
		// hovered/pressed — its artwork has baked-in colors. Mirrors
		// `decorated-window-core/WindowControlArea.kt`.
		val isCloseInteracted = isCloseButton && (hovered || pressed)
		val iconTint = style.colors.controlButtonIconColor
		val iconHoverTint = style.colors.controlButtonIconHoverColor
		val colorFilter =
			when {
				isCloseInteracted -> null
				(hovered || pressed) && iconHoverTint != Color.Unspecified -> ColorFilter.tint(iconHoverTint)
				iconTint != Color.Unspecified -> ColorFilter.tint(iconTint)
				else -> null
			}
		
		Image(
			painter = currentIcon,
			contentDescription = contentDescription,
			colorFilter = colorFilter,
			modifier =
				Modifier
					.onPointerEvent(PointerEventType.Enter) { hovered = true }
					.onPointerEvent(PointerEventType.Exit) {
						hovered = false
						pressed = false
					}.onPointerEvent(PointerEventType.Press) { pressed = true }
					.onPointerEvent(PointerEventType.Release) { pressed = false },
		)
	}
}