package com.example.kiddoabc.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.kiddoabc.data.models.AlphabetType

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey
    val letterId: String,
    val alphabetType: AlphabetType,
    val isCompleted: Boolean = false,
    val attempts: Int = 0,
    val lastPracticedTimestamp: Long = 0,
    val successRate: Float = 0f
)