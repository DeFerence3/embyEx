package app.deference.embcl

import androidx.activity.compose.setContent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateRelease
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.ui.core.update.UpdateAvailableDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidUpdateUiTest {
    @Test fun dialogRendersReleaseProgressErrorAndInstallStates() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val release = UpdateRelease("UI test", 3, "# Changes\n" + "- Release note\n".repeat(30))
        val update = mutableStateOf<AppUpdate>(AppUpdate.Available(release))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    MaterialTheme {
                        UpdateAvailableDialog(update.value, {}, {}, {}, {})
                    }
                }
            }
            suspend fun shows(text: String) {
                val found = withTimeoutOrNull(10_000) {
                    while (!windowText(instrumentation.uiAutomation.rootInActiveWindow).contains(text)) delay(100)
                    true
                }
                assertNotNull("Missing '$text' in: ${windowText(instrumentation.uiAutomation.rootInActiveWindow)}", found)
            }
            shows("Update available")
            shows("Download update")
            instrumentation.runOnMainSync { update.value = AppUpdate.Downloading(release, 512, 1024) }
            shows("50%")
            instrumentation.runOnMainSync {
                update.value = AppUpdate.Failed("Checksum mismatch", UpdateStage.Download, release)
            }
            shows("Checksum mismatch")
            shows("Retry")
            instrumentation.runOnMainSync { update.value = AppUpdate.ReadyToInstall(release) }
            shows("Install update")
            instrumentation.runOnMainSync { update.value = AppUpdate.AwaitingPermission(release) }
            shows("Open settings")
        }
    }

    private fun windowText(node: AccessibilityNodeInfo?): String = if (node == null) "" else buildString {
        append(node.text?.toString().orEmpty()).append('\n')
        for (index in 0 until node.childCount) append(windowText(node.getChild(index)))
    }
}
