package com.stashbase.data.repository

import com.stashbase.data.local.dao.MaterialDao
import com.stashbase.data.local.dao.RunDao
import com.stashbase.data.local.dao.StorageLocationDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.Material
import com.stashbase.domain.repository.MaterialRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialRepositoryImpl @Inject constructor(
    private val materialDao: MaterialDao,
    private val runDao: RunDao,
    private val locationDao: StorageLocationDao,
) : MaterialRepository {

    private suspend fun locationNameById(id: Long?): String? =
        id?.let { locationDao.getById(it).first()?.name }

    override fun getAll(): Flow<List<Material>> = combine(
        materialDao.getAll(),
        locationDao.getAll(),
        runDao.getAllAllocations(),
    ) { entities, locations, allocations ->
        val locationMap = locations.associateBy { it.id }
        val allocMap = allocations.associate { it.materialId to it.totalAllocated }
        entities.map { entity ->
            entity.toDomain(
                locationName = locationMap[entity.locationId]?.name,
                allocatedQuantity = allocMap[entity.id] ?: 0.0,
            )
        }
    }

    override fun getById(id: Long): Flow<Material?> =
        materialDao.getById(id).map { entity ->
            entity?.toDomain(locationName = locationNameById(entity.locationId))
        }

    override fun search(query: String): Flow<List<Material>> = combine(
        materialDao.search(query),
        locationDao.getAll(),
        runDao.getAllAllocations(),
    ) { entities, locations, allocations ->
        val locationMap = locations.associateBy { it.id }
        val allocMap = allocations.associate { it.materialId to it.totalAllocated }
        entities.map { entity ->
            entity.toDomain(
                locationName = locationMap[entity.locationId]?.name,
                allocatedQuantity = allocMap[entity.id] ?: 0.0,
            )
        }
    }

    override fun getByLocation(locationId: Long): Flow<List<Material>> =
        materialDao.getByLocation(locationId).map { entities ->
            val locationName = locationNameById(locationId)
            entities.map { it.toDomain(locationName = locationName) }
        }

    override fun getLowStock(): Flow<List<Material>> =
        materialDao.getLowStock().map { entities ->
            val locations = locationDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(locationName = locations[it.locationId]?.name) }
        }

    override suspend fun upsert(material: Material): Long = materialDao.upsert(material.toEntity())

    override suspend fun delete(id: Long) = materialDao.delete(id)

    override suspend fun updateQuantity(id: Long, quantity: Double) =
        materialDao.updateQuantity(id, quantity)
}
