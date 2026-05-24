package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val isChecked: Boolean = false,
    val sourceProjectId: Long? = null,
    val sourceMaterialId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
