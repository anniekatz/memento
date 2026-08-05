package com.annie.memento.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.annie.memento.model.AppSettings
import com.annie.memento.model.CardFont
import com.annie.memento.model.CardFontWeight
import com.annie.memento.model.CardTextSettings
import com.annie.memento.ui.richtext.RichText
import com.annie.memento.ui.theme.InsetShape
import com.annie.memento.ui.theme.scaledBy
import com.annie.memento.ui.theme.withCardFont
import kotlin.math.roundToInt

// font family, bold level, sizes - global and per deck settings
@Composable
fun CardTextControls(
    value: CardTextSettings,
    onChange: (CardTextSettings) -> Unit,
) {
    var mainDrag by remember(value.mainTextScale) { mutableStateOf(value.mainTextScale) }
    var examplesDrag by remember(value.examplesScale) { mutableStateOf(value.examplesScale) }
    var notesDrag by remember(value.notesScale) { mutableStateOf(value.notesScale) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Font", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        CardFont.entries.chunked(2).forEach { rowFonts ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowFonts.forEach { option ->
                    FontOption(
                        font = option,
                        selected = value.font == option,
                        onClick = { onChange(value.copy(font = option)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text("Main text boldness", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        CardFontWeight.entries.chunked(2).forEach { rowWeights ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowWeights.forEach { option ->
                    WeightOption(
                        weight = option,
                        font = value.font,
                        selected = value.fontWeight == option,
                        onClick = { onChange(value.copy(fontWeight = option)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        ScaleSliderRow(
            label = "Main text",
            scale = mainDrag,
            onScaleChange = { mainDrag = it },
            onScaleFinished = {
                val snapped = snapTextScale(mainDrag)
                mainDrag = snapped
                onChange(value.copy(mainTextScale = snapped))
            },
        )
        ScaleSliderRow(
            label = "Examples",
            scale = examplesDrag,
            onScaleChange = { examplesDrag = it },
            onScaleFinished = {
                val snapped = snapTextScale(examplesDrag)
                examplesDrag = snapped
                onChange(value.copy(examplesScale = snapped))
            },
        )
        ScaleSliderRow(
            label = "Notes",
            scale = notesDrag,
            onScaleChange = { notesDrag = it },
            onScaleFinished = {
                val snapped = snapTextScale(notesDrag)
                notesDrag = snapped
                onChange(value.copy(notesScale = snapped))
            },
        )

        CardTextPreview(
            mainScale = mainDrag,
            examplesScale = examplesDrag,
            notesScale = notesDrag,
            font = value.font,
            fontWeight = value.fontWeight,
        )
    }
}

@Composable
private fun FontOption(font: CardFont, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OptionChip(selected = selected, onClick = onClick, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "日本語 Aa Bb",
                style = MaterialTheme.typography.headlineSmall.withCardFont(font, CardFontWeight.REGULAR),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                font.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightOption(
    weight: CardFontWeight,
    font: CardFont,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionChip(selected = selected, onClick = onClick, modifier = modifier) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                weight.label.uppercase(),
                style = MaterialTheme.typography.titleSmall.withCardFont(font, weight),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun OptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.clip(InsetShape).clickable(onClick = onClick),
        shape = InsetShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        content = content,
    )
}

@Composable
private fun ScaleSliderRow(
    label: String,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onScaleFinished: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${(scale * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("A", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                onValueChangeFinished = onScaleFinished,
                valueRange = AppSettings.MIN_TEXT_SCALE..AppSettings.MAX_TEXT_SCALE,
                steps = TEXT_SCALE_STEPS,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.weight(1f),
            )
            Text("A", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardTextPreview(
    mainScale: Float,
    examplesScale: Float,
    notesScale: Float,
    font: CardFont,
    fontWeight: CardFontWeight,
) {
    Surface(
        shape = InsetShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            RichText(
                markup = "日本語[にほんご] Aa Bb",
                rich = true,
                style = MaterialTheme.typography.headlineMedium.scaledBy(mainScale).withCardFont(font, fontWeight),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            RichText(
                markup = "• 東京に行きました = I went to Tokyo",
                rich = false,
                style = MaterialTheme.typography.bodyMedium.scaledBy(examplesScale).withCardFont(font, weight = null),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            RichText(
                markup = "A note about this card",
                rich = false,
                style = MaterialTheme.typography.bodyMedium.scaledBy(notesScale).withCardFont(font, weight = null),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val CardFont.label: String
    get() = when (this) {
        CardFont.SYSTEM -> "System"
        CardFont.NOTO_SANS -> "Noto Sans"
        CardFont.NOTO_SERIF -> "Noto Serif"
        CardFont.ZEN_MARU -> "Zen Maru"
    }

private val CardFontWeight.label: String
    get() = when (this) {
        CardFontWeight.LIGHT -> "Light"
        CardFontWeight.REGULAR -> "Regular"
        CardFontWeight.MEDIUM -> "Medium"
        CardFontWeight.BOLD -> "Bold"
    }

private const val TEXT_SCALE_INCREMENT = 0.05f
private val TEXT_SCALE_STEPS =
    ((AppSettings.MAX_TEXT_SCALE - AppSettings.MIN_TEXT_SCALE) / TEXT_SCALE_INCREMENT).roundToInt() - 1

private fun snapTextScale(value: Float): Float {
    val steps = ((value - AppSettings.MIN_TEXT_SCALE) / TEXT_SCALE_INCREMENT).roundToInt()
    val snapped = AppSettings.MIN_TEXT_SCALE + steps * TEXT_SCALE_INCREMENT
    return snapped.coerceIn(AppSettings.MIN_TEXT_SCALE, AppSettings.MAX_TEXT_SCALE)
}
