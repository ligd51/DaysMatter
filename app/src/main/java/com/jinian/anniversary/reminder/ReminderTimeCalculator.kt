package com.jinian.anniversary.reminder

import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.model.Anniversary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTimeCalculator(
    private val calculator: AnniversaryCalculator,
) {

    fun nextTrigger(event: Anniversary, offsetDays: Int, now: LocalDateTime): LocalDateTime? {
        require(offsetDays >= 0)
        val reminderTime = LocalTime.of(event.reminderHour, event.reminderMinute)

        if (!event.repeatYearly) {
            val originalDate = LocalDate.ofEpochDay(event.originalSolarEpochDay)
            val trigger = originalDate.minusDays(offsetDays.toLong()).atTime(reminderTime)
            return if (trigger.isAfter(now)) trigger else null
        }

        var occurrence = calculator.nextOccurrenceOnOrAfter(
            event,
            now.toLocalDate().minusDays(offsetDays.toLong()),
        ) ?: return null

        repeat(10) {
            val trigger = occurrence.minusDays(offsetDays.toLong()).atTime(reminderTime)
            if (trigger.isAfter(now)) return trigger
            occurrence = calculator.nextOccurrenceOnOrAfter(event, occurrence.plusDays(1)) ?: return null
        }
        return null
    }

    fun notificationText(event: Anniversary, offsetDays: Int, occurrence: LocalDate): String {
        return if (offsetDays == 0) {
            val years = calculator.anniversaryYearsAt(event, occurrence)
            if (years > 0) "今天是「${event.title}」· $years 周年" else "今天是「${event.title}」"
        } else {
            val years = calculator.anniversaryYearsAt(event, occurrence)
            val suffix = if (years > 0) " · 第 $years 周年" else ""
            "「${event.title}」还有 $offsetDays 天$suffix"
        }
    }
}