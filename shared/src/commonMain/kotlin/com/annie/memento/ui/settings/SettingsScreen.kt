package com.annie.memento.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.di.LocalAppSettings
import com.annie.memento.model.AppSettings
import com.annie.memento.ui.components.MementoOutlineButton
import com.annie.memento.ui.components.MementoPanel
import com.annie.memento.ui.components.MementoScaffold
import com.annie.memento.ui.components.SectionHeader
import com.annie.memento.ui.richtext.RichText
import com.annie.memento.ui.theme.InsetShape
import com.annie.memento.ui.theme.scaledBy
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    val repo = LocalAppGraph.current.settingsRepository
    val scope = rememberCoroutineScope()

    MementoScaffold(
        title = "Settings",
        overline = "MEMENTO · CONFIG",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AudioAutoplaySetting(
                enabled = settings.audioAutoplay,
                onChange = { scope.launch { repo.setAudioAutoplay(it) } },
            )
            TextSizeSetting(
                mainScale = settings.mainTextScale,
                examplesScale = settings.examplesScale,
                notesScale = settings.notesScale,
                onMainChange = { scope.launch { repo.setMainTextScale(it) } },
                onExamplesChange = { scope.launch { repo.setExamplesScale(it) } },
                onNotesChange = { scope.launch { repo.setNotesScale(it) } },
            )
        }
    }
}

@Composable
private fun AudioAutoplaySetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(enabled) }
    MementoPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Audio")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Autoplay audio",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "When you reach or flip to a side that has audio, its first clip plays automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        onChange(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun TextSizeSetting(
    mainScale: Float,
    examplesScale: Float,
    notesScale: Float,
    onMainChange: (Float) -> Unit,
    onExamplesChange: (Float) -> Unit,
    onNotesChange: (Float) -> Unit,
) {
    var main by remember { mutableStateOf(mainScale) }
    var examples by remember { mutableStateOf(examplesScale) }
    var notes by remember { mutableStateOf(notesScale) }

    MementoPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Text size")
            Text(
                "Size each part of a card independently. Applies to every deck and card.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ScaleSliderRow(
                label = "Main text",
                scale = main,
                onScaleChange = { main = it },
                onScaleFinished = {
                    val snapped = snapTextScale(main)
                    main = snapped
                    onMainChange(snapped)
                },
            )
            ScaleSliderRow(
                label = "Examples",
                scale = examples,
                onScaleChange = { examples = it },
                onScaleFinished = {
                    val snapped = snapTextScale(examples)
                    examples = snapped
                    onExamplesChange(snapped)
                },
            )
            ScaleSliderRow(
                label = "Notes",
                scale = notes,
                onScaleChange = { notes = it },
                onScaleFinished = {
                    val snapped = snapTextScale(notes)
                    notes = snapped
                    onNotesChange(snapped)
                },
            )

            TextSizePreview(mainScale = main, examplesScale = examples, notesScale = notes)

            val anyChanged = listOf(main, examples, notes).any { (it * 100).roundToInt() != 100 }
            if (anyChanged) {
                MementoOutlineButton(
                    text = "Reset to default",
                    onClick = {
                        val d = AppSettings.DEFAULT_TEXT_SCALE
                        main = d
                        examples = d
                        notes = d
                        onMainChange(d)
                        onExamplesChange(d)
                        onNotesChange(d)
                    },
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
private fun TextSizePreview(mainScale: Float, examplesScale: Float, notesScale: Float) {
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
                markup = "日本語[にほんご]",
                rich = true,
                style = MaterialTheme.typography.headlineMedium.scaledBy(mainScale),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            RichText(
                markup = "• The quick brown fox",
                rich = false,
                style = MaterialTheme.typography.bodyMedium.scaledBy(examplesScale),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            RichText(
                markup = "A note about this card",
                rich = false,
                style = MaterialTheme.typography.bodyMedium.scaledBy(notesScale),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val TEXT_SCALE_INCREMENT = 0.05f
private val TEXT_SCALE_STEPS =
    ((AppSettings.MAX_TEXT_SCALE - AppSettings.MIN_TEXT_SCALE) / TEXT_SCALE_INCREMENT).roundToInt() - 1

private fun snapTextScale(value: Float): Float {
    val steps = ((value - AppSettings.MIN_TEXT_SCALE) / TEXT_SCALE_INCREMENT).roundToInt()
    val snapped = AppSettings.MIN_TEXT_SCALE + steps * TEXT_SCALE_INCREMENT
    return snapped.coerceIn(AppSettings.MIN_TEXT_SCALE, AppSettings.MAX_TEXT_SCALE)
}
