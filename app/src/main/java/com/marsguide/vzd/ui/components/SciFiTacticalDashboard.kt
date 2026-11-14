package com.marsguide.vzd.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.R
import com.marsguide.vzd.audio.SciFiAudioManager
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.model.MarsLocation
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.data.remote.NasaApiService
import com.marsguide.vzd.data.remote.MarsPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hyper-Realistic Sci-Fi Communication and Navigation Dashboard for Mars Rover.
 *
 * Implements:
 * 1. CRT Monitor Tactical Screen Modifier (scanlines, edge vignette, phosphor sweep).
 * 2. Glowing Amber & Tactical Cyan palette.
 * 3. Radio Comms Controller with squelch IN -> voice -> squelch OUT sequence + native VHF audio filter.
 * 4. SoundPool low-latency foley clicks and rotating telemetry radar.
 * 5. Tactile SciFiButton with combined haptic feedback, mechanical audio, and glowing states.
 * 6. Live Telemetry HUD: DSN Link, Aerocentric Coordinates, Rover Subsystems, and Atmospheric Gauges.
 */
@Composable
fun SciFiTacticalDashboard(
    selectedLocation: MarsLocation?,
    onNavigateToMap: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToMissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { SciFiAudioManager(context) }
    val isGlobalMuted by com.marsguide.vzd.util.SoundManager.isGlobalMuted.collectAsState()

    LaunchedEffect(isGlobalMuted) {
        audioManager.setMuted(isGlobalMuted)
    }

    DisposableEffect(audioManager) {
        onDispose {
            audioManager.release()
        }
    }

    val isTransmitting by audioManager.isTransmitting.collectAsState()
    val commsStatus by audioManager.currentCommsStatus.collectAsState()
    val isRadarSweeping by audioManager.isRadarSweeping.collectAsState()

    var selectedVoiceChannel by remember { mutableIntStateOf(0) }
    val voiceChannels = listOf(
        Pair("CH-01: JEZERO BASE", R.raw.comm_authentic_01),
        Pair("CH-02: GALE RECON", R.raw.comm_authentic_02),
        Pair("CH-03: DSN HOUSTON", R.raw.comm_authentic_03)
    )

    var simulatedPitch by remember { mutableFloatStateOf(4.2f) }
    var simulatedRoll by remember { mutableFloatStateOf(-1.8f) }
    var crtScanlinesEnabled by remember { mutableStateOf(true) }

    val baseModifier = modifier
        .fillMaxSize()
        .background(SpaceBlack)
        .testTag("scifi_tactical_dashboard")

    val screenModifier = if (crtScanlinesEnabled) {
        baseModifier.crtTacticalScreen(
            scanlineSpacing = 3.5.dp,
            scanlineAlpha = 0.20f,
            vignetteIntensity = 0.50f,
            enablePhosphorFlicker = true,
            tintColor = SciFiCyan
        )
    } else {
        baseModifier
    }

    Column(
        modifier = screenModifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. TOP STATUS BEZEL / DSN HEADER ---
        TacticalHeaderBezel(
            isTransmitting = isTransmitting,
            crtScanlinesEnabled = crtScanlinesEnabled,
            onToggleCrt = {
                audioManager.playHeavySwitch()
                crtScanlinesEnabled = !crtScanlinesEnabled
            }
        )

        // --- 2. COMMS & AUDIO CONTROLLER (RADIO TRANSMISSION SYSTEM) ---
        RadioCommsConsole(
            isTransmitting = isTransmitting,
            commsStatus = commsStatus,
            selectedChannelIndex = selectedVoiceChannel,
            onSelectChannel = { idx ->
                audioManager.playMechanicalClick(1.2f)
                selectedVoiceChannel = idx
            },
            onTransmit = {
                val chan = voiceChannels[selectedVoiceChannel]
                audioManager.playRadioTransmissionSequence(
                    voiceResId = chan.second,
                    missionCallsign = chan.first
                )
            },
            onAbort = {
                audioManager.stopRadioTransmission()
            }
        )

        // --- 3. DUAL TACTICAL RADAR & ARTIFICIAL HORIZON GAUGES ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 500.dp
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TacticalRadarDisplay(
                        isSweeping = isRadarSweeping,
                        onToggleSweep = {
                            audioManager.playHeavySwitch()
                            audioManager.toggleRadarSweep(!isRadarSweeping)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    RoverInertialDisplay(
                        pitch = simulatedPitch,
                        roll = simulatedRoll,
                        selectedLocation = selectedLocation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Radar Sweep Display
                    TacticalRadarDisplay(
                        isSweeping = isRadarSweeping,
                        onToggleSweep = {
                            audioManager.playHeavySwitch()
                            audioManager.toggleRadarSweep(!isRadarSweeping)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Rover Inertial Nav HUD (Pitch/Roll Gyro)
                    RoverInertialDisplay(
                        pitch = simulatedPitch,
                        roll = simulatedRoll,
                        selectedLocation = selectedLocation,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 4. TACTICAL ROVER NAVIGATION SWITCHBOARD ---
        TacticalSwitchboard(
            onNavigateToMap = onNavigateToMap,
            onNavigateToPlanner = onNavigateToPlanner,
            onNavigateToMissions = onNavigateToMissions,
            onAlertTest = {
                audioManager.playAlertChime()
            }
        )

        // --- 5. LIVE NASA ROVER TELEMETRY & IMAGE PANEL ---
        LiveNasaRoverPanel()

        // --- 6. REAL-TIME SUBSYSTEM TELEMETRY LOG ---
        TelemetrySubsystemLog(
            selectedLocation = selectedLocation,
            isTransmitting = isTransmitting
        )
    }
}

/**
 * Top Header Bezel: Station identification, Mission Sol, and CRT FX Toggle.
 */
@Composable
private fun TacticalHeaderBezel(
    isTransmitting: Boolean,
    crtScanlinesEnabled: Boolean,
    onToggleCrt: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .border(1.dp, SciFiBorder, CutCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
        color = SciFiDarkPanel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tactical Blink LED
                val transition = rememberInfiniteTransition(label = "bezel_blink")
                val ledAlpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(if (isTransmitting) 300 else 1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "led_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            (if (isTransmitting) SciFiAmber else SciFiCyan).copy(alpha = ledAlpha)
                        )
                )

                Column {
                    Text(
                        text = "MARS TACTICAL COMMS & NAV",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isTransmitting) "CARRIER LOCK // TX ACTIVE" else "DEEP SPACE NETWORK ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTransmitting) SciFiAmber else SciFiCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // CRT Scanline Shader Toggle
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, if (crtScanlinesEnabled) SciFiCyan.copy(alpha = 0.6f) else SpaceBorderDark, RoundedCornerShape(4.dp))
                    .clickable { onToggleCrt() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (crtScanlinesEnabled) SciFiCyan.copy(alpha = 0.15f) else SpaceBlack
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Toggle CRT Scanlines",
                        tint = if (crtScanlinesEnabled) SciFiCyan else Color.Gray,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (crtScanlinesEnabled) "CRT: ON" else "CRT: RAW",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (crtScanlinesEnabled) SciFiCyan else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Radio Comms Console:
 * Squelch sequence triggers, channel switches, and native VHF radio DSP indicator.
 */
@Composable
private fun RadioCommsConsole(
    isTransmitting: Boolean,
    commsStatus: String,
    selectedChannelIndex: Int,
    onSelectChannel: (Int) -> Unit,
    onTransmit: () -> Unit,
    onAbort: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .border(1.dp, if (isTransmitting) SciFiAmber.copy(alpha = 0.8f) else SciFiBorder, CutCornerShape(8.dp)),
        color = SciFiDarkPanel
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Radio Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = if (isTransmitting) SciFiAmber else SciFiCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VHF / UHF RADIO TERMINAL // DSP FILTER",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "437.450 MHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }

            // Radio Status Oscilloscope / Transmission Readout Screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, if (isTransmitting) SciFiAmber.copy(alpha = 0.5f) else SpaceBorderDark, RoundedCornerShape(4.dp)),
                color = SpaceBlack
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = commsStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTransmitting) SciFiAmberGlow else SciFiCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = if (isTransmitting) "[SQUELCH CLOSED -> VOICE ON AIR]" else "[SQUELCH STANDBY]",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )
                    }

                    // Dynamic Audio Waveform Simulation
                    AudioWaveformVisualizer(
                        isActive = isTransmitting,
                        color = if (isTransmitting) SciFiAmber else SciFiCyan
                    )
                }
            }

            // Frequency Channel Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("CH-01: JEZERO", "CH-02: GALE", "CH-03: DSN").forEachIndexed { idx, label ->
                    val isSelected = selectedChannelIndex == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CutCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) SciFiCyan else SpaceBorderDark,
                                CutCornerShape(4.dp)
                            )
                            .clickable(enabled = !isTransmitting) { onSelectChannel(idx) }
                            .padding(vertical = 6.dp),
                        color = if (isSelected) SciFiCyan.copy(alpha = 0.18f) else SpaceDark
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SciFiCyan else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }

            // Radio Actions (Transmit Sequence & Abort)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SciFiButton(
                    text = if (isTransmitting) "TRANSMITTING..." else "TRANSMIT SEQUENCE",
                    subtitle = "SQUELCH IN -> VHF VOICE -> SQUELCH OUT",
                    icon = Icons.AutoMirrored.Filled.Send,
                    isAmber = true,
                    enabled = !isTransmitting,
                    onClick = onTransmit,
                    modifier = Modifier.weight(1.8f)
                )

                SciFiButton(
                    text = "ABORT",
                    subtitle = "CUT RF",
                    icon = Icons.Default.Close,
                    isAmber = false,
                    enabled = isTransmitting,
                    onClick = onAbort,
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }
}

/**
 * Simulated Audio Waveform Bar Visualizer.
 */
@Composable
private fun AudioWaveformVisualizer(
    isActive: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "waveform_clock")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val w = size.width
        val h = size.height
        val barCount = 32
        val barWidth = (w / barCount) * 0.7f
        val step = w / barCount

        for (i in 0 until barCount) {
            val norm = i.toFloat() / barCount
            val amp = if (isActive) {
                ((sin(norm * 14f + phase) * 0.4f + cos(norm * 8f - phase) * 0.3f + 0.5f).coerceIn(0.15f, 0.95f))
            } else {
                0.08f
            }
            val barHeight = h * amp
            drawRect(
                color = color.copy(alpha = if (isActive) 0.85f else 0.25f),
                topLeft = Offset(i * step, h - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

/**
 * Tactical Telemetry Radar Display.
 */
@Composable
private fun TacticalRadarDisplay(
    isSweeping: Boolean,
    onToggleSweep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_rot")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Surface(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .border(1.dp, SciFiBorder, CutCornerShape(8.dp)),
        color = SciFiDarkPanel
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RADAR ALTIMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                Text(
                    text = if (isSweeping) "PULSING" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSweeping) SciFiCyanGlow else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.5.sp
                )
            }

            // Radar Circular Scope
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(SpaceBlack)
                    .border(1.dp, SciFiCyan.copy(alpha = 0.4f), CircleShape)
                    .clickable { onToggleSweep() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = cx - 4f

                    // Range Rings
                    drawCircle(SciFiCyan.copy(alpha = 0.2f), radius = r * 0.33f, style = Stroke(1f))
                    drawCircle(SciFiCyan.copy(alpha = 0.2f), radius = r * 0.66f, style = Stroke(1f))
                    drawCircle(SciFiCyan.copy(alpha = 0.4f), radius = r, style = Stroke(1.2f))

                    // Crosshairs
                    drawLine(SciFiCyan.copy(alpha = 0.25f), Offset(cx - r, cy), Offset(cx + r, cy), 1f)
                    drawLine(SciFiCyan.copy(alpha = 0.25f), Offset(cx, cy - r), Offset(cx, cy + r), 1f)

                    // Rotating Radar Sweep Beam
                    if (isSweeping) {
                        val rad = Math.toRadians(angle.toDouble())
                        val endX = cx + (r * cos(rad)).toFloat()
                        val endY = cy + (r * sin(rad)).toFloat()

                        drawLine(
                            color = SciFiCyanGlow,
                            start = Offset(cx, cy),
                            end = Offset(endX, endY),
                            strokeWidth = 2f
                        )

                        // Ping blips
                        drawCircle(
                            color = SciFiAmber,
                            radius = 3.5f,
                            center = Offset(cx + r * 0.45f, cy - r * 0.35f)
                        )
                    }
                }

                Text(
                    text = if (isSweeping) "TAP TO HALT" else "TAP TO PING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Rover Inertial Display: Artificial horizon with simulated pitch and roll angles.
 */
@Composable
private fun RoverInertialDisplay(
    pitch: Float,
    roll: Float,
    selectedLocation: MarsLocation?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .border(1.dp, SciFiBorder, CutCornerShape(8.dp)),
        color = SciFiDarkPanel
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATTITUDE GYRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                Text(
                    text = "P: ${pitch}° R: ${roll}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiAmberGlow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }

            // Gyro Pitch & Horizon Sphere
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(SpaceBlack)
                    .border(1.dp, SciFiAmber.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = cx - 4f

                    // Pitch ladder lines
                    for (offsetStep in listOf(-20f, -10f, 0f, 10f, 20f)) {
                        val lineY = cy + offsetStep * 1.5f + (pitch * 1.8f)
                        if (lineY in (cy - r)..(cy + r)) {
                            val lineHalfW = if (offsetStep == 0f) 28f else 14f
                            drawLine(
                                color = if (offsetStep == 0f) SciFiAmberGlow else SciFiAmber.copy(alpha = 0.5f),
                                start = Offset(cx - lineHalfW, lineY),
                                end = Offset(cx + lineHalfW, lineY),
                                strokeWidth = if (offsetStep == 0f) 2f else 1f
                            )
                        }
                    }

                    // Rover Center Reticle
                    drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
                    drawLine(Color.White, Offset(cx - 12f, cy), Offset(cx - 4f, cy), 1.5f)
                    drawLine(Color.White, Offset(cx + 4f, cy), Offset(cx + 12f, cy), 1.5f)
                }

                Text(
                    text = selectedLocation?.name?.take(10)?.uppercase() ?: "NO TARGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.5.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Tactical Navigation Switchboard with SciFiButtons.
 */
@Composable
private fun TacticalSwitchboard(
    onNavigateToMap: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToMissions: () -> Unit,
    onAlertTest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "TACTICAL NAVIGATION CONTROLS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SciFiButton(
                text = "MAP RADAR",
                subtitle = "PLANETARY CARTOGRAPHY",
                icon = Icons.Default.Map,
                isAmber = false,
                onClick = onNavigateToMap,
                modifier = Modifier.weight(1f)
            )

            SciFiButton(
                text = "GEODESIC",
                subtitle = "HAVERSINE TRAJECTORY",
                icon = Icons.Default.Route,
                isAmber = true,
                onClick = onNavigateToPlanner,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SciFiButton(
                text = "FLEET ARCHIVE",
                subtitle = "PERSEVERANCE & CURIOSITY",
                icon = Icons.Default.RocketLaunch,
                isAmber = false,
                onClick = onNavigateToMissions,
                modifier = Modifier.weight(1f)
            )

            SciFiButton(
                text = "ALARM KLX-9",
                subtitle = "AUDIO FOLEY TEST",
                icon = Icons.Default.Warning,
                isAmber = true,
                onClick = onAlertTest,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Real-Time Subsystem Telemetry Log.
 */
@Composable
private fun TelemetrySubsystemLog(
    selectedLocation: MarsLocation?,
    isTransmitting: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .border(1.dp, SciFiBorder, CutCornerShape(8.dp)),
        color = SciFiDarkPanel
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AEROCENTRIC TELEMETRY FEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                Text(
                    text = "RTG POWER: 110W",
                    style = MaterialTheme.typography.labelSmall,
                    color = SciFiAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp
                )
            }

            val coordStr = selectedLocation?.coordinate?.formatShort() ?: "18.3800° N | 77.5800° E"
            val elevStr = selectedLocation?.coordinate?.elevation?.let { "${it.toInt()} m" } ?: "-2500 m"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("COORDINATES", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    Text(coordStr, style = MaterialTheme.typography.bodySmall, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DATUM ELEVATION", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    Text(elevStr, style = MaterialTheme.typography.bodySmall, color = SciFiCyanGlow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Subsystem Status Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubsystemBadge(label = "RADIO", status = if (isTransmitting) "TX BUSY" else "RX IDLE", ok = true)
                SubsystemBadge(label = "WHEEL DRIVE", status = "NOMINAL", ok = true)
                SubsystemBadge(label = "HEATER CORE", status = "ACTIVE", ok = true)
                SubsystemBadge(label = "MASTCAM-Z", status = "STANDBY", ok = true)
            }
        }
    }
}

@Composable
private fun SubsystemBadge(
    label: String,
    status: String,
    ok: Boolean
) {
    Surface(
        color = SpaceBlack,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (ok) SciFiCyan else ScienceWarning,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class CuratedRoverFrame(
    val title: String,
    val sol: String,
    val camera: String,
    val date: String,
    val imageUrl: String
)

private val CURATED_NASA_ROVER_ARCHIVE = listOf(
    CuratedRoverFrame(
        title = "Jezero Delta Sediments — Perseverance",
        sol = "SOL 440",
        camera = "MASTCAM-Z",
        date = "2022-05-15",
        imageUrl = "https://images-assets.nasa.gov/image/PIA25287/PIA25287~orig.jpg"
    ),
    CuratedRoverFrame(
        title = "Van Zyl Overlook — Ingenuity Flight Zone",
        sol = "SOL 049",
        camera = "NAVCAM_LEFT",
        date = "2021-04-09",
        imageUrl = "https://images-assets.nasa.gov/image/PIA24546/PIA24546~small.jpg"
    ),
    CuratedRoverFrame(
        title = "Enchanted Lake Delta Scarp — Perseverance",
        sol = "SOL 419",
        camera = "MASTCAM-Z",
        date = "2022-04-24",
        imageUrl = "https://images-assets.nasa.gov/image/PIA25216/PIA25216~small.jpg"
    ),
    CuratedRoverFrame(
        title = "Mount Sharp Layered Strata — Curiosity",
        sol = "SOL 3640",
        camera = "MASTCAM",
        date = "2022-11-01",
        imageUrl = "https://images-assets.nasa.gov/image/PIA19808/PIA19808~orig.jpg"
    )
)

sealed interface NasaRoverUiState {
    object Loading : NasaRoverUiState
    data class LiveSuccess(val photos: List<MarsPhotoItem>) : NasaRoverUiState
    data class ArchiveFallback(val note: String, val frames: List<CuratedRoverFrame> = CURATED_NASA_ROVER_ARCHIVE) : NasaRoverUiState
}

@Composable
fun LiveNasaRoverPanel(modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf<NasaRoverUiState>(NasaRoverUiState.Loading) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    fun fetchPhotos() {
        uiState = NasaRoverUiState.Loading
        selectedPhotoIndex = 0
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = NasaApiService.instance.getPerseveranceLatestPhotos()
                withContext(Dispatchers.Main) {
                    if (response.latestPhotos.isNotEmpty()) {
                        uiState = NasaRoverUiState.LiveSuccess(response.latestPhotos)
                    } else {
                        uiState = NasaRoverUiState.ArchiveFallback("CURATED NASA PDS ARCHIVE")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = NasaRoverUiState.ArchiveFallback("CURATED NASA PDS ARCHIVE")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchPhotos()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(12.dp))
            .border(1.dp, SciFiCyan.copy(alpha = 0.4f), CutCornerShape(12.dp)),
        color = SciFiDarkPanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (uiState) {
                                    is NasaRoverUiState.LiveSuccess -> ScienceGreen
                                    is NasaRoverUiState.Loading -> SciFiAmber
                                    is NasaRoverUiState.ArchiveFallback -> SciFiCyan
                                },
                                CircleShape
                            )
                    )
                    Text(
                        text = "NASA TELEMETRY // ROVER CAM FEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SciFiCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = when (uiState) {
                        is NasaRoverUiState.LiveSuccess -> "API: PDS // LIVE"
                        is NasaRoverUiState.Loading -> "DSN SYNCING..."
                        is NasaRoverUiState.ArchiveFallback -> "PDS // ARCHIVE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            when (val state = uiState) {
                is NasaRoverUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF04060A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = SciFiCyan, modifier = Modifier.size(32.dp))
                            Text(
                                text = "ACQUIRING DEEP SPACE NETWORK FEED...",
                                style = MaterialTheme.typography.labelSmall,
                                color = SciFiCyan.copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                is NasaRoverUiState.ArchiveFallback -> {
                    val frame = state.frames.getOrNull(selectedPhotoIndex) ?: state.frames.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        coil.compose.AsyncImage(
                            model = frame.imageUrl,
                            contentDescription = frame.title,
                            fallback = androidx.compose.ui.res.painterResource(id = R.drawable.mars_surface_hero),
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.mars_surface_hero),
                            modifier = Modifier
                                .fillMaxSize()
                                .crtTacticalScreen(scanlineSpacing = 3.dp, scanlineAlpha = 0.22f, vignetteIntensity = 0.4f),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(1.dp, SciFiCyan.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                .clickable { fetchPhotos() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Live NASA", tint = SciFiCyan, modifier = Modifier.size(12.dp))
                            Text(
                                text = "SYNC LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SciFiCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = frame.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "${frame.sol} // ${frame.camera} // ${frame.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SciFiCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.frames.forEachIndexed { idx, _ ->
                            val isSelected = idx == selectedPhotoIndex
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { selectedPhotoIndex = idx },
                                color = if (isSelected) SciFiCyan.copy(alpha = 0.22f) else SciFiDarkPanel,
                                border = BorderStroke(1.dp, if (isSelected) SciFiCyan else SciFiBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "ARCHIVE-${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) SciFiCyan else Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
                is NasaRoverUiState.LiveSuccess -> {
                    val photo = state.photos.getOrNull(selectedPhotoIndex) ?: state.photos.first()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        coil.compose.AsyncImage(
                            model = photo.imgSrc,
                            contentDescription = "Perseverance Rover Camera Image",
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.mars_surface_hero),
                            modifier = Modifier
                                .fillMaxSize()
                                .crtTacticalScreen(scanlineSpacing = 3.dp, scanlineAlpha = 0.25f, vignetteIntensity = 0.4f),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SOL: ${photo.sol} // ${photo.camera.name} // ${photo.earthDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SciFiCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (state.photos.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.photos.take(5).forEachIndexed { idx, _ ->
                                val isSelected = idx == selectedPhotoIndex
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clickable { selectedPhotoIndex = idx },
                                    color = if (isSelected) SciFiCyan.copy(alpha = 0.2f) else SciFiDarkPanel,
                                    border = BorderStroke(1.dp, if (isSelected) SciFiCyan else SciFiBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "CAM-${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) SciFiCyan else Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
