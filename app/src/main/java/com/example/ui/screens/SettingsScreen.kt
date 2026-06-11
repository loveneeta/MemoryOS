package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.verticalScroll
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
                .padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = androidx.compose.runtime.remember { com.example.utils.PrefsHelper.getPrefs(context) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            var userName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getString("user_name", "") ?: "") }
            var userEmail by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getString("user_email", "") ?: "") }

            // User Profile Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2025)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("User Profile", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color(0xFFE2E2E9))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color(0xFFE2E2E9))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            prefs.edit().putString("user_name", userName).putString("user_email", userEmail).apply()
                            android.widget.Toast.makeText(context, "Profile saved", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Save Profile")
                    }
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
                    Text("No active subscription", color = Color(0xFFE2E2E9))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563))) {
                        Text("Coming Soon")
                    }
                }
            }

            // Sync/Recording settings
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
                    Button(
                        onClick = { 
                            scope.launch {
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            val intent = android.content.Intent(context, com.example.service.ContinuousRecordService::class.java)
                            context.stopService(intent)
                            android.widget.Toast.makeText(context, "Service Stopped", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Recording Service")
                    }
                }
            }

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
