package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.StorageLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageLocationDao {
    @Query("SELECT * FROM storage_locations ORDER BY name ASC")
    fun getAll(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE id = :id")
    fun getById(id: Long): Flow<StorageLocationEntity?>

    @Query("SELECT * FROM storage_locations WHERE parentLocationId = :parentId ORDER BY name ASC")
    fun getChildren(parentId: Long): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE parentLocationId IS NULL ORDER BY name ASC")
    fun getRoots(): Flow<List<StorageLocationEntity>>

    @Upsert
    suspend fun upsert(location: StorageLocationEntity): Long

    @Query("DELETE FROM storage_locations WHERE id = :id")
    suspend fun delete(id: Long)
}
