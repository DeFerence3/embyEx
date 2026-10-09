package app.deference.embycl.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

val Typography = Typography(
    displayLarge = type(57, 64, FontWeight.Bold, -1.5f),
    displayMedium = type(45, 52, FontWeight.Bold, -1f),
    displaySmall = type(36, 44, FontWeight.Bold, -0.8f),
    headlineLarge = type(32, 40, FontWeight.Bold, -0.6f),
    headlineMedium = type(28, 36, FontWeight.Bold, -0.4f),
    headlineSmall = type(24, 32, FontWeight.SemiBold, -0.3f),
    titleLarge = type(22, 28, FontWeight.SemiBold, -0.2f),
    titleMedium = type(16, 24, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24), bodyMedium = type(14, 20), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold, 0.1f),
    labelMedium = type(12, 16, FontWeight.Medium, 0.2f),
    labelSmall = type(11, 16, FontWeight.SemiBold, 0.4f),
)
