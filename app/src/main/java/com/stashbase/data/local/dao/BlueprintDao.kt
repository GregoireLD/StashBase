package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.BlueprintBomEntryEntity
import com.stashbase.data.local.entity.BlueprintEntity
import com.stashbase.data.local.entity.BlueprintStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlueprintDao {
    @Query("SELECT * FROM blueprints ORDER BY name ASC")
    fun getAll(): Flow<List<BlueprintEntity>>

    @Query("SELECT * FROM blueprints WHERE id = :id")
    fun getById(id: Long): Flow<BlueprintEntity?>

    @Query("SELECT * FROM blueprints WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<BlueprintEntity>>

    @Upsert
    suspend fun upsert(blueprint: BlueprintEntity): Long

    @Query("DELETE FROM blueprints WHERE id = :id")
    suspend fun delete(id: Long)

    // BOM entries
    @Query("SELECT * FROM blueprint_bom_entries WHERE blueprintId = :blueprintId")
    fun getBomEntries(blueprintId: Long): Flow<List<BlueprintBomEntryEntity>>

    @Upsert
    suspend fun upsertBomEntry(entry: BlueprintBomEntryEntity): Long

    @Query("DELETE FROM blueprint_bom_entries WHERE id = :id")
    suspend fun deleteBomEntry(id: Long)

    // Steps
    @Query("SELECT * FROM blueprint_steps WHERE blueprintId = :blueprintId ORDER BY stepOrder ASC")
    fun getSteps(blueprintId: Long): Flow<List<BlueprintStepEntity>>

    @Upsert
    suspend fun upsertStep(step: BlueprintStepEntity): Long

    @Query("DELETE FROM blueprint_steps WHERE id = :id OR parentStepId = :id")
    suspend fun deleteStep(id: Long)

    @Query("UPDATE blueprint_steps SET stepOrder = :order WHERE id = :id")
    suspend fun updateStepOrder(id: Long, order: Int)
}
