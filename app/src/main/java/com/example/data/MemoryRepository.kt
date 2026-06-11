package com.example.data

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun insert(memory: MemoryEntity) {
        memoryDao.insertMemory(memory)
    }

    suspend fun deleteById(id: Int) {
        memoryDao.deleteMemoryById(id)
    }
    
    suspend fun getMemoryById(id: Int): MemoryEntity? {
        return memoryDao.getMemoryById(id)
    }
}
