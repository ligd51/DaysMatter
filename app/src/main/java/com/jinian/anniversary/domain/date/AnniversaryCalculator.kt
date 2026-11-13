package com.jinian.anniversary.domain.date

import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.AnniversarySnapshot
import com.jinian.anniversary.domain.model.DateType
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

class AnniversaryCalculator(
    private val lunarConverter: LunarDateConverter,
) {

    fun snapshot(event: Anniversary, today: LocalDate): AnniversarySnapshot {
        val originalDate = LocalDate.ofEpochDay(event.originalSolarEpochDay)
        val signedDays = ChronoUnit.DAYS.between(today, originalDate)
        val isToday = signedDays == 0L
        val isFuture = signedDays > 0L
        val inclusiveDayNumber = if (signedDays <= 0L) abs(signedDays) + 1L else 0L
        val completedYears = max(0, Period.between(originalDate, today).years)
        val nextAnniversary = if (event.repeatYearly) {
            nextOccurrenceOnOrAfter(event, today)
        } else {
            null
        }
        return AnniversarySnapshot(
            originalDate = originalDate,
            targetDate = nextAnniversary ?: originalDate,
            isFuture = isFuture,
            isToday = isToday,
            signedDays = signedDays,
            inclusiveDayNumber = inclusiveDayNumber,
            completedYears = completedYears,
            nextAnniversaryDate = nextAnniversary,
            daysUntilNextAnniversary = nextAnniversary?.let { ChronoUnit.DAYS.between(today, it) },
        )
    }

    fun nextOccurrenceOnOrAfter(event: Anniversary, fromDate: LocalDate): LocalDate? {
        if (!event.repeatYearly) return null
        val originalDate = LocalDate.ofEpochDay(event.originalSolarEpochDay)
        if (fromDate < originalDate) return originalDate

        var candidate = occurrenceForYear(event, occurrenceYear(event, fromDate))
        if (candidate >= fromDate) return candidate

        repeat(3) {
            candidate = occurrenceForYear(event, nextOccurrenceYear(event, candidate))
            if (candidate >= fromDate) return candidate
        }
        error("无法计算下一次纪念日：${event.title}")
    }

    fun occurrenceForYear(event: Anniversary, year: Int): LocalDate {
        return when (event.dateType) {
            DateType.SOLAR -> {
                val original = LocalDate.ofEpochDay(event.originalSolarEpochDay)
                solarOccurrence(year, original.monthValue, original.dayOfMonth)
            }
            DateType.LUNAR -> {
                val normalized = lunarConverter.normalize(
                    event.lunarDate.copy(year = year.coerceIn(LunarDateConverter.MIN_LUNAR_YEAR, LunarDateConverter.MAX_LUNAR_YEAR))
                )
                lunarConverter.lunarToSolar(normalized)
            }
        }
    }

    fun anniversaryYearsAt(event: Anniversary, occurrence: LocalDate): Int {
        val original = LocalDate.ofEpochDay(event.originalSolarEpochDay)
        return max(0, Period.between(original, occurrence).years)
    }

    fun solarOccurrence(year: Int, month: Int, day: Int): LocalDate {
        return if (month == 2 && day == 29) {
            LocalDate.of(year, 2, if (java.time.Year.isLeap(year.toLong())) 29 else 28)
        } else {
            LocalDate.of(year, month, day)
        }
    }

    private fun occurrenceYear(event: Anniversary, fromDate: LocalDate): Int {
        return when (event.dateType) {
            DateType.SOLAR -> fromDate.year
            DateType.LUNAR -> lunarConverter.solarToLunar(fromDate).year
        }
    }

    private fun nextOccurrenceYear(event: Anniversary, occurrence: LocalDate): Int {
        return when (event.dateType) {
            DateType.SOLAR -> occurrence.year + 1
            DateType.LUNAR -> lunarConverter.solarToLunar(occurrence).year + 1
        }
    }
}