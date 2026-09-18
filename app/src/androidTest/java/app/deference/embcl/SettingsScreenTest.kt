package app.deference.embcl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.deference.embcl.domain.model.ServerDetails
import app.deference.embcl.domain.model.ServerInfo
import app.deference.embcl.domain.model.ThemeMode
import app.deference.embcl.ui.screens.settings.SettingsAccount
import app.deference.embcl.ui.screens.settings.SettingsContent
import app.deference.embcl.ui.screens.settings.SettingsState
import app.deference.embcl.ui.theme.EmbympvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
	@get:Rule val compose = createComposeRule()
	private val account = SettingsAccount("Viewer", "viewer-id", "Media server", "https://media.example.com:8920")

	@Test
	fun signOutRequiresConfirmationAndCancelKeepsSession() {
		var signOutCalls = 0
		showSettings(onSignOut = { signOutCalls++ })
		compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Sign out"))
		compose.onNodeWithText("Sign out").performClick()
		compose.runOnIdle { assertEquals(0, signOutCalls) }
		compose.onNodeWithText("Cancel").performClick()
		compose.runOnIdle { assertEquals(0, signOutCalls) }
		compose.onNodeWithText("Sign out").performClick()
		compose.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
		compose.runOnIdle { assertEquals(1, signOutCalls) }
	}

	@Test
	fun themeSelectionUpdatesImmediately() {
		var mode by mutableStateOf(ThemeMode.System)
		compose.setContent {
			EmbympvTheme(darkTheme = mode == ThemeMode.Dark, dynamicColor = false) {
				SettingsContent(SettingsState(), account, mode, { mode = it }, {}, {}, {})
			}
		}
		compose.onNodeWithText("Dark").performClick().assertIsSelected()
		compose.runOnIdle { assertEquals(ThemeMode.Dark, mode) }
		compose.onNodeWithText("Light").performClick().assertIsSelected()
		compose.runOnIdle { assertEquals(ThemeMode.Light, mode) }
	}

	@Test
	fun failedServerRefreshCanBeRetriedWithoutHidingSettings() {
		var refreshCalls = 0
		showSettings(SettingsState(error = "Could not refresh server information."), onRefresh = { refreshCalls++ })
		compose.onNodeWithText("Appearance").assertIsDisplayed()
		compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Retry"))
		compose.onNodeWithText("Retry").performClick()
		compose.runOnIdle { assertEquals(1, refreshCalls) }
	}

	@Test
	fun detailedServerValuesRemainReadableWhenScrolling() {
		showSettings(SettingsState(serverDetails = ServerDetails(ServerInfo(
			version = "4.9.0.0",
			operatingSystemDisplayName = "Linux",
			cachePath = "/srv/emby/cache",
		))))
		for (value in listOf("4.9.0.0", "Linux", account.serverUrl, "/srv/emby/cache")) {
			compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(value))
			compose.onNodeWithText(value).assertIsDisplayed()
		}
	}

	private fun showSettings(state: SettingsState = SettingsState(), onRefresh: () -> Unit = {}, onSignOut: () -> Unit = {}) {
		compose.setContent {
			EmbympvTheme(dynamicColor = false) {
				SettingsContent(state, account, ThemeMode.System, {}, onRefresh, {}, onSignOut)
			}
		}
	}
}
