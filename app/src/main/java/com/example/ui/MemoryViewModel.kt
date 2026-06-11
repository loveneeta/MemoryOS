package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiHelper
import com.example.data.MemoryEntity
import com.example.data.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val repository: MemoryRepository,
    private val geminiHelper: GeminiHelper = GeminiHelper()
) : ViewModel() {

    val allMemories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchMemory() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            
            val queryWords = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            
            // Filter by keyword match first
            var selectedMemories = allMemories.value.filter { memory ->
                val titleMatch = queryWords.any { memory.title.lowercase().contains(it) }
                val contentMatch = queryWords.any { memory.content.lowercase().contains(it) }
                titleMatch || contentMatch
            }
            
            // If very few matches, fallback to taking the 50 most recent
            if (selectedMemories.size < 5) {
                selectedMemories = (selectedMemories + allMemories.value.take(50)).distinctBy { it.id }
            }
            
            var contextString = ""
            for (memory in selectedMemories.sortedByDescending { it.timestamp }) {
                val entry = "Date: ${memory.timestamp}, Title: ${memory.title}, Category: ${memory.category}, Content: ${memory.content}\n---\n"
                if (contextString.length + entry.length > 8000) {
                    break // Cap total context at ~8000 chars to avoid prompt limits
                }
                contextString += entry
            }
            
            val response = geminiHelper.askMemoryOS(query, contextString)
            _aiResponse.value = response
            _isAnalyzing.value = false
        }
    }

    fun insertMemory(title: String, content: String, category: String) {
        viewModelScope.launch {
            val dbMemory = MemoryEntity(
                title = title,
                content = content,
                category = category
            )
            repository.insert(dbMemory)
            
            // Background summarization
            launch {
                val summary = geminiHelper.generateSummary(content)
                if (summary.isNotBlank()) {
                    val updatedList = allMemories.value
                    updatedList.find { it.title == title && it.timestamp == dbMemory.timestamp }?.let {
                        val withSummary = it.copy(summary = summary)
                        repository.insert(withSummary) // REPLACE strategy
                    }
                }
            }
        }
    }
    
    fun deleteMemory(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
    
    fun clearAiResponse() {
        _aiResponse.value = ""
        _searchQuery.value = ""
    }
}
