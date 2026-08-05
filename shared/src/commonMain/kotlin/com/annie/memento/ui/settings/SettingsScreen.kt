package com.annie.memento.ui.settings

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
import androidx.compose.ui.unit.dp
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.di.LocalAppSettings
import com.annie.memento.model.AppSettings
import com.annie.memento.model.CardFont
import com.annie.memento.model.CardFontWeight
import com.annie.memento.model.CardTextSettings
import com.annie.memento.model.cardTextFor
import com.annie.memento.ui.components.CardTextControls
import com.annie.memento.ui.components.MementoOutlineButton
import com.annie.memento.ui.components.MementoPanel
import com.annie.memento.ui.components.MementoScaffold
import com.annie.memento.ui.components.SectionHeader
import kotlinx.coroutines.launch

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
            CardTextSetting(
                current = settings.cardTextFor(),
                onMainChange = { scope.launch { repo.setMainTextScale(it) } },
                onExamplesChange = { scope.launch { repo.setExamplesScale(it) } },
                onNotesChange = { scope.launch { repo.setNotesScale(it) } },
                onFontChange = { scope.launch { repo.setCardFont(it) } },
                onWeightChange = { scope.launch { repo.setCardFontWeight(it) } },
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
private fun CardTextSetting(
    current: CardTextSettings,
    onMainChange: (Float) -> Unit,
    onExamplesChange: (Float) -> Unit,
    onNotesChange: (Float) -> Unit,
    onFontChange: (CardFont) -> Unit,
    onWeightChange: (CardFontWeight) -> Unit,
) {
    var text by remember { mutableStateOf(current) }

    fun commit(new: CardTextSettings) {
        val old = text
        text = new
        if (new.mainTextScale != old.mainTextScale) onMainChange(new.mainTextScale)
        if (new.examplesScale != old.examplesScale) onExamplesChange(new.examplesScale)
        if (new.notesScale != old.notesScale) onNotesChange(new.notesScale)
        if (new.font != old.font) onFontChange(new.font)
        if (new.fontWeight != old.fontWeight) onWeightChange(new.fontWeight)
    }

    MementoPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Card text")
            Text(
                "Font, boldness, and size for card text. Applies everywhere unless overridden in a deck's settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CardTextControls(value = text, onChange = { commit(it) })
            if (text != DEFAULT_CARD_TEXT) {
                MementoOutlineButton(
                    text = "Reset to default",
                    onClick = { commit(DEFAULT_CARD_TEXT) },
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val DEFAULT_CARD_TEXT = AppSettings().cardTextFor()
