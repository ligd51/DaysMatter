package com.jinian.anniversary.reminder

import com.google.common.truth.Truth.assertThat
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.DateType
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderTimeCalculatorTest {

    private val calculator = AnniversaryCalculator(LunarDateConverter())
    private val reminderCalculator = ReminderTimeCalculator(calculator)

    @Test
    fun `多偏移每年滚动到下一次有效提醒`() {
        val event = solarEvent(LocalDate.of(2020, 1, 2), listOf(7, 3, 0))
        val afterToday = LocalDateTime.of(2026, 1, 2, 10, 0)
        assertThat(reminderCalculator.nextTrigger(event, 0, afterToday))
            .isEqualTo(LocalDateTime.of(2027, 1, 2, 9, 0))
        assertThat(reminderCalculator.nextTrigger(event, 3, afterToday))
            .isEqualTo(LocalDateTime.of(2026, 12, 30, 9, 0))
        assertThat(reminderCalculator.nextTrigger(event, 7, afterToday))
            .isEqualTo(LocalDateTime.of(2026, 12, 26, 9, 0))
    }

    @Test
    fun `提前提醒可以落在原始日期所在年的上一年`() {
        val event = solarEvent(LocalDate.of(2027, 1, 2), listOf(30))
        assertThat(reminderCalculator.nextTrigger(event, 30, LocalDateTime.of(2026, 12, 1, 9, 0)))
            .isEqualTo(LocalDateTime.of(2026, 12, 3, 9, 0))
    }

    @Test
    fun `非重复事件到期后停止提醒`() {
        val event = solarEvent(LocalDate.of(2020, 1, 2), listOf(0), repeatYearly = false)
        assertThat(
            reminderCalculator.nextTrigger(
                event,
                0,
                LocalDateTime.of(2026, 11, 10, 9, 0),
            )
        ).isNull()
    }

    @Test
    fun `事件 ID 和偏移生成稳定且可区分的请求码`() {
        val a = ReminderRequestCode.forEvent(1L, 7)
        val b = ReminderRequestCode.forEvent(1L, 7)
        val c = ReminderRequestCode.forEvent(1L, 3)
        val d = ReminderRequestCode.forEvent(2L, 7)
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c)
        assertThat(a).isNotEqualTo(d)
    }

    private fun solarEvent(
        date: LocalDate,
        offsets: List<Int>,
        repeatYearly: Boolean = true,
    ): Anniversary {
        val converter = LunarDateConverter()
        return Anniversary(
            title = "测试",
            dateType = DateType.SOLAR,
            originalSolarEpochDay = date.toEpochDay(),
            lunarDate = converter.solarToLunar(date),
            category = "纪念日",
            notes = "",
            repeatYearly = repeatYearly,
            reminderOffsets = offsets.distinct().sortedDescending(),
            reminderHour = 9,
            reminderMinute = 0,
            createdAt = 0,
            updatedAt = 0,
        )
    }
}