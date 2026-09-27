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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.model.ReviewGrade
import com.annie.memento.model.ReviewMode
import com.annie.memento.model.isDueOn
import com.annie.memento.model.isMastered
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
import com.annie.memento.ui.review.SideOption
import com.annie.memento.ui.review.StartSidePicker
import com.annie.memento.ui.theme.MementoGreen
import com.annie.memento.ui.theme.MementoHazard
import com.annie.memento.ui.theme.MementoAmberBright
import com.annie.memento.ui.theme.MementoRed
import kotlin.random.Random
import kotlinx.coroutines.launch

private data class ShownCard(val key: Int, val cardId: Long?)

private fun wrongRequeuePosition(remaining: Int): Int =
    if (remaining < 20) remaining else Random.nextInt(remaining - remaining / 4, remaining + 1)

// review session for SRS decks: current review queue and mastered

// CURRENTLY DUE (default): the cards due today
// red == mastery 0, shuffled back into the last 1/4 of the deck or last card if <20 left
// orange == mastery -2
// yellow == mastery same
// green == mastery +1 ; mastery == days until next review

// MASTERED review:
// red == mastery 1, orange == 3, yellow == 7, green == stays mastered

@Composable
fun SrsReviewScreen(navigator: Navigator, deckId: Long) {
    val repo = LocalAppGraph.current.repository
    val scope = rememberCoroutineScope()
    val details by repo.observeDeckDetails(deckId).collectAsState(initial = null)
    val cards by repo.observeCards(deckId).collectAsState(initial = emptyList())

    var started by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(ReviewMode.Due) }
    var startWithA by remember { mutableStateOf(true) }
    var queue by remember { mutableStateOf<List<Long>>(emptyList()) }
    var doneCount by remember { mutableStateOf(0) }
    var demotedCount by remember { mutableStateOf(0) } 
    var shownKey by remember { mutableStateOf(0) }
    val flipStates = remember { mutableStateMapOf<Int, Boolean>() }
    var editingCardId by remember { mutableStateOf<Long?>(null) }

    val current = details
    val byId = remember(cards) { cards.associateBy { it.id } }

    LaunchedEffect(cards) {
        if (started && queue.any { it !in byId }) {
            queue = queue.filter { it in byId }
        }
    }

    PlatformBackHandler(enabled = editingCardId != null) { editingCardId = null }
    val editing = editingCardId
    if (editing != null) {
        CardEditorScreen(deckId = deckId, cardId = editing, onClose = { editingCardId = null })
        return
    }

    val sessionDone = started && queue.isEmpty()

    MementoScaffold(
        title = current?.deck?.name ?: "Review",
        overline = when {
            !started -> "REVIEW · SETUP"
            sessionDone -> "REVIEW · COMPLETE"
            mode == ReviewMode.Mastered -> "REVIEW · MASTERED"
            else -> "REVIEW · ACTIVE"
        },
        onBack = { if (started) started = false else navigator.pop() },
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@MementoScaffold
        }

        if (!started) {
            val today = todayEpochDay()
            val pool = when (mode) {
                ReviewMode.Due -> cards.filter { it.isDueOn(today) }
                ReviewMode.Mastered -> cards.filter { it.isMastered }
            }
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MementoPanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (mode) {
                                ReviewMode.Due -> {
                                    SectionHeader("Due today", trailing = "${pool.size}")
                                }
                                ReviewMode.Mastered -> {
                                    SectionHeader("Mastered", trailing = "${pool.size}")
                                }
                            }
                            GradeLegendRow(MementoRed, "Red: wrong")
                            GradeLegendRow(MementoAmberBright, "Orange: hard")
                            GradeLegendRow(MementoHazard, "Yellow: good")
                            GradeLegendRow(MementoGreen, "Green: easy")
                        }
                    }
                    ReviewModePicker(mode = mode, onChange = { mode = it })
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
                                    pool.isEmpty() && mode == ReviewMode.Due -> "Nothing due today"
                                    pool.isEmpty() -> "Nothing mastered yet"
                                    mode == ReviewMode.Due -> "Start · ${pool.size} due"
                                    else -> "Start · ${pool.size} mastered"
                                },
                                onClick = {
                                    queue = pool.shuffled().map { it.id }
                                    doneCount = 0
                                    demotedCount = 0
                                    shownKey = 0
                                    flipStates.clear()
                                    started = true
                                },
                                enabled = pool.isNotEmpty(),
                                leading = "▶",
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                            )
                        }
                    }
                }
            }
        } else if (sessionDone) {
            val cardsWord = if (doneCount == 1) "card" else "cards"
            SessionCompletePanel(
                title = "Review complete",
                message = when (mode) {
                    ReviewMode.Due -> "$doneCount $cardsWord reviewed. See you tomorrow."
                    ReviewMode.Mastered -> "$doneCount mastered $cardsWord checked. " + when (demotedCount) {
                        0 -> "Great job!"
                        1 -> "1 card is back in the review rotation."
                        else -> "$demotedCount cards are back in the review rotation."
                    }
                },
                onDone = { navigator.pop() },
                modifier = Modifier.padding(padding),
            )
        } else {
            val card = queue.firstOrNull()?.let { byId[it] }
            val flipped = flipStates[shownKey] == true

            fun grade(grade: ReviewGrade) {
                val graded = card ?: return
                val today = todayEpochDay()
                when (mode) {
                    ReviewMode.Due -> {
                        scope.launch { repo.gradeCard(graded.id, graded.mastery, grade, today) }
                        if (grade == ReviewGrade.Red) {
                            val rest = queue.drop(1).toMutableList()
                            rest.add(wrongRequeuePosition(rest.size), graded.id)
                            queue = rest
                        } else {
                            queue = queue.drop(1)
                            doneCount++
                        }
                    }
                    ReviewMode.Mastered -> {
                        scope.launch { repo.gradeMasteredCard(graded.id, grade, today) }
                        if (grade != ReviewGrade.Green) demotedCount++
                        queue = queue.drop(1)
                        doneCount++
                    }
                }
                shownKey++
            }

            CardAudioEffects(card = card, showBack = flipped, startWithA = startWithA, changeKey = shownKey)

            val frontName = if (startWithA) current.deck.frontName else current.deck.backName
            val backName = if (startWithA) current.deck.backName else current.deck.frontName

            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReviewQueueReadout(done = doneCount, remaining = queue.size)

                AnimatedContent(
                    targetState = ShownCard(shownKey, queue.firstOrNull()),
                    transitionSpec = {
                        (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
                    },
                    label = "srsReviewNav",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { shown ->
                    val shownCard = shown.cardId?.let { byId[it] }
                    if (shownCard == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        FlipCard(
                            flipped = flipStates[shown.key] == true,
                            frontSide = if (startWithA) shownCard.front else shownCard.back,
                            backSide = if (startWithA) shownCard.back else shownCard.front,
                            frontName = frontName,
                            backName = backName,
                            onToggle = { if (shown.key == shownKey) flipStates[shownKey] = flipStates[shownKey] != true },
                            modifier = Modifier.fillMaxSize(),
                            frontTextOverride = if (startWithA) current.deck.frontTextOverride else current.deck.backTextOverride,
                            backTextOverride = if (startWithA) current.deck.backTextOverride else current.deck.frontTextOverride,
                        )
                    }
                }

                if (!flipped) {
                    MementoButton(
                        text = "Flip",
                        onClick = { flipStates[shownKey] = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                } else {
                    val gradePadding = PaddingValues(horizontal = 6.dp, vertical = 14.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MementoButton(
                            text = "Wrong",
                            onClick = { grade(ReviewGrade.Red) },
                            container = MementoRed,
                            onContainer = Color(0xFF230200),
                            contentPadding = gradePadding,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        MementoButton(
                            text = "Hard",
                            onClick = { grade(ReviewGrade.Orange) },
                            container = MementoAmberBright,
                            onContainer = Color(0xFF241000),
                            contentPadding = gradePadding,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        MementoButton(
                            text = "Good",
                            onClick = { grade(ReviewGrade.Yellow) },
                            container = MementoHazard,
                            onContainer = Color(0xFF201400),
                            contentPadding = gradePadding,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        MementoButton(
                            text = "Easy",
                            onClick = { grade(ReviewGrade.Green) },
                            container = MementoGreen,
                            onContainer = Color(0xFF04140A),
                            contentPadding = gradePadding,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                    }
                }

                MementoOutlineButton(
                    text = "✎ Edit card",
                    onClick = { card?.let { editingCardId = it.id } },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GradeLegendRow(color: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(12.dp).width(4.dp).background(color))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewModePicker(mode: ReviewMode, onChange: (ReviewMode) -> Unit) {
    MementoPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Review mode")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SideOption("Due today", mode == ReviewMode.Due, { onChange(ReviewMode.Due) }, Modifier.weight(1f))
                SideOption("Mastered", mode == ReviewMode.Mastered, { onChange(ReviewMode.Mastered) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReviewQueueReadout(done: Int, remaining: Int) {
    val total = done + remaining
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "REVIEWED ${done.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "LEFT ${remaining.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
    }
}
