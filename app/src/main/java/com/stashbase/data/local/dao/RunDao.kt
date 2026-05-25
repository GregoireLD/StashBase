package com.stashbase.data.local.dao

import androidx.room.*
import androidx.room.ColumnInfo
import com.stashbase.data.local.entity.RunBomEntryEntity
import com.stashbase.data.local.entity.RunEntity
import com.stashbase.data.local.entity.RunStepEntity
import kotlinx.coroutines.flow.Flow

data class MaterialAllocation(
    @ColumnInfo(name = "actualMaterialId") val materialId: Long,
    @ColumnInfo(name = "totalAllocated") val totalAllocated: Double,
)

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY createdAtMillis DESC")
    fun getAll(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id")
    fun getById(id: Long): Flow<RunEntity?>

    @Query("SELECT * FROM runs WHERE status = :status ORDER BY createdAtMillis DESC")
    fun getByStatus(status: String): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE blueprintId = :blueprintId ORDER BY createdAtMillis DESC")
    fun getByBlueprint(blueprintId: Long): Flow<List<RunEntity>>

    @Upsert
    suspend fun upsert(run: RunEntity): Long

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE runs SET status = :status, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, completedAt: Long? = null)

    // BOM entries
    @Query("SELECT * FROM run_bom_entries WHERE runId = :runId")
    fun getBomEntries(runId: Long): Flow<List<RunBomEntryEntity>>

    @Query("SELECT * FROM run_bom_entries WHERE runId = :runId")
    suspend fun getBomEntriesOnce(runId: Long): List<RunBomEntryEntity>

    @Upsert
    suspend fun upsertBomEntry(entry: RunBomEntryEntity): Long

    @Query("DELETE FROM run_bom_entries WHERE id = :id")
    suspend fun deleteBomEntry(id: Long)

    @Query("UPDATE run_bom_entries SET actualQuantity = :qty WHERE id = :id")
    suspend fun setActualQuantity(id: Long, qty: Double)

    // Steps
    @Query("SELECT * FROM run_steps WHERE runId = :runId ORDER BY stepOrder ASC")
    fun getSteps(runId: Long): Flow<List<RunStepEntity>>

    @Upsert
    suspend fun upsertStep(step: RunStepEntity): Long

    @Query("DELETE FROM run_steps WHERE id = :id OR parentStepId = :id")
    suspend fun deleteStep(id: Long)

    @Query("UPDATE run_steps SET stepOrder = :order WHERE id = :id")
    suspend fun updateStepOrder(id: Long, order: Int)

    @Query("UPDATE run_steps SET isCompleted = :completed, completionNotes = :notes WHERE id = :id")
    suspend fun setStepCompleted(id: Long, completed: Boolean, notes: String?)

    // Net inventory: total planned quantity allocated to IN_PROGRESS runs
    @Query("""
        SELECT rbe.actualMaterialId, COALESCE(SUM(rbe.plannedQuantity), 0.0) AS totalAllocated
        FROM run_bom_entries rbe
        JOIN runs r ON rbe.runId = r.id
        WHERE r.status = 'IN_PROGRESS'
        GROUP BY rbe.actualMaterialId
    """)
    fun getAllAllocations(): Flow<List<MaterialAllocation>>

    @Query("""
        SELECT COALESCE(SUM(rbe.plannedQuantity), 0.0)
        FROM run_bom_entries rbe
        JOIN runs r ON rbe.runId = r.id
        WHERE rbe.actualMaterialId = :materialId
        AND r.status = 'IN_PROGRESS'
        AND rbe.runId != :excludeRunId
    """)
    suspend fun getAllocatedToOtherRuns(materialId: Long, excludeRunId: Long): Double
}
