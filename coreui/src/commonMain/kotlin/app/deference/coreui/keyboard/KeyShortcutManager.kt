package app.deference.coreui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import app.deference.coreui.LocalKeyShortcutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class KeyShortcutManager {
	private val _currentShortcuts = MutableStateFlow<Map<String, List<KeyShortcut>>>(emptyMap())
	
	val currentShortcuts = _currentShortcuts.asStateFlow()
	
	fun setTopShortcuts(shortcuts: List<KeyShortcut>) {
		setShortcuts(shortcuts = shortcuts, key = "top")
	}
	
	fun removeTopShortcuts() {
		remove("top")
	}
	
	fun setShortcuts(
		shortcuts: List<KeyShortcut>,
		key: String
	) {
		val snapshot = shortcuts.toList()
		_currentShortcuts.update { current ->
			current + (key to snapshot)
		}
	}
	
	fun remove(key: String) {
		_currentShortcuts.update { current ->
			current - key
		}
	}
	
	/**
	 * Executes the first matching shortcut.
	 *
	 * Priority:
	 * 1. top
	 * 2. master
	 * 3. remaining groups, in map iteration order
	 *
	 * Actions execute only for key-down events.
	 */
	fun processKeyEvent(event: KeyEvent): Boolean {
		if (event.type != KeyEventType.KeyDown) {
			return false
		}
		
		val shortcuts = _currentShortcuts.value
		
		val match =
			shortcuts["top"]
				.orEmpty()
				.firstOrNull { it.matches(event) }
				?: shortcuts["master"]
					.orEmpty()
					.firstOrNull { it.matches(event) }
				?: shortcuts.asSequence()
					.filter { (group, _) ->
						group != "top" && group != "master"
					}
					.flatMap { (_, groupShortcuts) ->
						groupShortcuts.asSequence()
					}
					.firstOrNull { it.matches(event) }
				?: return false
		
		match.onAction()
		return true
	}
}
/**
 * Registers a list of KeyShortcuts with the KeyShortcutManager when the composable enters
 * the composition and removes them when it leaves.
 *
 * @param shortcuts The list of KeyShortcut objects specific to the current screen.
 * @param key The unique key (typically the screen name) for this set of shortcuts.
 */
@Composable
fun RegisterShortcuts(
	shortcuts: List<KeyShortcut>,
	key: String
) {
    val keyShortcutManager = LocalKeyShortcutManager.current
    DisposableEffect(keyShortcutManager,key, shortcuts) {
        keyShortcutManager.setShortcuts(shortcuts = shortcuts, key = key)
        onDispose {
            keyShortcutManager.remove(key = key)
        }
    }
}
