package app.deference.embycl.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

@Composable
internal fun AndroidPlayerWindow(
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    val activity = LocalContext.current.playerActivity()
    val decor = activity.window.decorView
    val insets = remember(key1 = activity) { WindowCompat.getInsetsController(activity.window, decor) }
    val window = remember(key1 = activity) {
        lateinit var window: PlayerWindowState
        window = PlayerWindowState(
            supportsPictureInPicture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE),
            usesSystemPictureInPicture = true,
            togglePictureInPicture = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !window.isPictureInPicture) {
                    val source = Rect()
                    val bounds = window.videoBounds
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        source.set(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
                    } else {
                        decor.getGlobalVisibleRect(source)
                    }
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .setSourceRectHint(source)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setSeamlessResizeEnabled(true)
                    window.isPictureInPicture = true
                    val entered = try {
                        activity.enterPictureInPictureMode(params.build())
                    } catch (_: IllegalStateException) {
                        false
                    }
                    window.isPictureInPicture = entered
                    window.pictureInPictureFailed = !entered
                }
            },
        )
        window.isFullscreen = true
        window
    }
    DisposableEffect(key1 = activity) {
        val previousBehavior = insets.systemBarsBehavior
        val previousInsets = androidx.core.view.ViewCompat.getRootWindowInsets(decor)
        val statusBarsVisible = previousInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) != false
        val navigationBarsVisible = previousInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) != false
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            window.isPictureInPicture = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(pipListener)
            insets.systemBarsBehavior = previousBehavior
            if (statusBarsVisible) insets.show(WindowInsetsCompat.Type.statusBars())
            else insets.hide(WindowInsetsCompat.Type.statusBars())
            if (navigationBarsVisible) insets.show(WindowInsetsCompat.Type.navigationBars())
            else insets.hide(WindowInsetsCompat.Type.navigationBars())
            if (activity.isInPictureInPictureMode) activity.moveTaskToBack(true)
        }
    }
    LaunchedEffect(key1 = window.isPictureInPicture) {
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
    }
    BackHandler(enabled = !window.isPictureInPicture) { window.closeRequested = true }
    content(window)
}

internal fun Context.playerActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error(message = "The player requires an activity host.")
}
