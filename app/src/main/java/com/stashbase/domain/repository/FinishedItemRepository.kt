package com.stashbase.domain.repository

import com.stashbase.domain.model.FinishedItem
import com.stashbase.domain.model.FinishedItemStatus
import kotlinx.coroutines.flow.Flow

interface FinishedItemRepository {
    fun getAll(): Flow<List<FinishedItem>>
    fun getByRun(runId: Long): Flow<List<FinishedItem>>
    fun getByStatus(status: FinishedItemStatus): Flow<List<FinishedItem>>
    suspend fun upsert(item: FinishedItem): Long
    suspend fun delete(id: Long)
}
