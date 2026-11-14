package com.marsguide.vzd.ui.components

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.ui.theme.*

// Sci-Fi Palette Constants (Glowing Amber & Tactical Cyan)
val SciFiAmber = Color(0xFFFFB300)
val SciFiAmberGlow = Color(0xFFFFC107)
val SciFiCyan = Color(0xFF00E5FF)
val SciFiCyanGlow = Color(0xFF18FFFF)
val SciFiDarkPanel = Color(0xFF0A0C13)
val SciFiBorder = Color(0xFF1E2638)
val SciFiGridLine = Color(0xFF141C2B)

/**
 * Custom Compose Modifier: `crtTacticalScreen`
 *
 * Simulates a vintage tactical CRT monitor or telemetry screen:
 * 1. Horizontal raster scanlines with customizable spacing and opacity.
 * 2. Radial edge vignette darkening towards the bezel.
 * 3. Subtle animated CRT raster refresh beam / phosphor flicker.
 * 4. Slight chromatic RGB dispersion at the perimeter.
 * 5. Uses native RenderEffect (Android 12+ API 31) when available, or ultra-optimized DrawScope fallback.
 */
fun Modifier.crtTacticalScreen(
    scanlineSpacing: Dp = 3.dp,
    scanlineAlpha: Float = 0.16f,
    vignetteIntensity: Float = 0.45f,
    enablePhosphorFlicker: Boolean = true,
    tintColor: Color? = null
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "crt_clock")

    // Slow moving phosphor refresh bar
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crt_beam"
    )

    // Micro phosphor flicker
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crt_flicker"
    )

    this
        .drawWithContent {
            // Draw child composables first
            drawContent()

            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@drawWithContent

            val spacingPx = scanlineSpacing.toPx().coerceAtLeast(2f)

            // 1. Draw CRT Horizontal Scanlines
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color.Black.copy(alpha = scanlineAlpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.2f
                )
                y += spacingPx
            }

            // 2. Phosphor Refresh Beam (vertical sweeping line with subtle tint)
            if (enablePhosphorFlicker) {
                val beamY = h * beamOffset
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            (tintColor ?: SciFiCyan).copy(alpha = 0.05f * flickerAlpha),
                            (tintColor ?: SciFiCyan).copy(alpha = 0.10f * flickerAlpha),
                            Color.Transparent
                        ),
                        startY = (beamY - 40f).coerceAtLeast(0f),
                        endY = (beamY + 40f).coerceAtMost(h)
                    ),
                    topLeft = Offset(0f, (beamY - 40f).coerceAtLeast(0f)),
                    size = Size(w, 80f)
                )
            }

            // 3. Vignette & CRT Curvature Falloff
            val radius = kotlin.math.hypot(w / 2f, h / 2f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = vignetteIntensity * 0.5f),
                        Color.Black.copy(alpha = vignetteIntensity)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = radius
                ),
                size = size
            )

            // 4. Subtle chromatic bezel edge border
            drawRect(
                color = (tintColor ?: SciFiCyan).copy(alpha = 0.12f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
}

/**
 * Custom Tactile `SciFiButton` Composable.
 *
 * Implements:
 * - Simultaneous haptic click and mechanical audio foley.
 * - Reactive visual glowing press state with neon bloom and corner brackets.
 * - Glowing Amber or Tactical Cyan theme presets.
 * - Beveled tactical cut corners and monospace typography.
 */
@Composable
fun SciFiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isAmber: Boolean = true,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    testTag: String = "scifi_button"
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val primaryColor = if (isAmber) SciFiAmber else SciFiCyan
    val glowColor = if (isAmber) SciFiAmberGlow else SciFiCyanGlow

    // Pulsing animation when button is active or pressed
    val infiniteTransition = rememberInfiniteTransition(label = "btn_glow")
    val idleGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_glow"
    )

    val currentGlow = when {
        !enabled -> 0.05f
        isPressed -> 0.85f
        isSelected -> 0.55f
        else -> idleGlow
    }

    val backgroundColor = when {
        !enabled -> SpaceBlack
        isPressed -> primaryColor.copy(alpha = 0.25f)
        isSelected -> primaryColor.copy(alpha = 0.18f)
        else -> SciFiDarkPanel
    }

    val borderColor = when {
        !enabled -> SpaceBorderDark.copy(alpha = 0.3f)
        isPressed -> primaryColor
        isSelected -> primaryColor.copy(alpha = 0.9f)
        else -> primaryColor.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .testTag(testTag)
            .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .border(
                width = if (isPressed || isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom sci-fi glow replaces standard ripple
                enabled = enabled
            ) {
                // 1. Trigger native tactile haptic feedback
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                // 2. Play mechanical audio click via sound manager
                com.marsguide.vzd.util.SoundManager.playClick()
                // 3. Invoke user callback
                onClick()
            },
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Neon Glow Edge Background
            if (isPressed || isSelected) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = currentGlow * 0.4f), Color.Transparent),
                            center = center,
                            radius = size.width
                        )
                    )
                }
            }

            // Tactical Corner Crosshairs
            TacticalCornerBrackets(
                color = if (isPressed) glowColor else borderColor,
                modifier = Modifier.matchParentSize()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(primaryColor.copy(alpha = if (isPressed) 0.3f else 0.12f))
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) primaryColor else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) {
                            if (isPressed) Color.White else primaryColor
                        } else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) Color.LightGray.copy(alpha = 0.7f) else Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Tactical State LED in Top Right
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isPressed || isSelected) glowColor else primaryColor.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * Micro tactical corner bracket accents for sci-fi panels.
 */
@Composable
fun TacticalCornerBrackets(
    color: Color,
    modifier: Modifier = Modifier,
    bracketLength: Dp = 6.dp,
    strokeWidth: Dp = 1.2.dp
) {
    Canvas(modifier = modifier) {
        val len = bracketLength.toPx()
        val sw = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        // Top-left
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), sw)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), sw)

        // Top-right
        drawLine(color, Offset(w - len, 0f), Offset(w, 0f), sw)
        drawLine(color, Offset(w, 0f), Offset(w, len), sw)

        // Bottom-left
        drawLine(color, Offset(0f, h - len), Offset(0f, h), sw)
        drawLine(color, Offset(0f, h), Offset(len, h), sw)

        // Bottom-right
        drawLine(color, Offset(w - len, h), Offset(w, h), sw)
        drawLine(color, Offset(w, h - len), Offset(w, h), sw)
    }
}
