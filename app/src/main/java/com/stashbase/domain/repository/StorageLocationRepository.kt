package com.stashbase.domain.repository

import com.stashbase.domain.model.StorageLocation
import kotlinx.coroutines.flow.Flow

interface StorageLocationRepository {
    fun getAll(): Flow<List<StorageLocation>>
    fun getById(id: Long): Flow<StorageLocation?>
    fun getChildren(parentId: Long): Flow<List<StorageLocation>>
    suspend fun upsert(location: StorageLocation): Long
    suspend fun delete(id: Long)
}
