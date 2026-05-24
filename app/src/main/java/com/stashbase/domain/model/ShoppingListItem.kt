package com.stashbase.domain.model

data class ShoppingListItem(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val quantity: Double? = null,
    val unit: MaterialUnit? = null,
    val isChecked: Boolean = false,
    val sourceProjectId: Long? = null,
    val sourceProjectName: String? = null,
    val sourceMaterialId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
