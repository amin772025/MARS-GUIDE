package com.marsguide.vzd.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.usecase.SearchResultItem
import com.marsguide.vzd.domain.usecase.SearchResultType
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

@Composable
fun SearchDialog(
    viewModel: MarsGuideViewModel,
    onDismiss: () -> Unit,
    onNavigateToLocation: (String) -> Unit,
    onNavigateToMissions: () -> Unit,
    onSelectCoordinateOnMap: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val filters = listOf("All", "Locations", "Missions", "Rovers", "Science")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MarsOrangeBright.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .testTag("search_dialog"),
            color = SpaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLANETARY SEARCH DATABASE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search crater, mission, rover, or '18.4, 77.4'...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MarsOrangeBright
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MarsOrangeBright,
                        unfocusedBorderColor = SpaceBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MarsOrangeBright
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = searchFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) ScienceTeal else SpaceBorderDark,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.updateSearchFilter(filter) },
                            color = if (isSelected) ScienceTeal.copy(alpha = 0.2f) else SpaceBlack
                        ) {
                            Text(
                                text = filter.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) ScienceTeal else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SpaceBorderDark)
                Spacer(modifier = Modifier.height(8.dp))

                // Results list
                if (searchQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AWAITING SEARCH QUERY\nEnter keywords or coordinates",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO MATCHING RECORDS FOUND\nCheck spelling or coordinate format",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScienceWarning,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp))
                                    .clickable {
                                        when (item.type) {
                                            SearchResultType.LOCATION -> {
                                                onDismiss()
                                                onNavigateToLocation(item.id)
                                            }
                                            SearchResultType.MISSION, SearchResultType.ROVER -> {
                                                onDismiss()
                                                onNavigateToMissions()
                                            }
                                            SearchResultType.COORDINATE -> {
                                                if (item.latitude != null && item.longitude != null) {
                                                    viewModel.selectCoordinate(
                                                        MarsCoordinate(item.latitude, item.longitude)
                                                    )
                                                }
                                                onDismiss()
                                                onSelectCoordinateOnMap()
                                            }
                                            SearchResultType.SCIENCE_TARGET -> {
                                                if (item.latitude != null && item.longitude != null) {
                                                    viewModel.selectCoordinate(
                                                        MarsCoordinate(item.latitude, item.longitude)
                                                    )
                                                }
                                                onDismiss()
                                                onSelectCoordinateOnMap()
                                            }
                                        }
                                    },
                                color = SpaceBlack
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        DataProvenanceBadge(classification = item.classification)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ScienceTeal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 2
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
