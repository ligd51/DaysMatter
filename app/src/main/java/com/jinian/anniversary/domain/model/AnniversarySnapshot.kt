package com.jinian.anniversary.domain.model

import java.time.LocalDate

data class AnniversarySnapshot(
    val originalDate: LocalDate,
    val targetDate: LocalDate,
    val isFuture: Boolean,
    val isToday: Boolean,
    val signedDays: Long,
    val inclusiveDayNumber: Long,
    val completedYears: Int,
    val nextAnniversaryDate: LocalDate?,
    val daysUntilNextAnniversary: Long?,
)