package com.marsguide.vzd.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

data class TourStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val description: String,
    val provenanceNote: String
)

@Composable
fun GuidedTourDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        TourStep(
            stepNumber = 1,
            title = "MARS MISSION CONSOLE",
            subtitle = "OPERATIONAL SCIENTIFIC INTERFACE",
            icon = Icons.Default.Dashboard,
            description = "Welcome to MARS//GUIDE. This is not a toy or generic mobile app—it is an operational scientific navigation instrument designed for Martian surface exploration and mission planning.",
            provenanceNote = "STATUS: INITIALIZED // PROTOCOL: NASA SPACE APPS 2026"
        ),
        TourStep(
            stepNumber = 2,
            title = "PROCEDURAL MARS MAP",
            subtitle = "CARTOGRAPHIC VISUALIZATION",
            icon = Icons.Default.Map,
            description = "Explore Martian geography across 8 interactive layers. Note: The basemap is a scientific procedural visualization, NOT a real-time continuous MOLA DEM raster lookup.",
            provenanceNote = "LIMITATION TRANSPARENCY: Procedural surface rendering"
        ),
        TourStep(
            stepNumber = 3,
            title = "TAP ANYTHING TO LEARN",
            subtitle = "CONTEXTUAL MISSION GUIDE",
            icon = Icons.Default.Info,
            description = "Look for the ⓘ GUIDE button beside any instrument or telemetry reading. Tapping it reveals what the value is, how it was calculated, and why it matters to planetary science.",
            provenanceNote = "PHILOSOPHY: EXPLORE ➔ UNDERSTAND ➔ ANALYZE ➔ PLAN"
        ),
        TourStep(
            stepNumber = 4,
            title = "GREAT-CIRCLE GEODESY",
            subtitle = "SURFACE TRAJECTORY PLANNER",
            icon = Icons.Default.Route,
            description = "Calculate spherical great-circle distances and bearings between Martian features using Mars mean radius (3,389.5 km). Elevation profiles are mathematically interpolated estimates.",
            provenanceNote = "DATA CLASS: APPLICATION-CALCULATED // ESTIMATED PROFILE"
        ),
        TourStep(
            stepNumber = 5,
            title = "LIVE NASA ARCHIVES",
            subtitle = "AUTHENTIC PLANETARY DATASETS",
            icon = Icons.Default.CloudSync,
            description = "Connects to public NASA Image & Video Library endpoints for real observation photography, supported by offline-first curated NASA PDS and USGS Astrogeology records.",
            provenanceNote = "DATA CLASS: REAL LIVE API + STATIC SOURCE-DERIVED"
        ),
        TourStep(
            stepNumber = 6,
            title = "DATA PROVENANCE BADGES",
            subtitle = "SCIENTIFIC INTEGRITY SYSTEM",
            icon = Icons.Default.Verified,
            description = "Every value in MARS//GUIDE carries an honest classification badge: Real Live API, Static Source-Derived, Application-Calculated, or Estimated. We never fabricate telemetry.",
            provenanceNote = "SYSTEM INTEGRITY: ZERO FAKE TELEMETRY GUARANTEE"
        )
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MarsOrangeBright, RoundedCornerShape(8.dp))
                .testTag("guided_tour_dialog"),
            color = SpaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Step indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM ORIENTATION // STEP ${currentStep.stepNumber} OF ${steps.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "SKIP",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                // Progress bar
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    if (index <= currentStepIndex) MarsOrangeBright else SpaceBorderDark
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tour_content"
                ) { step ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, MarsOrangeBright.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                                color = SpaceBlack
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = MarsOrangeBright,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScienceTeal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1D5DB),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
                            color = SpaceBlack
                        ) {
                            Text(
                                text = step.provenanceNote,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = "BACK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(10.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MarsOrangeBright,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (currentStepIndex == steps.size - 1) "COMPLETE PROTOCOL" else "NEXT ➔",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
