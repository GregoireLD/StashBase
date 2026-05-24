package com.stashbase.domain.repository

import com.stashbase.domain.model.Tool
import kotlinx.coroutines.flow.Flow

interface ToolRepository {
    fun getAll(): Flow<List<Tool>>
    fun getById(id: Long): Flow<Tool?>
    fun search(query: String): Flow<List<Tool>>
    fun getByLocation(locationId: Long): Flow<List<Tool>>
    suspend fun upsert(tool: Tool): Long
    suspend fun delete(id: Long)
}
