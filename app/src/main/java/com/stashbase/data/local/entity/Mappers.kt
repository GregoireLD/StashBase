package com.stashbase.data.local.entity

import com.google.gson.Gson
import com.stashbase.domain.model.*

// ── Material ──────────────────────────────────────────────────────────────────

fun MaterialEntity.toDomain(locationName: String? = null, allocatedQuantity: Double = 0.0): Material =
    Material(
        id = id, name = name, type = MaterialType.valueOf(type), brand = brand,
        color = color, colorCode = colorCode, quantity = quantity,
        unit = MaterialUnit.valueOf(unit), locationId = locationId,
        locationName = locationName, purchasePrice = purchasePrice,
        purchaseDateMillis = purchaseDateMillis, notes = notes, photoPath = photoPath,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        lowStockThreshold = lowStockThreshold, createdAtMillis = createdAtMillis,
        allocatedQuantity = allocatedQuantity,
    )

fun Material.toEntity(): MaterialEntity =
    MaterialEntity(
        id = id, name = name, type = type.name, brand = brand, color = color,
        colorCode = colorCode, quantity = quantity, unit = unit.name,
        locationId = locationId, purchasePrice = purchasePrice,
        purchaseDateMillis = purchaseDateMillis, notes = notes, photoPath = photoPath,
        tags = tags.joinToString(","), lowStockThreshold = lowStockThreshold,
        createdAtMillis = createdAtMillis,
    )

// ── Tool ──────────────────────────────────────────────────────────────────────

fun ToolEntity.toDomain(locationName: String? = null): Tool =
    Tool(
        id = id, name = name, type = ToolType.valueOf(type), brand = brand,
        size = size, condition = ToolCondition.valueOf(condition),
        locationId = locationId, locationName = locationName,
        notes = notes, photoPath = photoPath, createdAtMillis = createdAtMillis,
    )

fun Tool.toEntity(): ToolEntity =
    ToolEntity(
        id = id, name = name, type = type.name, brand = brand, size = size,
        condition = condition.name, locationId = locationId,
        notes = notes, photoPath = photoPath, createdAtMillis = createdAtMillis,
    )

// ── StorageLocation ───────────────────────────────────────────────────────────

fun StorageLocationEntity.toDomain(parentName: String? = null): StorageLocation =
    StorageLocation(
        id = id, name = name, description = description,
        parentLocationId = parentLocationId, parentLocationName = parentName, colorHex = colorHex,
    )

fun StorageLocation.toEntity(): StorageLocationEntity =
    StorageLocationEntity(id = id, name = name, description = description, parentLocationId = parentLocationId, colorHex = colorHex)

// ── Blueprint ─────────────────────────────────────────────────────────────────

private val gson = Gson()

fun BlueprintEntity.toDomain(
    bomEntries: List<BlueprintBomEntry> = emptyList(),
    steps: List<BlueprintStep> = emptyList(),
): Blueprint = Blueprint(
    id = id, name = name, description = description, category = category,
    photoPath = photoPath,
    externalLinks = gson.fromJson(externalLinks, Array<String>::class.java)?.toList() ?: emptyList(),
    notes = notes, createdAtMillis = createdAtMillis, deadlineMillis = deadlineMillis,
    bomEntries = bomEntries, steps = steps,
)

fun Blueprint.toEntity(): BlueprintEntity = BlueprintEntity(
    id = id, name = name, description = description, category = category,
    photoPath = photoPath, externalLinks = gson.toJson(externalLinks),
    notes = notes, createdAtMillis = createdAtMillis, deadlineMillis = deadlineMillis,
)

fun BlueprintBomEntryEntity.toDomain(
    materialName: String = "",
    materialUnit: MaterialUnit = MaterialUnit.PIECE,
    availableQuantity: Double = 0.0,
    allocatedToOtherRuns: Double = 0.0,
): BlueprintBomEntry = BlueprintBomEntry(
    id = id, blueprintId = blueprintId, materialId = materialId,
    materialName = materialName, materialUnit = materialUnit,
    availableQuantity = availableQuantity, allocatedToOtherRuns = allocatedToOtherRuns,
    requiredQuantity = requiredQuantity, notes = notes,
)

fun BlueprintBomEntry.toEntity(): BlueprintBomEntryEntity = BlueprintBomEntryEntity(
    id = id, blueprintId = blueprintId, materialId = materialId,
    requiredQuantity = requiredQuantity, notes = notes,
)

fun BlueprintStepEntity.toDomain(): BlueprintStep = BlueprintStep(
    id = id, blueprintId = blueprintId, parentStepId = parentStepId, stepOrder = stepOrder,
    title = title, description = description, photoPath = photoPath,
)

fun BlueprintStep.toEntity(): BlueprintStepEntity = BlueprintStepEntity(
    id = id, blueprintId = blueprintId, parentStepId = parentStepId, stepOrder = stepOrder,
    title = title, description = description, photoPath = photoPath,
)

// ── Run ───────────────────────────────────────────────────────────────────────

fun RunEntity.toDomain(
    blueprintName: String = "",
    bomEntries: List<RunBomEntry> = emptyList(),
    steps: List<RunStep> = emptyList(),
): Run = Run(
    id = id, blueprintId = blueprintId, blueprintName = blueprintName, name = name,
    status = RunStatus.valueOf(status), notes = notes, photoPath = photoPath,
    startedAtMillis = startedAtMillis, completedAtMillis = completedAtMillis,
    deadlineMillis = deadlineMillis, createdAtMillis = createdAtMillis,
    bomEntries = bomEntries, steps = steps,
)

fun Run.toEntity(): RunEntity = RunEntity(
    id = id, blueprintId = blueprintId, name = name, status = status.name,
    notes = notes, photoPath = photoPath, startedAtMillis = startedAtMillis,
    completedAtMillis = completedAtMillis, deadlineMillis = deadlineMillis,
    createdAtMillis = createdAtMillis,
)

fun RunBomEntryEntity.toDomain(
    materialName: String = "",
    materialUnit: MaterialUnit = MaterialUnit.PIECE,
    availableQuantity: Double = 0.0,
    allocatedToOtherRuns: Double = 0.0,
): RunBomEntry = RunBomEntry(
    id = id, runId = runId, originalMaterialId = originalMaterialId,
    actualMaterialId = actualMaterialId, materialName = materialName,
    materialUnit = materialUnit, availableQuantity = availableQuantity,
    allocatedToOtherRuns = allocatedToOtherRuns,
    plannedQuantity = plannedQuantity, actualQuantity = actualQuantity, notes = notes,
)

fun RunBomEntry.toEntity(): RunBomEntryEntity = RunBomEntryEntity(
    id = id, runId = runId, originalMaterialId = originalMaterialId,
    actualMaterialId = actualMaterialId, plannedQuantity = plannedQuantity,
    actualQuantity = actualQuantity, notes = notes,
)

fun RunStepEntity.toDomain(): RunStep = RunStep(
    id = id, runId = runId, blueprintStepId = blueprintStepId, parentStepId = parentStepId,
    stepOrder = stepOrder, title = title, description = description, photoPath = photoPath,
    isCompleted = isCompleted, completionNotes = completionNotes,
)

fun RunStep.toEntity(): RunStepEntity = RunStepEntity(
    id = id, runId = runId, blueprintStepId = blueprintStepId, parentStepId = parentStepId,
    stepOrder = stepOrder, title = title, description = description, photoPath = photoPath,
    isCompleted = isCompleted, completionNotes = completionNotes,
)

// ── ShoppingListItem ──────────────────────────────────────────────────────────

fun ShoppingListItemEntity.toDomain(runName: String? = null): ShoppingListItem =
    ShoppingListItem(
        id = id, name = name, description = description, quantity = quantity,
        unit = unit?.let { MaterialUnit.valueOf(it) }, isChecked = isChecked,
        sourceRunId = sourceRunId, sourceRunName = runName,
        sourceMaterialId = sourceMaterialId, createdAtMillis = createdAtMillis,
    )

fun ShoppingListItem.toEntity(): ShoppingListItemEntity =
    ShoppingListItemEntity(
        id = id, name = name, description = description, quantity = quantity,
        unit = unit?.name, isChecked = isChecked, sourceRunId = sourceRunId,
        sourceMaterialId = sourceMaterialId, createdAtMillis = createdAtMillis,
    )

// ── FinishedItem ──────────────────────────────────────────────────────────────

fun FinishedItemEntity.toDomain(runName: String? = null): FinishedItem =
    FinishedItem(
        id = id, runId = runId, runName = runName, name = name, quantity = quantity,
        unit = MaterialUnit.valueOf(unit), status = FinishedItemStatus.valueOf(status),
        sellingPrice = sellingPrice, notes = notes, photoPath = photoPath,
        createdAtMillis = createdAtMillis,
    )

fun FinishedItem.toEntity(): FinishedItemEntity =
    FinishedItemEntity(
        id = id, runId = runId, name = name, quantity = quantity, unit = unit.name,
        status = status.name, sellingPrice = sellingPrice, notes = notes,
        photoPath = photoPath, createdAtMillis = createdAtMillis,
    )
