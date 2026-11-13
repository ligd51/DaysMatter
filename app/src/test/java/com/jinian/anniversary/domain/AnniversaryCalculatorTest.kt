package com.jinian.anniversary.domain

import com.google.common.truth.Truth.assertThat
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.DateType
import com.jinian.anniversary.domain.model.LunarDate
import org.junit.Test
import java.time.LocalDate

class AnniversaryCalculatorTest {

    private val converter = LunarDateConverter()
    private val calculator = AnniversaryCalculator(converter)

    @Test
    fun `未来日期显示相距天数且不显示已过天数`() {
        val event = solarEvent(LocalDate.of(2026, 11, 13))
        val result = calculator.snapshot(event, LocalDate.of(2026, 11, 10))
        assertThat(result.isFuture).isTrue()
        assertThat(result.signedDays).isEqualTo(3)
        assertThat(result.inclusiveDayNumber).isEqualTo(0)
    }

    @Test
    fun `当天按首日计入且周年为零`() {
        val event = solarEvent(LocalDate.of(2026, 11, 10))
        val result = calculator.snapshot(event, LocalDate.of(2026, 11, 10))
        assertThat(result.isToday).isTrue()
        assertThat(result.inclusiveDayNumber).isEqualTo(1)
        assertThat(result.completedYears).isEqualTo(0)
    }

    @Test
    fun `过去 1023 天显示第 1024 天`() {
        val today = LocalDate.of(2026, 11, 10)
        val event = solarEvent(today.minusDays(1023), repeatYearly = false)
        val result = calculator.snapshot(event, today)
        assertThat(result.signedDays).isEqualTo(-1023)
        assertThat(result.inclusiveDayNumber).isEqualTo(1024)
    }

    @Test
    fun `闰年二月二十九在平年回退到二月二十八`() {
        val event = solarEvent(LocalDate.of(2024, 2, 29))
        assertThat(calculator.occurrenceForYear(event, 2025)).isEqualTo(LocalDate.of(2025, 2, 28))
        assertThat(calculator.occurrenceForYear(event, 2028)).isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun `年度事件跨年计算下一次周年`() {
        val event = solarEvent(LocalDate.of(2020, 1, 2))
        assertThat(calculator.nextOccurrenceOnOrAfter(event, LocalDate.of(2026, 12, 31)))
            .isEqualTo(LocalDate.of(2027, 1, 2))
        assertThat(calculator.nextOccurrenceOnOrAfter(event, LocalDate.of(2027, 1, 1)))
            .isEqualTo(LocalDate.of(2027, 1, 2))
    }

    @Test
    fun `农历年度事件跨春节计算下一周年`() {
        val original = converter.lunarToSolar(LunarDate(2023, 12, 30, false))
        val event = lunarEvent(LunarDate(2023, 12, 30, false), original)
        assertThat(calculator.nextOccurrenceOnOrAfter(event, LocalDate.of(2026, 2, 10)))
            .isEqualTo(LocalDate.of(2026, 2, 16))
    }

    private fun solarEvent(date: LocalDate, repeatYearly: Boolean = true): Anniversary {
        return Anniversary(
            title = "测试",
            dateType = DateType.SOLAR,
            originalSolarEpochDay = date.toEpochDay(),
            lunarDate = converter.solarToLunar(date),
            category = "纪念日",
            notes = "",
            repeatYearly = repeatYearly,
            reminderOffsets = listOf(0),
            reminderHour = 9,
            reminderMinute = 0,
            createdAt = 0,
            updatedAt = 0,
        )
    }

    private fun lunarEvent(lunarDate: LunarDate, original: LocalDate): Anniversary {
        return Anniversary(
            title = "测试",
            dateType = DateType.LUNAR,
            originalSolarEpochDay = original.toEpochDay(),
            lunarDate = lunarDate,
            category = "纪念日",
            notes = "",
            repeatYearly = true,
            reminderOffsets = listOf(0),
            reminderHour = 9,
            reminderMinute = 0,
            createdAt = 0,
            updatedAt = 0,
        )
    }
}