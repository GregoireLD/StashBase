package com.stashbase.data.repository

import com.stashbase.data.local.dao.MaterialDao
import com.stashbase.data.local.dao.RunDao
import com.stashbase.data.local.dao.ShoppingListDao
import com.stashbase.data.local.entity.ShoppingListItemEntity
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.ShoppingListItem
import com.stashbase.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val runDao: RunDao,
    private val materialDao: MaterialDao,
) : ShoppingListRepository {

    override fun getAll(): Flow<List<ShoppingListItem>> = combine(
        shoppingListDao.getAll(),
        runDao.getAll(),
    ) { entities, runs ->
        val runById = runs.associateBy { it.id }
        entities.map { it.toDomain(runName = it.sourceRunId?.let { id -> runById[id]?.name }) }
    }

    override fun getUnchecked(): Flow<List<ShoppingListItem>> = combine(
        shoppingListDao.getUnchecked(),
        runDao.getAll(),
    ) { entities, runs ->
        val runById = runs.associateBy { it.id }
        entities.map { it.toDomain(runName = it.sourceRunId?.let { id -> runById[id]?.name }) }
    }

    override suspend fun upsert(item: ShoppingListItem): Long =
        shoppingListDao.upsert(item.toEntity())

    override suspend fun setChecked(id: Long, checked: Boolean) =
        shoppingListDao.setChecked(id, checked)

    override suspend fun delete(id: Long) = shoppingListDao.delete(id)

    override suspend fun deleteChecked() = shoppingListDao.deleteChecked()

    override suspend fun generateFromRunDeficits(runId: Long) {
        val run = runDao.getById(runId).first() ?: return
        val bomEntries = runDao.getBomEntriesOnce(runId)
        val materials = materialDao.getAll().first().associateBy { it.id }
        bomEntries.forEach { entry ->
            val mat = materials[entry.actualMaterialId] ?: return@forEach
            val allocatedElsewhere = runDao.getAllocatedToOtherRuns(entry.actualMaterialId, runId)
            val effectiveAvailable = mat.quantity - allocatedElsewhere
            val deficit = entry.plannedQuantity - effectiveAvailable
            if (deficit > 0) {
                shoppingListDao.upsert(
                    ShoppingListItemEntity(
                        name = mat.name,
                        quantity = deficit,
                        unit = mat.unit,
                        sourceRunId = runId,
                        sourceMaterialId = entry.actualMaterialId,
                    )
                )
            }
        }
    }
}
