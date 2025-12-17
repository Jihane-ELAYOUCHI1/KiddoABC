package com.example.kiddoabc.data.local.dao

import androidx.room.*
import com.example.kiddoabc.data.local.entities.ProgressEntity
import com.example.kiddoabc.data.models.AlphabetType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress WHERE alphabetType = :alphabetType")
    fun getProgressByAlphabet(alphabetType: AlphabetType): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE letterId = :letterId")
    suspend fun getProgressByLetter(letterId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Delete
    suspend fun deleteProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE alphabetType = :alphabetType")
    suspend fun deleteProgressByAlphabet(alphabetType: AlphabetType)

    @Query("DELETE FROM progress")
    suspend fun deleteAllProgress()

    @Query("SELECT COUNT(*) FROM progress WHERE alphabetType = :alphabetType AND isCompleted = 1")
    fun getCompletedCount(alphabetType: AlphabetType): Flow<Int>

    @Query("SELECT AVG(successRate) FROM progress WHERE alphabetType = :alphabetType")
    fun getAverageSuccessRate(alphabetType: AlphabetType): Flow<Float>
}