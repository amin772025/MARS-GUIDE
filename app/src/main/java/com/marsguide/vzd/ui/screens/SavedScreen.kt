package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: MarsGuideViewModel,
    onNavigateToLocation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedLocations by viewModel.savedLocations.collectAsState()
    val savedRoutes by viewModel.savedRoutes.collectAsState()
    var activeTab by remember { mutableStateOf(0) } // 0 = Locations, 1 = Routes

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = SpaceBlack
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MISSION ARCHIVE // SAVED DATA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MarsOrangeBright,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ContextualGuideButton(
                            guideInfo = GuideInfo(
                                title = "Mission Archives",
                                whatIsIt = "Local persistent database storing your bookmarked locations and planned geodesic trajectories.",
                                whyItMatters = "Allows offline review and navigation planning without continuous internet queries.",
                                dataClass = DataClassification.APPLICATION_CALCULATED
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (activeTab == 0) ScienceTeal else SpaceBorderDark,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { activeTab = 0 },
                            color = if (activeTab == 0) ScienceTeal.copy(alpha = 0.2f) else SpaceCardDark
                        ) {
                            Text(
                                text = "LOCATIONS (${savedLocations.size})",
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .align(Alignment.CenterVertically),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTab == 0) ScienceTeal else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (activeTab == 1) ScienceTeal else SpaceBorderDark,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { activeTab = 1 },
                            color = if (activeTab == 1) ScienceTeal.copy(alpha = 0.2f) else SpaceCardDark
                        ) {
                            Text(
                                text = "ROUTES (${savedRoutes.size})",
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .align(Alignment.CenterVertically),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTab == 1) ScienceTeal else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = SpaceBlack,
        modifier = modifier.testTag("saved_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab == 0) {
                if (savedLocations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO BOOKMARKED LOCATIONS\nTap bookmark icon on Location Analysis to save",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedLocations, key = { it.id }) { loc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
                                    .clickable { onNavigateToLocation(loc.id) },
                                color = SpaceCardDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = loc.name.uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${loc.type} • Lat: ${loc.latitude}°, Lon: ${loc.longitude}°",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ScienceTeal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedLocation(loc.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete bookmark",
                                            tint = ScienceCritical.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (savedRoutes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO SAVED TRAJECTORIES\nPlan a route and tap SAVE TRAJECTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedRoutes, key = { it.id }) { route ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
                                color = SpaceCardDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = route.name.uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MarsOrangeBright,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "DIST: ${String.format(java.util.Locale.US, "%,.1f km", route.distanceKm)} • BRG: ${String.format(java.util.Locale.US, "%.1f°", route.initialBearingDegrees)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ScienceTeal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedRoute(route.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete trajectory",
                                            tint = ScienceCritical.copy(alpha = 0.7f)
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
