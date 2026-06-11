package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Life") }
    
    val categories = listOf("Life", "Meeting", "Idea", "Task", "Conversation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Memory", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                viewModel.insertMemory(title, content, selectedCategory)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("save_memory_button")
                    ) {
                        Text("Save", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = Color(0xFF8E9199)) },
                modifier = Modifier.fillMaxWidth().testTag("memory_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF2D2F33),
                    focusedContainerColor = Color(0xFF2D2F33),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedTextColor = Color(0xFFE2E2E6),
                    focusedTextColor = Color(0xFFE2E2E6)
                )
            )
            
            // Category selector mapping to frosted glass UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { label ->
                    FilterChip(
                        selected = label == selectedCategory,
                        onClick = { selectedCategory = label },
                        label = { Text(label, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1F2125),
                            labelColor = Color(0xFFC4C6D0),
                            selectedContainerColor = Color(0x3360A5FA),
                            selectedLabelColor = Color(0xFF60A5FA)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = label == selectedCategory,
                            borderColor = Color(0x33FFFFFF),
                            selectedBorderColor = Color(0xFF60A5FA),
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Details / Transcript", color = Color(0xFF8E9199)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("memory_content_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF2D2F33),
                    focusedContainerColor = Color(0xFF2D2F33),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedTextColor = Color(0xFFE2E2E6),
                    focusedTextColor = Color(0xFFE2E2E6)
                )
            )
        }
    }
}
