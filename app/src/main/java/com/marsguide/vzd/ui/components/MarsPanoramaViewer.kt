package com.marsguide.vzd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.marsguide.vzd.R
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.MarsPanorama
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.SoundManager
import kotlin.math.roundToInt

/**
 * Fullscreen Interactive 360° Mars Surface Panorama Viewer.
 * Features 360° horizontal panning, vertical tilt, pinch-to-zoom,
 * compass heading indicator, authentic mission provenance, and Martian wind soundscape.
 */
@Composable
fun MarsPanoramaViewer(
    panorama: MarsPanorama,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pan, Tilt & Zoom States
    var panHeadingDeg by remember { mutableFloatStateOf(0f) }
    var pitchAngleDeg by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Audio lifecycle: play enter sound and launch surface wind ambience
    DisposableEffect(Unit) {
        SoundManager.playPanoramaEnter()
        SoundManager.startPanoramaAmbience()
        onDispose {
            SoundManager.stopPanoramaAmbience()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("mars_panorama_viewer")
    ) {
        // 1. PANORAMIC CYLINDRICAL VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(1.0f, 4.0f)
                        val headingSensitivity = 0.15f / zoomScale
                        val pitchSensitivity = 0.12f / zoomScale

                        panHeadingDeg = (panHeadingDeg - pan.x * headingSensitivity + 360f) % 360f
                        pitchAngleDeg = (pitchAngleDeg - pan.y * pitchSensitivity).coerceIn(-28f, 28f)
                    }
                }
        ) {
            // High-resolution Authentic NASA Panorama image with offline fallback
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(panorama.panoramaUrl)
                    .crossfade(true)
                    .error(R.drawable.mars_surface_hero)
                    .fallback(R.drawable.mars_surface_hero)
                    .build(),
                contentDescription = panorama.title,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = (-panHeadingDeg * 4f).dp,
                        y = (pitchAngleDeg * 3f).dp
                    ),
                contentScale = ContentScale.FillHeight
            )

            // Vignette and Atmosphere Horizon Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        }

        // 2. TOP COMPASS HEADING TAPE
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(8.dp),
            color = SpaceDark.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SpaceBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button
                Button(
                    onClick = {
                        SoundManager.playBack()
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarsTerracotta),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Panorama", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EXIT PANORAMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Compass Heading Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val cardinal = when (panHeadingDeg.roundToInt()) {
                        in 338..360, in 0..22 -> "N"
                        in 23..67 -> "NE"
                        in 68..112 -> "E"
                        in 113..157 -> "SE"
                        in 158..202 -> "S"
                        in 203..247 -> "SW"
                        in 248..292 -> "W"
                        else -> "NW"
                    }
                    Text(
                        text = "BEARING ${panHeadingDeg.roundToInt()}° $cardinal | PITCH ${pitchAngleDeg.roundToInt()}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "DRAG TO LOOK 360° // PINCH TO ZOOM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp
                    )
                }

                // Sound state pill
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, SpaceBorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(ScienceBlue, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MARTIAN WIND",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScienceBlue,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        // 3. BOTTOM AUTHENTIC MISSION METADATA & PROVENANCE HUD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            color = SpaceDark.copy(alpha = 0.90f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SpaceBorderDark)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${panorama.mission.uppercase()} // ${panorama.roverName.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    DataProvenanceBadge(
                        classification = DataClassification.STATIC_SOURCE_DERIVED
                    )
                }

                Text(
                    text = panorama.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = "INSTRUMENT: ${panorama.instrument} | SOL: ${panorama.sol}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceBlue,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )

                Text(
                    text = panorama.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
