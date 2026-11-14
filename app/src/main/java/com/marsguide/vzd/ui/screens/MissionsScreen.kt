package com.marsguide.vzd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.R
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.MarsMission
import com.marsguide.vzd.domain.model.MarsRover
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    viewModel: MarsGuideViewModel,
    onNavigateToLocationByName: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val missions by viewModel.missions.collectAsState()
    val rovers by viewModel.rovers.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf(
        "All", "Rovers", "Orbiters", "Landers"
    )

    val filteredMissions = remember(selectedFilter, missions) {
        when (selectedFilter) {
            "Rovers" -> missions.filter { it.type.contains("Rover", ignoreCase = true) }
            "Orbiters" -> missions.filter { it.type.contains("Orbiter", ignoreCase = true) }
            "Landers" -> missions.filter { it.type.contains("Lander", ignoreCase = true) }
            else -> missions
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
                    Text(
                        text = "MISSION ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ContextualGuideButton(
                        guideInfo = GuideInfo(
                            title = "Mission Archive",
                            whatIsIt = "Database of historical and active spacecraft missions sent to Mars by NASA and JPL.",
                            whyItMatters = "Provides context for scientific discoveries, surface data collection, and rover traverse information.",
                            dataClass = DataClassification.STATIC_SOURCE_DERIVED,
                            source = "NASA / JPL Mission Archives"
                        )
                    )
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
            // Filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { 
                                Text(
                                    text = filter.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ScienceTeal,
                                selectedLabelColor = Color.Black,
                                containerColor = SpaceCardDark,
                                labelColor = Color.Gray
                            )
                        )
                    }
                }
            }

            // Rovers Section if "All" or "Rovers"
            if (selectedFilter == "All" || selectedFilter == "Rovers") {
                item {
                    Text(
                        text = "SURFACE ROVER RECORDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                items(rovers) { rover ->
                    RoverInstrumentCard(rover = rover, onNavigateToLocation = onNavigateToLocationByName)
                }
            }

            // General Missions Section
            item {
                Text(
                    text = "SPACECRAFT MISSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(filteredMissions) { mission ->
                MissionInstrumentCard(mission = mission)
            }
        }
    }
}

@Composable
private fun RoverInstrumentCard(
    rover: MarsRover,
    onNavigateToLocation: (String) -> Unit
) {
    TelemetryInstrumentBlock(
        title = rover.name.uppercase(),
        modifier = Modifier.clickable { com.marsguide.vzd.util.SoundManager.playRotorSound() },
        guideInfo = GuideInfo(
            title = rover.name,
            whatIsIt = "A NASA Mars Rover.",
            whyItMatters = "Surface rovers provide in-situ geological analysis and high-resolution ground imagery.",
            dataClass = DataClassification.STATIC_SOURCE_DERIVED,
            source = rover.source.name
        )
    ) {
        val isActive = rover.operationalStatus.contains("Operational", ignoreCase = true)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isActive) "ACTIVE MISSION" else "HISTORICAL MISSION",
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) ScienceGreen else MarsOrangeBright,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LANDING SITE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = rover.landingLocation.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onNavigateToLocation(rover.landingLocation) }
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = "LANDING DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = rover.landingDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Context for Location/Telemetry
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = SpaceBlack
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "RECORDED LOCATION (STATIC)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceWarning,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = rover.currentLocationName?.uppercase() ?: "UNKNOWN",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                rover.totalDistanceTraveledKm?.let {
                    Text(
                        text = "TOTAL TRAVERSE: ${String.format(java.util.Locale.US, "%,.2f km", it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionInstrumentCard(mission: MarsMission) {
    TelemetryInstrumentBlock(
        title = mission.name.uppercase(),
        guideInfo = GuideInfo(
            title = mission.name,
            whatIsIt = "A NASA Mars Spacecraft.",
            whyItMatters = "Instrumental in discovering information about Mars' surface, atmosphere, and history.",
            dataClass = DataClassification.STATIC_SOURCE_DERIVED,
            source = mission.nasaSource.name
        )
    ) {
        val isActive = mission.status.contains("Operational", ignoreCase = true)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isActive) "ACTIVE" else "COMPLETED/HISTORICAL",
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) ScienceGreen else MarsOrangeBright,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "MISSION TYPE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = mission.type.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "OBJECTIVES / DESCRIPTION",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = mission.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E6F0),
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 22.sp
        )
    }
}
