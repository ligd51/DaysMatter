package com.jinian.anniversary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anniversaries")
data class AnniversaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateType: String,
    val originalSolarEpochDay: Long,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val lunarIsLeap: Boolean,
    val category: String,
    val notes: String,
    val repeatYearly: Boolean,
    val reminderOffsets: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Long = 0,
)