package com.marsguide.vzd.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.model.MarsLocation
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.SoundManager

enum class SpacecraftButtonState {
    NORMAL,
    ACTIVE,
    DISABLED,
    WARNING
}

/**
 * Tactical aerospace spacecraft button with metallic bevels, tactile feedback,
 * active indicator LED, and accessibility support.
 */
@Composable
fun SpacecraftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subText: String? = null,
    state: SpacecraftButtonState = SpacecraftButtonState.NORMAL,
    testTag: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "button_press_scale"
    )

    val (borderColor, containerColor, contentColor, ledColor) = when (state) {
        SpacecraftButtonState.NORMAL -> Quad(SpaceBorderDark, SpaceCardDark, Color.White, ScienceTeal)
        SpacecraftButtonState.ACTIVE -> Quad(MarsOrangeBright, SpaceCardElevated, MarsOrangeBright, MarsOrangeBright)
        SpacecraftButtonState.DISABLED -> Quad(SpaceBorderDark.copy(alpha = 0.5f), SpaceBlack, Color.Gray, Color.DarkGray)
        SpacecraftButtonState.WARNING -> Quad(ScienceWarning, SpaceCardDark, ScienceWarning, ScienceWarning)
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = state != SpacecraftButtonState.DISABLED
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                SoundManager.playClick()
                onClick()
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Aerospace status LED
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ledColor)
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (!subText.isNullOrBlank()) {
                        Text(
                            text = subText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
            // Tactical chevron indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Outer spacecraft framing panel with technical rivets, illuminated status lines,
 * and carbon-titanium bevels.
 */
@Composable
fun SpacecraftPanel(
    title: String,
    modifier: Modifier = Modifier,
    statusText: String = "NOMINAL",
    statusColor: Color = ScienceGreen,
    onGuideClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(6.dp)),
        color = SpaceCardDark
    ) {
        Column {
            // Header bar with technical framing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceCardElevated)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(SpaceBorderDark, SpaceBorderDark.copy(alpha = 0.3f))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bolt/rivet dot
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.6f))
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (onGuideClick != null) {
                        GuideButton(onClick = onGuideClick)
                    }
                }
            }

            // Panel body
            Column(
                modifier = Modifier.padding(14.dp),
                content = content
            )
        }
    }
}

/**
 * Animated targeting bracket callout for detected Martian surface features.
 */
@Composable
fun HolographicTargetCallout(
    location: MarsLocation,
    onAnalyze: () -> Unit,
    onPlanRoute: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bracket_pulse")
    val bracketAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MarsOrangeBright.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
        color = SpaceCardDark.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Target Detected
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MarsOrangeBright.copy(alpha = bracketAlpha))
                    )
                    Text(
                        text = "TARGET ACQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                DataClassificationBadge(classification = DataClassification.STATIC_SOURCE_DERIVED)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Name & Type
            Text(
                text = location.name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
            Text(
                text = location.type.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = ScienceTeal,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinates & MOLA Elevation Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryReadout(
                    label = "LATITUDE",
                    value = location.coordinate.formatLatitude(),
                    modifier = Modifier.weight(1f)
                )
                TelemetryReadout(
                    label = "LONGITUDE",
                    value = location.coordinate.formatLongitude(),
                    modifier = Modifier.weight(1f)
                )
                TelemetryReadout(
                    label = "MOLA DATUM",
                    value = location.elevationMeters?.let { "${String.format(java.util.Locale.US, "%,.0f", it)} m" } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Why it matters excerpt
            Text(
                text = "GEOLOGICAL CONTEXT: ${location.geologicalContext.take(130)}...",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCAD0E0),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1.2f).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScienceTeal,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ANALYZE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPlanRoute,
                    modifier = Modifier.weight(1.2f).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceCardElevated,
                        contentColor = MarsOrangeBright
                    )
                ) {
                    Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ROUTE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScienceTeal.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScienceTeal)
                ) {
                    Text("GUIDE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tactical data strip showing live system status or mission telemetry.
 */
@Composable
fun TacticalDataStrip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = ScienceTeal
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
        Text(
            text = "${label.uppercase()}: $value",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Semi-transparent glass panel with technical framing and subtle inner glow.
 */
@Composable
fun SpacecraftGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(8.dp)
            )
            .padding(1.dp)
    ) {
        content()
    }
}
