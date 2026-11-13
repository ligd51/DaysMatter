package com.jinian.anniversary.data.backup

import com.jinian.anniversary.data.settings.ThemeColor
import com.jinian.anniversary.data.settings.ThemeMode
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.DateType
import com.jinian.anniversary.domain.model.LunarDate
import kotlinx.serialization.json.Json
import java.time.LocalDate

class BackupValidator(private val lunarConverter: LunarDateConverter = LunarDateConverter()) {

    fun validate(content: String): Result<BackupPayload> = runCatching {
        val envelope = json.decodeFromString<BackupEnvelope>(content)
        val errors = mutableListOf<String>()
        if (envelope.schemaVersion != 1) errors.add("备份版本不受支持：${envelope.schemaVersion}")
        val ids = mutableSetOf<Long>()
        envelope.events.forEachIndexed { index, event ->
            validateEvent(event, index, ids, errors)
        }
        val themeColor = runCatching { ThemeColor.valueOf(envelope.themeColor) }.getOrNull()
        val themeMode = runCatching { ThemeMode.valueOf(envelope.themeMode) }.getOrNull()
        if (themeColor == null) errors.add("主题色无效")
        if (themeMode == null) errors.add("主题模式无效")
        if (errors.isNotEmpty()) error(errors.joinToString("\n"))
        BackupPayload(envelope.events, envelope.themeColor, envelope.themeMode)
    }

    private fun validateEvent(
        event: BackupEventDto,
        index: Int,
        ids: MutableSet<Long>,
        errors: MutableList<String>,
    ) {
        val prefix = "第 ${index + 1} 条"
        if (event.title.isBlank()) errors.add("$prefix：标题不能为空")
        if (event.id <= 0L || !ids.add(event.id)) errors.add("$prefix：事件 ID 重复或无效")
        val dateType = runCatching { DateType.valueOf(event.dateType) }.getOrNull()
        if (dateType == null) {
            errors.add("$prefix：日期类型无效")
            return
        }
        if (event.lunarYear !in LunarDateConverter.MIN_LUNAR_YEAR..LunarDateConverter.MAX_LUNAR_YEAR) {
            errors.add("$prefix：农历年份超出 1901-2100")
        }
        val lunar = LunarDate(event.lunarYear, event.lunarMonth, event.lunarDay, event.lunarIsLeap)
        if (!lunarConverter.isValid(lunar)) {
            errors.add("$prefix：农历日期非法")
        }
        val originalEpoch = event.originalSolarEpochDay
        val date = runCatching { LocalDate.ofEpochDay(originalEpoch) }.getOrNull()
        if (date == null || date.year !in 1900..2100) {
            errors.add("$prefix：公历日期非法或超出支持范围")
        } else {
            val actualLunar = lunarConverter.solarToLunar(date)
            if (actualLunar != lunar) errors.add("$prefix：农历字段与公历日期不一致")
        }
        val offsets = event.reminderOffsets.split(',').mapNotNull { it.trim().toIntOrNull() }
        if (offsets.size != event.reminderOffsets.split(',').count { it.isNotBlank() }) {
            errors.add("$prefix：提醒偏移包含非整数")
        }
        if (offsets.any { it < 0 || it > 3650 }) errors.add("$prefix：提醒偏移必须在 0-3650 天")
        if (offsets.distinct().size != offsets.size) errors.add("$prefix：提醒偏移重复")
        if (event.reminderHour !in 0..23 || event.reminderMinute !in 0..59) {
            errors.add("$prefix：提醒时间非法")
        }
        if (event.createdAt < 0 || event.updatedAt < 0) errors.add("$prefix：时间戳非法")
        if (event.sortOrder < 0) errors.add("$prefix：排序值非法")
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = false
            isLenient = false
        }
    }
}