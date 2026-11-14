package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.marsguide.vzd.domain.model.MarsEnvironmentalConditions
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

@Composable
fun MarsWeatherScreen(
    viewModel: MarsGuideViewModel,
    modifier: Modifier = Modifier
) {
    var isArabic by remember { mutableStateOf(false) }
    var useFahrenheit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.marsguide.vzd.util.SoundManager.playDataSync()
    }

    val conditions = MarsEnvironmentalConditions(
        temperatureCelsius = -62.5,
        surfacePressurePa = 712.0,
        solarFluxWm2 = 590.0,
        seasonalContext = "Northern Hemisphere Late Winter (L_s = 312°)",
        provenanceClassification = com.marsguide.vzd.domain.model.DataClassification.STATIC_SOURCE_DERIVED,
        sourceCitation = "NASA Curiosity REMS / Perseverance MEDA Telemetry Archive"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SpaceBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Language / Unit Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(8.dp))
                    .border(1.dp, ScienceTeal.copy(alpha = 0.5f), CutCornerShape(8.dp)),
                color = SpaceCardDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (isArabic) "مرصد الطقس المريخي // REMS & MEDA" else "MARS ATMOSPHERIC TELEMETRY // REMS & MEDA",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "بيانات حية ومؤرشفة من وكالة ناسا" else "Official NASA PDS Environmental Telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isArabic = !isArabic },
                            colors = ButtonDefaults.buttonColors(containerColor = SpaceCardElevated),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isArabic) "EN" else "عربي",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ScienceTeal
                            )
                        }
                        Button(
                            onClick = { useFahrenheit = !useFahrenheit },
                            colors = ButtonDefaults.buttonColors(containerColor = SpaceCardElevated),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (useFahrenheit) "°C" else "°F",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MarsOrangeBright
                            )
                        }
                    }
                }
            }

            // Main Weather Card (Temperature & Pressure)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SpaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SpaceBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "درجة الحرارة السطحية" else "SURFACE TEMPERATURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(imageVector = Icons.Default.Thermostat, contentDescription = "Temp", tint = MarsOrangeBright)
                    }
                    Text(
                        text = conditions.formatTemperature(useFahrenheit),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider(color = SpaceBorderDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الضغط الجوي" else "SURFACE PRESSURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = conditions.formatPressure(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ScienceTeal,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isArabic) "التدفق الشمسي" else "SOLAR IRRADIANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${conditions.solarFluxWm2} W/m²",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MarsDuneGold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Seasonal & Atmospheric Dynamics Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SpaceCardDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SpaceBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "الحالة الموسمية والغلاف الجوي" else "SEASONAL & ATMOSPHERIC DYNAMICS",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = conditions.seasonalContext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Source: ${conditions.sourceCitation}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
