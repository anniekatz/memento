package com.annie.memento.model

// global prefs
data class AppSettings(
    val audioAutoplay: Boolean = DEFAULT_AUDIO_AUTOPLAY,
    val mainTextScale: Float = DEFAULT_TEXT_SCALE,
    val examplesScale: Float = DEFAULT_TEXT_SCALE,
    val notesScale: Float = DEFAULT_TEXT_SCALE,
) {
    companion object {
        const val DEFAULT_AUDIO_AUTOPLAY = false
        const val DEFAULT_TEXT_SCALE = 1f
        const val MIN_TEXT_SCALE = 0.8f
        const val MAX_TEXT_SCALE = 2.0f
    }
}
