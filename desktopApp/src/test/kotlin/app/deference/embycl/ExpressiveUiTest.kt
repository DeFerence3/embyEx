package app.deference.embycl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.deference.embycl.ui.screens.settings.components.CurrentBuildChangelogDialog
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.deference.embycl.core.session.Session
import app.deference.embycl.core.session.Server
import app.deference.embycl.core.utils.HttpScheme
import app.deference.embycl.ui.core.ExitConfirmationDialog
import app.deference.embycl.data.preference.EmbyPreference
import app.deference.embycl.domain.model.*
import app.deference.embycl.domain.model.update.*
import app.deference.embycl.ui.ConnectionRecoveryContent
import app.deference.embycl.ui.core.components.*
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.core.update.UpdateAvailableDialog
import app.deference.embycl.ui.screens.details.components.adaptive.*
import app.deference.embycl.ui.screens.home.*
import app.deference.embycl.ui.screens.libraries.*
import app.deference.embycl.ui.screens.library.*
import app.deference.embycl.ui.screens.search.*
import app.deference.embycl.ui.screens.settings.*
import app.deference.embycl.ui.screens.settings.components.SettingsAccount
import app.deference.embycl.ui.screens.shell.*
import app.deference.embycl.ui.screens.signin.*
import app.deference.embycl.ui.theme.EmbympvTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/** Offline UI checkpoints: real Compose layouts and interactions, no user data or server required. */
@OptIn(ExperimentalTestApi::class)
class ExpressiveUiTest {
    private val movies = List(24) { EmbyItem("movie$it", listOf("The quiet horizon", "After the rain", "A world beyond", "Somewhere, together")[it % 4] + " $it", type = "Movie", productionYear = 2026) }
    private val libraries = List(12) { EmbyItem("library$it", "Collection ${it + 1}", type = "CollectionFolder", isFolder = true) }
    private val episodes = List(8) { EmbyItem("episode$it", "A very long episode title for a story worth coming back to ${it + 1}", type = "Episode", indexNumber = it + 1, parentIndexNumber = 1, runTimeTicks = 24_000_000_000) }
    private val home get() = HomeState(Result.success(EmbyHome(libraries, movies.take(3).map { it.copy(userData = EmbyUserData(playedPercentage = 42.0)) }, movies)))
    private val details = ItemDetailsData("Somewhere, together", null, null, "2026", "1h 48m", "4K · HEVC", "English · Dolby Atmos", "English · French · Spanish", "An unexpected journey brings two strangers together, and a familiar world begins to look a little different.", listOf("Alex Morgan"), listOf("Sam Rivera"), true, .42f, 63)

    @BeforeTest fun session() {
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(data.value).also { data.value = it }
        }
        Session.init(EmbyPreference(store))
        mapOf("username" to "Alex", "user_id" to "preview", "server-name" to "Home cinema", "server-url" to "http://127.0.0.1:1", "server_id" to "preview").forEach { (key, value) -> Session.preferences.save(key, value) }
    }

    private fun checkScreen(
        name: String, width: Int = 390, height: Int = 844, dark: Boolean = false, fontScale: Float = 1f,
        content: @Composable () -> Unit,
        verify: suspend SkikoComposeUiTest.() -> Unit = {},
    ) = runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat()), density = Density(1f, fontScale), testTimeout = 30.seconds) {
        setContent { EmbympvTheme(darkTheme = dark, dynamicColor = false) { Surface(Modifier.fillMaxSize()) { content() } } }
        waitForIdle()
        val directory = File("../build/ui-verification/screenshots").apply { mkdirs() }
        val roots = onAllNodes(isRoot())
        ImageIO.write(roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().toAwtImage(), "png", File(directory, "$name.png"))
        verify()
    }

    @Test fun signInPhoneAndDesktop() {
        checkScreen("01-signin-phone", content = { SignInContent(SignInState(server = "https://cinema.example"), {}) }) {
            onNodeWithText("Continue").assertIsEnabled()
        }
        checkScreen("01-signin-desktop-dark", 1280, 800, dark = true, content = { SignInContent(SignInState(), {}) }) {
            onNodeWithText("Continue").assertIsNotEnabled()
        }
    }

    @Test fun signInLargeTextAndErrorRemainReachable() = checkScreen("01-signin-large-text", 320, 640, fontScale = 2f, content = {
        SignInContent(SignInState(server = "https://cinema.example", error = "Could not reach the server. Check the address and try again."), {})
    }) {
        onNodeWithText("Continue").performScrollTo().assertIsDisplayed()
        onNodeWithText("Use local files").performScrollTo().assertIsDisplayed()
    }

    @Test fun passwordVisibilityHasAccessibleActions() = checkScreen("01-password", content = {
        var visible by remember { mutableStateOf(false) }
        PasswordField("sample", {}, visible, { visible = !visible }, {})
    }) {
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
    }

    @Test fun homePhoneAndDesktop() {
        checkScreen("02-home-phone", content = { HomeContent(home, {}, onItemClick = {}, onLibraryClick = {}) }) {
            onNodeWithText("Continue watching").assertIsDisplayed()
        }
        checkScreen("02-home-desktop-dark", 1280, 900, dark = true, content = { HomeContent(home, {}, onItemClick = {}, onLibraryClick = {}) })
    }

    @Test fun librariesLastRowIsReachable() {
        var opened = ""
        checkScreen("03-libraries-phone", content = { LibrariesContent(LibrariesState(Result.success(libraries)), {}, onLibraryClick = { opened = it.id }) }) {
            onNode(hasScrollToIndexAction()).performScrollToIndex(libraries.size)
            onNodeWithText("Collection 12").assertIsDisplayed().performClick()
            assertEquals("library11", opened)
        }
        checkScreen("03-libraries-empty", content = { LibrariesContent(LibrariesState(Result.success(emptyList())), {}, onLibraryClick = {}) })
    }

    @Test fun libraryAndEpisodes() {
        checkScreen("04-library-desktop", 1280, 800, content = { LibraryContent("Movies", LibraryState(Result.success(EmbyItemsResult(movies))), {}) })
        var opened = ""
        checkScreen("04-episodes-phone-dark", dark = true, content = { LibraryContent("Season 1", LibraryState(Result.success(EmbyItemsResult(episodes))), {}, onItemClick = { opened = it.id }) }) {
            onNodeWithText(episodes.first().name).performClick()
            assertEquals("episode0", opened)
        }
    }

    @Test fun searchFiltersAndClear() = checkScreen("05-search-phone", content = {
        var state by remember { mutableStateOf(SearchState(query = "story", results = movies.take(4) + episodes.take(2))) }
        SearchContent(state, { if (it is SearchAction.QueryChanged) state = SearchState(query = it.query) }, onItemClick = {})
    }) {
        onNodeWithText("Episodes (2)").performClick()
        onNodeWithText(episodes.first().name).assertIsDisplayed()
        onNodeWithText(movies.first().name).assertDoesNotExist()
        onNodeWithContentDescription("Clear search").performClick()
        onNodeWithText("What are you in the mood for?").assertIsDisplayed()
    }

    @Test fun detailsAcrossSizesAndReadableMetadata() {
        var played = false
        checkScreen("06-details-phone-dark", dark = true, content = { PortraitDetailsLayout(details, false) { played = true } }) {
            onNodeWithText("Resume").performScrollTo().performClick()
            assertTrue(played)
        }
        checkScreen("06-details-desktop", 1440, 900, content = { DesktopDetailsLayout(details) {} })
        checkScreen("06-details-landscape", 844, 390, dark = true, content = { LandscapeDetailsLayout(details, false) {} }) {
            onNodeWithText("Resume").performScrollTo().assertIsDisplayed()
        }
        checkScreen("06-details-large-text", 320, 700, fontScale = 2f, content = { PortraitDetailsLayout(details, false) {} }) {
            onNodeWithText("English · Dolby Atmos").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun settingsAndSignOutConfirmation() = checkScreen("07-settings-desktop", 1280, 900, content = {
        SettingsContent(SettingsState(), AppUpdate.Idle, SettingsAccount("Alex", "preview", "Home cinema", "http://127.0.0.1:1"), {}, {}, {}, {})
    }) {
        onNodeWithContentDescription("Sign out").performClick()
        onNodeWithText("Sign out?").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Sign out?").assertDoesNotExist()
    }

    @Test fun reconnectShortWindowAndLargeText() {
        var reconnect = false
        checkScreen("08-reconnect-small", 320, 540, fontScale = 1.5f, content = {
            ConnectionRecoveryContent("https://cinema.example", {}, false, false, emptyList(), "Server unavailable", { reconnect = true }, {}, {}, {})
        }) {
            onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Reconnect"))
            onNodeWithText("Reconnect").performClick()
            assertTrue(reconnect)
            onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Sign out of this server"))
            onNodeWithText("Sign out of this server").assertIsDisplayed()
        }
    }

    @Test fun errorRetryAndEmptyStates() {
        var retries = 0
        checkScreen("09-error-dark", dark = true, content = { ErrorState("The server is unavailable. Check your connection.") { retries++ } }) {
            onNodeWithText("Try again").performClick()
            assertEquals(1, retries)
        }
        checkScreen("09-home-empty", content = { HomeContent(HomeState(Result.success(EmbyHome())), {}, onItemClick = {}, onLibraryClick = {}) })
    }

    @Test fun shellAdaptiveNavigationAndTabState() {
        for ((name, width) in listOf("phone" to 390, "desktop" to 1280)) {
            checkScreen("00-shell-$name", width, 844, dark = name == "desktop", content = {
                val navigator = remember { Navigator(EmbyShellScreen) }
                var state by remember { mutableStateOf(ShellState()) }
                CompositionLocalProvider(LocalNavigator provides navigator) {
                    EmbyShellContent(state, { if (it is ShellAction.SelectTab) state = state.copy(selectedTab = it.tab) },
                        emptyFlow(), home, {}, LibrariesState(Result.success(libraries)), {}, SearchState(), {})
                }
            }) {
                onNodeWithText("Libraries").performClick()
                onNode(hasScrollToIndexAction()).performScrollToIndex(libraries.size)
                onNodeWithText("Collection 12").assertIsDisplayed()
                onNodeWithText("Home").performClick()
                onNodeWithText("Libraries").performClick()
                onNodeWithText("Collection 12").assertIsDisplayed()
            }
        }
    }
    @Test fun accountPickerManualAndPasswordStates() {
        val user = EmbyUser("alex", "Alex", hasPassword = true)
        val discovery = EmbyServerDiscovery(Server("127.0.0.1", 1, HttpScheme.Http), PublicSystemInfo("Home cinema"), listOf(user), "preview")
        var action: SignInAction? = null
        checkScreen("01-accounts", content = { SignInContent(SignInState(discovery = discovery), { action = it }) }) {
            onNodeWithText("Alex").performClick()
            assertEquals(SignInAction.SelectUser(user), action)
        }
        checkScreen("01-manual-dark", dark = true, content = { SignInContent(SignInState(discovery = discovery, isManualSignIn = true, username = "Alex"), {}) }) {
            onNodeWithText("Sign in").assertIsEnabled()
            onNodeWithText("Username").assertExists()
        }
        checkScreen("01-account-password", content = { SignInContent(SignInState(discovery = discovery, selectedUser = user), {}) }) {
            onNodeWithContentDescription("Show password").assertExists()
            onNodeWithText("Choose another user").assertExists()
        }
    }

    @Test fun updateDialogActionsAndShortWindow() {
        val release = UpdateRelease("2.0", "v2.0", 20, "# What’s new\n\n" + "- A more expressive experience.\n".repeat(15))
        var downloads = 0
        checkScreen("10-update-available", 390, 700, content = {
            UpdateAvailableDialog(AppUpdate.Available(release), {}, { downloads++ }, {}, {})
        }) {
            onNodeWithText("Download update").assertIsDisplayed().performClick()
            assertEquals(1, downloads)
        }
        var hidden = false
        checkScreen("10-update-download-short", 640, 360, dark = true, fontScale = 1.5f, content = {
            UpdateAvailableDialog(AppUpdate.Downloading(release, 5_242_880, 10_485_760), {}, {}, {}, { hidden = true })
        }) {
            onNodeWithText("Hide").assertIsDisplayed().performClick()
            assertTrue(hidden)
        }
        checkScreen("10-update-unknown-size", content = {
            UpdateAvailableDialog(AppUpdate.Downloading(release, 5_242_880, 0), {}, {}, {}, {})
        }) { onNodeWithText("Downloading… 5.00 MB").assertExists() }
        checkScreen("10-update-current", content = { UpdateAvailableDialog(AppUpdate.NotAvailable, {}, {}, {}, {}) }) {
            onNodeWithText("You’re up to date").assertExists()
            onNodeWithText("Download update").assertDoesNotExist()
        }
    }

    @Test fun exitAndReconnectDisabledStates() {
        var closed = false
        checkScreen("10-exit", content = { ExitConfirmationDialog({ closed = true }, {}) }) {
            onNodeWithText("Exit").performClick()
            assertTrue(closed)
        }
        checkScreen("08-reconnect-blank", content = {
            ConnectionRecoveryContent("", {}, false, false, emptyList(), null, {}, {}, {}, {})
        }) { onNodeWithText("Reconnect").assertIsNotEnabled() }
    }

    @Test fun detailFailureKeepsBackAndRetryAvailable() {
        var back = false
        var retry = false
        checkScreen("06-details-error", content = {
            LoadingScaffold<String>(Result.failure(IllegalStateException("Server unavailable")),
                onRetry = { retry = true }, topBar = { DetailTopBar("Details") { back = true } }) {}
        }) {
            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Try again").performClick()
            assertTrue(back)
            assertTrue(retry)
        }
    }

    @Test fun settingsCurrentBuildReleaseActions() {
        var openedUrl = ""
        var loads = 0
        checkScreen("07-settings-release-actions", 1280, 900, content = {
            CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) { openedUrl = uri }
            }) {
                SettingsContent(SettingsState(changelog = Result.success("## This build\nPlayback improvements.")),
                    AppUpdate.Idle, SettingsAccount("Alex", "preview", "Home cinema", "http://127.0.0.1:1"),
                    {}, {}, {}, {}, onLoadChangelog = { loads++ })
            }
        }) {
            onNodeWithText("View release page").performClick()
            assertEquals(CurrentBuildRelease.pageUrl, openedUrl)
            onNodeWithText("Current build changelog").performClick()
            onNodeWithText("Changelog · ${CurrentBuildRelease.tag}").assertIsDisplayed()
            assertEquals(1, loads)
            onNodeWithText("Close").performClick()
            onNodeWithText("Changelog · ${CurrentBuildRelease.tag}").assertDoesNotExist()
        }
    }

    @Test fun currentBuildChangelogStates() {
        checkScreen("07-changelog-content", content = {
            CurrentBuildChangelogDialog(SettingsState(changelog = Result.success("## This build\n\nPlayback improvements.")), {}, {}, {})
        }) { onNodeWithText("Playback improvements.", substring = true).assertExists() }
        var retries = 0
        checkScreen("07-changelog-error", content = {
            CurrentBuildChangelogDialog(SettingsState(changelog = Result.failure(IllegalStateException("Release notes unavailable"))), { retries++ }, {}, {})
        }) {
            onNodeWithText("Try again").performClick()
            assertEquals(1, retries)
        }
        checkScreen("07-changelog-empty", content = {
            CurrentBuildChangelogDialog(SettingsState(changelog = Result.success("")), {}, {}, {})
        }) { onNodeWithText("No changelog was provided for this build.").assertIsDisplayed() }
    }

}
