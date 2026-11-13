package com.jinian.anniversary.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jinian.anniversary.JinianApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext as JinianApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = appContext.container.anniversaryRepository
                appContext.container.reminderScheduler.rescheduleAll(repository.getAll())
            } finally {
                pendingResult.finish()
            }
        }
    }
}