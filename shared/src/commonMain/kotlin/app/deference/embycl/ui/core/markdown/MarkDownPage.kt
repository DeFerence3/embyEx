package app.deference.embycl.ui.core.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.compose.extendedspans.rememberSquigglyUnderlineAnimator
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkDownPage(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }

    SelectionContainer(
		modifier = modifier
			.clip(RoundedCornerShape(16.dp))
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
	) {
        Markdown(
            markdownState = rememberMarkdownState(markdown) { markdown },
			typography = markdownTypography(
				h1 = MaterialTheme.typography.headlineMedium,
				h2 = MaterialTheme.typography.titleLarge,
				list = MaterialTheme.typography.bodyMedium,
				bullet = MaterialTheme.typography.labelMedium,
				text = MaterialTheme.typography.bodyMedium,
				textLink = TextLinkStyles(
					style = MaterialTheme.typography.labelLarge.copy(
						fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline
					).toSpanStyle()
				),
				paragraph = MaterialTheme.typography.bodyMedium
			),
            components = markdownComponents(
                codeBlock = {
                    MarkdownHighlightedCodeBlock(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                        showHeader = true,
                    )
                },
                codeFence = {
                    MarkdownHighlightedCodeFence(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                        showHeader = true,
                    )
                },
                checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
            ),
            annotator = rememberCheckAnnotator(markdownAnnotatorConfig(showImageAltTooltip = true)),
            inlineContent = rememberCheckInlineContent(),
            imageTransformer = Coil3ImageTransformerImpl,
            extendedSpans = markdownExtendedSpans {
                val animator = rememberSquigglyUnderlineAnimator()
                remember {
                    ExtendedSpans(
                        RoundedCornerSpanPainter(),
                        SquigglyUnderlineSpanPainter(animator = animator)
                    )
                }
            },
            loading = {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
					LoadingIndicator()
                }
            },
            modifier = Modifier
                .verticalScroll(rememberScrollState())
				.padding(8.dp)
        )
    }
}

