package com.example.kiddoabc.data.models

data class Alphabet(
    val type: AlphabetType,
    val name: String,
    val letters: List<Letter>,
    val colorResource: Int,
    val displayText: String
)