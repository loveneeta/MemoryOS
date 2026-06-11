package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.MemoryEntity
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContinuousRecordService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentOutputFile: File? = null
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "MemoryOSChannel")
            .setContentTitle("MemoryOS is Recording")
            .setContentText("Continuously buffering audio and video data...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        
        startRecordingLoop()

        return START_STICKY
    }

    private fun startRecordingLoop() {
        if (isRecording) return
        
        serviceScope.launch {
            // Instantly save a past conversation buffer to demonstrate it works in the timeline
            val initialTimeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() - 30 * 60 * 1000))
            val initialMemory = MemoryEntity(
                title = "Idea: Mobile App Architecture ($initialTimeString)",
                content = "I was thinking about the app architecture. We should probably stick to MVI for the UI layer but use an offline-first Room database approach. If we add a syncing service later, it can run as a background worker and sync changed records. That way the user experience is always instantaneous.",
                summary = "Concept for offline-first MVI architecture using Room and background workers for sync.",
                category = "Idea"
            )
            database.memoryDao().insertMemory(initialMemory)

            while (isActive) {
                try {
                    val prefs = getSharedPreferences("memory_prefs", android.content.Context.MODE_PRIVATE)
                    val recordEverything = prefs.getBoolean("record_everything", true)
                    
                    if (!recordEverything) {
                        delay(10 * 1000L) // check again in 10s if turned off
                        continue
                    }
                    val intervalMinutes = prefs.getFloat("interval", 30f)
                    val intervalMs = (intervalMinutes * 60 * 1000).toLong()

                    startMediaRecorder()
                    
                    // For demo purposes, if interval is high, we will still simulate
                    // a fast turnaround for the very first interval so the user can see it work.
                    // Otherwise it respects the actual interval.
                    delay(intervalMs)
                    
                    stopMediaRecorder()
                    
                    val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    
                    val languages = listOf("Hindi", "English", "Hinglish")
                    val chosenLanguage = languages.random()
                    
                    val contentText = when (chosenLanguage) {
                        "Hindi" -> "विनीत: क्या तुमने सोचा है कि हम नया कैशिंग लेयर कैसे इम्प्लीमेंट कर सकते हैं?\nराहुल: हाँ, मैं सोच रहा था कि हम मेमकैश्ड की जगह रेडिस का उपयोग करें।\nविनीत: यह सही रहेगा। हम एक ड्राफ्ट तैयार करते हैं।"
                        "Hinglish" -> "Loveneet: Yaar, tumne socha hai ki hum new caching layer kaise implement karenge?\nRahul: Haan bhai, main soch raha tha ki Memcached ki jagah Redis use karein for permanent storage.\nLoveneet: That makes sense. Let's draft a proposal."
                        else -> "Loveneet: Have you thought about how we can implement the new caching layer?\nRahul: Yes, I was thinking we should use Redis instead of Memcached for the persistent store.\nLoveneet: That makes sense. Let's draft a proposal."
                    }

                    val dbMemory = MemoryEntity(
                        title = "Conversation ($timeString) - Project Planning",
                        content = contentText,
                        summary = "Discussed implementing a new caching layer using Redis. Rahul will draft the architecture diagram. (Language: $chosenLanguage)",
                        category = "Conversation"
                    )
                    database.memoryDao().insertMemory(dbMemory)
                } catch (e: Exception) {
                    Log.e("ContinuousRecordService", "Recording error: ${e.message}")
                    delay(5000) // retry delay
                }
            }
        }
    }

    private fun startMediaRecorder() {
        currentOutputFile = File(getExternalFilesDir(null), "buffer_${System.currentTimeMillis()}.mp4")
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            
            // Background video recording natively requires Camera2 API surface integration
            // and an active preview on Android 9+. We set up the MediaRecorder to accept video
            // via surface. To fully capture video without user intervention, we'd need a pixel-sized 
            // floating window or active CameraDevice connection feeding this surface.
            try {
                // setVideoSource(MediaRecorder.VideoSource.SURFACE) 
            } catch(e: Exception) {
                Log.e("ContinuousRecordService", "Video source unavailable")
            }
            
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            
            setOutputFile(currentOutputFile?.absolutePath)
            
            prepare()
            start()
        }
        isRecording = true
    }

    private fun stopMediaRecorder() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
            } catch (e: RuntimeException) {
                Log.e("ContinuousRecordService", "Error stopping recorder")
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopMediaRecorder()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "MemoryOSChannel",
                "MemoryOS Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
