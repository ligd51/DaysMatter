package com.jinian.anniversary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryDao {
    @Query("SELECT * FROM anniversaries ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<AnniversaryEntity>>

    @Query("SELECT * FROM anniversaries ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAll(): List<AnniversaryEntity>

    @Query("SELECT * FROM anniversaries WHERE id = :id")
    fun observeById(id: Long): Flow<AnniversaryEntity?>

    @Query("SELECT * FROM anniversaries WHERE id = :id")
    suspend fun getById(id: Long): AnniversaryEntity?

    @Query("SELECT MAX(sortOrder) FROM anniversaries")
    suspend fun getMaxSortOrder(): Long?

    @Query("UPDATE anniversaries SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AnniversaryEntity): Long

    @Upsert
    suspend fun upsert(entity: AnniversaryEntity)

    @Query("DELETE FROM anniversaries WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("DELETE FROM anniversaries")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<AnniversaryEntity>) {
        clearAll()
        entities.forEach { insert(it) }
    }
}