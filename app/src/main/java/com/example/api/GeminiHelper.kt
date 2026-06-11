package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class GeminiHelper {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun askMemoryOS(query: String, contextMemories: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing. Please configure it in AI Studio Secrets."
        }
        
        val systemPrompt = "You are the MemoryOS AI, an intelligent personal memory assistant. " +
            "You help users recall details about their life based solely on their provided logs. " +
            "Do not invent details. Answer concisely and use a helpful tone."
            
        val prompt = "User Query: $query\n\nRecent Memory Context:\n$contextMemories"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I'm sorry, I couldn't process this request."
        } catch (e: Exception) {
            "Error analyzing memory: ${e.message}"
        }
    }
    
    suspend fun generateSummary(content: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext ""
        
        val systemPrompt = "You are the MemoryOS Summarization tool. Given life log text, produce a very brief 1-2 sentence summary."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = content)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun detectCategory(transcript: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext "Life"
        
        val prompt = "Given this transcript, classify it into exactly one of these categories: Conversation, Meeting, Idea, Task, Life. Return only the category word, nothing else."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "$prompt\n\nTranscript: $transcript"))))
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val category = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Life"
            val valid = listOf("Conversation", "Meeting", "Idea", "Task", "Life")
            if (valid.contains(category)) category else "Life"
        } catch (e: Exception) {
            "Life"
        }
    }

    suspend fun transcribeAudio(file: File): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext "API Key is missing."

        try {
            val uploadUrl = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=$apiKey"
            val fileBody = file.asRequestBody("audio/mp4".toMediaType())
            
            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .addHeader("X-Goog-Upload-Command", "start, upload, finalize")
                .addHeader("X-Goog-Upload-Header-Content-Length", file.length().toString())
                .addHeader("X-Goog-Upload-Header-Content-Type", "audio/mp4")
                .addHeader("Content-Type", "audio/mp4")
                .post(fileBody)
                .build()

            val uploadResponse = RetrofitClient.okHttpClient.newCall(uploadRequest).execute()
            val uploadResponseBody = uploadResponse.body?.string() ?: ""
            if (!uploadResponse.isSuccessful) {
                return@withContext "Error uploading file: $uploadResponseBody"
            }

            val uploadData = jsonParser.decodeFromString<UploadFileResponse>(uploadResponseBody)
            val fileUri = uploadData.file?.uri
            
            if (fileUri == null) {
                return@withContext "Failed to get file URI from upload response."
            }

            val prompt = "Transcribe this audio recording accurately. If multiple speakers, label them Speaker 1, Speaker 2, etc. Return only the transcript, nothing else."
            val contentRequest = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(
                    Part(fileData = FileData(mimeType = "audio/mp4", fileUri = fileUri)),
                    Part(text = prompt)
                )))
            )

            val apiResponse = RetrofitClient.service.generateContent(apiKey, contentRequest)
            apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Transcription empty."
        } catch (e: Exception) {
            "Transcription failed: ${e.message}"
        }
    }
}
