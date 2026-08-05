package com.annie.memento.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.annie.memento.db.MementoDatabase
import com.annie.memento.model.AppSettings
import com.annie.memento.model.CardFont
import com.annie.memento.model.CardFontWeight
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// reads/writes global app settings
class SettingsRepository(
    db: MementoDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val q = db.appSettingQueries

    // current settings, re-emits on change
    fun observe(): Flow<AppSettings> =
        q.selectAll { key, value -> key to value }
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.toMap().toAppSettings() }

    suspend fun setAudioAutoplay(enabled: Boolean) = put(KEY_AUDIO_AUTOPLAY, enabled.toString())

    suspend fun setMainTextScale(scale: Float) = put(KEY_MAIN_TEXT_SCALE, scale.toString())

    suspend fun setExamplesScale(scale: Float) = put(KEY_EXAMPLES_SCALE, scale.toString())

    suspend fun setNotesScale(scale: Float) = put(KEY_NOTES_SCALE, scale.toString())

    suspend fun setCardFont(font: CardFont) = put(KEY_CARD_FONT, font.name)

    suspend fun setCardFontWeight(weight: CardFontWeight) = put(KEY_CARD_FONT_WEIGHT, weight.name)

    private suspend fun put(key: String, value: String) = withContext(dispatcher) {
        q.upsert(key, value)
    }
}

private const val KEY_AUDIO_AUTOPLAY = "audio_autoplay"
private const val KEY_MAIN_TEXT_SCALE = "main_text_scale"
private const val KEY_EXAMPLES_SCALE = "examples_scale"
private const val KEY_NOTES_SCALE = "notes_scale"
private const val KEY_CARD_FONT = "card_font"
private const val KEY_CARD_FONT_WEIGHT = "card_font_weight"

//default fallback
private fun Map<String, String>.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        audioAutoplay = this[KEY_AUDIO_AUTOPLAY]?.toBooleanStrictOrNull() ?: defaults.audioAutoplay,
        mainTextScale = this[KEY_MAIN_TEXT_SCALE]?.toFloatOrNull() ?: defaults.mainTextScale,
        examplesScale = this[KEY_EXAMPLES_SCALE]?.toFloatOrNull() ?: defaults.examplesScale,
        notesScale = this[KEY_NOTES_SCALE]?.toFloatOrNull() ?: defaults.notesScale,
        cardFont = this[KEY_CARD_FONT]?.let { v -> CardFont.entries.firstOrNull { it.name == v } } ?: defaults.cardFont,
        cardFontWeight = this[KEY_CARD_FONT_WEIGHT]?.let { v -> CardFontWeight.entries.firstOrNull { it.name == v } }
            ?: defaults.cardFontWeight,
    )
}
