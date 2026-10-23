package app.deference.embycl.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun AndroidPlayerWindow(
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    val activity = LocalContext.current.playerActivity()
    val parent = rememberCompositionContext()
    val currentContent by rememberUpdatedState(newValue = content)
    DisposableEffect(key1 = activity, key2 = parent) {
        // PiP captures the activity window. A Dialog would be a different native surface.
        val decor = activity.window.decorView as ViewGroup
        val view = ComposeView(activity)
        val insets = WindowCompat.getInsetsController(activity.window, decor)
        val previousBehavior = insets.systemBarsBehavior
        val previousInsets = androidx.core.view.ViewCompat.getRootWindowInsets(decor)
        val statusBarsVisible = previousInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) != false
        val navigationBarsVisible = previousInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) != false
        lateinit var window: PlayerWindowState
        window = PlayerWindowState(
            supportsPictureInPicture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE),
            usesSystemPictureInPicture = true,
            togglePictureInPicture = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !window.isPictureInPicture) {
                    val source = Rect()
                    view.getGlobalVisibleRect(source)
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
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            window.isPictureInPicture = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(pipListener)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setParentCompositionContext(parent)
        view.setContent {
            BackHandler(enabled = !window.isPictureInPicture) { window.closeRequested = true }
            currentContent(window)
        }
        decor.addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(pipListener)
            view.disposeComposition()
            decor.removeView(view)
            insets.systemBarsBehavior = previousBehavior
            if (statusBarsVisible) insets.show(WindowInsetsCompat.Type.statusBars())
            if (navigationBarsVisible) insets.show(WindowInsetsCompat.Type.navigationBars())
            if (activity.isInPictureInPictureMode) activity.moveTaskToBack(true)
        }
    }
}

internal fun Context.playerActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error(message = "The player requires an activity host.")
}
