package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "finished_items",
    foreignKeys = [
        ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["runId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("runId")]
)
data class FinishedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long? = null,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "PIECE",
    val status: String = "PERSONAL_USE",
    val sellingPrice: Double? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
