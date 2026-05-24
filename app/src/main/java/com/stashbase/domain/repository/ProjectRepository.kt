package com.stashbase.domain.repository

import com.stashbase.domain.model.BomEntry
import com.stashbase.domain.model.Project
import com.stashbase.domain.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAll(): Flow<List<Project>>
    fun getById(id: Long): Flow<Project?>
    fun getByStatus(status: ProjectStatus): Flow<List<Project>>
    fun search(query: String): Flow<List<Project>>
    suspend fun upsert(project: Project): Long
    suspend fun delete(id: Long)

    fun getBomEntries(projectId: Long): Flow<List<BomEntry>>
    suspend fun upsertBomEntry(entry: BomEntry): Long
    suspend fun deleteBomEntry(id: Long)

    fun getAllocatedQuantity(materialId: Long, excludeProjectId: Long? = null): Flow<Double>
}
