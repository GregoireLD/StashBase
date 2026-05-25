package com.stashbase.data.repository

import com.stashbase.data.local.dao.FinishedItemDao
import com.stashbase.data.local.dao.RunDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.FinishedItem
import com.stashbase.domain.model.FinishedItemStatus
import com.stashbase.domain.repository.FinishedItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FinishedItemRepositoryImpl @Inject constructor(
    private val dao: FinishedItemDao,
    private val runDao: RunDao,
) : FinishedItemRepository {

    override fun getAll(): Flow<List<FinishedItem>> = combine(
        dao.getAll(),
        runDao.getAll(),
    ) { entities, runs ->
        val runById = runs.associateBy { it.id }
        entities.map { it.toDomain(runName = it.runId?.let { id -> runById[id]?.name }) }
    }

    override fun getByRun(runId: Long): Flow<List<FinishedItem>> =
        dao.getByRun(runId).map { entities ->
            val runName = runDao.getById(runId).first()?.name ?: ""
            entities.map { it.toDomain(runName = runName) }
        }

    override fun getByStatus(status: FinishedItemStatus): Flow<List<FinishedItem>> = combine(
        dao.getByStatus(status.name),
        runDao.getAll(),
    ) { entities, runs ->
        val runById = runs.associateBy { it.id }
        entities.map { it.toDomain(runName = it.runId?.let { id -> runById[id]?.name }) }
    }

    override suspend fun upsert(item: FinishedItem): Long = dao.upsert(item.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
