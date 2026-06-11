package com.example.ui.screens

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.MemoryEntity
import com.example.service.ContinuousRecordService
import com.example.ui.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MemoryViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { com.example.utils.PrefsHelper.getPrefs(context) }
    var isRecording by remember { mutableStateOf(isServiceRunning(context, ContinuousRecordService::class.java)) }
    val apiKeyMissing = BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"

    // Re-check service state when recomposed
    LaunchedEffect(Unit) {
        isRecording = isServiceRunning(context, ContinuousRecordService::class.java)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canRecord = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (canRecord) {
            val intent = Intent(context, ContinuousRecordService::class.java)
            if (!isRecording) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRecording = true
            } else {
                context.stopService(intent)
                isRecording = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        Text("MemoryOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .background(if (isRecording) Color(0x1A22C55E) else Color(0x1AEF4444), RoundedCornerShape(50))
                                .border(1.dp, if (isRecording) Color(0x3322C55E) else Color(0x33EF4444), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable {
                                    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(permissions.toTypedArray())
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(if (isRecording) Color(0xFF22C55E) else Color(0xFFEF4444), CircleShape))
                            Text(if (isRecording) "RECORDING" else "PAUSED", color = if (isRecording) Color(0xFF22C55E) else Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF44474E), CircleShape)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            val name = prefs.getString("user_name", "User") ?: "User"
                            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                            Text(if (initials.isEmpty()) "U" else initials, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                        .clickable { onNavigateToSearch() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("search_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFFC4C6D0))
                        Text("Search your life...", color = Color(0xFF8E9199), fontSize = 14.sp)
                    }
                }
                if (apiKeyMissing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFF854D0E), RoundedCornerShape(8.dp))
                            .clickable { onNavigateToSettings() }
                            .padding(12.dp)
                    ) {
                        Text(
                            "Gemini API key not configured. AI features disabled.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier = Modifier.testTag("add_memory_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Memory")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x333B82F6), Color(0x0C4F46E5))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(32.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DAILY INSIGHT", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            Text("92% Indexing Complete", color = Color(0xFFC4C6D0), fontSize = 11.sp)
                        }
                        Text("Welcome to MemoryOS.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                        Text("Your secure, private, life-logging companion is active and recording safely on device.", color = Color(0xFFC4C6D0), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Timeline",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E2E6)
                    )
                    Text(
                        "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { }
                    )
                }
            }
            if (memories.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No memories yet. Begin logging.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(memories) { memory ->
                    MemoryCard(memory = memory, onClick = { onNavigateToDetail(memory.id) })
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryEntity, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(memory.timestamp))
    
    // Create timeline timeline dot & line layout
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("memory_item_card")) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(Color(0xFF60A5FA), CircleShape).padding(top = 8.dp))
            Box(modifier = Modifier.width(2.dp).weight(1f).background(Color(0x0DFFFFFF)))
        }
        
        Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            Text(
                text = "${dateStr} • ${memory.category}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E9199),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2125)),
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = memory.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E2E6)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val displayContent = memory.summary ?: memory.content
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E9199),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (memory.category == "Meeting" || memory.category == "Conversation") {
                        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.background(Color(0x1A60A5FA), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("AUDIO RECAP", color = Color(0xFF60A5FA), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Box(modifier = Modifier.background(Color(0x1AFBBF24), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("3 ACTIONS", color = Color(0xFFFBBF24), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
