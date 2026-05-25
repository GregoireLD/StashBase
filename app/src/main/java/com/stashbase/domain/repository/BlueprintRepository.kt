package com.stashbase.domain.repository

import com.stashbase.domain.model.Blueprint
import com.stashbase.domain.model.BlueprintBomEntry
import com.stashbase.domain.model.BlueprintStep
import com.stashbase.domain.model.Run
import kotlinx.coroutines.flow.Flow

interface BlueprintRepository {
    fun getAll(): Flow<List<Blueprint>>
    fun getById(id: Long): Flow<Blueprint?>
    fun search(query: String): Flow<List<Blueprint>>
    suspend fun upsert(blueprint: Blueprint): Long
    suspend fun delete(id: Long)
    suspend fun createFromRun(run: Run): Long

    fun getBomEntries(blueprintId: Long): Flow<List<BlueprintBomEntry>>
    suspend fun upsertBomEntry(entry: BlueprintBomEntry): Long
    suspend fun deleteBomEntry(id: Long)

    fun getSteps(blueprintId: Long): Flow<List<BlueprintStep>>
    suspend fun upsertStep(step: BlueprintStep): Long
    suspend fun deleteStep(id: Long)
    suspend fun reorderSteps(steps: List<BlueprintStep>)
}
