package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiHelper {
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
}
