package com.marsguide.vzd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.R
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.ui.theme.*

@Composable
fun NetworkStatusPill(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isOnline) ScienceGreen.copy(alpha = 0.15f) else ScienceWarning.copy(alpha = 0.15f)
    val dotColor = if (isOnline) ScienceGreen else ScienceWarning
    val text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline)

    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) ScienceGreen else ScienceWarning,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CoordinateBadge(
    coordinate: MarsCoordinate,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(8.dp)),
        color = SpaceCardDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "MARS COORD",
                style = MaterialTheme.typography.labelSmall,
                color = MarsOrangeBright,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = coordinate.formatShort(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ElevationBadge(
    elevationMeters: Double?,
    modifier: Modifier = Modifier
) {
    if (elevationMeters == null) return
    val isPositive = elevationMeters >= 0
    val color = if (isPositive) MarsOrangeBright else ScienceTeal
    val sign = if (isPositive) "+" else ""

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = "MOLA $sign${String.format(java.util.Locale.US, "%,.0f", elevationMeters)} m",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .background(MarsOrangeBright, RoundedCornerShape(2.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun DataProvenanceBadge(
    classification: DataClassification,
    modifier: Modifier = Modifier
) {
    DataClassificationBadge(classification = classification, modifier = modifier)
}

@Composable
fun DataClassificationBadge(
    classification: DataClassification,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (classification) {
        DataClassification.REAL_LIVE_API -> ScienceGreen.copy(alpha = 0.15f) to ScienceGreen
        DataClassification.STATIC_SOURCE_DERIVED -> ScienceTeal.copy(alpha = 0.15f) to ScienceTeal
        DataClassification.APPLICATION_CALCULATED -> MarsOrangeBright.copy(alpha = 0.15f) to MarsOrangeBright
        DataClassification.ESTIMATED_APPROXIMATION -> ScienceWarning.copy(alpha = 0.15f) to ScienceWarning
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, textColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
        color = bgColor
    ) {
        Text(
            text = classification.badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

