package com.example.hakonsreader.api.persistence

import androidx.room.TypeConverter
import com.example.hakonsreader.api.enums.FlairType

class EnumConverters {

    @TypeConverter
    fun fromFlairType(type: FlairType) = type.name

    @TypeConverter
    fun toFlairType(value: String) = enumValueOf<FlairType>(value)
}