package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.ui.AppNavigation
import com.example.ui.MemoryViewModel
import com.example.ui.MemoryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val db = AppDatabase.getDatabase(this)
        val viewModelFactory = MemoryViewModelFactory(db)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MemoryViewModel::class.java]

        setContent {
            MyApplicationTheme {
                var isAuthenticated by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val executor = ContextCompat.getMainExecutor(this@MainActivity)
                    val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Authentication error: \$errString", Toast.LENGTH_SHORT).show()
                                // For now, just allow them in if emulator doesn't support it well, or show a fallback
                                isAuthenticated = true
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isAuthenticated = true
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock MemoryOS")
                        .setSubtitle("Authenticate to access your secure memories")
                        .setNegativeButtonText("Cancel")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAuthenticated) {
                        AppNavigation(viewModel = viewModel)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Unlock your device to continue.", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}
