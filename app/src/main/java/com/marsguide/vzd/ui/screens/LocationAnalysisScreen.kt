package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.marsguide.vzd.R
import com.marsguide.vzd.domain.model.*
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAnalysisScreen(
    locationId: String,
    viewModel: MarsGuideViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlanner: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.collectAsState()
    val location = locations.find { it.id == locationId } ?: viewModel.selectedLocation.collectAsState().value
    val images by viewModel.imagesForLocation.collectAsState()
    val isLoadingImages by viewModel.isLoadingImages.collectAsState()
    
    val isSaved by if (location != null) {
        viewModel.isLocationSaved(location.id).collectAsState(initial = false)
    } else {
        remember { mutableStateOf(false) }
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
                            tint = ScienceTeal
                        )
                    }
                    Text(
                        text = "LOCATION ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    location?.let { loc ->
                        IconButton(onClick = { viewModel.toggleSaveLocation(loc, isSaved) }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isSaved) MarsOrangeBright else ScienceTeal
                            )
                        }
                    } ?: Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        bottomBar = {
            location?.let { loc ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SpaceCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToPlanner(loc.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MarsOrangeBright, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALCULATE ROUTE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = SpaceBlack,
        modifier = modifier
    ) { innerPadding ->
        if (location == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("LOCATION DATA UNAVAILABLE", color = ScienceWarning, fontFamily = FontFamily.Monospace)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
                        .background(SpaceCardDark)
                        .padding(16.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = location.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        ContextualGuideButton(
                            guideInfo = GuideInfo(
                                title = "Feature Type",
                                whatIsIt = "The geological classification of the Martian surface feature (e.g. Crater, Mons, Planitia).",
                                whyItMatters = "Helps categorize the geological origin and structural characteristics of the region.",
                                source = "USGS Gazetteer of Planetary Nomenclature",
                                dataClass = DataClassification.STATIC_SOURCE_DERIVED
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = location.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Coordinate Telemetry
            item {
                TelemetryInstrumentBlock(
                    title = "SPATIAL TELEMETRY",
                    guideInfo = GuideInfo(
                        title = "Coordinate Telemetry",
                        whatIsIt = "The precise IAU/IAG 2000 aerocentric coordinates and MGS MOLA elevation datum for the feature center.",
                        whyItMatters = "Critical for surface navigation, orbit calculations, and terrain height mapping.",
                        source = location.source.name,
                        dataClass = DataClassification.STATIC_SOURCE_DERIVED
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryReadout("LATITUDE", location.coordinate.formatLatitude(), modifier = Modifier.weight(1f))
                        TelemetryReadout("LONGITUDE", location.coordinate.formatLongitude(), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val elevStr = location.elevationMeters?.let { "${String.format(java.util.Locale.US, "%.0f", it)} m" } ?: "UNAVAILABLE"
                        TelemetryReadout("MOLA ELEVATION", elevStr, modifier = Modifier.weight(1f))
                        
                        val diamStr = location.diameterKm?.let { "${String.format(java.util.Locale.US, "%.1f", it)} km" } ?: "N/A"
                        TelemetryReadout("DIAMETER", diamStr, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Geological Context
            item {
                TelemetryInstrumentBlock(
                    title = "GEOLOGICAL CONTEXT",
                    guideInfo = GuideInfo(
                        title = "Geological Context",
                        whatIsIt = "Scientific overview and terrain classification of the area.",
                        whyItMatters = "Provides mission planners with information on surface hazards, material composition, and scientific value.",
                        source = location.source.name,
                        dataClass = DataClassification.STATIC_SOURCE_DERIVED
                    )
                ) {
                    Text(
                        text = "TERRAIN CLASSIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = location.terrainClassification.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    
                    Text(
                        text = "DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = location.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E6F0),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        lineHeight = 22.sp
                    )

                    Text(
                        text = "GEOLOGICAL SIGNIFICANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = location.geologicalContext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E6F0),
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            // NASA Imagery
            item {
                TelemetryInstrumentBlock(
                    title = "NASA ARCHIVE IMAGERY",
                    guideInfo = GuideInfo(
                        title = "NASA Archive Imagery",
                        whatIsIt = "Photographic evidence and orbital survey data relating to this Martian region.",
                        whyItMatters = "Provides visual confirmation of terrain features.",
                        howCalculated = "When online, queries the live NASA Image and Video Library. If offline or unavailable, uses static curated image fallback.",
                        dataClass = DataClassification.REAL_LIVE_API,
                        source = "NASA Image and Video Library API"
                    )
                ) {
                    if (isLoadingImages) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ScienceTeal, modifier = Modifier.size(24.dp))
                        }
                    } else if (images.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(SpaceBlack), contentAlignment = Alignment.Center) {
                            Text("NO IMAGERY AVAILABLE IN ARCHIVE", color = Color.Gray, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        images.forEach { img ->
                            NasaImageBlock(img)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NasaImageBlock(img: MarsImage) {
    val isLiveApi = img.source.id.startsWith("NASA_IMAGE_ARCHIVE")
    val badgeColor = if (isLiveApi) ScienceGreen else MarsOrangeBright
    val badgeText = if (isLiveApi) "LIVE NASA ARCHIVE" else "OFFLINE CURATED RECORD"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
            .background(SpaceBlack)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(img.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = img.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Surface(
                color = badgeColor.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp
                )
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = img.title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            img.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 3
                )
            }
            img.earthDate?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DATE: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
