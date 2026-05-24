package com.stashbase.data.repository

import com.stashbase.data.local.dao.ProjectDao
import com.stashbase.data.local.dao.ShoppingListDao
import com.stashbase.data.local.entity.toDomain
import com.stashbase.data.local.entity.toEntity
import com.stashbase.domain.model.ShoppingListItem
import com.stashbase.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val projectDao: ProjectDao,
) : ShoppingListRepository {

    override fun getAll(): Flow<List<ShoppingListItem>> =
        shoppingListDao.getAll().map { entities ->
            val projects = projectDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(projectName = it.sourceProjectId?.let { pid -> projects[pid]?.name }) }
        }

    override fun getUnchecked(): Flow<List<ShoppingListItem>> =
        shoppingListDao.getUnchecked().map { entities ->
            val projects = projectDao.getAll().first().associateBy { it.id }
            entities.map { it.toDomain(projectName = it.sourceProjectId?.let { pid -> projects[pid]?.name }) }
        }

    override suspend fun upsert(item: ShoppingListItem): Long =
        shoppingListDao.upsert(item.toEntity())

    override suspend fun setChecked(id: Long, checked: Boolean) =
        shoppingListDao.setChecked(id, checked)

    override suspend fun delete(id: Long) = shoppingListDao.delete(id)

    override suspend fun deleteChecked() = shoppingListDao.deleteChecked()

    override suspend fun generateFromProjectDeficits(projectId: Long) {
        val project = projectDao.getById(projectId).first() ?: return
        val bomEntries = projectDao.getBomEntries(projectId).first()
        bomEntries.forEach { entry ->
            val allocated = projectDao.getAllocatedQuantity(entry.materialId, excludeProjectId = projectId).first()
            shoppingListDao.upsert(
                com.stashbase.data.local.entity.ShoppingListItemEntity(
                    name = "Matériau #${entry.materialId}",
                    quantity = entry.requiredQuantity - allocated,
                    sourceProjectId = projectId,
                    sourceMaterialId = entry.materialId,
                )
            )
        }
    }
}
