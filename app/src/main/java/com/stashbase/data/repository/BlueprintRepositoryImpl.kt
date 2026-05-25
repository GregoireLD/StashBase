package com.stashbase.data.repository

import com.stashbase.data.local.dao.BlueprintDao
import com.stashbase.data.local.dao.MaterialDao
import com.stashbase.data.local.dao.RunDao
import com.stashbase.data.local.entity.*
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.BlueprintRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class BlueprintRepositoryImpl @Inject constructor(
    private val blueprintDao: BlueprintDao,
    private val materialDao: MaterialDao,
    private val runDao: RunDao,
) : BlueprintRepository {

    override fun getAll(): Flow<List<Blueprint>> =
        blueprintDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Blueprint?> = combine(
        blueprintDao.getById(id),
        blueprintDao.getBomEntries(id),
        blueprintDao.getSteps(id),
        materialDao.getAll(),
        runDao.getAllAllocations(),
    ) { entity, bomEntities, stepEntities, materials, allocations ->
        entity ?: return@combine null
        val matById = materials.associateBy { it.id }
        val allocMap = allocations.associate { it.materialId to it.totalAllocated }
        val bom = bomEntities.map { entry ->
            val mat = matById[entry.materialId]
            entry.toDomain(
                materialName = mat?.name ?: "",
                materialUnit = mat?.unit?.let { MaterialUnit.valueOf(it) } ?: MaterialUnit.PIECE,
                availableQuantity = mat?.quantity ?: 0.0,
                allocatedToOtherRuns = allocMap[entry.materialId] ?: 0.0,
            )
        }
        entity.toDomain(bomEntries = bom, steps = stepEntities.map { it.toDomain() })
    }

    override fun search(query: String): Flow<List<Blueprint>> =
        blueprintDao.search(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(blueprint: Blueprint): Long =
        blueprintDao.upsert(blueprint.toEntity())

    override suspend fun delete(id: Long) = blueprintDao.delete(id)

    override suspend fun createFromRun(run: Run): Long {
        val blueprint = Blueprint(
            name = run.name,
            notes = run.notes,
            bomEntries = run.bomEntries.map { entry ->
                BlueprintBomEntry(
                    blueprintId = 0,
                    materialId = entry.actualMaterialId,
                    materialName = entry.materialName,
                    materialUnit = entry.materialUnit,
                    requiredQuantity = entry.plannedQuantity,
                    notes = entry.notes,
                )
            },
            steps = run.steps.map { step ->
                BlueprintStep(
                    blueprintId = 0, stepOrder = step.stepOrder, title = step.title,
                    description = step.description,
                )
            },
        )
        val newId = blueprintDao.upsert(blueprint.toEntity())
        blueprint.bomEntries.forEach { entry ->
            blueprintDao.upsertBomEntry(entry.copy(blueprintId = newId).toEntity())
        }
        blueprint.steps.forEach { step ->
            blueprintDao.upsertStep(step.copy(blueprintId = newId).toEntity())
        }
        return newId
    }

    override fun getBomEntries(blueprintId: Long): Flow<List<BlueprintBomEntry>> =
        blueprintDao.getBomEntries(blueprintId).map { it.map { e -> e.toDomain() } }

    override suspend fun upsertBomEntry(entry: BlueprintBomEntry): Long =
        blueprintDao.upsertBomEntry(entry.toEntity())

    override suspend fun deleteBomEntry(id: Long) = blueprintDao.deleteBomEntry(id)

    override fun getSteps(blueprintId: Long): Flow<List<BlueprintStep>> =
        blueprintDao.getSteps(blueprintId).map { it.map { s -> s.toDomain() } }

    override suspend fun upsertStep(step: BlueprintStep): Long =
        blueprintDao.upsertStep(step.toEntity())

    override suspend fun deleteStep(id: Long) = blueprintDao.deleteStep(id)

    override suspend fun reorderSteps(steps: List<BlueprintStep>) {
        steps.forEachIndexed { index, step ->
            blueprintDao.updateStepOrder(step.id, index)
        }
    }
}
