package com.stashbase.domain.repository

import com.stashbase.domain.model.Run
import com.stashbase.domain.model.RunBomEntry
import com.stashbase.domain.model.RunStatus
import com.stashbase.domain.model.RunStep
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    fun getAll(): Flow<List<Run>>
    fun getById(id: Long): Flow<Run?>
    fun getByStatus(status: RunStatus): Flow<List<Run>>
    fun getByBlueprint(blueprintId: Long): Flow<List<Run>>
    suspend fun upsert(run: Run): Long
    suspend fun delete(id: Long)

    fun getBomEntries(runId: Long): Flow<List<RunBomEntry>>
    suspend fun upsertBomEntry(entry: RunBomEntry): Long
    suspend fun deleteBomEntry(id: Long)

    fun getSteps(runId: Long): Flow<List<RunStep>>
    suspend fun upsertStep(step: RunStep): Long
    suspend fun deleteStep(id: Long)
    suspend fun toggleStep(stepId: Long, isCompleted: Boolean, notes: String?)

    // bomEntryId -> actualQuantity consumed; also decrements material stock
    suspend fun completeRun(runId: Long, consumptions: Map<Long, Double>)
}
