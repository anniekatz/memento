package com.annie.memento.ui.srs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.model.MAX_MASTERY
import com.annie.memento.model.srsDeckStatus
import com.annie.memento.platform.todayEpochDay
import com.annie.memento.ui.cardeditor.CardEditorScreen
import com.annie.memento.ui.components.MementoButton
import com.annie.memento.ui.components.MementoOutlineButton
import com.annie.memento.ui.components.MementoPanel
import com.annie.memento.ui.components.MementoScaffold
import com.annie.memento.ui.components.SectionHeader
import com.annie.memento.ui.navigation.Navigator
import com.annie.memento.ui.navigation.PlatformBackHandler
import com.annie.memento.ui.review.CardAudioEffects
import com.annie.memento.ui.review.FlipCard
import com.annie.memento.ui.review.ProgressReadout
import com.annie.memento.ui.review.StartSidePicker
import com.annie.memento.ui.theme.MementoGreen
import kotlinx.coroutines.launch

private const val LEARN_MORE_BATCH = 5

@Composable
fun SrsLearnScreen(navigator: Navigator, deckId: Long) {
    val repo = LocalAppGraph.current.repository
    val scope = rememberCoroutineScope()
    val details by repo.observeDeckDetails(deckId).collectAsState(initial = null)
    val cards by repo.observeCards(deckId).collectAsState(initial = emptyList())

    var started by remember { mutableStateOf(false) }
    var practicing by remember { mutableStateOf(false) }
    var startWithA by remember { mutableStateOf(true) }
    var sessionIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var index by remember { mutableStateOf(0) }
    val flipStates = remember { mutableStateMapOf<Long, Boolean>() }
    val seenMarked = remember { mutableSetOf<Long>() }
    var editingCardId by remember { mutableStateOf<Long?>(null) }

    val current = details
    val byId = remember(cards) { cards.associateBy { it.id } }

    LaunchedEffect(cards) {
        if (started && sessionIds.any { it !in byId }) {
            val removedBefore = sessionIds.take(index).count { it !in byId }
            sessionIds = sessionIds.filter { it in byId }
            index = (index - removedBefore).coerceIn(0, sessionIds.size)
        }
    }

    PlatformBackHandler(enabled = editingCardId != null) { editingCardId = null }
    val editing = editingCardId
    if (editing != null) {
        CardEditorScreen(deckId = deckId, cardId = editing, onClose = { editingCardId = null })
        return
    }

    val sessionDone = started && index >= sessionIds.size
    val preStatus = current?.let { srsDeckStatus(cards, it.deck.newCardsPerDay, todayEpochDay()) }
    val dayComplete = !started && preStatus != null && preStatus.learnableToday == 0 && preStatus.learnedToday > 0

    LaunchedEffect(sessionDone, practicing) {
        if (sessionDone && practicing) {
            practicing = false
            started = false
        }
    }

    MementoScaffold(
        title = current?.deck?.name ?: "Learn",
        overline = when {
            dayComplete || (sessionDone && !practicing) -> "LEARN - COMPLETE"
            !started -> "LEARN - SETUP"
            practicing -> "REVIEW - TODAY'S CARDS"
            else -> "LEARN - ACTIVE"
        },
        onBack = {
            if (started) {
                started = false
                practicing = false
            } else {
                navigator.pop()
            }
        },
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@MementoScaffold
        }

        if (!started) {
            val status = preStatus ?: return@MementoScaffold
            if (dayComplete) {
                val today = todayEpochDay()
                val reviewableToday = cards.count { it.seenDay == today && it.mastery < MAX_MASTERY }
                DayCompletePanel(
                    message = if (status.unseen > 0) {
                        "Daily limit reached - ${status.learnedToday} ${if (status.learnedToday == 1) "card" else "cards"} learned today. " +
                            "${status.unseen} unseen ${if (status.unseen == 1) "card" else "cards"} left in the deck."
                    } else {
                        "${status.learnedToday} ${if (status.learnedToday == 1) "card" else "cards"} learned today. " +
                            "Every card in the deck has been seen."
                    },
                    reviewableToday = reviewableToday,
                    nextBatch = minOf(LEARN_MORE_BATCH, status.unseen),
                    onReview = {
                        sessionIds = cards.filter { it.seenDay == today && it.mastery < MAX_MASTERY }.map { it.id }.shuffled()
                        flipStates.clear()
                        index = 0
                        practicing = true
                        started = true
                    },
                    onLearnMore = {
                        sessionIds = cards.filter { !it.seen }.map { it.id }.take(LEARN_MORE_BATCH)
                        seenMarked.clear()
                        flipStates.clear()
                        index = 0
                        practicing = false
                        started = true
                    },
                    onDone = { navigator.pop() },
                    modifier = Modifier.padding(padding),
                )
                return@MementoScaffold
            }
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MementoPanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader("New cards today")
                            StatRow("Unseen cards", "${status.unseen}")
                            StatRow("Learned today", "${status.learnedToday} / ${current.deck.newCardsPerDay}")
                            StatRow("Up next", "${status.learnableToday}")
                        }
                    }
                    StartSidePicker(
                        frontName = current.deck.frontName,
                        backName = current.deck.backName,
                        startWithA = startWithA,
                        onChange = { startWithA = it },
                    )
                }
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                            MementoButton(
                                text = when {
                                    status.learnableToday > 0 ->
                                        "Start - ${status.learnableToday} ${if (status.learnableToday == 1) "card" else "cards"}"
                                    status.unseen == 0 -> "Every card is seen"
                                    else -> "Daily limit reached"
                                },
                                onClick = {
                                    sessionIds = cards.filter { !it.seen }.map { it.id }.take(status.learnableToday)
                                    seenMarked.clear()
                                    flipStates.clear()
                                    index = 0
                                    started = true
                                },
                                enabled = status.learnableToday > 0,
                                leading = "▶",
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                            )
                        }
                    }
                }
            }
        } else if (sessionDone) {
            if (practicing) {
                Box(Modifier.fillMaxSize().padding(padding))
                return@MementoScaffold
            }
            val today = todayEpochDay()
            val reviewableToday = cards.count { it.seenDay == today && it.mastery < MAX_MASTERY }
            val sessionIdSet = sessionIds.toSet()
            val remainingUnseen = cards.count { !it.seen && it.id !in sessionIdSet }

            DayCompletePanel(
                message = "${sessionIds.size} new ${if (sessionIds.size == 1) "card" else "cards"} learned this session. " +
                    if (remainingUnseen > 0) {
                        "$remainingUnseen unseen ${if (remainingUnseen == 1) "card" else "cards"} left in the deck."
                    } else {
                        "Every card in the deck has been seen."
                    },
                reviewableToday = reviewableToday,
                nextBatch = minOf(LEARN_MORE_BATCH, remainingUnseen),
                onReview = {
                    sessionIds = cards.filter { it.seenDay == today && it.mastery < MAX_MASTERY }.map { it.id }.shuffled()
                    flipStates.clear()
                    index = 0
                    practicing = true
                },
                onLearnMore = {
                    val extra = cards.filter { !it.seen && it.id !in sessionIdSet }
                        .map { it.id }
                        .take(LEARN_MORE_BATCH)
                    if (extra.isNotEmpty()) sessionIds = sessionIds + extra
                },
                onDone = { navigator.pop() },
                modifier = Modifier.padding(padding),
            )
        } else {
            val card = sessionIds.getOrNull(index)?.let { byId[it] }
            val flipped = card != null && flipStates[card.id] == true

            fun toggleFlip() {
                val c = card ?: return
                val reveal = flipStates[c.id] != true
                flipStates[c.id] = reveal
                if (reveal && !practicing && seenMarked.add(c.id)) {
                    scope.launch { repo.markCardSeen(c.id, todayEpochDay()) }
                }
            }

            fun advance() {
                index++
            }

            CardAudioEffects(card = card, showBack = flipped, startWithA = startWithA, changeKey = index)

            val frontName = if (startWithA) current.deck.frontName else current.deck.backName
            val backName = if (startWithA) current.deck.backName else current.deck.frontName

            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressReadout(index = index, total = sessionIds.size)

                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
                    },
                    label = "learnNav",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { cardIndex ->
                    val shown = sessionIds.getOrNull(cardIndex)?.let { byId[it] }
                    if (shown == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        FlipCard(
                            flipped = flipStates[shown.id] == true,
                            frontSide = if (startWithA) shown.front else shown.back,
                            backSide = if (startWithA) shown.back else shown.front,
                            frontName = frontName,
                            backName = backName,
                            onToggle = { if (cardIndex == index) toggleFlip() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (!flipped) {
                    MementoButton(
                        text = "Flip",
                        onClick = { toggleFlip() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                } else {
                    MementoButton(
                        text = if (index == sessionIds.lastIndex) "Finish" else "Next ›",
                        onClick = {
                            if (practicing && index >= sessionIds.lastIndex) {
                                practicing = false
                                started = false
                            } else {
                                advance()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!practicing) {
                        MementoOutlineButton(
                            text = "✓ Already mastered",
                            onClick = {
                                card?.let { c ->
                                    seenMarked.add(c.id)
                                    scope.launch { repo.markCardMastered(c.id, todayEpochDay()) }
                                }
                                advance()
                            },
                            accent = MementoGreen,
                            modifier = Modifier.weight(1.2f),
                        )
                        MementoOutlineButton(
                            text = "✎ Edit",
                            onClick = { card?.let { editingCardId = it.id } },
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.8f),
                        )
                    } else {
                        MementoOutlineButton(
                            text = "✎ Edit card",
                            onClick = { card?.let { editingCardId = it.id } },
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCompletePanel(
    message: String,
    reviewableToday: Int,
    nextBatch: Int,
    onReview: () -> Unit,
    onLearnMore: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        MementoPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("✦", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("Learning complete".uppercase(), style = MaterialTheme.typography.titleMedium)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (reviewableToday > 0) {
                    Text(
                        "Same-day review is an ungraded flip-through - red/yellow/green starts tomorrow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MementoButton(
                    text = "Review today's cards - $reviewableToday",
                    onClick = onReview,
                    enabled = reviewableToday > 0,
                    leading = "▶",
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                )
                if (nextBatch > 0) {
                    MementoOutlineButton(
                        text = "Learn $nextBatch more",
                        onClick = onLearnMore,
                        leading = "+",
                        accent = MementoGreen,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MementoOutlineButton(
                    text = "Done",
                    onClick = onDone,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun SessionCompletePanel(
    title: String,
    message: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        MementoPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("✦", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(title.uppercase(), style = MaterialTheme.typography.titleMedium)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                MementoButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                )
            }
        }
    }
}
