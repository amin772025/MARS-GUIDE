package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.R
import com.marsguide.vzd.domain.model.MarsLocation
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel
import com.marsguide.vzd.util.SoundManager
import com.marsguide.vzd.util.VisualQuality
import com.marsguide.vzd.util.VisualQualitySettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    viewModel: MarsGuideViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToLocation: (String) -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToMissions: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qualitySettings = remember { VisualQualitySettings(context) }
    val visualQuality by qualitySettings.quality.collectAsState()
    val reducedMotion by qualitySettings.reducedMotion.collectAsState()
    val soundEnabled by qualitySettings.soundEnabled.collectAsState()
    val ambienceEnabled by qualitySettings.ambienceEnabled.collectAsState()
    val radioEnabled by qualitySettings.radioEnabled.collectAsState()
    val uiEnabled by qualitySettings.uiEnabled.collectAsState()
    val alertsEnabled by qualitySettings.alertsEnabled.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> SoundManager.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> SoundManager.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isOnline by viewModel.isOnline.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isCommLinkReceiving = SoundManager.isCommLinkActive.collectAsState().value

    var showDemoDialog by remember { mutableStateOf(false) }
    var demoStepIndex by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Spacecraft Environment Background
        Image(
            painter = painterResource(id = R.drawable.spacecraft_cockpit_overlay_1789741189998),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.15f),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP COMMAND HEADER
            CommandDeckHeader(
                isOnline = isOnline,
                isCommLinkReceiving = isCommLinkReceiving,
                soundEnabled = soundEnabled,
                onToggleSound = {
                    val next = !soundEnabled
                    qualitySettings.setSoundEnabled(next)
                    SoundManager.setEnabled(next)
                },
                onOpenSettings = { showSettingsDialog = true },
                onOpenSearch = onOpenSearch
            )

            // 2. MAIN MISSION VIEWPORT (Huge Cinematic Window)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                CinematicMarsViewport(
                    locations = locations,
                    selectedLocation = selectedLocation,
                    onLocationSelected = { loc ->
                        if (selectedLocation?.id != loc.id) {
                            scope.launch {
                                SoundManager.playClick()
                                delay(100)
                                SoundManager.playTargetScan()
                                delay(600)
                                viewModel.selectLocation(loc)
                                SoundManager.playTargetLock()
                                delay(300)
                                SoundManager.playLaserShot()
                            }
                        }
                    },
                    visualQuality = visualQuality,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. HORIZONTAL INFORMATION STRIP
            MissionTelemetryStrip(selectedLocation = selectedLocation)

            // 4. SCI-FI TACTICAL DASHBOARD & COMMS
            SciFiTacticalDashboard(
                selectedLocation = selectedLocation,
                onNavigateToMap = onNavigateToMap,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToMissions = onNavigateToMissions,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        // Mission Demo Dialog Walkthrough
        if (showDemoDialog) {
            MissionDemoDialog(
                currentStepIndex = demoStepIndex,
                onStepChange = { demoStepIndex = it },
                onNavigateToRoute = { route ->
                    when (route) {
                        "planner" -> onNavigateToPlanner()
                        "missions" -> onNavigateToMissions()
                        "map" -> onNavigateToMap()
                    }
                },
                onDismiss = { showDemoDialog = false }
            )
        }

        // Quality & Sound Settings Dialog
        if (showSettingsDialog) {
            ConsoleSettingsDialog(
                quality = visualQuality,
                reducedMotion = reducedMotion,
                soundEnabled = soundEnabled,
                ambienceEnabled = ambienceEnabled,
                radioEnabled = radioEnabled,
                uiEnabled = uiEnabled,
                alertsEnabled = alertsEnabled,
                onQualityChange = { qualitySettings.setQuality(it) },
                onReducedMotionChange = { qualitySettings.setReducedMotion(it) },
                onSoundToggle = { qualitySettings.setSoundEnabled(it) },
                onAmbienceToggle = { qualitySettings.setAmbienceEnabled(it) },
                onRadioToggle = { qualitySettings.setRadioEnabled(it) },
                onUiToggle = { qualitySettings.setUiEnabled(it) },
                onAlertsToggle = { qualitySettings.setAlertsEnabled(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun CommandDeckHeader(
    isOnline: Boolean,
    isCommLinkReceiving: Boolean,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val isNarrow = maxWidth < 400.dp

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MarsOrangeBright))
                        Text(
                            text = stringResource(R.string.app_name).uppercase(),
                            style = if (isNarrow) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = stringResource(R.string.mission_system_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                        Text(
                            text = "SOL 1042",
                            style = MaterialTheme.typography.labelMedium,
                            color = MarsOrangeBright,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.mission_control_link),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = {
                            onToggleSound()
                            if (!soundEnabled) SoundManager.playClick()
                        },
                        modifier = Modifier.size(44.dp).testTag("header_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute Audio" else "Enable Audio",
                            tint = if (soundEnabled) ScienceTeal else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            SoundManager.playClick()
                            onOpenSettings()
                        },
                        modifier = Modifier.size(44.dp).testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Settings",
                            tint = MarsOrangeBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            SoundManager.playClick()
                            onOpenSearch()
                        },
                        modifier = Modifier.size(44.dp).testTag("header_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Mars",
                            tint = ScienceTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusIndicator(label = "SYS", status = stringResource(R.string.sys_status_ready), color = ScienceGreen)
                StatusIndicator(label = "DATA", status = if (isOnline) stringResource(R.string.sys_status_available) else stringResource(R.string.sys_status_offline), color = if (isOnline) ScienceGreen else ScienceWarning)
                StatusIndicator(label = "AUDIO", status = if (!soundEnabled) "MUTED" else if (isCommLinkReceiving) "RX" else "READY", color = if (!soundEnabled) ScienceWarning else if (isCommLinkReceiving) ScienceGreen else ScienceTeal)
            }
        }
    }
}

@Composable
private fun StatusIndicator(label: String, status: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(
            text = "$label: $status",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MissionTelemetryStrip(selectedLocation: MarsLocation?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, SpaceBorderDark)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            val isNarrow = maxWidth < 400.dp
            
            if (isNarrow) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text(
                            text = "TARGET ACQUISITION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedLocation?.name?.uppercase() ?: "SCANNING SECTOR...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "GEOGRAPHIC COORDINATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedLocation?.coordinate?.formatShort() ?: "0.0000° N | 0.0000° E",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TARGET ACQUISITION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedLocation?.name?.uppercase() ?: "SCANNING SECTOR...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "GEOGRAPHIC COORDINATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedLocation?.coordinate?.formatShort() ?: "0.0000° N | 0.0000° E",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleSettingsDialog(
    quality: VisualQuality,
    reducedMotion: Boolean,
    soundEnabled: Boolean,
    ambienceEnabled: Boolean,
    radioEnabled: Boolean,
    uiEnabled: Boolean,
    alertsEnabled: Boolean,
    onQualityChange: (VisualQuality) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onAmbienceToggle: (Boolean) -> Unit,
    onRadioToggle: (Boolean) -> Unit,
    onUiToggle: (Boolean) -> Unit,
    onAlertsToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceDark,
        title = {
            Text(
                text = "SPACECRAFT SYSTEM CONFIG",
                style = MaterialTheme.typography.titleMedium,
                color = MarsOrangeBright,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "VISUAL RENDERING QUALITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace
                )
                VisualQuality.entries.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (quality == q) SpaceCardElevated else SpaceBlack)
                            .border(1.dp, if (quality == q) MarsOrangeBright else SpaceBorderDark, RoundedCornerShape(4.dp))
                            .clickable { onQualityChange(q) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = q.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (quality == q) MarsOrangeBright else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = q.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 8.5.sp
                            )
                        }
                        if (quality == q) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MarsOrangeBright, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = SpaceBorderDark)

                // Reduced Motion switch
                SettingsToggle(
                    label = "REDUCED MOTION",
                    subtitle = "Disables parallax & continuous animation",
                    checked = reducedMotion,
                    onCheckedChange = onReducedMotionChange
                )

                HorizontalDivider(color = SpaceBorderDark)

                Text(
                    text = "AUDIO ENGINE CONFIG",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace
                )

                // Master Sound switch
                SettingsToggle(
                    label = "MASTER AUDIO",
                    subtitle = "Global mission sound systems",
                    checked = soundEnabled,
                    onCheckedChange = onSoundToggle,
                    activeColor = ScienceTeal
                )

                // Ambience switch
                SettingsToggle(
                    label = "MISSION AMBIENCE",
                    subtitle = "Spacecraft room tone & ventilation",
                    checked = ambienceEnabled,
                    onCheckedChange = onAmbienceToggle,
                    enabled = soundEnabled
                )

                // Radio switch
                SettingsToggle(
                    label = "COMM-LINK RADIO",
                    subtitle = "Fictional mission radio chatter",
                    checked = radioEnabled,
                    onCheckedChange = onRadioToggle,
                    enabled = soundEnabled
                )

                // UI Sounds switch
                SettingsToggle(
                    label = "TACTILE INTERFACE",
                    subtitle = "UI clicks and system alerts",
                    checked = uiEnabled,
                    onCheckedChange = onUiToggle,
                    enabled = soundEnabled
                )

                // Alerts switch
                SettingsToggle(
                    label = "MISSION ALERTS",
                    subtitle = "Critical warnings and notifications",
                    checked = alertsEnabled,
                    onCheckedChange = onAlertsToggle,
                    enabled = soundEnabled
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ScienceTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("CLOSE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingsToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    activeColor: Color = MarsOrangeBright
) {
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 8.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = SpaceCardElevated
            )
        )
    }
}
