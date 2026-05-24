package com.stashbase.domain.repository

import com.stashbase.domain.model.Material
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    fun getAll(): Flow<List<Material>>
    fun getById(id: Long): Flow<Material?>
    fun search(query: String): Flow<List<Material>>
    fun getByLocation(locationId: Long): Flow<List<Material>>
    fun getLowStock(): Flow<List<Material>>
    suspend fun upsert(material: Material): Long
    suspend fun delete(id: Long)
    suspend fun updateQuantity(id: Long, quantity: Double)
}
