package com.stashbase.data.repository

import com.stashbase.data.local.dao.StorageLocationDao
import com.stashbase.data.local.dao.ToolDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.Tool
import com.stashbase.domain.repository.ToolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ToolRepositoryImpl @Inject constructor(
    private val toolDao: ToolDao,
    private val locationDao: StorageLocationDao,
) : ToolRepository {

    override fun getAll(): Flow<List<Tool>> =
        toolDao.getAll().map { entities ->
            val locations = locationDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(locationName = locations[it.locationId]?.name) }
        }

    override fun getById(id: Long): Flow<Tool?> =
        toolDao.getById(id).map { entity ->
            val locationName = entity?.locationId?.let { locationDao.getById(it).first()?.name }
            entity?.toDomain(locationName = locationName)
        }

    override fun search(query: String): Flow<List<Tool>> =
        toolDao.search(query).map { entities ->
            val locations = locationDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(locationName = locations[it.locationId]?.name) }
        }

    override fun getByLocation(locationId: Long): Flow<List<Tool>> =
        toolDao.getByLocation(locationId).map { entities ->
            val locationName = locationDao.getById(locationId).first()?.name
            entities.map { it.toDomain(locationName = locationName) }
        }

    override suspend fun upsert(tool: Tool): Long = toolDao.upsert(tool.toEntity())

    override suspend fun delete(id: Long) = toolDao.delete(id)
}
