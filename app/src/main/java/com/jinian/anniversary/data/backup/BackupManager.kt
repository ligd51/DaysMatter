package com.jinian.anniversary.data.backup

import android.content.Context
import com.jinian.anniversary.data.repository.AnniversaryRepository
import com.jinian.anniversary.data.settings.SettingsRepository
import com.jinian.anniversary.domain.date.LunarDateConverter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupManager(
    private val context: Context,
    private val anniversaryRepository: AnniversaryRepository,
    private val settingsRepository: SettingsRepository,
    private val validator: BackupValidator = BackupValidator(),
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportJson(): String {
        val settings = settingsRepository.settings.first()
        val envelope = BackupEnvelope(
            schemaVersion = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            events = anniversaryRepository.getAll().map { event ->
                BackupEventDto(
                    id = event.id,
                    title = event.title,
                    dateType = event.dateType.name,
                    originalSolarEpochDay = event.originalSolarEpochDay,
                    lunarYear = event.lunarDate.year,
                    lunarMonth = event.lunarDate.month,
                    lunarDay = event.lunarDate.day,
                    lunarIsLeap = event.lunarDate.isLeap,
                    category = event.category,
                    notes = event.notes,
                    repeatYearly = event.repeatYearly,
                    reminderOffsets = event.reminderOffsets.joinToString(","),
                    reminderHour = event.reminderHour,
                    reminderMinute = event.reminderMinute,
                    createdAt = event.createdAt,
                    updatedAt = event.updatedAt,
                    sortOrder = event.sortOrder,
                )
            },
            themeColor = settings.themeColor.name,
            themeMode = settings.themeMode.name,
        )
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun validate(content: String): Result<BackupPayload> = validator.validate(content)

    suspend fun import(payload: BackupPayload) {
        val converter = LunarDateConverter()
        anniversaryRepository.replaceAll(
            payload.events.map { dto ->
                converter.let { _ ->
                    com.jinian.anniversary.domain.model.Anniversary(
                        id = dto.id,
                        title = dto.title,
                        dateType = com.jinian.anniversary.domain.model.DateType.valueOf(dto.dateType),
                        originalSolarEpochDay = dto.originalSolarEpochDay,
                        lunarDate = com.jinian.anniversary.domain.model.LunarDate(
                            dto.lunarYear,
                            dto.lunarMonth,
                            dto.lunarDay,
                            dto.lunarIsLeap,
                        ),
                        category = dto.category,
                        notes = dto.notes,
                        repeatYearly = dto.repeatYearly,
                        reminderOffsets = dto.reminderOffsets.split(',').mapNotNull { it.trim().toIntOrNull() }.sortedDescending(),
                        reminderHour = dto.reminderHour,
                        reminderMinute = dto.reminderMinute,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        sortOrder = dto.sortOrder,
                    )
                }
            }
        )
        settingsRepository.setThemeColor(com.jinian.anniversary.data.settings.ThemeColor.valueOf(payload.themeColor))
        settingsRepository.setThemeMode(com.jinian.anniversary.data.settings.ThemeMode.valueOf(payload.themeMode))
    }

    fun writeJson(uri: android.net.Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("无法写入备份文件")
    }

    fun readJson(uri: android.net.Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("无法读取备份文件")
    }

    companion object {
        fun backupFileName(): String =
            "记念-backup-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
    }
}