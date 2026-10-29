package app.deference.embycl.ui.player

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerTooltip(
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val delegate = rememberTooltipState()
    val state = remember(key1 = delegate) {
        object : TooltipState by delegate {
            private var showing: Job? = null

            override suspend fun show(mutatePriority: MutatePriority) = coroutineScope {
                val job = coroutineContext.job
                showing?.cancel()
                showing = job
                try {
                    // Material uses UserInput for hover and PreventUserInput for long press.
                    if (mutatePriority == MutatePriority.UserInput) delay(timeMillis = 800L)
                    delegate.show(mutatePriority = mutatePriority)
                } finally {
                    if (showing === job) showing = null
                }
            }

            override fun dismiss() {
                showing?.cancel()
                delegate.dismiss()
            }

            override fun onDispose() {
                showing?.cancel()
                delegate.onDispose()
            }
        }
    }
    // TooltipBox applies its modifier to an inner anchor. Keep parent layout data
    // (player alignment and unlock-thumb offset) on our actual root.
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
            ),
            tooltip = { PlainTooltip { Text(text = description) } },
            state = state,
        ) {
            content(state::dismiss)
        }
    }
}
