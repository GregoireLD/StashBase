package com.stashbase.data.repository

import com.stashbase.data.local.dao.StorageLocationDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.StorageLocation
import com.stashbase.domain.repository.StorageLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StorageLocationRepositoryImpl @Inject constructor(
    private val dao: StorageLocationDao,
) : StorageLocationRepository {

    override fun getAll(): Flow<List<StorageLocation>> =
        dao.getAll().map { entities ->
            val byId = entities.associateBy { it.id }
            entities.map { it.toDomain(parentName = it.parentLocationId?.let { pid -> byId[pid]?.name }) }
        }

    override fun getById(id: Long): Flow<StorageLocation?> =
        dao.getById(id).map { entity ->
            val parentName = entity?.parentLocationId?.let { dao.getById(it).first()?.name }
            entity?.toDomain(parentName = parentName)
        }

    override fun getChildren(parentId: Long): Flow<List<StorageLocation>> =
        dao.getChildren(parentId).map { entities ->
            val parentName = dao.getById(parentId).first()?.name
            entities.map { it.toDomain(parentName = parentName) }
        }

    override suspend fun upsert(location: StorageLocation): Long = dao.upsert(location.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
