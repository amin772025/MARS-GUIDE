package com.marsguide.vzd.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.SoundManager

data class DemoStep(
    val stepIndex: Int,
    val title: String,
    val phase: String,
    val icon: ImageVector,
    val description: String,
    val technicalNote: String,
    val targetScreenRoute: String? = null
)

val MISSION_DEMO_STEPS = listOf(
    DemoStep(
        stepIndex = 1,
        title = "INITIALIZATION & BOOT SEQUENCE",
        phase = "PHASE 1: SYSTEM VERIFICATION",
        icon = Icons.Default.PowerSettingsNew,
        description = "Verification of core Martian navigation subsystems: MOLA Areoid datum baseline (3,396.19 km), NASA PDS records, Curiosity/Perseverance mission archives, offline cache, and NASA Live API Gateway.",
        technicalNote = "PROVENANCE: SYSTEM HARDWARE & DATA LINK SELF-TEST"
    ),
    DemoStep(
        stepIndex = 2,
        title = "MISSION CONSOLE & SPACECRAFT HABITAT",
        phase = "PHASE 2: HABITAT INTERFACE",
        icon = Icons.Default.Dashboard,
        description = "The mission control room. Built with dark aerospace carbon walls, illuminated status LEDs, technical panel framing, and tactile aerospace buttons designed for surface exploration.",
        technicalNote = "UI ARCHITECTURE: TACTILE CONTROLS // MATERIAL 3 INTEGRATION"
    ),
    DemoStep(
        stepIndex = 3,
        title = "CINEMATIC MARS VIEWPORT & HORIZON",
        phase = "PHASE 3: PLANETARY VIEWPORT",
        icon = Icons.Default.Visibility,
        description = "Procedural 8K-style Mars rendering with atmospheric Rayleigh scattering limb, Olympus Mons, Valles Marineris rift, polar residual ice caps, multi-layer starfield, and surface horizon perspectives.",
        technicalNote = "RENDERING: PROCEDURAL CANVAS // MULTI-LAYER PARALLAX // VISUAL AID"
    ),
    DemoStep(
        stepIndex = 4,
        title = "FEATURE TARGETING & HUD CALLOUT",
        phase = "PHASE 4: ACQUISITION",
        icon = Icons.Default.GpsFixed,
        description = "Lock onto catalogued Martian geological landmarks like Olympus Mons or Jezero Crater. Displays animated targeting brackets, MOLA elevation, exact IAU coordinates, and scientific significance.",
        technicalNote = "TARGETING: USGS ASTROGEOLOGY GAZETTEER INTEGRATION"
    ),
    DemoStep(
        stepIndex = 5,
        title = "CONTEXTUAL MISSION GUIDE (TAP TO LEARN)",
        phase = "PHASE 5: SCIENCE PRIMER",
        icon = Icons.Default.School,
        description = "The core educational philosophy: Every single telemetry value, elevation number, and coordinate in MARS//GUIDE has a contextual ⓘ GUIDE explaining WHAT it is, HOW it is calculated, and WHY it matters.",
        technicalNote = "FEATURE: CONTEXTUAL BOTTOM SHEETS ON ALL TELEMETRY READOUTS"
    ),
    DemoStep(
        stepIndex = 6,
        title = "SCIENTIFIC LOCATION ANALYSIS",
        phase = "PHASE 6: IN-DEPTH GEODESY",
        icon = Icons.Default.Analytics,
        description = "Deep planetary analysis across 6 dedicated tabs: Planetary Overview, Topography & MOLA elevation, Geological & Scientific Context, Nearby Rovers & Landers, NASA Orbital Imagery, and Official Sources.",
        technicalNote = "DATA INTEGRITY: RAW DATA & CITATION TRANSPARENCY",
        targetScreenRoute = "explore"
    ),
    DemoStep(
        stepIndex = 7,
        title = "ASTROBIOLOGY SCIENCE TARGETS",
        phase = "PHASE 7: BIOSIGNATURE EXPLORATION",
        icon = Icons.Default.Biotech,
        description = "Explores high-priority Martian astrobiological zones including ancient lacustrine fan deltas (Jezero Crater), hydrothermal mineral deposits (Gusev Crater), and stratified clay-sulfate mounds (Mount Sharp).",
        technicalNote = "SOURCE: NASA MARS 2020 & MSL SCIENCE TEAM PUBLICATIONS"
    ),
    DemoStep(
        stepIndex = 8,
        title = "SURFACE ROUTE PLANNER & GEODESY",
        phase = "PHASE 8: TRAJECTORY PLANNING",
        icon = Icons.Default.Route,
        description = "Calculates authentic spherical great-circle distances and initial true bearings across the Martian sphere (R = 3,389.5 km). Explicitly alerts users that waypoint elevations are linear estimates, not a continuous DEM raster.",
        technicalNote = "HONEST SCIENCE: APPLICATION-CALCULATED GEODESIC WITH DISCLAIMER",
        targetScreenRoute = "planner"
    ),
    DemoStep(
        stepIndex = 9,
        title = "LIVE NASA IMAGERY ARCHIVE",
        phase = "PHASE 9: OBSERVATION ARCHIVES",
        icon = Icons.Default.Image,
        description = "Integrates with live NASA public image repositories for authentic surface photography from Mastcam-Z, Navcam, and orbital MRO HiRISE captures, backed by seamless offline fallback.",
        technicalNote = "DATA CLASS: REAL LIVE API + CURATED OFFLINE CACHE",
        targetScreenRoute = "missions"
    ),
    DemoStep(
        stepIndex = 10,
        title = "DATA PROVENANCE & SCIENTIFIC INTEGRITY",
        phase = "PHASE 10: ZERO FAKE TELEMETRY",
        icon = Icons.Default.Verified,
        description = "Every value in MARS//GUIDE carries an honest classification badge: Real Live API, Static Source-Derived, Application-Calculated, or Estimated. We never fabricate physical sensor telemetry.",
        technicalNote = "COMMITMENT: 100% FACTUAL TRANSPARENCY & ATTRIBUTION",
        targetScreenRoute = "about"
    )
)

@Composable
fun MissionDemoDialog(
    currentStepIndex: Int,
    onStepChange: (Int) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val step = MISSION_DEMO_STEPS[currentStepIndex.coerceIn(0, MISSION_DEMO_STEPS.lastIndex)]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.5.dp, MarsOrangeBright, RoundedCornerShape(8.dp))
                .testTag("mission_demo_dialog"),
            color = SpaceDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MarsOrangeBright)
                        )
                        Text(
                            text = "MISSION DEMO // ${step.phase}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MarsOrangeBright,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Text(
                        text = "${step.stepIndex} / ${MISSION_DEMO_STEPS.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SpaceBorderDark)
                Spacer(modifier = Modifier.height(14.dp))

                // Title & Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                        color = SpaceCardElevated
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = ScienceTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCAD0E0),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Technical note
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(SpaceBlack)
                        .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = step.technicalNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Step
                    OutlinedButton(
                        onClick = {
                            if (currentStepIndex > 0) {
                                SoundManager.playClick()
                                onStepChange(currentStepIndex - 1)
                            }
                        },
                        enabled = currentStepIndex > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderDark)
                    ) {
                        Text("PREV", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Next Step or Finish
                    if (currentStepIndex < MISSION_DEMO_STEPS.lastIndex) {
                        Button(
                            onClick = {
                                val nextIdx = currentStepIndex + 1
                                when (nextIdx) {
                                    1 -> SoundManager.playNavigationClick()
                                    3 -> SoundManager.playTargetScan()
                                    5 -> SoundManager.playTargetLock()
                                    8 -> SoundManager.playMissionConfirm()
                                    10 -> SoundManager.playDataSync()
                                    else -> SoundManager.playClick()
                                }
                                onStepChange(nextIdx)
                                MISSION_DEMO_STEPS[nextIdx].targetScreenRoute?.let { onNavigateToRoute(it) }
                            },
                            modifier = Modifier.weight(1.5f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MarsOrangeBright,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("NEXT STEP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                SoundManager.playMissionConfirm()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.5f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScienceGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("FINISH DEMO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Exit Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("EXIT", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
