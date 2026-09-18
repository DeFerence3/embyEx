package app.deference.embcl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.JsonUtils
import app.deference.embcl.data.preference.EmbyPreference
import app.deference.embcl.domain.model.AuthenticationResult
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.model.ServerInfo
import app.deference.embcl.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsPersistenceTest {
	@get:Rule val temporaryFolder = TemporaryFolder()

	@Test
	fun themeSurvivesSessionReloadAndSignOut() {
		val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
		val store = PreferenceDataStoreFactory.create(scope = scope) {
			temporaryFolder.root.resolve("settings.preferences_pb")
		}
		try {
			Session.init(EmbyPreference(store))
			assertEquals(ThemeMode.System, Session.themeMode.value)
			Session.setThemeMode(ThemeMode.Dark)
			Session.init(EmbyPreference(store))
			assertEquals(ThemeMode.Dark, Session.themeMode.value)
			Session.login(AuthenticationResult(EmbyUser("test-user", "Viewer"), "test-token"), "http://localhost:8096", "Test server")
			Session.logout()
			Session.init(EmbyPreference(store))
			assertEquals(ThemeMode.Dark, Session.themeMode.value)
			assertNull(Session.accessToken)
			assertFalse(Session.isLoggedInState.value)
			assertEquals("http://localhost:8096", Session.getLastServerUrl())
		} finally {
			scope.cancel()
		}
	}

	@Test
	fun publicServerResponseDoesNotInventUnsupportedCapabilities() {
		val info = JsonUtils.json.decodeFromString<ServerInfo>("""
			{"ServerName":"Media server","Version":"4.9.0","Id":"server-id","LocalAddresses":null,"ExtraField":"ignored"}
		""")
		assertEquals("Media server", info.serverName)
		assertEquals("4.9.0", info.version)
		assertEquals(emptyList<String>(), info.localAddresses)
		assertNull(info.operatingSystem)
		assertNull(info.supportsHttps)
		assertNull(info.hasPendingRestart)
	}

	@Test
	fun detailedServerResponsePreservesStatusAndPaths() {
		val info = JsonUtils.json.decodeFromString<ServerInfo>("""
			{"OperatingSystemDisplayName":"Linux","SupportsHttps":false,"HasPendingRestart":true,
			"HttpServerPortNumber":8096,"LocalAddresses":["http://192.0.2.1:8096"],"CachePath":"/config/cache"}
		""")
		assertEquals("Linux", info.operatingSystemDisplayName)
		assertEquals(false, info.supportsHttps)
		assertEquals(true, info.hasPendingRestart)
		assertEquals(8096, info.httpPort)
		assertEquals(listOf("http://192.0.2.1:8096"), info.localAddresses)
		assertEquals("/config/cache", info.cachePath)
	}
}
