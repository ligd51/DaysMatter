package com.jinian.anniversary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AnniversaryEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(ReminderOffsetsConverter::class)
abstract class JinianDatabase : RoomDatabase() {
    abstract fun anniversaryDao(): AnniversaryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE anniversaries ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun build(context: Context): JinianDatabase =
            Room.databaseBuilder(context, JinianDatabase::class.java, "jinian.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}