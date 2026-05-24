package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tools",
    foreignKeys = [
        ForeignKey(
            entity = StorageLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("locationId")]
)
data class ToolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val brand: String? = null,
    val size: String? = null,
    val condition: String = "GOOD",
    val locationId: Long? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
