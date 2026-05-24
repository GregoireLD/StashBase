package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.BomEntryEntity
import com.stashbase.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAtMillis DESC")
    fun getAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY createdAtMillis DESC")
    fun getByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ProjectEntity>>

    @Upsert
    suspend fun upsert(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM bom_entries WHERE projectId = :projectId")
    fun getBomEntries(projectId: Long): Flow<List<BomEntryEntity>>

    @Upsert
    suspend fun upsertBomEntry(entry: BomEntryEntity): Long

    @Query("DELETE FROM bom_entries WHERE id = :id")
    suspend fun deleteBomEntry(id: Long)

    @Query("SELECT COALESCE(SUM(requiredQuantity), 0.0) FROM bom_entries WHERE materialId = :materialId AND (:excludeProjectId IS NULL OR projectId != :excludeProjectId)")
    fun getAllocatedQuantity(materialId: Long, excludeProjectId: Long?): Flow<Double>
}
