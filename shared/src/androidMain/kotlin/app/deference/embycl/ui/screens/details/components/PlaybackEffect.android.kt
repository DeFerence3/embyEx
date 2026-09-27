package app.deference.embycl.ui.screens.details.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.screens.details.EmbyDetailsAction
import app.deference.embycl.ui.screens.details.EmbyDetailsEvent
import kotlinx.coroutines.flow.Flow

@Composable
actual fun PlaybackEffect(events: Flow<EmbyDetailsEvent>, onAction: (EmbyDetailsAction) -> Unit) {
	val context = LocalContext.current
	val mpvLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult(),
	) { result ->
		val data = result.data
		val positionMs = data?.getIntExtra("position", - 1)?.takeIf { it >= 0 }?.toLong()
			?: data?.getLongExtra("position", - 1L)?.takeIf { it >= 0L }
		onAction(EmbyDetailsAction.PlaybackFinished(positionMs))
	}
	events.ObserveEvent { event ->
		when (event) {
			is EmbyDetailsEvent.Error -> Unit
			is EmbyDetailsEvent.LaunchPlayback -> {
				val request = event.request
				val uris = ArrayList(request.urls.map { it.toUri() })
				val intent = Intent(Intent.ACTION_VIEW).apply {
					setDataAndType(uris.getOrNull(request.selectedIndex), "video/*")
					setClassName("app.marlboroadvance.mpvex", "app.marlboroadvance.mpvex.ui.player.PlayerActivity")
					putExtra("launch_source", "emby")
					putExtra("title", request.title)
					putExtra("emby_item_id", request.itemId)
					putExtra("position", request.positionMs)
					putParcelableArrayListExtra("playlist", uris)
					putExtra("playlist_index", request.selectedIndex)
					putExtra("headers", arrayOf("User-Agent", "mpvEx", "X-Emby-Token", request.accessToken))
				}
				try {
					mpvLauncher.launch(intent)
				} catch (_: ActivityNotFoundException) {
					Toast.makeText(context, "mpvEx not installed, install it.", Toast.LENGTH_SHORT).show()
					val url = "https://github.com/marlboro-advance/mpvEx/releases/latest"
					val intent = Intent(Intent.ACTION_VIEW, url.toUri())
					context.startActivity(intent)
				}
			}
		}
	}
}
