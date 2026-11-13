package com.jinian.anniversary.domain.model

data class Anniversary(
    val id: Long = 0,
    val title: String,
    val dateType: DateType,
    val originalSolarEpochDay: Long,
    val lunarDate: LunarDate,
    val category: String,
    val notes: String,
    val repeatYearly: Boolean,
    val reminderOffsets: List<Int>,
    val reminderHour: Int,
    val reminderMinute: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Long = 0,
)