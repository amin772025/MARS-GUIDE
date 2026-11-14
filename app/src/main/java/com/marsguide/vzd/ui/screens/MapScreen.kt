package com.marsguide.vzd.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.domain.model.*
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel
import com.marsguide.vzd.util.MarsMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MarsGuideViewModel,
    onNavigateToLocation: (String) -> Unit,
    onNavigateToPlannerWithStart: (String) -> Unit,
    onSwitchToViewport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.collectAsState()
    val rovers by viewModel.rovers.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val scienceTargets by viewModel.scienceTargets.collectAsState()
    val plannedRoute by viewModel.plannedRoute.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedCoordinate by viewModel.selectedCoordinate.collectAsState()

    var showTopography by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var showFeatures by remember { mutableStateOf(true) }
    var showLandingSites by remember { mutableStateOf(true) }
    var showRovers by remember { mutableStateOf(true) }
    var showScienceTargets by remember { mutableStateOf(true) }
    var showRoute by remember { mutableStateOf(true) }
    var is3DGlobeMode by remember { mutableStateOf(true) }

    var showLayersSheet by remember { mutableStateOf(false) }

    val currentCoord = selectedLocation?.coordinate ?: selectedCoordinate

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // 1. Central Mars Interactive Canvas (3D Globe or Flat Equirectangular)
        if (is3DGlobeMode) {
            MarsGlobe3D(
                locations = locations,
                selectedLocation = selectedLocation,
                onLocationSelected = { loc -> viewModel.selectLocation(loc) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MarsMapCanvas(
                locations = locations,
                rovers = rovers,
                scienceTargets = scienceTargets,
                missions = missions,
                activeRoute = plannedRoute,
                selectedCoordinate = selectedCoordinate,
                selectedLocation = selectedLocation,
                onLocationSelected = { loc -> viewModel.selectLocation(loc) },
                onCoordinateSelected = { coord -> viewModel.selectCoordinate(coord) },
                showBaseLayer = true,
                showTopographyLayer = showTopography,
                showGridLayer = showGrid,
                showFeaturesLayer = showFeatures,
                showLandingSitesLayer = showLandingSites,
                showRoversLayer = showRovers,
                showScienceTargetsLayer = showScienceTargets,
                showRouteLayer = showRoute,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. HUD Top Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter)
                .widthIn(max = 700.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
            color = SpaceCardDark.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SURFACE NAV",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentCoord?.formatShort() ?: "STANDBY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick switch to cinematic viewport
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MarsOrangeBright.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable { onSwitchToViewport() }
                            .padding(horizontal = 8.dp),
                        color = SpaceBlack
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Switch to Viewport",
                                tint = MarsOrangeBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "VIEWPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MarsOrangeBright,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    // Toggle 3D Globe / 2D Flat
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, ScienceTeal.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable {
                                com.marsguide.vzd.util.SoundManager.playClick()
                                is3DGlobeMode = !is3DGlobeMode
                            }
                            .padding(horizontal = 8.dp),
                        color = if (is3DGlobeMode) ScienceTeal.copy(alpha = 0.2f) else SpaceBlack
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (is3DGlobeMode) Icons.Default.Public else Icons.Default.Map,
                                contentDescription = "Toggle 3D Globe",
                                tint = if (is3DGlobeMode) ScienceTeal else Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (is3DGlobeMode) "3D GLOBE" else "2D FLAT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (is3DGlobeMode) ScienceTeal else Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    ContextualGuideButton(
                        guideInfo = GuideInfo(
                            title = "Map Coordinate System",
                            whatIsIt = "The map displays Mars using Planetocentric Latitude (-90° to 90°) and East Longitude (0° to 360°).",
                            whyItMatters = "Coordinates are essential for accurately pinpointing surface features, calculating distances, and determining trajectories.",
                            dataClass = DataClassification.STATIC_SOURCE_DERIVED,
                            source = "IAU/IAG 2000"
                        )
                    )
                    
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, ScienceTeal.copy(alpha = 0.5f), CircleShape)
                            .clickable { showLayersSheet = true },
                        color = ScienceTeal.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Map Layers",
                                tint = ScienceTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Selection HUD
        AnimatedVisibility(
            visible = currentCoord != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 600.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
                color = SpaceCardDark.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARGET SELECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MarsOrangeBright,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (selectedLocation != null) {
                            Text(
                                text = selectedLocation!!.type.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ScienceTeal,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "CUSTOM COORDINATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (selectedLocation != null) {
                        Text(
                            text = selectedLocation!!.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToLocation(selectedLocation!!.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ScienceTeal, contentColor = Color.Black)
                            ) {
                                Text(
                                    "ANALYZE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { onNavigateToPlannerWithStart(selectedLocation!!.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceCardElevated, contentColor = MarsOrangeBright)
                            ) {
                                Text(
                                    "ROUTE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (selectedCoordinate != null) {
                        Text(
                            text = selectedCoordinate!!.formatLatitude() + " " + selectedCoordinate!!.formatLongitude(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToPlannerWithStart("custom_${selectedCoordinate!!.latitude}_${selectedCoordinate!!.longitude}") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpaceCardElevated, contentColor = MarsOrangeBright)
                        ) {
                            Text(
                                "PLAN ROUTE FROM HERE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Layer Controls Sheet
        if (showLayersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLayersSheet = false },
                containerColor = SpaceCardDark,
                dragHandle = { BottomSheetDefaults.DragHandle(color = ScienceTeal) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM LAYERS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        ContextualGuideButton(
                            guideInfo = GuideInfo(
                                title = "Map Layers",
                                whatIsIt = "Scientific and geological datasets rendered over the Martian sphere.",
                                whyItMatters = "Overlaying different data sources allows for comprehensive mission planning and contextual analysis.",
                                dataClass = DataClassification.STATIC_SOURCE_DERIVED
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ConsoleSwitchRow("Scientific Topography", "Simulated MOLA crustal dichotomy", showTopography) { showTopography = it }
                    ConsoleSwitchRow("Graticule", "IAU coordinate grid", showGrid) { showGrid = it }
                    ConsoleSwitchRow("Geological Features", "USGS recognized regions", showFeatures) { showFeatures = it }
                    ConsoleSwitchRow("Landing Sites", "Historic spacecraft landings", showLandingSites) { showLandingSites = it }
                    ConsoleSwitchRow("Rover Positions", "Recorded surface telemetry", showRovers) { showRovers = it }
                    ConsoleSwitchRow("Science Targets", "Curated geological areas of interest", showScienceTargets) { showScienceTargets = it }
                    ConsoleSwitchRow("Active Route", "Planned surface trajectory", showRoute) { showRoute = it }
                }
            }
        }
    }
}

@Composable
private fun ConsoleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (checked) Color.White else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = ScienceTeal,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = SpaceCardElevated
            )
        )
    }
}
