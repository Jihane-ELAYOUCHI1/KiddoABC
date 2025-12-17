package com.example.kiddoabc.utils

import androidx.room.TypeConverter
import com.example.kiddoabc.data.models.AlphabetType

class Converters {

    @TypeConverter
    fun fromAlphabetType(value: AlphabetType): String {
        return value.name
    }

    @TypeConverter
    fun toAlphabetType(value: String): AlphabetType {
        return AlphabetType.valueOf(value)
    }
}