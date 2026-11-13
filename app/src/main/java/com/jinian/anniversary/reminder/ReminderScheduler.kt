package com.jinian.anniversary.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jinian.anniversary.JinianApplication
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.jinian.anniversary.MainActivity
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val timeCalculator = ReminderTimeCalculator(
        AnniversaryCalculator(LunarDateConverter())
    )

    suspend fun rescheduleAll(events: List<Anniversary>) {
        events.forEach { event ->
            cancel(event)
            event.reminderOffsets.forEach { schedule(event, it) }
        }
    }

    suspend fun schedule(event: Anniversary, offsetDays: Int) {
        val trigger = timeCalculator.nextTrigger(event, offsetDays, LocalDateTime.now())
        if (trigger == null) {
            cancelAlarm(event.id, offsetDays)
            return
        }

        val pendingIntent = pendingIntent(event.id, offsetDays, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (canUseExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancel(event: Anniversary) {
        event.reminderOffsets.forEach { cancelAlarm(event.id, it) }
    }

    fun cancelAlarm(eventId: Long, offsetDays: Int) {
        val pendingIntent = pendingIntent(eventId, offsetDays, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) alarmManager.cancel(pendingIntent)
    }

    private fun canUseExact(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(eventId: Long, offsetDays: Int, flags: Int): PendingIntent? {
        if (flags == PendingIntent.FLAG_NO_CREATE) {
            return PendingIntent.getBroadcast(
                context,
                ReminderRequestCode.forEvent(eventId, offsetDays),
                intent(eventId, offsetDays),
                flags or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderRequestCode.forEvent(eventId, offsetDays),
            intent(eventId, offsetDays),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun intent(eventId: Long, offsetDays: Int): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            action = "com.jinian.anniversary.ACTION_REMINDER"
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_OFFSET_DAYS, offsetDays)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val offsetDays = intent.getIntExtra(EXTRA_OFFSET_DAYS, 0)
                if (eventId <= 0L) return@launch

                val container = (appContext as JinianApplication).container
                val event = container.anniversaryRepository.getById(eventId) ?: return@launch

                NotificationHelper(appContext).showReminder(event, offsetDays)

                val calculator = AnniversaryCalculator(LunarDateConverter())
                val occurrence = calculator.nextOccurrenceOnOrAfter(event, LocalDate.now())
                if (occurrence != null) {
                }

                container.reminderScheduler.schedule(event, offsetDays)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_OFFSET_DAYS = "offset_days"
    }
}