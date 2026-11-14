package com.marsguide.vzd.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.marsguide.vzd.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Advanced Cinematic Mission Audio Engine for MARS//GUIDE.
 * Replaces simplistic tones with a multi-layered spacecraft soundscape.
 * Features: Room Tone (Ventilation/Structural), Electronic Noise, Radio Squelch/Chatter.
 * Authentic NASA Mission recordings and tactical UI sound effects.
 */
object SoundManager {
    private var soundPool: SoundPool? = null
    
    // SoundPool IDs
    private var uiButtonId: Int = -1
    private var uiTargetLockId: Int = -1
    private var uiRouteConfirmId: Int = -1
    private var uiDataSyncId: Int = -1
    private var uiWarningId: Int = -1
    private var radioSquelchId: Int = -1
    private var cameraApproachId: Int = -1
    private var locationArrivalId: Int = -1
    private var nasaImageOpenId: Int = -1
    private var panoramaEnterId: Int = -1
    private var globeRotateId: Int = -1
    private var uiBackId: Int = -1
    private var uiErrorId: Int = -1
    private var bootSequenceId: Int = -1
    
    // New NASA Authentic IDs
    private var laserShotId: Int = -1
    private var ingenuityRotorId: Int = -1
    private var marsquakeId: Int = -1
    private var quindarOnId: Int = -1
    private var quindarOffId: Int = -1
    private var drivingNoiseId: Int = -1
    private var neilArmstrongId: Int = -1

    private var ambiencePlayer: MediaPlayer? = null
    private var radioPlayer: MediaPlayer? = null
    private var panoramaPlayer: MediaPlayer? = null
    private var quotePlayer: MediaPlayer? = null
    
    private var isEnabled: Boolean = true
    private var isAmbienceEnabled: Boolean = true
    private var isRadioEnabled: Boolean = true
    private var isUiEnabled: Boolean = true
    private var isAlertsEnabled: Boolean = true

    private var isPaused: Boolean = false
    private val _isGlobalMuted = MutableStateFlow(true)
    val isGlobalMuted: StateFlow<Boolean> = _isGlobalMuted.asStateFlow()

    private var masterVolume = 1.0f
    private var ambienceVolume = 0.18f // Spacecraft interior background
    private var radioVolume = 0.30f
    private var uiVolume = 0.45f
    private var alertsVolume = 0.65f // High priority alerts
    private var panoramaVolume = 0.22f // Surface wind texture

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var radioJob: Job? = null
    
    private val _isCommLinkActive = MutableStateFlow(false)
    val isCommLinkActive: StateFlow<Boolean> = _isCommLinkActive.asStateFlow()

    private val radioVoices = listOf(
        R.raw.radio_transmission_01,
        R.raw.comm_authentic_01,
        R.raw.comm_authentic_02,
        R.raw.comm_authentic_03
    )

    private var appContext: Context? = null

    fun init(context: Context, enabled: Boolean = true) {
        appContext = context.applicationContext
        val ctx = appContext ?: return

        // Load persistent settings
        val prefs = ctx.getSharedPreferences("MarsGuideSettings", Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean("sound_effects_enabled", enabled)
        isAmbienceEnabled = prefs.getBoolean("audio_ambience_enabled", true)
        isRadioEnabled = prefs.getBoolean("audio_radio_enabled", true)
        isUiEnabled = prefs.getBoolean("audio_ui_enabled", true)
        isAlertsEnabled = prefs.getBoolean("audio_alerts_enabled", true)

        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(16)
                .setAudioAttributes(audioAttributes)
                .build()

            uiButtonId = soundPool?.load(ctx, R.raw.ui_button, 1) ?: -1
            uiTargetLockId = soundPool?.load(ctx, R.raw.ui_target_lock, 1) ?: -1
            uiRouteConfirmId = soundPool?.load(ctx, R.raw.ui_route_confirm, 1) ?: -1
            uiDataSyncId = soundPool?.load(ctx, R.raw.ui_data_sync, 1) ?: -1
            uiWarningId = soundPool?.load(ctx, R.raw.ui_warning, 1) ?: -1
            radioSquelchId = soundPool?.load(ctx, R.raw.radio_squelch, 1) ?: -1
            cameraApproachId = soundPool?.load(ctx, R.raw.camera_approach, 1) ?: -1
            locationArrivalId = soundPool?.load(ctx, R.raw.location_arrival, 1) ?: -1
            nasaImageOpenId = soundPool?.load(ctx, R.raw.nasa_image_open, 1) ?: -1
            panoramaEnterId = soundPool?.load(ctx, R.raw.panorama_enter, 1) ?: -1
            globeRotateId = soundPool?.load(ctx, R.raw.globe_rotate, 1) ?: -1
            uiBackId = soundPool?.load(ctx, R.raw.ui_back, 1) ?: -1
            uiErrorId = soundPool?.load(ctx, R.raw.ui_error, 1) ?: -1
            bootSequenceId = soundPool?.load(ctx, R.raw.boot_sequence, 1) ?: -1
            quindarOnId = soundPool?.load(ctx, R.raw.quindar_tone, 1) ?: -1

            // Reuse already loaded short effects rather than duplicating PCM buffers
            laserShotId = uiTargetLockId
            ingenuityRotorId = panoramaEnterId
            marsquakeId = uiWarningId
            quindarOffId = quindarOnId
            drivingNoiseId = uiDataSyncId
            neilArmstrongId = -1
        }

        if (isEnabled && !isPaused && !_isGlobalMuted.value) {
            startAmbience()
            startRadioScheduler()
        }
    }

    fun setGlobalMute(muted: Boolean) {
        _isGlobalMuted.value = muted
        if (_isGlobalMuted.value) {
            pauseAll()
        } else {
            if (isEnabled && !isPaused) {
                resumeAll()
            }
        }
    }

    fun onPause() {
        isPaused = true
        pauseAll()
    }

    fun onResume() {
        isPaused = false
        if (isEnabled && !_isGlobalMuted.value) {
            resumeAll()
        }
    }

    private fun pauseAll() {
        try {
            if (ambiencePlayer?.isPlaying == true) ambiencePlayer?.pause()
        } catch (_: Exception) {}
        try {
            if (radioPlayer?.isPlaying == true) radioPlayer?.pause()
        } catch (_: Exception) {}
        try {
            if (panoramaPlayer?.isPlaying == true) panoramaPlayer?.pause()
        } catch (_: Exception) {}
        try {
            if (quotePlayer?.isPlaying == true) quotePlayer?.pause()
        } catch (_: Exception) {}
        radioJob?.cancel()
        radioJob = null
    }

    private fun resumeAll() {
        if (_isGlobalMuted.value || !isEnabled || isPaused) return
        if (panoramaPlayer != null) {
            try {
                panoramaPlayer?.start()
            } catch (_: Exception) {}
        } else if (isAmbienceEnabled) {
            if (ambiencePlayer != null) {
                try {
                    ambiencePlayer?.start()
                } catch (_: Exception) {
                    stopAmbience()
                    startAmbience()
                }
            } else {
                startAmbience()
            }
        }
        if (radioPlayer != null) {
            try {
                radioPlayer?.start()
            } catch (_: Exception) {}
        }
        if (isRadioEnabled) startRadioScheduler()
    }

    fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        if (isEnabled && !_isGlobalMuted.value && !isPaused) {
            resumeAll()
        } else {
            stopAll()
        }
    }

    fun setAmbienceEnabled(enabled: Boolean) {
        isAmbienceEnabled = enabled
        if (isAmbienceEnabled && isEnabled && !isPaused && !_isGlobalMuted.value) startAmbience() else stopAmbience()
    }

    fun setRadioEnabled(enabled: Boolean) {
        isRadioEnabled = enabled
        if (isRadioEnabled && isEnabled && !isPaused && !_isGlobalMuted.value) startRadioScheduler() else stopRadio()
    }
    
    fun setUiEnabled(enabled: Boolean) {
        isUiEnabled = enabled
    }

    fun setAlertsEnabled(enabled: Boolean) {
        isAlertsEnabled = enabled
    }

    fun setMasterVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
        updatePlayerVolumes()
    }

    private fun updatePlayerVolumes() {
        val effectiveAmbience = if (_isGlobalMuted.value) 0f else masterVolume * ambienceVolume
        try { ambiencePlayer?.setVolume(effectiveAmbience, effectiveAmbience) } catch (_: Exception) {}
        val effectiveRadio = if (_isGlobalMuted.value) 0f else masterVolume * radioVolume
        try { radioPlayer?.setVolume(effectiveRadio, effectiveRadio) } catch (_: Exception) {}
        try { quotePlayer?.setVolume(effectiveRadio, effectiveRadio) } catch (_: Exception) {}
        val effectivePanorama = if (_isGlobalMuted.value) 0f else masterVolume * panoramaVolume
        try { panoramaPlayer?.setVolume(effectivePanorama, effectivePanorama) } catch (_: Exception) {}
    }

    fun playClick() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiButtonId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(uiButtonId, v, v, 1, 0, 1.0f)
    }

    fun playBack() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiBackId <= 0) return
        val v = (masterVolume * uiVolume * 0.8f).coerceIn(0f, 1f)
        soundPool?.play(uiBackId, v, v, 1, 0, 1.0f)
    }

    fun playError() {
        if (!isEnabled || !isAlertsEnabled || isPaused || _isGlobalMuted.value || uiErrorId <= 0) return
        val v = (masterVolume * alertsVolume).coerceIn(0f, 1f)
        soundPool?.play(uiErrorId, v, v, 1, 0, 1.0f)
    }

    fun playGlobeRotate() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || globeRotateId <= 0) return
        val v = (masterVolume * uiVolume * 0.4f).coerceIn(0f, 1f)
        soundPool?.play(globeRotateId, v, v, 1, 0, 1.0f)
    }

    fun playBootSequence() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || bootSequenceId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(bootSequenceId, v, v, 1, 0, 1.0f)
    }

    fun playNavigationClick() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiButtonId <= 0) return
        val v = (masterVolume * uiVolume * 0.7f).coerceIn(0f, 1f)
        soundPool?.play(uiButtonId, v, v, 1, 0, 1.2f)
    }

    fun playTargetScan() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiButtonId <= 0) return
        val v = (masterVolume * uiVolume * 0.5f).coerceIn(0f, 1f)
        soundPool?.play(uiButtonId, v, v, 1, 0, 1.8f)
    }

    fun playTargetLock() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiTargetLockId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(uiTargetLockId, v, v, 1, 0, 1.0f)
    }

    fun playCameraApproach() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || cameraApproachId <= 0) return
        val v = (masterVolume * uiVolume * 0.85f).coerceIn(0f, 1f)
        soundPool?.play(cameraApproachId, v, v, 1, 0, 1.0f)
    }

    fun playLocationArrival() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || locationArrivalId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(locationArrivalId, v, v, 1, 0, 1.0f)
    }

    fun playNasaImageOpen() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || nasaImageOpenId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(nasaImageOpenId, v, v, 1, 0, 1.0f)
    }

    fun playPanoramaEnter() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || panoramaEnterId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(panoramaEnterId, v, v, 1, 0, 1.0f)
    }

    fun playLaserShot() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || laserShotId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(laserShotId, v, v, 2, 0, 1.5f)
    }

    fun playRotorSound() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || ingenuityRotorId <= 0) return
        val v = (masterVolume * uiVolume * 0.6f).coerceIn(0f, 1f)
        soundPool?.play(ingenuityRotorId, v, v, 1, 0, 1.0f)
    }

    fun playMarsquake() {
        if (!isEnabled || !isAlertsEnabled || isPaused || _isGlobalMuted.value || marsquakeId <= 0) return
        val v = (masterVolume * alertsVolume).coerceIn(0f, 1f)
        soundPool?.play(marsquakeId, v, v, 1, 0, 0.5f)
    }

    fun playQuindarOn() {
        if (!isEnabled || !isRadioEnabled || isPaused || _isGlobalMuted.value || quindarOnId <= 0) return
        val v = (masterVolume * radioVolume).coerceIn(0f, 1f)
        soundPool?.play(quindarOnId, v, v, 1, 0, 1.0f)
    }

    fun playQuindarOff() {
        if (!isEnabled || !isRadioEnabled || isPaused || _isGlobalMuted.value || quindarOffId <= 0) return
        val v = (masterVolume * radioVolume * 0.8f).coerceIn(0f, 1f)
        soundPool?.play(quindarOffId, v, v, 1, 0, 0.8f)
    }

    fun playDrivingNoise() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || drivingNoiseId <= 0) return
        val v = (masterVolume * uiVolume * 0.5f).coerceIn(0f, 1f)
        soundPool?.play(drivingNoiseId, v, v, 1, 0, 0.7f)
    }

    fun playArmstrongQuote() {
        if (!isEnabled || !isRadioEnabled || isPaused || _isGlobalMuted.value) return
        val ctx = appContext ?: return
        stopQuotePlayer()
        try {
            val v = (masterVolume * radioVolume).coerceIn(0f, 1f)
            quotePlayer = MediaPlayer.create(ctx, R.raw.mission_voice)?.apply {
                setVolume(v, v)
                setOnCompletionListener { mp ->
                    try { mp.release() } catch (_: Exception) {}
                    if (quotePlayer === mp) quotePlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Quote playback failed", e)
            stopQuotePlayer()
        }
    }

    private fun stopQuotePlayer() {
        try {
            if (quotePlayer?.isPlaying == true) quotePlayer?.stop()
        } catch (_: Exception) {}
        try {
            quotePlayer?.release()
        } catch (_: Exception) {}
        quotePlayer = null
    }

    fun startPanoramaAmbience() {
        val ctx = appContext ?: return
        if (panoramaPlayer != null || !isEnabled || isPaused || _isGlobalMuted.value) return
        try {
            try {
                if (ambiencePlayer?.isPlaying == true) ambiencePlayer?.pause()
            } catch (_: Exception) {}
            val effVol = (masterVolume * panoramaVolume).coerceIn(0f, 1f)
            panoramaPlayer = MediaPlayer.create(ctx, R.raw.martian_ambient_wind)?.apply {
                isLooping = true
                setVolume(effVol, effVol)
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Panorama ambience failed", e)
        }
    }

    fun stopPanoramaAmbience(restartAmbience: Boolean = true) {
        try {
            if (panoramaPlayer?.isPlaying == true) panoramaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            panoramaPlayer?.release()
        } catch (_: Exception) {}
        panoramaPlayer = null
        if (restartAmbience && isAmbienceEnabled && isEnabled && !isPaused && !_isGlobalMuted.value) {
            if (ambiencePlayer != null) {
                try {
                    ambiencePlayer?.start()
                } catch (_: Exception) {
                    stopAmbience()
                    startAmbience()
                }
            } else {
                startAmbience()
            }
        }
    }

    fun playMissionConfirm() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiRouteConfirmId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(uiRouteConfirmId, v, v, 1, 0, 1.0f)
    }

    fun playDataSync() {
        if (!isEnabled || !isUiEnabled || isPaused || _isGlobalMuted.value || uiDataSyncId <= 0) return
        val v = (masterVolume * uiVolume).coerceIn(0f, 1f)
        soundPool?.play(uiDataSyncId, v, v, 1, 0, 1.0f)
    }

    fun playWarning() {
        if (!isEnabled || !isAlertsEnabled || isPaused || _isGlobalMuted.value || uiWarningId <= 0) return
        val v = (masterVolume * alertsVolume).coerceIn(0f, 1f)
        soundPool?.play(uiWarningId, v, v, 1, 0, 1.0f)
    }

    private fun startAmbience() {
        val ctx = appContext ?: return
        if (!isAmbienceEnabled || isPaused || panoramaPlayer != null || _isGlobalMuted.value) return
        if (ambiencePlayer != null) {
            try {
                if (ambiencePlayer?.isPlaying == false) ambiencePlayer?.start()
            } catch (_: Exception) {}
            return
        }
        try {
            val effVol = (masterVolume * ambienceVolume).coerceIn(0f, 1f)
            ambiencePlayer = MediaPlayer.create(ctx, R.raw.martian_ambient_wind)?.apply {
                isLooping = true
                setVolume(effVol, effVol)
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Ambience failed", e)
        }
    }

    private fun stopAmbience() {
        try {
            if (ambiencePlayer?.isPlaying == true) ambiencePlayer?.stop()
        } catch (_: Exception) {}
        try {
            ambiencePlayer?.release()
        } catch (_: Exception) {}
        ambiencePlayer = null
    }

    private fun startRadioScheduler() {
        if (radioJob != null || !isRadioEnabled || !isEnabled || isPaused || _isGlobalMuted.value) return
        radioJob = scope.launch {
            while (isActive && isRadioEnabled && isEnabled && !isPaused && !_isGlobalMuted.value) {
                delay(Random.nextLong(15000, 45000))
                if (isActive && isRadioEnabled && isEnabled && !isPaused && !_isGlobalMuted.value) {
                    playRadioTransmission()
                }
            }
        }
    }

    private suspend fun playRadioTransmission() {
        val ctx = appContext ?: return
        if (_isGlobalMuted.value || isPaused || !isEnabled || !isRadioEnabled) return
        _isCommLinkActive.value = true
        val effRadio = (masterVolume * radioVolume).coerceIn(0f, 1f)
        if (radioSquelchId > 0) {
            soundPool?.play(radioSquelchId, effRadio, effRadio, 1, 0, 1.0f)
        }
        delay(200)
        if (_isGlobalMuted.value || isPaused || !isEnabled || !isRadioEnabled) {
            _isCommLinkActive.value = false
            return
        }
        val voiceResId = radioVoices.random()
        try {
            stopRadioPlayerOnly()
            val player = MediaPlayer.create(ctx, voiceResId)
            if (player == null) {
                _isCommLinkActive.value = false
                return
            }
            radioPlayer = player.apply {
                setVolume(effRadio, effRadio)
                setOnCompletionListener { mp ->
                    _isCommLinkActive.value = false
                    val squelchOut = (effRadio * 0.7f).coerceIn(0f, 1f)
                    if (!_isGlobalMuted.value && !isPaused && isEnabled && radioSquelchId > 0) {
                        soundPool?.play(radioSquelchId, squelchOut, squelchOut, 1, 0, 0.8f)
                    }
                    try { mp.release() } catch (_: Exception) {}
                    if (radioPlayer === mp) radioPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            _isCommLinkActive.value = false
        }
    }

    private fun stopRadioPlayerOnly() {
        try {
            if (radioPlayer?.isPlaying == true) radioPlayer?.stop()
        } catch (_: Exception) {}
        try {
            radioPlayer?.release()
        } catch (_: Exception) {}
        radioPlayer = null
    }

    private fun stopRadio() {
        radioJob?.cancel()
        radioJob = null
        stopRadioPlayerOnly()
        _isCommLinkActive.value = false
    }

    private fun stopAll() {
        stopAmbience()
        stopRadio()
        stopQuotePlayer()
        stopPanoramaAmbience(restartAmbience = false)
    }

    fun release() {
        stopAll()
        try { soundPool?.release() } catch (_: Exception) {}
        soundPool = null
        scope.cancel()
    }
}
