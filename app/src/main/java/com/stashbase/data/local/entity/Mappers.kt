package com.stashbase.data.local.entity

import com.stashbase.domain.model.*

fun MaterialEntity.toDomain(locationName: String? = null, allocatedQuantity: Double = 0.0): Material =
    Material(
        id = id,
        name = name,
        type = MaterialType.valueOf(type),
        brand = brand,
        color = color,
        colorCode = colorCode,
        quantity = quantity,
        unit = MaterialUnit.valueOf(unit),
        locationId = locationId,
        locationName = locationName,
        purchasePrice = purchasePrice,
        purchaseDateMillis = purchaseDateMillis,
        notes = notes,
        photoPath = photoPath,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        lowStockThreshold = lowStockThreshold,
        createdAtMillis = createdAtMillis,
    )

fun Material.toEntity(): MaterialEntity =
    MaterialEntity(
        id = id,
        name = name,
        type = type.name,
        brand = brand,
        color = color,
        colorCode = colorCode,
        quantity = quantity,
        unit = unit.name,
        locationId = locationId,
        purchasePrice = purchasePrice,
        purchaseDateMillis = purchaseDateMillis,
        notes = notes,
        photoPath = photoPath,
        tags = tags.joinToString(","),
        lowStockThreshold = lowStockThreshold,
        createdAtMillis = createdAtMillis,
    )

fun ToolEntity.toDomain(locationName: String? = null): Tool =
    Tool(
        id = id,
        name = name,
        type = ToolType.valueOf(type),
        brand = brand,
        size = size,
        condition = ToolCondition.valueOf(condition),
        locationId = locationId,
        locationName = locationName,
        notes = notes,
        photoPath = photoPath,
        createdAtMillis = createdAtMillis,
    )

fun Tool.toEntity(): ToolEntity =
    ToolEntity(
        id = id,
        name = name,
        type = type.name,
        brand = brand,
        size = size,
        condition = condition.name,
        locationId = locationId,
        notes = notes,
        photoPath = photoPath,
        createdAtMillis = createdAtMillis,
    )

fun StorageLocationEntity.toDomain(parentName: String? = null): StorageLocation =
    StorageLocation(
        id = id,
        name = name,
        description = description,
        parentLocationId = parentLocationId,
        parentLocationName = parentName,
        colorHex = colorHex,
    )

fun StorageLocation.toEntity(): StorageLocationEntity =
    StorageLocationEntity(
        id = id,
        name = name,
        description = description,
        parentLocationId = parentLocationId,
        colorHex = colorHex,
    )

fun ProjectEntity.toDomain(): Project =
    Project(
        id = id,
        name = name,
        description = description,
        status = ProjectStatus.valueOf(status),
        category = category,
        photoPath = photoPath,
        externalLinks = com.google.gson.Gson().fromJson(externalLinks, Array<String>::class.java)?.toList() ?: emptyList(),
        notes = notes,
        createdAtMillis = createdAtMillis,
        deadlineMillis = deadlineMillis,
    )

fun Project.toEntity(): ProjectEntity =
    ProjectEntity(
        id = id,
        name = name,
        description = description,
        status = status.name,
        category = category,
        photoPath = photoPath,
        externalLinks = com.google.gson.Gson().toJson(externalLinks),
        notes = notes,
        createdAtMillis = createdAtMillis,
        deadlineMillis = deadlineMillis,
    )

fun BomEntryEntity.toDomain(
    materialName: String = "",
    materialUnit: MaterialUnit = MaterialUnit.PIECE,
    availableQuantity: Double = 0.0,
    allocatedToOtherProjects: Double = 0.0,
): BomEntry =
    BomEntry(
        id = id,
        projectId = projectId,
        materialId = materialId,
        materialName = materialName,
        materialUnit = materialUnit,
        requiredQuantity = requiredQuantity,
        availableQuantity = availableQuantity,
        allocatedToOtherProjects = allocatedToOtherProjects,
        notes = notes,
    )

fun BomEntry.toEntity(): BomEntryEntity =
    BomEntryEntity(
        id = id,
        projectId = projectId,
        materialId = materialId,
        requiredQuantity = requiredQuantity,
        notes = notes,
    )

fun ShoppingListItemEntity.toDomain(projectName: String? = null): ShoppingListItem =
    ShoppingListItem(
        id = id,
        name = name,
        description = description,
        quantity = quantity,
        unit = unit?.let { MaterialUnit.valueOf(it) },
        isChecked = isChecked,
        sourceProjectId = sourceProjectId,
        sourceProjectName = projectName,
        sourceMaterialId = sourceMaterialId,
        createdAtMillis = createdAtMillis,
    )

fun ShoppingListItem.toEntity(): ShoppingListItemEntity =
    ShoppingListItemEntity(
        id = id,
        name = name,
        description = description,
        quantity = quantity,
        unit = unit?.name,
        isChecked = isChecked,
        sourceProjectId = sourceProjectId,
        sourceMaterialId = sourceMaterialId,
        createdAtMillis = createdAtMillis,
    )
