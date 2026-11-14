package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.R
import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.ElevationSource
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.model.MarsLocation
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel
import com.marsguide.vzd.util.MarsMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    viewModel: MarsGuideViewModel,
    initialStartId: String? = null,
    initialDestId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.collectAsState()
    val plannerStart by viewModel.plannerStart.collectAsState()
    val plannerDest by viewModel.plannerDest.collectAsState()
    val plannedRoute by viewModel.plannedRoute.collectAsState()

    LaunchedEffect(initialStartId, locations) {
        if (initialStartId != null) {
            if (initialStartId.startsWith("custom_")) {
                val parts = initialStartId.removePrefix("custom_").split("_")
                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                val lon = parts.getOrNull(1)?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    val coord = MarsCoordinate(lat.coerceIn(-90.0, 90.0), ((lon % 360.0) + 360.0) % 360.0)
                    val customLoc = MarsLocation(
                        id = initialStartId,
                        name = "Custom Target (${coord.formatShort()})",
                        type = "Custom Coordinate",
                        coordinate = coord,
                        description = "User-selected surface coordinate from planetary map.",
                        geologicalContext = "Planetocentric surface point selected for geodesic trajectory.",
                        terrainClassification = "Unclassified Surface Point",
                        pdsDatasetId = "IAU-IAG-2000",
                        source = AuthenticMarsDatasets.USGS_GAZETTEER_SOURCE
                    )
                    viewModel.setPlannerStart(customLoc)
                }
            } else {
                locations.find { it.id == initialStartId }?.let { viewModel.setPlannerStart(it) }
            }
        }
    }
    LaunchedEffect(initialDestId, locations) {
        if (initialDestId != null) {
            locations.find { it.id == initialDestId }?.let { viewModel.setPlannerDest(it) }
        }
    }

    var startExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(plannedRoute) {
        if (plannedRoute != null) {
            com.marsguide.vzd.util.SoundManager.playDrivingNoise()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = SpaceBlack
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MarsOrangeBright
                        )
                    }
                    Text(
                        text = "SURFACE ROUTE PLANNER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        containerColor = SpaceBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Point Selector Console
            item {
                TelemetryInstrumentBlock(
                    title = "TRAJECTORY SETUP",
                    guideInfo = GuideInfo(
                        title = "Trajectory Setup",
                        whatIsIt = "Defines the origin and destination coordinates on the Martian surface.",
                        whyItMatters = "Necessary to calculate the great-circle path, heading, and distance between two features."
                    )
                ) {
                    // Origin
                    Text(
                        text = "ORIGIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, if (startExpanded) ScienceTeal else SpaceBorderDark, RoundedCornerShape(4.dp))
                                .clickable { startExpanded = true },
                            color = SpaceBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plannerStart?.name?.uppercase() ?: "[ SELECT ORIGIN ]",
                                    color = if (plannerStart != null) Color.White else MarsOrangeBright,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MarsOrangeBright)
                            }
                        }
                        DropdownMenu(
                            expanded = startExpanded,
                            onDismissRequest = { startExpanded = false },
                            modifier = Modifier.background(SpaceCardDark).border(1.dp, SpaceBorderDark)
                        ) {
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = loc.name.uppercase(), 
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setPlannerStart(loc)
                                        startExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    plannerStart?.let { start ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = start.coordinate.formatShort(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ScienceTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            ElevationBadge(elevationMeters = start.elevationMeters)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Destination
                    Text(
                        text = "DESTINATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, if (destExpanded) ScienceTeal else SpaceBorderDark, RoundedCornerShape(4.dp))
                                .clickable { destExpanded = true },
                            color = SpaceBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plannerDest?.name?.uppercase() ?: "[ SELECT DESTINATION ]",
                                    color = if (plannerDest != null) Color.White else MarsOrangeBright,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MarsOrangeBright)
                            }
                        }
                        DropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false },
                            modifier = Modifier.background(SpaceCardDark).border(1.dp, SpaceBorderDark)
                        ) {
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = loc.name.uppercase(), 
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setPlannerDest(loc)
                                        destExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    plannerDest?.let { dest ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dest.coordinate.formatShort(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ScienceTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            ElevationBadge(elevationMeters = dest.elevationMeters)
                        }
                    }
                }
            }

            // 2. Trajectory Calculation Results
            item {
                plannedRoute?.let { route ->
                    TelemetryInstrumentBlock(
                        title = "TRAJECTORY TELEMETRY",
                        guideInfo = GuideInfo(
                            title = "Geodesic Trajectory",
                            whatIsIt = "The shortest path between the origin and destination across the surface of Mars (Great-Circle Distance).",
                            whyItMatters = "Essential for determining travel time, fuel requirements, and navigation heading.",
                            howCalculated = "Calculated locally using spherical trigonometry based on the mean Martian radius of 3,389.5 km.",
                            dataClass = DataClassification.APPLICATION_CALCULATED
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val distStr = String.format(java.util.Locale.US, "%,.1f km", route.distanceKm)
                            TelemetryReadout("DISTANCE", distStr, modifier = Modifier.weight(1f))
                            
                            val bearStr = String.format(java.util.Locale.US, "%.1f°", route.initialBearingDegrees)
                            TelemetryReadout("INITIAL BEARING", bearStr, modifier = Modifier.weight(1f))
                        }

                        route.elevationChangeMeters?.let { diff ->
                            Spacer(modifier = Modifier.height(12.dp))
                            val diffPrefix = if (diff >= 0) "+" else ""
                            val diffStr = "$diffPrefix${String.format(java.util.Locale.US, "%,.0f", diff)} m"
                            TelemetryReadout("NET ELEVATION DELTA", diffStr, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.saveCurrentRoute()
                                saveSuccessMessage = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MarsOrangeBright, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (saveSuccessMessage) "TRAJECTORY SAVED TO DATABANKS" else "SAVE TRAJECTORY",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Terrain Profile (Estimated)
            item {
                val startElev = plannerStart?.elevationMeters
                val destElev = plannerDest?.elevationMeters
                val route = plannedRoute
                if (route != null && startElev != null && destElev != null) {
                    TelemetryInstrumentBlock(
                        title = "TERRAIN PROFILE",
                        guideInfo = GuideInfo(
                            title = "Elevation Profile",
                            whatIsIt = "A mathematical interpolation of elevation between the start and end points.",
                            whyItMatters = "Provides a rough estimate of the terrain grade, but is not guaranteed accurate.",
                            howCalculated = "This profile is mathematically interpolated between available endpoint values. It is NOT sampled from a continuous MOLA DEM.",
                            dataClass = DataClassification.ESTIMATED_APPROXIMATION
                        )
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, ScienceWarning.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            color = SpaceBlack
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ELEVATION: INTERPOLATED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScienceWarning,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%,.0f m", startElev)} → ${String.format(java.util.Locale.US, "%,.0f m", destElev)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScienceTeal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    )
                                }

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val steps = 20
                                    val minE = kotlin.math.min(startElev, destElev) - 500.0
                                    val maxE = kotlin.math.max(startElev, destElev) + 500.0
                                    val span = (maxE - minE).coerceAtLeast(1.0)

                                    // Gridlines
                                    for (g in 0..2) {
                                        val gy = h * g / 2f
                                        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, gy), Offset(w, gy), 1f)
                                    }

                                    val linePath = Path()
                                    val fillPath = Path()
                                    for (i in 0..steps) {
                                        val t = i.toFloat() / steps
                                        // Smooth S-curve interpolation between endpoint elevations
                                        val s = t * t * (3f - 2f * t)
                                        val elev = startElev + (destElev - startElev) * s
                                        val normY = ((elev - minE) / span).toFloat().coerceIn(0.05f, 0.95f)
                                        val px = w * t
                                        val py = h * (1f - normY)
                                        if (i == 0) {
                                            linePath.moveTo(px, py)
                                            fillPath.moveTo(px, h)
                                            fillPath.lineTo(px, py)
                                        } else {
                                            linePath.lineTo(px, py)
                                            fillPath.lineTo(px, py)
                                        }
                                        if (i == steps) {
                                            fillPath.lineTo(px, h)
                                            fillPath.close()
                                        }
                                    }
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            listOf(ScienceWarning.copy(alpha = 0.35f), Color.Transparent)
                                        )
                                    )
                                    drawPath(
                                        path = linePath,
                                        color = ScienceWarning,
                                        style = Stroke(width = 2.5f)
                                    )
                                }

                                Text(
                                    text = "ESTIMATED APPROXIMATION: Not sampled from continuous MOLA DEM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
