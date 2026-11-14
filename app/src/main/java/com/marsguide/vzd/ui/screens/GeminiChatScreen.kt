package com.marsguide.vzd.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
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
import com.marsguide.vzd.data.repository.GeminiRepository
import com.marsguide.vzd.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "SOL-084 // 14:22"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    modifier: Modifier = Modifier
) {
    var queryInput by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "VZD // GEMINI MARTIAN ASTROBIOLOGY SYSTEM ONLINE. Ask any question regarding Martian terrain, rover telemetry, exobiology, or mission protocols.",
                isUser = false
            )
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val geminiRepository = remember { GeminiRepository() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SpaceBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Gemini AI",
                            tint = ScienceTeal
                        )
                        Column {
                            Text(
                                text = "GEMINI 2.5 // MARS AI INTEL",
                                style = MaterialTheme.typography.labelMedium,
                                color = ScienceTeal,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SECURE DSN DEEP SPACE LINK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Surface(
                        color = ScienceTeal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "FREE TIER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Error banner if rate limited (429) or other error
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2A1010),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ TRANSMISSION WARNING",
                            color = Color.Red,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (msg.isUser) MarsOrangeBright.copy(alpha = 0.2f) else SpaceCardDark,
                            border = BorderStroke(
                                1.dp,
                                if (msg.isUser) MarsOrangeBright.copy(alpha = 0.5f) else ScienceTeal.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (msg.isUser) "ASTRONAUT" else "GEMINI AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (msg.isUser) MarsOrangeBright else ScienceTeal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = SpaceCardDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ScienceTeal.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ScienceTeal,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "SYNTHESIZING MARTIAN INTEL...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScienceTeal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Query Gemini AI about Mars...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScienceTeal,
                        unfocusedBorderColor = SpaceBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SpaceCardDark,
                        unfocusedContainerColor = SpaceCardDark
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (queryInput.isNotBlank() && !isLoading) {
                            val prompt = queryInput.trim()
                            queryInput = ""
                            errorMessage = null
                            messages.add(ChatMessage(text = prompt, isUser = true))
                            isLoading = true

                            coroutineScope.launch {
                                val result = geminiRepository.askMarsAI(prompt)
                                isLoading = false
                                result.fold(
                                    onSuccess = { answer ->
                                        messages.add(ChatMessage(text = answer, isUser = false))
                                    },
                                    onFailure = { err ->
                                        errorMessage = err.localizedMessage ?: "Unknown transmission error"
                                        messages.add(ChatMessage(text = "[TRANSMISSION FAILED]: ${err.localizedMessage}", isUser = false))
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScienceTeal),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = SpaceBlack
                    )
                }
            }
        }
    }
}
