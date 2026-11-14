package com.marsguide.vzd.util

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisualQuality(
    val label: String,
    val description: String,
    val particleMultiplier: Float = 1.0f,
    val enableParallax: Boolean = true
) {
    HIGH("HIGH", "Full starfield, particle ambience & smooth multi-layer parallax", 1.0f, true),
    MEDIUM("MEDIUM", "Balanced particles, subtle parallax, optimized for mid-range", 0.5f, true),
    LOW("LOW", "Minimal particles, static background, minimal animations", 0.1f, false)
}

class VisualQualitySettings(context: Context) {
    private val prefs = context.getSharedPreferences("MarsGuideSettings", Context.MODE_PRIVATE)

    private val _quality = MutableStateFlow(
        VisualQuality.valueOf(prefs.getString("visual_quality", VisualQuality.HIGH.name) ?: VisualQuality.HIGH.name)
    )
    val quality: StateFlow<VisualQuality> = _quality.asStateFlow()

    private val _reducedMotion = MutableStateFlow(prefs.getBoolean("reduced_motion", false))
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_effects_enabled", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _ambienceEnabled = MutableStateFlow(prefs.getBoolean("audio_ambience_enabled", true))
    val ambienceEnabled: StateFlow<Boolean> = _ambienceEnabled.asStateFlow()

    private val _radioEnabled = MutableStateFlow(prefs.getBoolean("audio_radio_enabled", true))
    val radioEnabled: StateFlow<Boolean> = _radioEnabled.asStateFlow()

    private val _uiEnabled = MutableStateFlow(prefs.getBoolean("audio_ui_enabled", true))
    val uiEnabled: StateFlow<Boolean> = _uiEnabled.asStateFlow()

    private val _alertsEnabled = MutableStateFlow(prefs.getBoolean("audio_alerts_enabled", true))
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    fun setQuality(newQuality: VisualQuality) {
        _quality.value = newQuality
        prefs.edit().putString("visual_quality", newQuality.name).apply()
    }

    fun setReducedMotion(enabled: Boolean) {
        _reducedMotion.value = enabled
        prefs.edit().putBoolean("reduced_motion", enabled).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean("sound_effects_enabled", enabled).apply()
        SoundManager.setEnabled(enabled)
    }

    fun setAmbienceEnabled(enabled: Boolean) {
        _ambienceEnabled.value = enabled
        prefs.edit().putBoolean("audio_ambience_enabled", enabled).apply()
        SoundManager.setAmbienceEnabled(enabled)
    }

    fun setRadioEnabled(enabled: Boolean) {
        _radioEnabled.value = enabled
        prefs.edit().putBoolean("audio_radio_enabled", enabled).apply()
        SoundManager.setRadioEnabled(enabled)
    }

    fun setUiEnabled(enabled: Boolean) {
        _uiEnabled.value = enabled
        prefs.edit().putBoolean("audio_ui_enabled", enabled).apply()
        SoundManager.setUiEnabled(enabled)
    }

    fun setAlertsEnabled(enabled: Boolean) {
        _alertsEnabled.value = enabled
        prefs.edit().putBoolean("audio_alerts_enabled", enabled).apply()
        SoundManager.setAlertsEnabled(enabled)
    }
}
