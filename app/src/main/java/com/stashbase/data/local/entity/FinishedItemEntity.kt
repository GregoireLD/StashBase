package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "finished_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class FinishedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "PIECE",
    val status: String = "PERSONAL_USE",
    val sellingPrice: Double? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
