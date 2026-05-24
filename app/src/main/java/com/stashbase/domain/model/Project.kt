package com.stashbase.domain.model

data class Project(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val status: ProjectStatus = ProjectStatus.IDEA,
    val category: String? = null,
    val photoPath: String? = null,
    val externalLinks: List<String> = emptyList(),
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val deadlineMillis: Long? = null,
    val bomEntries: List<BomEntry> = emptyList(),
) {
    val totalBomItems: Int get() = bomEntries.size

    val missingMaterials: List<BomEntry> get() = bomEntries.filter { it.deficit > 0 }
}

data class BomEntry(
    val id: Long = 0,
    val projectId: Long,
    val materialId: Long,
    val materialName: String = "",
    val materialUnit: MaterialUnit = MaterialUnit.PIECE,
    val requiredQuantity: Double,
    val availableQuantity: Double = 0.0,
    val allocatedToOtherProjects: Double = 0.0,
    val notes: String? = null,
) {
    val effectiveAvailable: Double get() = availableQuantity - allocatedToOtherProjects
    val deficit: Double get() = maxOf(0.0, requiredQuantity - effectiveAvailable)
    val isCovered: Boolean get() = deficit == 0.0
}
