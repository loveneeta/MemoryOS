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
        if (_searchQuery.value.isBlank()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            val memories = allMemories.value.take(20).joinToString(separator = "\n---\n") { 
                "Date: ${it.timestamp}, Title: ${it.title}, Category: ${it.category}, Content: ${it.content}" 
            }
            val response = geminiHelper.askMemoryOS(_searchQuery.value, memories)
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
