package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools ORDER BY name ASC")
    fun getAll(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE id = :id")
    fun getById(id: Long): Flow<ToolEntity?>

    @Query("SELECT * FROM tools WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR size LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE locationId = :locationId ORDER BY name ASC")
    fun getByLocation(locationId: Long): Flow<List<ToolEntity>>

    @Upsert
    suspend fun upsert(tool: ToolEntity): Long

    @Query("DELETE FROM tools WHERE id = :id")
    suspend fun delete(id: Long)
}
