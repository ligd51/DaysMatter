package com.jinian.anniversary

import android.app.Application
import com.jinian.anniversary.data.backup.BackupManager
import com.jinian.anniversary.data.local.JinianDatabase
import com.jinian.anniversary.data.repository.AnniversaryRepository
import com.jinian.anniversary.data.settings.SettingsRepository
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.reminder.ReminderScheduler

class JinianApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(appContext: Application) {
    val lunarConverter = LunarDateConverter()
    val calculator = AnniversaryCalculator(lunarConverter)
    val database = JinianDatabase.build(appContext)
    val anniversaryRepository = AnniversaryRepository(database, database.anniversaryDao(), lunarConverter)
    val settingsRepository = SettingsRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val backupManager = BackupManager(appContext, anniversaryRepository, settingsRepository)
}