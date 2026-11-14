package com.marsguide.vzd.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.Composable
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
import com.marsguide.vzd.ui.components.DataClassificationBadge
import com.marsguide.vzd.ui.components.GuideInfo
import com.marsguide.vzd.ui.components.TelemetryInstrumentBlock
import com.marsguide.vzd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSourcesScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                        text = "MISSION & DATA SOURCES",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
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
            item {
                TelemetryInstrumentBlock(
                    title = "NASA SPACE APPS CHALLENGE",
                    guideInfo = GuideInfo(
                        title = "NASA Space Apps",
                        whatIsIt = "The largest annual space & science hackathon in the world.",
                        whyItMatters = "MARS//GUIDE was created as a submission for this event."
                    )
                ) {
                    Text(
                        text = "VIRTUAL EXPLORATION OF MARS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This application is an educational, scientific exploration tool designed to provide an immersive, truthful, spacecraft-like experience of the Martian surface without resorting to fabricated telemetry or fake data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        lineHeight = 22.sp
                    )
                }
            }

            item {
                TelemetryInstrumentBlock(
                    title = "SCIENTIFIC DATA INTEGRITY STANDARD",
                    guideInfo = GuideInfo(
                        title = "Data Integrity",
                        whatIsIt = "The transparency system used throughout the app.",
                        whyItMatters = "Ensures users always know whether data is real, calculated, or estimated."
                    )
                ) {
                    Text(
                        text = "MARS//GUIDE explicitly categorizes all data to maintain scientific transparency.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    DataClassification.values().forEach { classification ->
                        ProvenanceExplainerRow(classification = classification)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            item {
                TelemetryInstrumentBlock(
                    title = "PRIMARY DATA SOURCES",
                    guideInfo = GuideInfo(
                        title = "Primary Data Sources",
                        whatIsIt = "The actual NASA and USGS systems that power this app.",
                        whyItMatters = "Transparency allows for external validation and research."
                    )
                ) {
                    SourceLinkRow(
                        name = "NASA Image and Video Library",
                        usage = "Live API queries for region-specific imagery and Rover photos.",
                        classification = DataClassification.REAL_LIVE_API,
                        url = "https://images.nasa.gov/",
                        context = context
                    )
                    HorizontalDivider(color = SpaceBorderDark, modifier = Modifier.padding(vertical = 12.dp))
                    
                    SourceLinkRow(
                        name = "MOLA (Mars Orbiter Laser Altimeter)",
                        usage = "USGS/NASA PDS elevations and topographical visualization basis.",
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        url = "https://pds-geosciences.wustl.edu/missions/mgs/mola.html",
                        context = context
                    )
                    HorizontalDivider(color = SpaceBorderDark, modifier = Modifier.padding(vertical = 12.dp))
                    
                    SourceLinkRow(
                        name = "USGS Gazetteer of Planetary Nomenclature",
                        usage = "Coordinates, diameters, and geological contexts for surface features.",
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        url = "https://planetarynames.wr.usgs.gov/",
                        context = context
                    )
                    HorizontalDivider(color = SpaceBorderDark, modifier = Modifier.padding(vertical = 12.dp))
                    
                    SourceLinkRow(
                        name = "NASA / JPL Mission Archives",
                        usage = "Spacecraft information, landing coordinates, and historical context.",
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        url = "https://mars.nasa.gov/",
                        context = context
                    )
                    HorizontalDivider(color = SpaceBorderDark, modifier = Modifier.padding(vertical = 12.dp))

                    SourceLinkRow(
                        name = "HiRISE (High Resolution Imaging Science Experiment)",
                        usage = "Source material for specific curated offline images when the Live API is disconnected.",
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        url = "https://hirise.lpl.arizona.edu/",
                        context = context
                    )
                }
            }

            item {
                TelemetryInstrumentBlock(
                    title = "CRITICAL LIMITATIONS",
                    guideInfo = GuideInfo(
                        title = "Limitations",
                        whatIsIt = "Scientific boundaries of MARS//GUIDE.",
                        whyItMatters = "Prevents the application from being misinterpreted as a live telemetry system."
                    )
                ) {
                    LimitationBullet("The Mars map is a procedural scientific visualization, not a real continuous MOLA DEM raster.")
                    LimitationBullet("Route waypoint elevations are mathematically interpolated between known endpoints; they are not sampled from real terrain.")
                    LimitationBullet("Rover positions are historical mission records, not live telemetry.")
                    LimitationBullet("All data, unless marked LIVE, is bundled offline for guaranteed competition availability.")
                }
            }
        }
    }
}

@Composable
private fun ProvenanceExplainerRow(
    classification: DataClassification
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SpaceBlack)
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        DataClassificationBadge(classification = classification)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = classification.fullTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = classification.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SourceLinkRow(
    name: String,
    usage: String,
    classification: DataClassification,
    url: String,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Link",
                tint = ScienceTeal,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "USAGE: $usage",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        DataClassificationBadge(classification = classification)
    }
}

@Composable
private fun LimitationBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = ScienceWarning,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E6F0),
            lineHeight = 20.sp
        )
    }
}
