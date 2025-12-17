package com.example.kiddoabc.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modèle de données représentant une lettre
 */
@Parcelize
data class Letter(
    val id: String,
    val character: String,
    val name: String,
    val alphabetType: AlphabetType,
    val soundFileName: String, // Nom du fichier audio dans assets/sounds/
    val order: Int
) : Parcelable

/**
 * Type d'alphabet
 */
enum class AlphabetType {
    ARABIC,
    FRENCH
}