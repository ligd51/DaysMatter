package com.jinian.anniversary.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jinian.anniversary.MainActivity
import com.jinian.anniversary.R
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import java.time.LocalDate

class NotificationHelper(private val context: Context) {

    fun showReminder(event: Anniversary, offsetDays: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "纪念日提醒", NotificationManager.IMPORTANCE_HIGH)
        )
        val canNotify = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        if (!canNotify) return

        val occurrence = AnniversaryCalculator(LunarDateConverter())
            .nextOccurrenceOnOrAfter(event, LocalDate.now().minusDays(offsetDays.toLong())) ?: return
        val text = ReminderTimeCalculator(AnniversaryCalculator(LunarDateConverter()))
            .notificationText(event, offsetDays, occurrence)

        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            ReminderRequestCode.forEvent(event.id, offsetDays),
            android.content.Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, event.id)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ReminderRequestCode.forEvent(event.id, offsetDays), notification)
    }

    companion object {
        const val CHANNEL_ID = "anniversary_reminders"
        const val EXTRA_EVENT_ID = "event_id"
    }
}