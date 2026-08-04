package com.annie.memento

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.annie.memento.data.SampleData
import com.annie.memento.di.AppGraph
import com.annie.memento.di.LocalAppGraph
import com.annie.memento.di.LocalAppSettings
import com.annie.memento.model.AppSettings
import com.annie.memento.ui.bulkedit.BulkCardEditorScreen
import com.annie.memento.ui.cardeditor.CardEditorScreen
import com.annie.memento.ui.deckdetail.DeckDetailScreen
import com.annie.memento.ui.deckeditor.DeckEditorScreen
import com.annie.memento.ui.decklist.DeckListScreen
import com.annie.memento.ui.importdeck.ImportDeckScreen
import com.annie.memento.ui.navigation.PlatformBackHandler
import com.annie.memento.ui.navigation.Screen
import com.annie.memento.ui.navigation.rememberNavigator
import com.annie.memento.ui.review.ReviewScreen
import com.annie.memento.ui.settings.SettingsScreen
import com.annie.memento.ui.srs.SrsLearnScreen
import com.annie.memento.ui.srs.SrsReviewScreen
import com.annie.memento.ui.theme.MementoTheme

@Composable
fun App(graph: AppGraph) {
    CompositionLocalProvider(LocalAppGraph provides graph) {
        LaunchedEffect(Unit) { SampleData.seedIfEmpty(graph.repository) }
        // global settings
        val settings by graph.settingsRepository.observe().collectAsState(initial = AppSettings())
        CompositionLocalProvider(LocalAppSettings provides settings) {
            MementoTheme {
                val navigator = rememberNavigator()

                // back button
                PlatformBackHandler(enabled = navigator.canGoBack) { navigator.pop() }

                when (val screen = navigator.current) {
                    is Screen.DeckList -> DeckListScreen(navigator)
                    is Screen.ImportDeck -> ImportDeckScreen(navigator)
                    is Screen.Settings -> SettingsScreen(onBack = { navigator.pop() })
                    is Screen.DeckEditor -> DeckEditorScreen(navigator, screen.deckId)
                    is Screen.DeckDetail -> DeckDetailScreen(navigator, screen.deckId)
                    is Screen.CardEditor -> CardEditorScreen(screen.deckId, screen.cardId, onClose = { navigator.pop() })
                    is Screen.BulkCardEditor -> BulkCardEditorScreen(screen.deckId, screen.cardIds, onClose = { navigator.pop() })
                    is Screen.Review -> ReviewScreen(navigator, screen.deckId)
                    is Screen.SrsLearn -> SrsLearnScreen(navigator, screen.deckId)
                    is Screen.SrsReview -> SrsReviewScreen(navigator, screen.deckId)
                }
            }
        }
    }
}
