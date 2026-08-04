package com.annie.memento.di

import androidx.compose.runtime.compositionLocalOf
import com.annie.memento.model.AppSettings

// current global settings
val LocalAppSettings = compositionLocalOf { AppSettings() }
