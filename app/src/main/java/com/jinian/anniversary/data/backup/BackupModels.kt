package com.jinian.anniversary.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,
    val exportedAt: String,
    val events: List<BackupEventDto>,
    val themeColor: String,
    val themeMode: String,
)

@Serializable
data class BackupEventDto(
    val id: Long,
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
    val sortOrder: Long = 0,
)

@Serializable
data class BackupPayload(
    val events: List<BackupEventDto>,
    val themeColor: String,
    val themeMode: String,
)