package com.stashbase.data.repository

import com.stashbase.data.local.dao.BlueprintDao
import com.stashbase.data.local.dao.MaterialDao
import com.stashbase.data.local.dao.RunDao
import com.stashbase.data.local.entity.*
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.RunRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class RunRepositoryImpl @Inject constructor(
    private val runDao: RunDao,
    private val blueprintDao: BlueprintDao,
    private val materialDao: MaterialDao,
) : RunRepository {

    override fun getAll(): Flow<List<Run>> = combine(
        runDao.getAll(),
        blueprintDao.getAll(),
    ) { runs, blueprints ->
        val bpById = blueprints.associateBy { it.id }
        runs.map { it.toDomain(blueprintName = it.blueprintId?.let { id -> bpById[id]?.name } ?: "") }
    }

    override fun getById(id: Long): Flow<Run?> = combine(
        runDao.getById(id),
        runDao.getBomEntries(id),
        runDao.getSteps(id),
        materialDao.getAll(),
        runDao.getAllAllocations(),
    ) { entity, bomEntities, stepEntities, materials, allocations ->
        entity ?: return@combine null
        val matById = materials.associateBy { it.id }
        val allocMap = allocations.associate { it.materialId to it.totalAllocated }
        val bom = bomEntities.map { entry ->
            val mat = matById[entry.actualMaterialId]
            val totalAlloc = allocMap[entry.actualMaterialId] ?: 0.0
            // subtract this run's own planned qty from total to get "other runs" allocation
            val otherAlloc = (totalAlloc - entry.plannedQuantity).coerceAtLeast(0.0)
            entry.toDomain(
                materialName = mat?.name ?: "",
                materialUnit = mat?.unit?.let { MaterialUnit.valueOf(it) } ?: MaterialUnit.PIECE,
                availableQuantity = mat?.quantity ?: 0.0,
                allocatedToOtherRuns = otherAlloc,
            )
        }
        val blueprintName = entity.blueprintId?.let { bpId ->
            blueprintDao.getById(bpId).firstOrNull()?.name
        } ?: ""
        entity.toDomain(
            blueprintName = blueprintName,
            bomEntries = bom,
            steps = stepEntities.map { it.toDomain() },
        )
    }

    override fun getByStatus(status: RunStatus): Flow<List<Run>> =
        runDao.getByStatus(status.name).map { it.map { e -> e.toDomain() } }

    override fun getByBlueprint(blueprintId: Long): Flow<List<Run>> =
        runDao.getByBlueprint(blueprintId).map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(run: Run): Long = runDao.upsert(run.toEntity())

    override suspend fun delete(id: Long) = runDao.delete(id)

    override fun getBomEntries(runId: Long): Flow<List<RunBomEntry>> =
        runDao.getBomEntries(runId).map { it.map { e -> e.toDomain() } }

    override suspend fun upsertBomEntry(entry: RunBomEntry): Long =
        runDao.upsertBomEntry(entry.toEntity())

    override suspend fun deleteBomEntry(id: Long) = runDao.deleteBomEntry(id)

    override fun getSteps(runId: Long): Flow<List<RunStep>> =
        runDao.getSteps(runId).map { it.map { s -> s.toDomain() } }

    override suspend fun upsertStep(step: RunStep): Long = runDao.upsertStep(step.toEntity())

    override suspend fun deleteStep(id: Long) = runDao.deleteStep(id)

    override suspend fun toggleStep(stepId: Long, isCompleted: Boolean, notes: String?) =
        runDao.setStepCompleted(stepId, isCompleted, notes)

    override suspend fun completeRun(runId: Long, consumptions: Map<Long, Double>) {
        consumptions.forEach { (bomEntryId, actualQty) ->
            runDao.setActualQuantity(bomEntryId, actualQty)
        }
        // Decrement material stock
        val bomEntries = runDao.getBomEntriesOnce(runId)
        bomEntries.forEach { entry ->
            val actualQty = consumptions[entry.id] ?: entry.plannedQuantity
            val material = materialDao.getById(entry.actualMaterialId).firstOrNull() ?: return@forEach
            val newQty = material.quantity - actualQty
            materialDao.updateQuantity(entry.actualMaterialId, newQty)
        }
        runDao.setStatus(runId, "COMPLETED", System.currentTimeMillis())
    }
}
