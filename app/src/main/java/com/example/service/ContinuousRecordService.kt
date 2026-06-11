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

    companion object {
        const val ACTION_PAUSE = "com.example.service.action.PAUSE"
        const val ACTION_START = "com.example.service.action.START"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var currentOutputFile: File? = null
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = AppDatabase.getDatabase(applicationContext)
    }

    private val geminiHelper = com.example.api.GeminiHelper()

    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(this, ContinuousRecordService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, if (action == ACTION_PAUSE) 1 else 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildNotification(title: String, content: String, isRecordingNow: Boolean): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(this, "MemoryOSChannel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (isRecordingNow) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", getActionIntent(ACTION_PAUSE))
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Start", getActionIntent(ACTION_START))
        }
        
        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("memory_prefs", android.content.Context.MODE_PRIVATE)
        val recordEverything = prefs.getBoolean("record_everything", true)

        if (intent?.action == ACTION_PAUSE) {
            prefs.edit().putBoolean("record_everything", false).apply()
            recordingJob?.cancel()
            stopMediaRecorder()
            startRecordingLoop()
            return START_STICKY
        } else if (intent?.action == ACTION_START) {
            prefs.edit().putBoolean("record_everything", true).apply()
            recordingJob?.cancel()
            stopMediaRecorder()
            startRecordingLoop()
            return START_STICKY
        }

        val notification = buildNotification(
            if (recordEverything) "MemoryOS is Recording" else "MemoryOS Not Recording", 
            if (recordEverything) "Listening \u2022 Next save in calculating..." else "Recording paused.", 
            recordEverything
        )

        startForeground(1, notification)
        
        startRecordingLoop()

        return START_STICKY
    }

    private fun startRecordingLoop() {
        recordingJob?.cancel()
        recordingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = getSharedPreferences("memory_prefs", android.content.Context.MODE_PRIVATE)
                    val recordEverything = prefs.getBoolean("record_everything", true)
                    
                    val manager = getSystemService(NotificationManager::class.java)

                    if (!recordEverything) {
                        manager.notify(1, buildNotification("MemoryOS Not Recording", "Recording paused.", false))
                        delay(10 * 1000L) // check again in 10s if turned off
                        continue
                    }
                    val intervalMinutes = prefs.getFloat("interval", 30f).toInt()

                    startMediaRecorder()
                    
                    for (i in intervalMinutes downTo 1) {
                        manager.notify(1, buildNotification("MemoryOS is Recording", "Listening \u2022 Next save in $i min", true))
                        delay(60 * 1000L)
                    }

                    stopMediaRecorder()
                    
                    val fileToProcess = currentOutputFile
                    if (fileToProcess != null && fileToProcess.exists()) {
                        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                        val title = "Recording at $timeString"
                        
                        manager.notify(1, buildNotification("MemoryOS Processing", "Transcribing audio...", true))

                        val transcript = geminiHelper.transcribeAudio(fileToProcess)
                        val autoTranscribe = prefs.getBoolean("auto_transcribe", true)
                        
                        if (transcript.startsWith("Transcription empty") || transcript.startsWith("Transcription failed") || transcript.startsWith("Error") || transcript.startsWith("API Key is missing")) {
                            val dbMemory = MemoryEntity(
                                title = title,
                                content = "[Transcription failed - audio saved] $transcript",
                                category = "Life"
                            )
                            database.memoryDao().insertMemory(dbMemory)
                        } else {
                            val summary = if (autoTranscribe) geminiHelper.generateSummary(transcript) else ""
                            val category = geminiHelper.detectCategory(transcript)
                            
                            val dbMemory = MemoryEntity(
                                title = title,
                                content = transcript,
                                summary = summary,
                                category = category
                            )
                            database.memoryDao().insertMemory(dbMemory)
                            
                            fileToProcess.delete() // Save storage on success
                        }
                    }
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
