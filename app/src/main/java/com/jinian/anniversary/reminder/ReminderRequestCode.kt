package com.jinian.anniversary.reminder

object ReminderRequestCode {
    fun forEvent(eventId: Long, offsetDays: Int): Int {
        return (eventId xor (offsetDays.toLong() * 0x9E3779B1L)).toInt()
    }
}