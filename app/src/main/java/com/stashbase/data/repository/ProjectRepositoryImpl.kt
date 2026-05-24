package com.stashbase.data.repository

import com.stashbase.data.local.dao.MaterialDao
import com.stashbase.data.local.dao.ProjectDao
import com.stashbase.data.local.dao.ShoppingListDao
import com.stashbase.data.local.entity.*
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val materialDao: MaterialDao,
    private val shoppingListDao: ShoppingListDao,
) : ProjectRepository {

    override fun getAll(): Flow<List<Project>> =
        projectDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Project?> =
        combine(
            projectDao.getById(id),
            projectDao.getBomEntries(id),
        ) { projectEntity, bomEntities ->
            projectEntity?.toDomain()?.copy(
                bomEntries = bomEntities.map { it.toDomain() }
            )
        }

    override fun getByStatus(status: ProjectStatus): Flow<List<Project>> =
        projectDao.getByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Project>> =
        projectDao.search(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(project: Project): Long =
        projectDao.upsert(project.toEntity())

    override suspend fun delete(id: Long) = projectDao.delete(id)

    override fun getBomEntries(projectId: Long): Flow<List<BomEntry>> =
        projectDao.getBomEntries(projectId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertBomEntry(entry: BomEntry): Long =
        projectDao.upsertBomEntry(entry.toEntity())

    override suspend fun deleteBomEntry(id: Long) = projectDao.deleteBomEntry(id)

    override fun getAllocatedQuantity(materialId: Long, excludeProjectId: Long?): Flow<Double> =
        projectDao.getAllocatedQuantity(materialId, excludeProjectId)
}
