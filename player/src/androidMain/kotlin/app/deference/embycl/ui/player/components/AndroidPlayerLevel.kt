package app.deference.embycl.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.deference.embycl.ui.player.AndroidPlayerFeedback
import app.deference.embycl.ui.theme.PlayerColors
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

@Composable
internal fun AndroidPlayerLevel(
    feedback: AndroidPlayerFeedback,
    modifier: Modifier = Modifier,
) {
    val level = feedback.level ?: return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(size = 24.dp),
        color = PlayerColors.controlsSurface,
        contentColor = PlayerColors.content,
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(text = NumberFormat.getPercentInstance().format(level))
            Box(
                modifier = Modifier
                    .width(width = 6.dp)
                    .height(height = 96.dp)
                    .clip(shape = RoundedCornerShape(percent = 50))
                    .background(color = PlayerColors.controlsOutline),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = level)
                    .background(color = PlayerColors.content))
            }
            Icon(
                imageVector = if (feedback.onLeft) Icons.Default.VolumeUp else Icons.Default.BrightnessHigh,
                contentDescription = feedback.message,
            )
        }
    }
}
