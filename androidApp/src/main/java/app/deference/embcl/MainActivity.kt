package app.deference.embcl

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.deference.coreui.LocalKeyShortcutManager
import app.deference.coreui.keyboard.KeyShortcutManager
import app.deference.embycl.EmbyExApp
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.repository.AppUpdater
import app.deference.embycl.ui.theme.EmbympvTheme
import org.koin.android.ext.android.inject
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

class MainActivity : ComponentActivity() {
	private val appUpdater: AppUpdater by inject()
	private val keyShortcutManager = KeyShortcutManager()
	private val consumedKeys = mutableSetOf<Pair<Int, Int>>()

	override fun onResume() {
		super.onResume()
		appUpdater.onAppResumed()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val keyShortcutManager = remember { keyShortcutManager }
			EmbympvTheme {
				CompositionLocalProvider(
					LocalKeyShortcutManager provides keyShortcutManager
				) {
					val isLoggedIn by Session.isLoggedInState.collectAsState()
					EmbyExApp(isLoggedIn)
				}
			}
		}
	}
	
	@SuppressLint("RestrictedApi")
	override fun dispatchKeyEvent(event: AndroidKeyEvent): Boolean {
		val keyId = event.deviceId to event.keyCode
		
		val handled = when (event.action) {
			AndroidKeyEvent.ACTION_DOWN -> {
				// A fresh press starts a new sequence.
				// Also clears stale tracking if an earlier key-up was missed.
				if (event.repeatCount == 0) {
					consumedKeys.remove(keyId)
				}
				
				val processed = keyShortcutManager.processKeyEvent(
					ComposeKeyEvent(event)
				)
				
				if (processed) {
					consumedKeys.add(keyId)
				}
				
				// Once consumed, keep consuming this press sequence.
				// For example, the shortcut may navigate away and unregister
				// itself while the key is still held down.
				keyId in consumedKeys
			}
			
			AndroidKeyEvent.ACTION_UP -> {
				// Consume the release if we consumed this key's press.
				// remove() also clears its tracking entry.
				consumedKeys.remove(keyId)
			}
			
			else -> false
		}
		
		return handled || super.dispatchKeyEvent(event)
		// Essential: allow normal typing, focus navigation, etc.
	}
	
	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		if (!hasFocus) {
			consumedKeys.clear()
		}
	}
	
	override fun onPause() {
		consumedKeys.clear()
		super.onPause()
	}
}

