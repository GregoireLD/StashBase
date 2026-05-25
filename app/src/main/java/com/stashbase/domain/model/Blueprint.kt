package com.stashbase.domain.model

data class Blueprint(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val photoPath: String? = null,
    val externalLinks: List<String> = emptyList(),
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val deadlineMillis: Long? = null,
    val bomEntries: List<BlueprintBomEntry> = emptyList(),
    val steps: List<BlueprintStep> = emptyList(),
) {
    val missingMaterials: List<BlueprintBomEntry> get() = bomEntries.filter { it.deficit > 0 }
}

data class BlueprintBomEntry(
    val id: Long = 0,
    val blueprintId: Long,
    val materialId: Long,
    val materialName: String = "",
    val materialUnit: MaterialUnit = MaterialUnit.PIECE,
    val availableQuantity: Double = 0.0,
    val allocatedToOtherRuns: Double = 0.0,
    val requiredQuantity: Double,
    val notes: String? = null,
) {
    val effectiveAvailable: Double get() = availableQuantity - allocatedToOtherRuns
    val deficit: Double get() = maxOf(0.0, requiredQuantity - effectiveAvailable)
    val isCovered: Boolean get() = deficit == 0.0
}

data class BlueprintStep(
    val id: Long = 0,
    val blueprintId: Long,
    val parentStepId: Long? = null,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val photoPath: String? = null,
)
