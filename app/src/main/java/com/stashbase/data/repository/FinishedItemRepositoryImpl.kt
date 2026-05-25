package com.stashbase.data.repository

import com.stashbase.data.local.dao.FinishedItemDao
import com.stashbase.data.local.dao.ProjectDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.FinishedItem
import com.stashbase.domain.model.FinishedItemStatus
import com.stashbase.domain.repository.FinishedItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FinishedItemRepositoryImpl @Inject constructor(
    private val dao: FinishedItemDao,
    private val projectDao: ProjectDao,
) : FinishedItemRepository {

    override fun getAll(): Flow<List<FinishedItem>> =
        dao.getAll().map { entities ->
            val projects = projectDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(projectName = projects[it.projectId]?.name ?: "") }
        }

    override fun getByProject(projectId: Long): Flow<List<FinishedItem>> =
        dao.getByProject(projectId).map { entities ->
            val projectName = projectDao.getById(projectId).first()?.name ?: ""
            entities.map { it.toDomain(projectName = projectName) }
        }

    override fun getByStatus(status: FinishedItemStatus): Flow<List<FinishedItem>> =
        dao.getByStatus(status.name).map { entities ->
            val projects = projectDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(projectName = projects[it.projectId]?.name ?: "") }
        }

    override suspend fun upsert(item: FinishedItem): Long = dao.upsert(item.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
