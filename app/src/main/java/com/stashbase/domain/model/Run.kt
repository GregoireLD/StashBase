package com.stashbase.domain.model

data class Run(
    val id: Long = 0,
    val blueprintId: Long? = null,
    val blueprintName: String = "",
    val name: String,
    val status: RunStatus = RunStatus.IN_PROGRESS,
    val notes: String? = null,
    val photoPath: String? = null,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
    val deadlineMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val bomEntries: List<RunBomEntry> = emptyList(),
    val steps: List<RunStep> = emptyList(),
) {
    val completedSteps: Int get() = steps.count { it.isCompleted }
    val totalSteps: Int get() = steps.size
    val missingMaterials: List<RunBomEntry> get() = bomEntries.filter { it.deficit > 0 }
}

data class RunBomEntry(
    val id: Long = 0,
    val runId: Long,
    val originalMaterialId: Long? = null,
    val actualMaterialId: Long,
    val materialName: String = "",
    val materialUnit: MaterialUnit = MaterialUnit.PIECE,
    val availableQuantity: Double = 0.0,
    val allocatedToOtherRuns: Double = 0.0,
    val plannedQuantity: Double,
    val actualQuantity: Double? = null,
    val notes: String? = null,
) {
    val effectiveAvailable: Double get() = availableQuantity - allocatedToOtherRuns
    val deficit: Double get() = maxOf(0.0, plannedQuantity - effectiveAvailable)
    val isCovered: Boolean get() = deficit == 0.0
}

data class RunStep(
    val id: Long = 0,
    val runId: Long,
    val blueprintStepId: Long? = null,
    val parentStepId: Long? = null,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val photoPath: String? = null,
    val isCompleted: Boolean = false,
    val completionNotes: String? = null,
)
