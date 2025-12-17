package com.example.kiddoabc.data.repository

import android.content.Context
import com.example.kiddoabc.data.local.dao.ProgressDao
import com.example.kiddoabc.data.local.entities.ProgressEntity
import com.example.kiddoabc.data.models.AlphabetType
import com.example.kiddoabc.data.models.Letter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository pour gérer les lettres et la progression
 */
class LetterRepository(
    private val progressDao: ProgressDao
) {

    /**
     * Charge les lettres depuis le fichier JSON
     */
    suspend fun getLettersByType(
        context: Context,
        alphabetType: AlphabetType
    ): List<Letter> = withContext(Dispatchers.IO) {
        try {
            val fileName = when (alphabetType) {
                AlphabetType.ARABIC -> "arabic_letters.json"
                AlphabetType.FRENCH -> "french_letters.json"
            }

            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Letter>>() {}.type
            val letters: List<Letter> = Gson().fromJson(jsonString, type)

            letters.sortedBy { it.order }
        } catch (e: IOException) {
            emptyList()
        }
    }

    /**
     * Récupère une lettre par son ID
     */
    suspend fun getLetterById(
        context: Context,
        letterId: String,
        alphabetType: AlphabetType
    ): Letter? = withContext(Dispatchers.IO) {
        getLettersByType(context, alphabetType).find { it.id == letterId }
    }

    /**
     * Récupère la progression pour un type d'alphabet
     */
    fun getProgressByAlphabet(alphabetType: AlphabetType): Flow<List<ProgressEntity>> {
        return progressDao.getProgressByAlphabet(alphabetType)
    }

    /**
     * Récupère la progression d'une lettre spécifique
     */
    suspend fun getProgressByLetter(letterId: String): ProgressEntity? {
        return progressDao.getProgressByLetter(letterId)
    }

    /**
     * Sauvegarde ou met à jour la progression d'une lettre
     */
    suspend fun saveProgress(
        letterId: String,
        alphabetType: AlphabetType,
        isCompleted: Boolean,
        attempts: Int,
        successRate: Float
    ) {
        val progress = ProgressEntity(
            letterId = letterId,
            alphabetType = alphabetType,
            isCompleted = isCompleted,
            attempts = attempts,
            lastPracticedTimestamp = System.currentTimeMillis(),
            successRate = successRate
        )
        progressDao.insertProgress(progress)
    }

    /**
     * Compte le nombre de lettres complétées
     */
    fun getCompletedCount(alphabetType: AlphabetType): Flow<Int> {
        return progressDao.getCompletedCount(alphabetType)
    }

    /**
     * Calcule le taux de réussite moyen
     */
    fun getAverageSuccessRate(alphabetType: AlphabetType): Flow<Float> {
        return progressDao.getAverageSuccessRate(alphabetType)
    }

    /**
     * Supprime toute la progression
     */
    suspend fun deleteAllProgress() {
        progressDao.deleteAllProgress()
    }
}