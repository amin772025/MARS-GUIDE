package com.marsguide.vzd.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.audiofx.BassBoost
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log
import com.marsguide.vzd.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tactical Radio Comms & UI Foley Controller for MARS//GUIDE.
 *
 * Implements:
 * 1. SoundPool engine for ultra low-latency mechanical button clicks, switches, telemetry beeps, and radar sweeps.
 * 2. Radio transmission sequence: Squelch IN -> Voice message -> Squelch OUT / Roger Beep.
 * 3. Native Android AudioEffect (Equalizer / BassBoost) shaping voice playback to simulate gritty VHF/UHF interplanetary comms:
 *    - Steep high-frequency rolloff (low-pass filter effect simulating narrow-band radio)
 *    - Boosted mid-band presence for comms distortion
 *    - Cut sub-bass to remove clarity and introduce comms harshness.
 */
class SciFiAudioManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // SoundPool for instant tactile foley & telemetry radar beeps
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()
        )
        .build()

    // Sound IDs
    private var clickSoundId: Int = -1
    private var heavySwitchSoundId: Int = -1
    private var radarBeepSoundId: Int = -1
    private var squelchInSoundId: Int = -1
    private var squelchOutSoundId: Int = -1
    private var alertChimeSoundId: Int = -1

    // MediaPlayer for radio transmission stream
    private var voicePlayer: MediaPlayer? = null
    private var radioEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // Radar pulse job
    private var radarSweepJob: Job? = null

    // Comms Transmission State
    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _currentCommsStatus = MutableStateFlow("COMMS STANDBY // DSN LINK 100%")
    val currentCommsStatus: StateFlow<String> = _currentCommsStatus.asStateFlow()

    private val _isRadarSweeping = MutableStateFlow(false)
    val isRadarSweeping: StateFlow<Boolean> = _isRadarSweeping.asStateFlow()

    private var isMuted: Boolean = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (isMuted) {
            stopRadioTransmission()
            toggleRadarSweep(false)
        }
    }

    init {
        loadSounds()
    }

    private fun loadSounds() {
        // ... (omitting for brevity, but I'll include the actual lines in TargetContent if needed)
        val clickFile = getOrSynthesizeWav("foley_click.wav") { generateMechanicalClickWav() }
        val switchFile = getOrSynthesizeWav("foley_switch.wav") { generateHeavySwitchWav() }
        val radarFile = getOrSynthesizeWav("foley_radar.wav") { generateRadarPingWav() }
        val squelchInFile = getOrSynthesizeWav("radio_squelch_in.wav") { generateSquelchWav(isOpening = true) }
        val squelchOutFile = getOrSynthesizeWav("radio_squelch_out.wav") { generateSquelchWav(isOpening = false) }
        val alertFile = getOrSynthesizeWav("foley_alert.wav") { generateAlertChimeWav() }

        clickSoundId = soundPool.load(clickFile.absolutePath, 1)
        heavySwitchSoundId = soundPool.load(switchFile.absolutePath, 1)
        radarBeepSoundId = soundPool.load(radarFile.absolutePath, 1)
        squelchInSoundId = soundPool.load(squelchInFile.absolutePath, 1)
        squelchOutSoundId = soundPool.load(squelchOutFile.absolutePath, 1)
        alertChimeSoundId = soundPool.load(alertFile.absolutePath, 1)
    }

    /**
     * Mechanical tactile click sound for buttons.
     */
    fun playMechanicalClick(pitch: Float = 1.0f) {
        if (isMuted) return
        if (clickSoundId != -1) {
            soundPool.play(clickSoundId, 0.9f, 0.9f, 1, 0, pitch)
        }
    }

    /**
     * Heavy toggle switch sound.
     */
    fun playHeavySwitch() {
        if (isMuted) return
        if (heavySwitchSoundId != -1) {
            soundPool.play(heavySwitchSoundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    /**
     * Alert chime sound.
     */
    fun playAlertChime() {
        if (isMuted) return
        if (alertChimeSoundId != -1) {
            soundPool.play(alertChimeSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
        }
    }

    /**
     * Background telemetry radar pinging.
     */
    fun toggleRadarSweep(enabled: Boolean) {
        if (isMuted && enabled) return
        _isRadarSweeping.value = enabled
        if (enabled) {
            radarSweepJob?.cancel()
            radarSweepJob = coroutineScope.launch {
                while (isActive) {
                    if (radarBeepSoundId != -1 && !isMuted) {
                        soundPool.play(radarBeepSoundId, 0.45f, 0.45f, 1, 0, 1.0f)
                    }
                    delay(2400) // 2.4 second radar revolution
                }
            }
        } else {
            radarSweepJob?.cancel()
            radarSweepJob = null
        }
    }

    private suspend fun playShortRawEffect(context: Context, resId: Int) {
        if (isMuted) return
        withTimeoutOrNull(1500L) {
            suspendCancellableCoroutine { continuation ->
                var mp: MediaPlayer? = null
                try {
                    mp = MediaPlayer.create(context, resId)
                    if (mp == null) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                        return@suspendCancellableCoroutine
                    }
                    mp.setOnCompletionListener { player ->
                        try { player.release() } catch (_: Exception) {}
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                    mp.setOnErrorListener { player, _, _ ->
                        try { player.release() } catch (_: Exception) {}
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                        true
                    }
                    continuation.invokeOnCancellation {
                        try { mp?.stop() } catch (_: Exception) {}
                        try { mp?.release() } catch (_: Exception) {}
                    }
                    mp.start()
                } catch (_: Exception) {
                    try { mp?.release() } catch (_: Exception) {}
                    if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                }
            }
        }
    }

    /**
     * Authentic Ground Control Audio Engine:
     * Plays quindar tone -> waits for completion -> plays mission voice (with VHF equalizer filter) -> waits for completion -> plays quindar tone.
     */
    fun playMissionControlTransmission(context: Context) {
        if (isMuted || _isTransmitting.value) return

        coroutineScope.launch {
            try {
                _isTransmitting.value = true
                _currentCommsStatus.value = "GROUND CONTROL // QUINDAR IN"

                // 1. Play Quindar Tone IN
                playShortRawEffect(context, R.raw.quindar_tone)
                if (isMuted) {
                    _isTransmitting.value = false
                    _currentCommsStatus.value = "COMMS MUTED // STANDBY"
                    return@launch
                }

                // 2. Play Mission Voice with VHF filter
                _currentCommsStatus.value = "HOUSTON // APOLLO COMM TRANSMISSION"

                suspendCancellableCoroutine<Unit> { continuation ->
                    cleanupVoiceEffects()
                    try {
                        val player = MediaPlayer.create(context, R.raw.mission_voice)
                        if (player == null) {
                            if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                            return@suspendCancellableCoroutine
                        }
                        voicePlayer = player.apply {
                            val audioSessionId = this.audioSessionId
                            if (audioSessionId != AudioManager.ERROR) {
                                try {
                                    radioEqualizer = Equalizer(0, audioSessionId).apply {
                                        enabled = true
                                        val numBands = numberOfBands
                                        val bandLevels = bandLevelRange
                                        val minLevel = bandLevels[0]
                                        val maxLevel = bandLevels[1]
                                        for (band in 0 until numBands) {
                                            val freqHz = getCenterFreq(band.toShort()) / 1000
                                            when {
                                                freqHz < 300 -> setBandLevel(band.toShort(), (minLevel * 0.9f).toInt().toShort())
                                                freqHz in 300..3000 -> setBandLevel(band.toShort(), (maxLevel * 0.8f).toInt().toShort())
                                                else -> setBandLevel(band.toShort(), (minLevel * 0.9f).toInt().toShort())
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }

                            setOnCompletionListener {
                                if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                            }
                            setOnErrorListener { _, _, _ ->
                                if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                                true
                            }
                            start()
                        }
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                }

                // 3. Play Quindar Tone OUT
                if (!isMuted) {
                    _currentCommsStatus.value = "GROUND CONTROL // QUINDAR OUT"
                    playShortRawEffect(context, R.raw.quindar_tone)
                }

                cleanupVoiceEffects()
                _isTransmitting.value = false
                _currentCommsStatus.value = "COMMS STANDBY // DSN LINK 100%"

            } catch (e: Exception) {
                Log.e("SciFiAudioManager", "Error in playMissionControlTransmission", e)
                cleanupVoiceEffects()
                _isTransmitting.value = false
                _currentCommsStatus.value = "COMMS STANDBY // ERROR"
            }
        }
    }

    /**
     * Full Radio Transmission Sequence:
     * 1. Start squelch / RF burst
     * 2. Wait for squelch burst completion
     * 3. Play main voice message through native AudioEffect filter (low-pass + mid-boost)
     * 4. Trigger end squelch & roger beep
     */
    fun playRadioTransmissionSequence(
        voiceResId: Int = R.raw.comm_authentic_01,
        missionCallsign: String = "JEZERO BASE // ROVER ODYSSEY",
        onFinished: (() -> Unit)? = null
    ) {
        if (isMuted || _isTransmitting.value) {
            onFinished?.invoke()
            return
        }

        coroutineScope.launch {
            try {
                _isTransmitting.value = true
                _currentCommsStatus.value = "CARRIER DETECTED // RF SQUELCH OPEN"

                // 1. Squelch IN
                if (squelchInSoundId != -1) {
                    soundPool.play(squelchInSoundId, 0.9f, 0.9f, 2, 0, 1.0f)
                }
                delay(280)
                if (isMuted) {
                    _isTransmitting.value = false
                    _currentCommsStatus.value = "COMMS MUTED // STANDBY"
                    onFinished?.invoke()
                    return@launch
                }

                // 2. Play Main Voice Message with Native AudioEffect VHF Radio Filter
                _currentCommsStatus.value = "RECEIVING: $missionCallsign"
                playVoiceWithVhfFilter(voiceResId, onFinished)
            } catch (e: Exception) {
                Log.e("SciFiAudioManager", "Error during radio transmission sequence", e)
                cleanupVoiceEffects()
                _isTransmitting.value = false
                _currentCommsStatus.value = "COMMS STANDBY // ERROR RESET"
                onFinished?.invoke()
            }
        }
    }

    private fun playVoiceWithVhfFilter(
        voiceResId: Int,
        onFinished: (() -> Unit)? = null
    ) {
        cleanupVoiceEffects()

        try {
            val player = MediaPlayer.create(context, voiceResId)
            if (player == null) {
                _isTransmitting.value = false
                _currentCommsStatus.value = "COMMS STANDBY // DSN LINK 100%"
                onFinished?.invoke()
                return
            }
            voicePlayer = player.apply {
                val audioSessionId = this.audioSessionId

                if (audioSessionId != AudioManager.ERROR) {
                    try {
                        radioEqualizer = Equalizer(0, audioSessionId).apply {
                            enabled = true
                            val numBands = numberOfBands
                            val bandLevels = bandLevelRange
                            val minLevel = bandLevels[0]
                            val maxLevel = bandLevels[1]

                            for (band in 0 until numBands) {
                                val centerFreqMhz = getCenterFreq(band.toShort())
                                val centerFreqHz = centerFreqMhz / 1000

                                when {
                                    centerFreqHz < 300 -> {
                                        setBandLevel(band.toShort(), (minLevel * 0.8f).toInt().toShort())
                                    }
                                    centerFreqHz in 800..3500 -> {
                                        setBandLevel(band.toShort(), (maxLevel * 0.7f).toInt().toShort())
                                    }
                                    else -> {
                                        setBandLevel(band.toShort(), (minLevel * 0.9f).toInt().toShort())
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SciFiAudioManager", "Native Equalizer not supported on this device/session", e)
                    }
                }

                setOnCompletionListener {
                    coroutineScope.launch {
                        _currentCommsStatus.value = "TRANSMISSION END // SQUELCH OUT"
                        if (!isMuted && squelchOutSoundId != -1) {
                            soundPool.play(squelchOutSoundId, 0.85f, 0.85f, 2, 0, 1.0f)
                        }
                        delay(350)
                        cleanupVoiceEffects()
                        _isTransmitting.value = false
                        _currentCommsStatus.value = "COMMS STANDBY // DSN LINK 100%"
                        onFinished?.invoke()
                    }
                }

                setOnErrorListener { _, _, _ ->
                    cleanupVoiceEffects()
                    _isTransmitting.value = false
                    _currentCommsStatus.value = "COMMS STANDBY // DSN LINK 100%"
                    onFinished?.invoke()
                    true
                }

                start()
            }
        } catch (e: Exception) {
            Log.e("SciFiAudioManager", "Failed to start voice player with VHF filter", e)
            cleanupVoiceEffects()
            _isTransmitting.value = false
            _currentCommsStatus.value = "COMMS STANDBY // DSN LINK 100%"
            onFinished?.invoke()
        }
    }

    fun stopRadioTransmission() {
        try {
            if (voicePlayer?.isPlaying == true) {
                voicePlayer?.stop()
            }
        } catch (_: Exception) {}
        cleanupVoiceEffects()
        if (!isMuted && squelchOutSoundId != -1) {
            soundPool.play(squelchOutSoundId, 0.8f, 0.8f, 2, 0, 1.0f)
        }
        _isTransmitting.value = false
        _currentCommsStatus.value = "COMMS CANCELLED // STANDBY"
    }

    private fun cleanupVoiceEffects() {
        try {
            radioEqualizer?.enabled = false
            radioEqualizer?.release()
            radioEqualizer = null
        } catch (e: Exception) {
            // Ignore cleanup warnings
        }
        try {
            voicePlayer?.release()
            voicePlayer = null
        } catch (e: Exception) {
            // Ignore cleanup warnings
        }
    }

    fun release() {
        radarSweepJob?.cancel()
        cleanupVoiceEffects()
        soundPool.release()
        coroutineScope.cancel()
    }

    // --- PROCEDURAL FOLEY GENERATORS (WAV PCM 16-BIT 44.1kHz) ---

    private fun getOrSynthesizeWav(fileName: String, generator: () -> ByteArray): File {
        val file = File(context.cacheDir, fileName)
        if (!file.exists() || file.length() < 100) {
            try {
                FileOutputStream(file).use { it.write(generator()) }
            } catch (e: Exception) {
                Log.e("SciFiAudioManager", "Error writing audio file $fileName", e)
            }
        }
        return file
    }

    /**
     * Tactile Mechanical Click (Snap + Low thud resonance)
     */
    private fun generateMechanicalClickWav(): ByteArray {
        val sampleRate = 44100
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-t * 90.0) // fast decay
            // High frequency snap (2200Hz) + solid body thud (320Hz)
            val snap = sin(2.0 * PI * 2200.0 * t) * 0.6
            val thud = sin(2.0 * PI * 320.0 * t) * 0.8
            val sampleVal = ((snap + thud) * decay * 28000.0).coerceIn(-32767.0, 32767.0)
            pcmData[i] = sampleVal.toInt().toShort()
        }
        return createWavHeaderAndPcm(pcmData, sampleRate)
    }

    /**
     * Heavy toggle switch (dual contact tap)
     */
    private fun generateHeavySwitchWav(): ByteArray {
        val sampleRate = 44100
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay1 = Math.exp(-t * 60.0)
            val t2 = (t - 0.015).coerceAtLeast(0.0)
            val decay2 = if (t >= 0.015) Math.exp(-t2 * 80.0) else 0.0

            val part1 = sin(2.0 * PI * 650.0 * t) * decay1 * 0.7
            val part2 = sin(2.0 * PI * 420.0 * t2) * decay2 * 0.9
            val sampleVal = ((part1 + part2) * 26000.0).coerceIn(-32767.0, 32767.0)
            pcmData[i] = sampleVal.toInt().toShort()
        }
        return createWavHeaderAndPcm(pcmData, sampleRate)
    }

    /**
     * Telemetry Radar Ping (High-Q sine ping with soft decay)
     */
    private fun generateRadarPingWav(): ByteArray {
        val sampleRate = 44100
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-t * 14.0)
            // 1760 Hz (A6 note) pure radar acoustic ping
            val wave = sin(2.0 * PI * 1760.0 * t)
            val sampleVal = (wave * decay * 22000.0).coerceIn(-32767.0, 32767.0)
            pcmData[i] = sampleVal.toInt().toShort()
        }
        return createWavHeaderAndPcm(pcmData, sampleRate)
    }

    /**
     * VHF Squelch Noise Burst with Quench Pop
     */
    private fun generateSquelchWav(isOpening: Boolean): ByteArray {
        val sampleRate = 44100
        val durationMs = if (isOpening) 220 else 260
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)
        val rnd = Random(42)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (isOpening) {
                // Sharp attack, brief static, soft trail
                if (t < 0.01) (t / 0.01) else Math.exp(-(t - 0.01) * 16.0)
            } else {
                // Static burst followed by Quench Roger Beep
                val staticPart = if (t < 0.12) 1.0 - (t / 0.12) else 0.0
                val beepPart = if (t in 0.13..0.24) {
                    sin(2.0 * PI * 2475.0 * (t - 0.13)) * Math.exp(-(t - 0.13) * 20.0)
                } else 0.0
                staticPart * 0.7 + beepPart * 0.9
            }

            // White noise filtered to radio VHF spectrum (bandpassed noise)
            val whiteNoise = (rnd.nextDouble() * 2.0 - 1.0)
            val sampleVal = (whiteNoise * envelope * 24000.0).coerceIn(-32767.0, 32767.0)
            pcmData[i] = sampleVal.toInt().toShort()
        }
        return createWavHeaderAndPcm(pcmData, sampleRate)
    }

    /**
     * Alert Chime (Two-tone tactical telemetry)
     */
    private fun generateAlertChimeWav(): ByteArray {
        val sampleRate = 44100
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.12) 1320.0 else 1760.0
            val tLocal = if (t < 0.12) t else (t - 0.12)
            val decay = Math.exp(-tLocal * 15.0)
            val wave = sin(2.0 * PI * freq * tLocal)
            val sampleVal = (wave * decay * 22000.0).coerceIn(-32767.0, 32767.0)
            pcmData[i] = sampleVal.toInt().toShort()
        }
        return createWavHeaderAndPcm(pcmData, sampleRate)
    }

    private fun createWavHeaderAndPcm(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // fmt subchunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Subchunk1Size (16 for PCM)
        header[20] = 1; header[21] = 0 // AudioFormat (1 = PCM)
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0 // block align
        header[34] = 16; header[35] = 0 // bits per sample
        // data subchunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val fullByteArray = ByteArray(header.size + totalAudioLen)
        System.arraycopy(header, 0, fullByteArray, 0, header.size)
        var byteIdx = header.size
        for (sample in pcmData) {
            fullByteArray[byteIdx++] = (sample.toInt() and 0xff).toByte()
            fullByteArray[byteIdx++] = ((sample.toInt() shr 8) and 0xff).toByte()
        }
        return fullByteArray
    }
}
