package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val response by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    
    val isReady = allMemories.isNotEmpty()
    
    // Clear response when leaving
    DisposableEffect(Unit) {
        onDispose { viewModel.clearAiResponse() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Search", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFFE2E2E6),
                    navigationIconContentColor = Color(0xFFC4C6D0)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Ask MemoryOS...") },
                placeholder = { Text("e.g. What did I discuss on Tuesday?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_search_input"),
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = Color(0xFF60A5FA)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.searchMemory()
                        },
                        enabled = isReady
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (isReady) Color(0xFF60A5FA) else Color.Gray)
                    }
                },
                enabled = isReady,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF2D2F33),
                    focusedContainerColor = Color(0xFF2D2F33),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedTextColor = Color(0xFFE2E2E6),
                    focusedTextColor = Color(0xFFE2E2E6)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    if (isReady) viewModel.searchMemory()
                }),
                singleLine = true
            )
            
            if (!isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "No memories recorded yet. Start recording or add one manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E9199)
                    )
                }
            } else if (isAnalyzing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF60A5FA))
                }
            } else if (response.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x26A855F7), Color(0x0C3B82F6))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, Color(0x33A855F7), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Insight",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E2E6)
                            )
                        }
                        Text(
                            text = response,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFC4C6D0),
                            lineHeight = 24.sp
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "Search across your entire life log naturally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E9199)
                    )
                }
            }
        }
    }
}
