package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memoryId: Int,
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    // Find the specific memory from the flow
    val memories by viewModel.allMemories.collectAsState()
    val memory = memories.find { it.id == memoryId }

    if (memory == null) {
        return
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(memory.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteMemory(memoryId)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Memory", tint = Color(0xFFEF4444))
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = memory.category.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = memory.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E2E6)
            )
            
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF8E9199)
            )

            HorizontalDivider(color = Color(0x33FFFFFF))

            if (!memory.summary.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x3360A5FA), Color(0x0C3B82F6))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, Color(0x3360A5FA), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E2E6)
                            )
                        }
                        Text(
                            text = memory.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFC4C6D0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = "Full Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC4C6D0)
            )
            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE2E2E6)
            )
        }
    }
}
