package com.marsguide.vzd.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.marsguide.vzd.R
import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.domain.model.MarsImage
import com.marsguide.vzd.domain.model.MarsLocation
import com.marsguide.vzd.domain.model.MarsPanorama
import com.marsguide.vzd.domain.model.MarsEnvironmentalConditions
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.MarsMath
import com.marsguide.vzd.util.SoundManager
import com.marsguide.vzd.util.VisualQuality
import kotlin.math.*
import kotlin.random.Random

enum class ViewportPerspective(val labelRes: Int) {
    ORBITAL(R.string.viewport_orbital),
    HORIZON(R.string.viewport_horizon),
    PANORAMA(R.string.panorama_enter)
}

/**
 * High-fidelity Mars Cinematic Viewport.
 * Features an Interactive 3D Mars Globe in Orbital mode,
 * detailed surface reconnaissance in Horizon mode,
 * and immersive 360° landed rover panoramas when authentic NASA mosaics exist.
 */
@Composable
fun CinematicMarsViewport(
    locations: List<MarsLocation>,
    selectedLocation: MarsLocation?,
    onLocationSelected: (MarsLocation) -> Unit,
    visualQuality: VisualQuality = VisualQuality.HIGH,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    var perspective by remember { mutableStateOf(ViewportPerspective.ORBITAL) }
    var activePanorama by remember { mutableStateOf<MarsPanorama?>(null) }

    // When selectedLocation changes, check if panorama is available
    val locationPanorama: MarsPanorama? = remember(selectedLocation) {
        val loc = selectedLocation
        if (loc != null) AuthenticMarsDatasets.getPanoramaForLocation(loc.id) else null
    }

    // Safety guard against null panorama without mutating state in composition
    LaunchedEffect(perspective, activePanorama) {
        if (perspective == ViewportPerspective.PANORAMA && activePanorama == null) {
            perspective = ViewportPerspective.ORBITAL
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceBlack)
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(12.dp))
            .testTag("cinematic_viewport")
    ) {
        // Perspective Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(SpaceCardElevated, SpaceBlack)))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MarsOrangeBright))
                Text(
                    text = when (perspective) {
                        ViewportPerspective.ORBITAL -> "3D MARS GLOBE // ORBITAL FEED"
                        ViewportPerspective.HORIZON -> "SURFACE RECONNAISSANCE // LOCAL SECTOR"
                        ViewportPerspective.PANORAMA -> "AUTHENTIC 360° ROVER PANORAMA"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Mode Selector Tabs
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
                color = SpaceDark
            ) {
                Row {
                    listOf(ViewportPerspective.ORBITAL, ViewportPerspective.HORIZON).forEach { p ->
                        val isSelected = perspective == p
                        Box(
                            modifier = Modifier
                                .clickable {
                                    SoundManager.playClick()
                                    perspective = p
                                }
                                .background(if (isSelected) MarsOrangeBright.copy(alpha = 0.2f) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(p.labelRes).split(" ").first().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isSelected) MarsOrangeBright else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Main Dynamic Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .background(Color.Black)
        ) {
            when (perspective) {
                ViewportPerspective.ORBITAL -> {
                    MarsGlobe3D(
                        locations = locations,
                        selectedLocation = selectedLocation,
                        onLocationSelected = onLocationSelected,
                        visualQuality = visualQuality,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ViewportPerspective.HORIZON -> {
                    SurfaceInspectionView(
                        location = selectedLocation,
                        panorama = locationPanorama,
                        onEnterPanorama = { pano ->
                            activePanorama = pano
                            perspective = ViewportPerspective.PANORAMA
                        },
                        onReturnToOrbit = {
                            perspective = ViewportPerspective.ORBITAL
                        }
                    )
                }
                ViewportPerspective.PANORAMA -> {
                    val pano = activePanorama
                    if (pano != null) {
                        MarsPanoramaViewer(
                            panorama = pano,
                            onExit = {
                                perspective = ViewportPerspective.HORIZON
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Detailed Surface Inspection View with contextual telemetry and direct panorama/imagery launch.
 */
@Composable
private fun SurfaceInspectionView(
    location: MarsLocation?,
    panorama: MarsPanorama?,
    onEnterPanorama: (MarsPanorama) -> Unit,
    onReturnToOrbit: () -> Unit
) {
    val context = LocalContext.current
    val conditions: MarsEnvironmentalConditions? = remember(location) {
        val loc = location
        if (loc != null) MarsMath.calculateContextualMarsConditions(loc.coordinate) else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Surface Background Image
        Image(
            painter = painterResource(id = R.drawable.mars_surface_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Return & Location Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        SoundManager.playBack()
                        onReturnToOrbit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceDark),
                    border = BorderStroke(1.dp, SpaceBorderDark),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Orbit", tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ORBIT VIEW", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }

                if (location != null) {
                    DataProvenanceBadge(
                        classification = DataClassification.STATIC_SOURCE_DERIVED
                    )
                }
            }

            if (location != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SpaceDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SpaceBorderDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = location.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MarsOrangeBright,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "TYPE: ${location.type.uppercase()} | ${location.coordinate.formatShort()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Bottom Surface Reconnaissance Card (Conditions & Panorama CTA)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Surface(
                color = SpaceDark.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SpaceBorderDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Environmental Conditions Readout
                    if (conditions != null && location != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ESTIMATED SURFACE TEMP", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f°C", conditions.temperatureCelsius),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("ATMOSPHERIC PRESSURE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.0f Pa (%.2f mbar)", conditions.surfacePressurePa, conditions.surfacePressurePa / 100.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ScienceTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("ELEVATION (MOLA)", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = String.format(java.util.Locale.US, "%,.0f m", location.coordinate.elevation ?: 0.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MarsOrangeBright,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Panorama Launch or Honest "No Landed Panorama" Notice
                    if (panorama != null) {
                        Button(
                            onClick = {
                                onEnterPanorama(panorama)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MarsOrangeBright),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PanoramaHorizontal, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ENTER 360° ROVER PANORAMA (${panorama.roverName.uppercase()} SOL ${panorama.sol})",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, SpaceBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ScienceTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ORBITAL RECONNAISSANCE ONLY // NO LANDED ROVER AT THIS SITE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * NASA Media Reconnaissance Panel.
 * Displays high-quality mission imagery from NASA PDS/Archive.
 */
@Composable
fun MissionMediaReconPanel(
    selectedLocation: MarsLocation?,
    nasaImage: MarsImage?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SpaceCardDark)
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MarsOrangeBright))
                Text(
                    text = stringResource(R.string.target_recon).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (nasaImage != null) {
                Surface(
                    color = ScienceGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(0.5.dp, ScienceGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "NASA IMAGE LIBRARY",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = ScienceGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Imagery Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
            color = SpaceDark
        ) {
            if (nasaImage != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(nasaImage.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = nasaImage.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Provenance Box
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = nasaImage.source.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NASA SOURCE DATA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 7.sp,
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MARS//GUIDE: ACQUIRING MISSION MEDIA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        if (selectedLocation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GEOLOGICAL CLASSIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = selectedLocation.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 3,
                        lineHeight = 16.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DATA CLASS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = DataClassification.STATIC_SOURCE_DERIVED.badgeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tactical Target Selector.
 */
@Composable
fun MissionTargetSelector(
    locations: List<MarsLocation>,
    selectedId: String?,
    onSelect: (MarsLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyFeatures = remember(locations) {
        locations.filter {
            it.name.contains("Olympus", ignoreCase = true) ||
            it.name.contains("Jezero", ignoreCase = true) ||
            it.name.contains("Valles", ignoreCase = true) ||
            it.name.contains("Gale", ignoreCase = true)
        }.take(4)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exploration_targets).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keyFeatures.forEach { feature ->
                val isSelected = selectedId == feature.id
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            1.dp,
                            if (isSelected) MarsOrangeBright else SpaceBorderDark,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            SoundManager.playTargetLock()
                            onSelect(feature)
                        },
                    color = if (isSelected) SpaceCardElevated else SpaceBlack
                ) {
                    Text(
                        text = feature.name.split(" ").first().uppercase(),
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MarsOrangeBright else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}
