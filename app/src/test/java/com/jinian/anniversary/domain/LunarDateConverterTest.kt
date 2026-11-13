package com.jinian.anniversary.domain

import com.google.common.truth.Truth.assertThat
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.LunarDate
import org.junit.Test
import java.time.LocalDate

class LunarDateConverterTest {

    private val converter = LunarDateConverter()

    @Test
    fun `已知公农历日期可双向转换`() {
        assertThat(converter.solarToLunar(LocalDate.of(2024, 2, 10)))
            .isEqualTo(LunarDate(2024, 1, 1, false))
        assertThat(converter.lunarToSolar(LunarDate(2024, 1, 1, false)))
            .isEqualTo(LocalDate.of(2024, 2, 10))
        assertThat(converter.solarToLunar(LocalDate.of(2023, 2, 21)))
            .isEqualTo(LunarDate(2023, 2, 2, false))
    }

    @Test
    fun `2023 年闰二月可识别并换算`() {
        assertThat(converter.solarToLunar(LocalDate.of(2023, 3, 22)))
            .isEqualTo(LunarDate(2023, 2, 1, true))
        assertThat(converter.lunarToSolar(LunarDate(2023, 2, 1, true)))
            .isEqualTo(LocalDate.of(2023, 3, 22))
        assertThat(converter.monthOptions(2023).count { it.isLeap && it.month == 2 }).isEqualTo(1)
    }

    @Test
    fun `闰月不存在时自动回退到普通月`() {
        val normalized = converter.normalize(LunarDate(2024, 2, 1, true))
        assertThat(normalized.isLeap).isFalse()
        assertThat(converter.lunarToSolar(normalized)).isEqualTo(LocalDate.of(2024, 3, 10))
    }

    @Test
    fun `农历三十不存在时回退到当月最后一天`() {
        val normalized = converter.normalize(LunarDate(2026, 2, 30, false))
        assertThat(normalized.day).isEqualTo(29)
        assertThat(converter.lunarToSolar(normalized)).isEqualTo(LocalDate.of(2026, 4, 16))
    }

    @Test
    fun `非法闰月会被拒绝`() {
        assertThat(converter.isValid(LunarDate(2024, 2, 1, true))).isFalse()
        assertThat(converter.isValid(LunarDate(2023, 2, 1, true))).isTrue()
        assertThat(converter.isValid(LunarDate(2023, 2, 30, true))).isFalse()
    }
}