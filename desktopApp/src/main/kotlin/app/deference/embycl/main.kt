package app.deference.embycl

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import app.deference.embycl.core.di.initKoin
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.local.preference.EmbyPreference
import app.deference.embycl.data.local.preference.createDataStore
import app.deference.embycl.data.repository.download.DesktopDownloadRuntime
import app.deference.embycl.ui.core.ExitConfirmationDialog
import app.deference.embycl.ui.theme.EmbympvTheme
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.tao.LocalWindowExceptionHandlerFactory
import dev.nucleusframework.window.tao.WindowExceptionHandlerFactory
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.app_name
import embyex.shared.generated.resources.embyex_logo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatformTools
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
	// Must execute before nucleusApplication: restarted child waits outside
	// the single-instance lock until the previous process has exited.
	val effectiveArgs = waitForPreviousProcessIfRequested(args)
	
	nucleusApplication(effectiveArgs) {
		val downloads = remember {
			Session.init(EmbyPreference(createDataStore()))
			initKoin()
			KoinPlatformTools.defaultContext().get().get<DesktopDownloadRuntime>().also { it.start() }
		}
		val applicationScope = rememberCoroutineScope()
		DisposableEffect(downloads) {
			val shutdown = Thread(
				{ runBlocking { withTimeoutOrNull(5.seconds) { downloads.close() } } },
				"offline-download-shutdown",
			)
			Runtime.getRuntime().addShutdownHook(shutdown)
			onDispose {
				runBlocking { withTimeoutOrNull(5.seconds) { downloads.close() } }
				runCatching { Runtime.getRuntime().removeShutdownHook(shutdown) }
			}
		}
		
		val mainWindowState = rememberWindowState(placement = WindowPlacement.Maximized)
		val crashWindowState = rememberWindowState(
			position = WindowPosition.Aligned(Alignment.Center),
			size = DpSize(1200.dp,700.dp)
		)
		var capturedCrash by remember { mutableStateOf<Throwable?>(null) }
		
		val crashHandlerFactory = WindowExceptionHandlerFactory{ _ ->
			WindowExceptionHandler { throwable ->
				capturedCrash = throwable
			}
		}
		
		EmbympvTheme(darkTheme = isSystemInDarkMode()) {
			CompositionLocalProvider(
				LocalWindowExceptionHandlerFactory provides crashHandlerFactory,
			) {
				val crash = capturedCrash
				if (crash != null) {
					MaterialDecoratedWindow(
						state = crashWindowState,
						onCloseRequest = {
							applicationScope.launch {
								downloads.close()
								exitProcess(0)
							}
						},
						title = "EmbyEx crashed",
						icon = painterResource(Res.drawable.embyex_logo),
					) {
						EmbyExTitleBar()
						DesktopCrashScreen(
							throwable = crash,
							appVersion = BuildConfig.VERSION_NAME,
							onRestart = {
								restartDesktopProcess()
								// Shutdown hook flushes and closes downloads.
								exitProcess(0)
							},
							onExit = {
								applicationScope.launch {
									downloads.close()
									exitProcess(0)
								}
							},
						)
					}
				} else {
					var isLogoutRequested by remember { mutableStateOf(false) }
					MaterialDecoratedWindow(
						state = mainWindowState,
						onCloseRequest = { isLogoutRequested = true },
						title = stringResource(Res.string.app_name),
						icon = painterResource(Res.drawable.embyex_logo),
					) {
						if (isLogoutRequested) {
							ExitConfirmationDialog(
								onConfirm = {
									applicationScope.launch {
										downloads.close()
										exitApplication()
									}
								},
								onDismiss = { isLogoutRequested = false },
							)
						}
						val isLoggedIn by Session.isLoggedInState.collectAsState()
						EmbyExTitleBar()
						EmbyExApp(isLoggedIn)
					}
				}
			}
		}
	}
}


/*
fun main() = nucleusApplication {
    val downloads = remember {
        Session.init(EmbyPreference(createDataStore()))
        initKoin()
        KoinPlatformTools.defaultContext().get().get<DesktopDownloadRuntime>().also { it.start() }
    }
    val applicationScope = rememberCoroutineScope()
    DisposableEffect(downloads) {
        val shutdown = Thread({ runBlocking { withTimeoutOrNull(5_000) { downloads.close() } } }, "offline-download-shutdown")
        Runtime.getRuntime().addShutdownHook(shutdown)
        onDispose {
            runBlocking { withTimeoutOrNull(5_000) { downloads.close() } }
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdown) }
        }
    }
	val state = rememberWindowState(placement = WindowPlacement.Maximized)
	var isLogoutRequested by remember { mutableStateOf(false) }
	MaterialDecoratedWindow(
		state = state,
		onCloseRequest = { isLogoutRequested = true },
		title = stringResource(Res.string.app_name),
		icon = painterResource(Res.drawable.embyex_logo)
	) {
		EmbympvTheme(
			darkTheme = isSystemInDarkMode()
		) {
			if (isLogoutRequested){
				ExitConfirmationDialog(
					onConfirm = { applicationScope.launch { downloads.close(); exitApplication() } },
					onDismiss = { isLogoutRequested = false }
				)
			}
			val isLoggedIn by Session.isLoggedInState.collectAsState()
			EmbyExTitleBar()
			EmbyExApp(isLoggedIn)
		}
	}
}
*/
