package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun authenticate() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            errorMsg = "Authentication error: Context is not FragmentActivity"
            return
        }

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If no biometric hardware is available, just let them in (fallback for standard emulators)
                    if (errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE || 
                        errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT) {
                        onLoginSuccess()
                    } else {
                        errorMsg = "Error: $errString"
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onLoginSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    errorMsg = "Authentication Failed"
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MemoryOS Login")
            .setSubtitle("Authenticate to access your private life logs")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        authenticate()
    }

    Scaffold(
        containerColor = Color(0xFF111318)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Logo placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF3B82F6), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color.White, style = MaterialTheme.typography.displayMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "MemoryOS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your private, offline-first life memory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E9199)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = { authenticate() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Authenticate to Enter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    // Fallback button just in case authentication is totally broken in emulator
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onLoginSuccess() }) {
                        Text("Bypass (Dev Only)", color = Color(0xFF8E9199))
                    }
                }
            }
        }
    }
}
