package com.jinian.anniversary.data.local

import androidx.room.TypeConverter

class ReminderOffsetsConverter {
    @TypeConverter
    fun fromOffsets(offsets: List<Int>): String = offsets.joinToString(",")

    @TypeConverter
    fun toOffsets(value: String): List<Int> =
        value.split(',').mapNotNull { it.trim().toIntOrNull() }.sortedDescending()
}