package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val summary: String? = null,
    val category: String = "Life", // e.g. Idea, Meeting, Life, Work
    val timestamp: Long = System.currentTimeMillis()
)
