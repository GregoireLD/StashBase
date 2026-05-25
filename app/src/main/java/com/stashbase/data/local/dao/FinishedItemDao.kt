package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.FinishedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinishedItemDao {
    @Query("SELECT * FROM finished_items ORDER BY createdAtMillis DESC")
    fun getAll(): Flow<List<FinishedItemEntity>>

    @Query("SELECT * FROM finished_items WHERE runId = :runId ORDER BY createdAtMillis DESC")
    fun getByRun(runId: Long): Flow<List<FinishedItemEntity>>

    @Query("SELECT * FROM finished_items WHERE status = :status ORDER BY createdAtMillis DESC")
    fun getByStatus(status: String): Flow<List<FinishedItemEntity>>

    @Upsert
    suspend fun upsert(item: FinishedItemEntity): Long

    @Query("DELETE FROM finished_items WHERE id = :id")
    suspend fun delete(id: Long)
}
