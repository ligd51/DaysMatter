package com.jinian.anniversary.data.repository

import androidx.room.withTransaction
import com.jinian.anniversary.data.local.AnniversaryDao
import com.jinian.anniversary.data.local.AnniversaryEntity
import com.jinian.anniversary.data.local.JinianDatabase
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.DateType
import com.jinian.anniversary.domain.model.LunarDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnniversaryRepository(
    private val database: JinianDatabase,
    private val dao: AnniversaryDao,
    private val lunarConverter: LunarDateConverter,
) {
    fun observeAll(): Flow<List<Anniversary>> =
        dao.observeAll().map { list -> list.map { it.toDomain(lunarConverter) } }

    fun observeById(id: Long): Flow<Anniversary?> =
        dao.observeById(id).map { it?.toDomain(lunarConverter) }

    suspend fun getAll(): List<Anniversary> =
        dao.getAll().map { it.toDomain(lunarConverter) }

    suspend fun getById(id: Long): Anniversary? =
        dao.getById(id)?.toDomain(lunarConverter)

    suspend fun save(event: Anniversary): Long = database.withTransaction {
        if (event.id <= 0L) {
            val nextSortOrder = (dao.getMaxSortOrder() ?: -1L) + 1L
            dao.insert(event.toEntity(lunarConverter).copy(sortOrder = nextSortOrder))
        } else {
            dao.upsert(event.toEntity(lunarConverter))
            event.id
        }
    }

    suspend fun move(eventId: Long, direction: Int): Boolean = database.withTransaction {
        val items = dao.getAll()
        val fromIndex = items.indexOfFirst { it.id == eventId }
        val toIndex = fromIndex + direction
        if (fromIndex < 0 || toIndex !in items.indices) return@withTransaction false

        val ordered = items.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
        ordered.forEachIndexed { index, entity ->
            if (entity.sortOrder != index.toLong()) {
                dao.updateSortOrder(entity.id, index.toLong())
            }
        }
        true
    }

    suspend fun delete(id: Long) = database.withTransaction { dao.delete(id) }

    suspend fun restore(event: Anniversary) = database.withTransaction {
        dao.upsert(event.toEntity(lunarConverter))
    }

    suspend fun repairLegacyEventIds() = database.withTransaction {
        val legacyEvents = dao.getAll().filter { it.id <= 0L }
        legacyEvents.forEach { legacy ->
            dao.insert(legacy.copy(id = 0))
            dao.delete(legacy.id)
        }
    }

    suspend fun replaceAll(events: List<Anniversary>) = database.withTransaction {
        dao.replaceAll(events.map { it.toEntity(lunarConverter) })
    }
}

private fun AnniversaryEntity.toDomain(converter: LunarDateConverter): Anniversary {
    return Anniversary(
        id = id,
        title = title,
        dateType = runCatching { DateType.valueOf(dateType) }.getOrDefault(DateType.SOLAR),
        originalSolarEpochDay = originalSolarEpochDay,
        lunarDate = LunarDate(lunarYear, lunarMonth, lunarDay, lunarIsLeap),
        category = category,
        notes = notes,
        repeatYearly = repeatYearly,
        reminderOffsets = reminderOffsets.split(',').mapNotNull { it.trim().toIntOrNull() }.sortedDescending(),
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
    )
}

private fun Anniversary.toEntity(converter: LunarDateConverter): AnniversaryEntity {
    val lunar = converter.normalize(lunarDate)
    return AnniversaryEntity(
        id = id,
        title = title,
        dateType = dateType.name,
        originalSolarEpochDay = originalSolarEpochDay,
        lunarYear = lunar.year,
        lunarMonth = lunar.month,
        lunarDay = lunar.day,
        lunarIsLeap = lunar.isLeap,
        category = category,
        notes = notes,
        repeatYearly = repeatYearly,
        reminderOffsets = reminderOffsets.sortedDescending().joinToString(","),
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
    )
}