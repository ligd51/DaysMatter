package com.jinian.anniversary

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jinian.anniversary.data.local.AnniversaryEntity
import com.jinian.anniversary.data.local.JinianDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnniversaryDaoTest {

    private lateinit var database: JinianDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JinianDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertUpdateDeleteAndObserve() = runBlocking {
        val entity = AnniversaryEntity(
            title = "测试",
            dateType = "LUNAR",
            originalSolarEpochDay = 20765,
            lunarYear = 2026,
            lunarMonth = 9,
            lunarDay = 30,
            lunarIsLeap = false,
            category = "生日",
            notes = "",
            repeatYearly = true,
            reminderOffsets = "7,0",
            reminderHour = 9,
            reminderMinute = 0,
            createdAt = 1,
            updatedAt = 1,
        )
        val id = database.anniversaryDao().insert(entity)
        val saved = database.anniversaryDao().getById(id)
        assertNotNull(saved)
        assertEquals("7,0", saved?.reminderOffsets)

        database.anniversaryDao().upsert(saved!!.copy(title = "更新"))
        assertEquals("更新", database.anniversaryDao().getById(id)?.title)

        database.anniversaryDao().delete(id)
        assertNull(database.anniversaryDao().getById(id))
        assertEquals(emptyList<AnniversaryEntity>(), database.anniversaryDao().getAll())
    }

    @Test
    fun observeAllUpdatesAfterInsert() = runBlocking {
        val entity = AnniversaryEntity(
            title = "观察",
            dateType = "SOLAR",
            originalSolarEpochDay = 20765,
            lunarYear = 2026,
            lunarMonth = 9,
            lunarDay = 30,
            lunarIsLeap = false,
            category = "纪念日",
            notes = "",
            repeatYearly = false,
            reminderOffsets = "0",
            reminderHour = 9,
            reminderMinute = 0,
            createdAt = 1,
            updatedAt = 1,
        )
        val initial = database.anniversaryDao().observeAll().first()
        database.anniversaryDao().insert(entity)
        val updated = database.anniversaryDao().observeAll().first()
        assertEquals(initial.size + 1, updated.size)
    }
}