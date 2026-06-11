package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111318),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF111318)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Profile Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2025)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("User Profile", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: Loveneet Arora", color = Color(0xFFE2E2E9))
                    Text("Email: loveneetarora.ai@gmail.com", color = Color(0xFFE2E2E9))
                }
            }

            // SaaS Subscription Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2025)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subscription", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Plan: Premium SaaS", color = Color(0xFFE2E2E9))
                    Text("Status: Active", color = Color(0xFF22C55E))
                    Text("Next billing: July 11, 2026", color = Color(0xFFE2E2E9))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                        Text("Manage Subscription")
                    }
                }
            }

            // Sync/Recording settings
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = androidx.compose.runtime.remember { context.getSharedPreferences("memory_prefs", android.content.Context.MODE_PRIVATE) }
            var recordEverything by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("record_everything", true)) }
            var autoTranscribe by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("auto_transcribe", true)) }
            var intervalMinutes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getFloat("interval", 30f)) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2025)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recording Preferences", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Record Everything", color = Color(0xFFE2E2E9))
                        Switch(checked = recordEverything, onCheckedChange = { recordEverything = it })
                    }
                    
                    if (recordEverything) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Interval: ${intervalMinutes.toInt()} minutes", color = Color(0xFFE2E2E9))
                        Slider(
                            value = intervalMinutes,
                            onValueChange = { intervalMinutes = it },
                            valueRange = 5f..60f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color(0xFF44474E)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Auto-Transcribe", color = Color(0xFFE2E2E9))
                        Switch(checked = autoTranscribe, onCheckedChange = { autoTranscribe = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    
                    Button(
                        onClick = { 
                            scope.launch {
                                val prefs = context.getSharedPreferences("memory_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putBoolean("record_everything", recordEverything)
                                    putFloat("interval", intervalMinutes)
                                    putBoolean("auto_transcribe", autoTranscribe)
                                    apply()
                                }
                                android.widget.Toast.makeText(context, "Preferences saved", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Preferences")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}
