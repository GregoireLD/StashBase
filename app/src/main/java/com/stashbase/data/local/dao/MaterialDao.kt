package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY name ASC")
    fun getAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    fun getById(id: Long): Flow<MaterialEntity?>

    @Query("SELECT * FROM materials WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR colorCode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE locationId = :locationId ORDER BY name ASC")
    fun getByLocation(locationId: Long): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE lowStockThreshold IS NOT NULL AND quantity <= lowStockThreshold ORDER BY name ASC")
    fun getLowStock(): Flow<List<MaterialEntity>>

    @Query("""
        SELECT COALESCE(SUM(rbe.plannedQuantity), 0.0)
        FROM run_bom_entries rbe
        JOIN runs r ON rbe.runId = r.id
        WHERE rbe.actualMaterialId = :materialId AND r.status = 'IN_PROGRESS'
    """)
    fun getAllocatedQuantity(materialId: Long): Flow<Double>

    @Upsert
    suspend fun upsert(material: MaterialEntity): Long

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE materials SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double)
}
