package app.deference.embycl.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.screens.details.components.adaptive.ItemDetailsData
import coil3.compose.AsyncImage

@Composable
fun DetailsContent(
	data: ItemDetailsData,
	modifier: Modifier = Modifier,
	compact: Boolean,
	fillPlayButton: Boolean,
	onPlay: () -> Unit,
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(
			if (compact) 18.dp else 22.dp
		),
	) {
		
		if (!data.logo.isNullOrBlank()) {
			AsyncImage(
				model = data.logo,
				contentDescription = data.title,
				modifier = Modifier
					.height(
						if (compact) 54.dp
						else 82.dp
					)
					.fillMaxWidth(
						if (compact) 0.72f
						else 0.62f
					),
				contentScale = ContentScale.Fit,
				alignment = Alignment.CenterStart,
			)
		}
		
		Column(
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			
			Text(
				text = data.title,
				style = if (compact) {
					MaterialTheme.typography.headlineSmall
				} else {
					MaterialTheme.typography.headlineLarge
				},
				fontWeight = FontWeight.Bold,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
			
			MetadataPills(
				airDate = data.airDate,
				runtime = data.runtime,
				resolution = data.resolution,
			)
		}
		
		MediaInformationCard(
			compact = compact,
			videoResolution = data.resolution,
			audioTitle = data.audio,
			subtitles = data.subtitles,
		)
		
		PlayButton(
			compact = fillPlayButton,
			isResume = data.isResume,
			onClick = onPlay,
		)
		
		ResumeProgress(
			visible = data.isResume &&
					data.progress > 0f,
			progress = data.progress,
			remainingMinutes = data.remainingMinutes,
		)
		
		data.overview
			?.takeIf(String::isNotBlank)
			?.let { overview ->
				
				Text(
					text = overview,
					style = if (compact) {
						MaterialTheme.typography.bodyMedium
					} else {
						MaterialTheme.typography.bodyLarge
					},
					lineHeight =
						if (compact) 22.sp else 25.sp
				)
			}
		
		if (
			data.directors.isNotEmpty() ||
			data.writers.isNotEmpty()
		) {
			CreditsSection(
				compact = compact,
				directors = data.directors,
				writers = data.writers,
			)
		}
	}
}

@Composable
private fun MetadataPills(
	airDate: String?,
	runtime: String?,
	resolution: String,
) {
	/*
	 * Horizontal scrolling protects small phones from metadata wrapping
	 * into awkward three/four-line layouts.
	 */
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		airDate
			?.takeIf { it.isNotBlank() }
			?.let {
				MetadataPill(it)
			}
		
		runtime
			?.takeIf { it.isNotBlank() }
			?.let {
				MetadataPill(it)
			}
		
		MetadataPill(resolution)
	}
}

@Composable
private fun MetadataPill(
	text: String,
) {
	Surface(
		shape = RoundedCornerShape(50),
		border = BorderStroke(
			width = 1.dp,
			color = MaterialTheme.colorScheme.outlineVariant,
		),
	) {
		Text(
			text = text,
			modifier = Modifier.padding(
				horizontal = 11.dp,
				vertical = 6.dp,
			),
			style = MaterialTheme.typography.labelMedium,
			fontWeight = FontWeight.Medium,
			maxLines = 1,
		)
	}
}

@Composable
private fun MediaInformationCard(
	compact: Boolean,
	videoResolution: String,
	audioTitle: String,
	subtitles: String,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(18.dp),
		border = BorderStroke(
			1.dp,
			MaterialTheme.colorScheme.outlineVariant,
		),
	) {
		if (compact) {
			Column(
				modifier = Modifier.padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				MediaValue(
					label = "VIDEO",
					value = videoResolution,
				)
				
				MediaValue(
					label = "AUDIO",
					value = audioTitle,
				)
				
				MediaValue(
					label = "SUBTITLES",
					value = subtitles,
				)
			}
		} else {
			Row(
				modifier = Modifier.padding(
					horizontal = 20.dp,
					vertical = 16.dp,
				),
				horizontalArrangement = Arrangement.spacedBy(28.dp),
			) {
				MediaValue(
					modifier = Modifier.weight(0.7f),
					label = "VIDEO",
					value = videoResolution,
				)
				
				MediaValue(
					modifier = Modifier.weight(1.2f),
					label = "AUDIO",
					value = audioTitle,
				)
				
				MediaValue(
					modifier = Modifier.weight(1.2f),
					label = "SUBTITLES",
					value = subtitles,
				)
			}
		}
	}
}

@Composable
private fun MediaValue(
	label: String,
	value: String,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(2.dp),
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			fontWeight = FontWeight.SemiBold,
			letterSpacing = 0.7.sp,
		)
		
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium,
			autoSize = TextAutoSize.StepBased(
				minFontSize = 8.sp,
				maxFontSize = 14.sp,
				stepSize = 3.sp
			),
		)
	}
}

@Composable
private fun PlayButton(
	compact: Boolean,
	isResume: Boolean,
	onClick: () -> Unit,
) {
	val interactionSource = remember {
		MutableInteractionSource()
	}
	
	val hovered by interactionSource.collectIsHoveredAsState()
	val pressed by interactionSource.collectIsPressedAsState()
	
	val scale by animateFloatAsState(
		targetValue = when {
			pressed -> 0.97f
			hovered -> 1.025f
			else -> 1f
		},
		animationSpec = tween(120),
		label = "playButtonScale",
	)
	
	Button(
		onClick = onClick,
		interactionSource = interactionSource,
		modifier = Modifier
			.then(
				if (compact) {
					Modifier.fillMaxWidth()
				} else {
					Modifier.widthIn(
						min = 230.dp,
						max = 310.dp,
					)
				},
			)
			.height(54.dp)
			.animateWithHover(interactionSource),
		shape = RoundedCornerShape(50),
		contentPadding = PaddingValues(
			horizontal = 24.dp,
		),
	) {
		Icon(
			imageVector = Icons.Filled.PlayArrow,
			contentDescription = null,
			modifier = Modifier.size(24.dp),
		)
		
		Spacer(Modifier.width(8.dp))
		
		Text(
			text = if (isResume) {
				"Resume"
			} else {
				"Play"
			},
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Composable
private fun ResumeProgress(
	visible: Boolean,
	progress: Float,
	remainingMinutes: Long?,
) {
	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = tween(durationMillis = 650),
		label = "resumeProgress",
	)
	
	AnimatedVisibility(
		visible = visible,
		enter = fadeIn() + expandVertically(),
		exit = fadeOut() + shrinkVertically(),
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Text(
					text = "Continue watching",
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.Medium,
				)
				
				if (remainingMinutes != null) {
					Text(
						text = "$remainingMinutes min remaining",
						style = MaterialTheme.typography.labelMedium,
					)
				}
			}
			
			LinearProgressIndicator(
				progress = { animatedProgress },
				modifier = Modifier
					.fillMaxWidth()
					.height(5.dp)
					.clip(RoundedCornerShape(50)),
				trackColor = MaterialTheme.colorScheme.secondaryContainer,
			)
		}
	}
}

@Composable
private fun CreditsSection(
	compact: Boolean,
	directors: List<String>,
	writers: List<String>,
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(18.dp),
	) {
		if (compact) {
			Column(
				modifier = Modifier.padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				if (directors.isNotEmpty()) {
					CreditValue(
						label = if (directors.size == 1) {
							"DIRECTOR"
						} else {
							"DIRECTORS"
						},
						value = directors.joinToString(", "),
					)
				}
				
				if (writers.isNotEmpty()) {
					CreditValue(
						label = if (writers.size == 1) {
							"WRITER"
						} else {
							"WRITERS"
						},
						value = writers.joinToString(", "),
					)
				}
			}
		} else {
			Row(
				modifier = Modifier.padding(18.dp),
				horizontalArrangement = Arrangement.spacedBy(32.dp),
			) {
				if (directors.isNotEmpty()) {
					CreditValue(
						modifier = Modifier.weight(1f),
						label = if (directors.size == 1) {
							"DIRECTOR"
						} else {
							"DIRECTORS"
						},
						value = directors.joinToString(", "),
					)
				}
				
				if (writers.isNotEmpty()) {
					CreditValue(
						modifier = Modifier.weight(1f),
						label = if (writers.size == 1) {
							"WRITER"
						} else {
							"WRITERS"
						},
						value = writers.joinToString(", "),
					)
				}
			}
		}
	}
}
@Composable
private fun CreditValue(
	label: String,
	value: String,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(5.dp),
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			fontWeight = FontWeight.SemiBold,
			letterSpacing = 0.7.sp,
		)
		
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			lineHeight = 20.sp,
		)
	}
}
