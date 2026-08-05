package com.annie.memento.model

import kotlinx.serialization.Serializable

// global prefs
data class AppSettings(
    val audioAutoplay: Boolean = DEFAULT_AUDIO_AUTOPLAY,
    val mainTextScale: Float = DEFAULT_TEXT_SCALE,
    val examplesScale: Float = DEFAULT_TEXT_SCALE,
    val notesScale: Float = DEFAULT_TEXT_SCALE,
    val cardFont: CardFont = DEFAULT_CARD_FONT,
    val cardFontWeight: CardFontWeight = DEFAULT_CARD_FONT_WEIGHT,
) {
    companion object {
        const val DEFAULT_AUDIO_AUTOPLAY = false
        const val DEFAULT_TEXT_SCALE = 1f
        const val MIN_TEXT_SCALE = 0.8f
        const val MAX_TEXT_SCALE = 2.0f
        val DEFAULT_CARD_FONT = CardFont.SYSTEM
        val DEFAULT_CARD_FONT_WEIGHT = CardFontWeight.BOLD
    }
}

enum class CardFont { SYSTEM, NOTO_SANS, NOTO_SERIF, ZEN_MARU }
enum class CardFontWeight { LIGHT, REGULAR, MEDIUM, BOLD }

// font choice overrides per deck
@Serializable
data class CardTextOverride(
    val mainTextScale: Float? = null,
    val examplesScale: Float? = null,
    val notesScale: Float? = null,
    val font: CardFont? = null,
    val fontWeight: CardFontWeight? = null,
) {
    fun isEmpty(): Boolean =
        mainTextScale == null && examplesScale == null && notesScale == null && font == null && fontWeight == null
}

data class CardTextSettings(
    val mainTextScale: Float,
    val examplesScale: Float,
    val notesScale: Float,
    val font: CardFont,
    val fontWeight: CardFontWeight,
)

fun AppSettings.cardTextFor(override: CardTextOverride? = null): CardTextSettings = CardTextSettings(
    mainTextScale = override?.mainTextScale ?: mainTextScale,
    examplesScale = override?.examplesScale ?: examplesScale,
    notesScale = override?.notesScale ?: notesScale,
    font = override?.font ?: cardFont,
    fontWeight = override?.fontWeight ?: cardFontWeight,
)

fun CardTextSettings.asOverride(): CardTextOverride =
    CardTextOverride(mainTextScale, examplesScale, notesScale, font, fontWeight)
