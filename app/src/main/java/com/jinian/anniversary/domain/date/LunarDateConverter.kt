package com.jinian.anniversary.domain.date

import com.jinian.anniversary.domain.model.LunarDate
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import java.time.LocalDate

class LunarDateConverter {

    fun solarToLunar(date: LocalDate): LunarDate {
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
        val month = lunar.month
        return LunarDate(
            year = lunar.year,
            month = kotlin.math.abs(month),
            day = lunar.day,
            isLeap = month < 0,
        )
    }

    fun lunarToSolar(date: LunarDate): LocalDate {
        require(isValid(date)) { "非法的农历日期：$date" }
        val signedMonth = if (date.isLeap) -date.month else date.month
        val lunar = Lunar.fromYmd(date.year, signedMonth, date.day)
        return LocalDate.of(lunar.solar.year, lunar.solar.month, lunar.solar.day)
    }

    fun isValid(date: LunarDate): Boolean {
        if (date.year !in MIN_LUNAR_YEAR..MAX_LUNAR_YEAR) return false
        if (date.month !in 1..12 || date.day !in 1..30) return false
        return runCatching {
            val year = LunarYear.fromYear(date.year)
            if (date.isLeap && year.leapMonth != date.month) return@runCatching false
            val month = year.getMonth(if (date.isLeap) -date.month else date.month)
            date.day <= month.dayCount
        }.getOrDefault(false)
    }

    fun normalize(date: LunarDate): LunarDate {
        val year = date.year.coerceIn(MIN_LUNAR_YEAR, MAX_LUNAR_YEAR)
        val lunarYear = LunarYear.fromYear(year)
        val leapAvailable = lunarYear.leapMonth == date.month
        val isLeap = date.isLeap && leapAvailable
        val month = if (isLeap) -date.month else date.month
        val dayCount = lunarYear.getMonth(month).dayCount
        return LunarDate(
            year = year,
            month = date.month,
            day = date.day.coerceIn(1, dayCount),
            isLeap = isLeap,
        )
    }

    fun monthOptions(year: Int): List<LunarMonthOption> {
        require(year in MIN_LUNAR_YEAR..MAX_LUNAR_YEAR)
        return LunarYear.fromYear(year).monthsInYear.map { month ->
            LunarMonthOption(
                month = kotlin.math.abs(month.month),
                isLeap = month.isLeap,
            )
        }
    }

    fun dayCount(year: Int, month: Int, isLeap: Boolean): Int {
        val lunarYear = LunarYear.fromYear(year)
        val safeLeap = isLeap && lunarYear.leapMonth == month
        return lunarYear.getMonth(if (safeLeap) -month else month).dayCount
    }

    fun format(date: LunarDate): String {
        val signedMonth = if (date.isLeap) -date.month else date.month
        val lunar = Lunar.fromYmd(date.year, signedMonth, date.day)
        val leap = if (date.isLeap) "闰" else ""
        return "农历${lunar.yearInChinese}年${leap}${lunar.monthInChinese}月${lunar.dayInChinese}"
    }

    fun formatShort(date: LunarDate): String {
        val month = MONTH_NAMES[date.month - 1]
        val leap = if (date.isLeap) "闰" else ""
        return "农历$leap$month${dayName(date.day)}"
    }

    private fun dayName(day: Int): String {
        if (day <= 10) return "初" + DAY_NAMES[day - 1]
        if (day < 20) return "十" + DAY_NAMES[day - 11]
        if (day == 20) return "二十"
        if (day < 30) return "廿" + DAY_NAMES[day - 21]
        return "三十"
    }

    data class LunarMonthOption(
        val month: Int,
        val isLeap: Boolean,
    ) {
        val label: String
            get() = (if (isLeap) "闰" else "") + MONTH_NAMES[month - 1] + "月"
    }

    companion object {
        const val MIN_LUNAR_YEAR = 1901
        const val MAX_LUNAR_YEAR = 2100
        private val MONTH_NAMES = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
        private val DAY_NAMES = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    }
}