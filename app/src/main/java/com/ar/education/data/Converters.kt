package com.ar.education.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun intListToString(list: List<Int>): String = gson.toJson(list)

    @TypeConverter
    fun stringToIntList(value: String): List<Int> =
        try {
            gson.fromJson(value, object : TypeToken<List<Int>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
}
