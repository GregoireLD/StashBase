package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blueprints")
data class BlueprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val photoPath: String? = null,
    val externalLinks: String = "[]",
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val deadlineMillis: Long? = null,
)
