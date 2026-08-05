@file:OptIn(ExperimentalLayoutApi::class)

package com.annie.memento.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.di.LocalAppSettings
import com.annie.memento.model.Card
import com.annie.memento.model.CardFont
import com.annie.memento.model.CardSide
import com.annie.memento.model.CardTextOverride
import com.annie.memento.model.cardTextFor
import com.annie.memento.platform.ioDispatcher
import com.annie.memento.ui.components.AudioPlayButton
import com.annie.memento.ui.components.MementoPanel
import com.annie.memento.ui.components.MementoScanlines
import com.annie.memento.ui.components.SectionHeader
import com.annie.memento.ui.components.StoredImageAutoHeight
import com.annie.memento.ui.components.cornerBrackets
import com.annie.memento.ui.richtext.RichText
import com.annie.memento.ui.theme.CardPanelShape
import com.annie.memento.ui.theme.InsetShape
import com.annie.memento.ui.theme.scaledBy
import com.annie.memento.ui.theme.withCardFont
import kotlinx.coroutines.withContext

@Composable
fun CardAudioEffects(card: Card?, showBack: Boolean, startWithA: Boolean, changeKey: Any) {
    val graph = LocalAppGraph.current
    val audioPlayer = graph.audioPlayer
    DisposableEffect(changeKey) {
        onDispose { audioPlayer.stop() }
    }

    val autoplay = LocalAppSettings.current.audioAutoplay
    LaunchedEffect(changeKey, showBack, autoplay) {
        if (!autoplay) return@LaunchedEffect
        val current = card ?: return@LaunchedEffect
        val visibleSide = if (showBack) {
            if (startWithA) current.back else current.front
        } else {
            if (startWithA) current.front else current.back
        }
        val firstAudio = visibleSide.audioPaths.firstOrNull() ?: return@LaunchedEffect
        withContext(ioDispatcher) { audioPlayer.play(graph.mediaStorage.absolutePath(firstAudio)) }
    }
}

@Composable
fun ProgressReadout(index: Int, total: Int) {
    val fraction = (index + 1).toFloat() / total
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "CARD ${(index + 1).toString().padStart(2, '0')} / ${total.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun StartSidePicker(
    frontName: String,
    backName: String,
    startWithA: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MementoPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Start side")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SideOption(frontName, startWithA, { onChange(true) }, Modifier.weight(1f))
                SideOption(backName, !startWithA, { onChange(false) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SideOption(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(InsetShape).clickable(onClick = onClick),
        shape = InsetShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
            Text(
                name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun FlipCard(
    flipped: Boolean,
    frontSide: CardSide,
    backSide: CardSide,
    frontName: String,
    backName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    frontTextOverride: CardTextOverride? = null,
    backTextOverride: CardTextOverride? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "cardFlip",
    )

    val flash = remember { Animatable(0f) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(flipped) {
        if (armed) {
            flash.snapTo(FLASH_ALPHA)
            flash.animateTo(0f, animationSpec = tween(durationMillis = 450))
        }
        armed = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(CardPanelShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            },
    ) {
        if (rotation <= 90f) {
            CardFace(sideName = frontName, side = frontSide, textOverride = frontTextOverride)
        } else {
            //rotate back, so content not mirrored
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                CardFace(sideName = backName, side = backSide, textOverride = backTextOverride)
            }
        }
        if (flash.value > 0f) {
            Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = flash.value)))
        }
    }
}

private const val FLASH_ALPHA = 0.20f

@Composable
private fun CardFace(sideName: String, side: CardSide, textOverride: CardTextOverride?) {
    val text = LocalAppSettings.current.cardTextFor(textOverride)
    Surface(
        modifier = Modifier.fillMaxSize().cornerBrackets(MaterialTheme.colorScheme.primary, topStart = false, bottomEnd = false),
        shape = CardPanelShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Box(Modifier.fillMaxSize().MementoScanlines()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader(sideName, accent = MaterialTheme.colorScheme.primary)
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    RichText(
                        markup = side.text,
                        rich = side.isRichText,
                        style = MaterialTheme.typography.headlineMedium.scaledBy(text.mainTextScale).withCardFont(text.font, text.fontWeight),
                        textAlign = TextAlign.Center,
                    )
                    if (side.examples.isNotEmpty()) {
                        ExpandableTextSection(
                            title = "Examples",
                            text = side.examples.joinToString("\n") { "• $it" },
                            textColor = MaterialTheme.colorScheme.onSurface,
                            rich = side.isRichText,
                            textScale = text.examplesScale,
                            font = text.font,
                        )
                    }
                    side.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        ExpandableTextSection(
                            title = "Notes",
                            text = notes,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            rich = side.isRichText,
                            textScale = text.notesScale,
                            font = text.font,
                        )
                    }
                    if (side.audioPaths.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            side.audioPaths.forEach { audioPath -> AudioPlayButton(audioPath) }
                        }
                    }
                    side.imagePaths.forEach { imagePath ->
                        StoredImageAutoHeight(imagePath, Modifier.fillMaxWidth(0.95f), shape = InsetShape)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableTextSection(
    title: String,
    text: String,
    textColor: Color,
    collapsedMaxLines: Int = 3,
    rich: Boolean = false,
    textScale: Float = 1f,
    font: CardFont = CardFont.SYSTEM,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(Modifier.fillMaxWidth()) {
            RichText(
                markup = text,
                rich = rich,
                style = MaterialTheme.typography.bodyMedium.scaledBy(textScale).withCardFont(font, null),
                color = textColor,
                maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout -> if (!expanded) overflows = layout.hasVisualOverflow },
                modifier = Modifier.fillMaxWidth().padding(end = if (overflows) 24.dp else 0.dp),
            )
            if (overflows) {
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(InsetShape)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}
