package com.stashbase.domain.model

data class Tool(
    val id: Long = 0,
    val name: String,
    val type: ToolType,
    val brand: String? = null,
    val size: String? = null,
    val condition: ToolCondition = ToolCondition.GOOD,
    val locationId: Long? = null,
    val locationName: String? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
