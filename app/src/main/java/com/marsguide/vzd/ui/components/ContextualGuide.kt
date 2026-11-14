package com.marsguide.vzd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
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
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.ui.theme.MarsOrangeBright
import com.marsguide.vzd.ui.theme.ScienceTeal
import com.marsguide.vzd.ui.theme.SpaceBorderDark
import com.marsguide.vzd.ui.theme.SpaceCardDark
import com.marsguide.vzd.ui.theme.SpaceCardElevated

data class GuideInfo(
    val title: String,
    val whatIsIt: String,
    val whyItMatters: String,
    val howCalculated: String? = null,
    val dataClass: DataClassification? = null,
    val source: String? = null
)

@Composable
fun GuideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(24.dp)
            .clickable { onClick() }
            .clip(CircleShape)
            .border(1.dp, ScienceTeal.copy(alpha = 0.5f), CircleShape),
        color = ScienceTeal.copy(alpha = 0.1f),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Mission Guide",
                tint = ScienceTeal,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ContextualGuideButton(
    guideInfo: GuideInfo,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    GuideButton(
        onClick = { showSheet = true },
        modifier = modifier
    )

    if (showSheet) {
        GuideBottomSheet(
            guideInfo = guideInfo,
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideBottomSheet(
    guideInfo: GuideInfo,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpaceCardDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ScienceTeal) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MISSION GUIDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = guideInfo.title.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GuideSection(
                title = "WHAT IS THIS?",
                content = guideInfo.whatIsIt
            )

            if (guideInfo.howCalculated != null) {
                Spacer(modifier = Modifier.height(16.dp))
                GuideSection(
                    title = "HOW IT IS CALCULATED / SOURCED",
                    content = guideInfo.howCalculated
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            GuideSection(
                title = "WHY IT MATTERS",
                content = guideInfo.whyItMatters,
                accent = MarsOrangeBright
            )

            if (guideInfo.dataClass != null || guideInfo.source != null) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = SpaceBorderDark)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "DATA PROVENANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (guideInfo.dataClass != null) {
                        DataClassificationBadge(
                            classification = guideInfo.dataClass,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (guideInfo.source != null) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            color = SpaceCardElevated
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "SOURCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = guideInfo.source.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(
    title: String,
    content: String,
    accent: Color = ScienceTeal
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E6F0),
            lineHeight = 22.sp
        )
    }
}
