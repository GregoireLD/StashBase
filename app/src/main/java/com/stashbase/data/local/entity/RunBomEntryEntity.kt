package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_bom_entries",
    foreignKeys = [
        ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["runId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["actualMaterialId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("runId"), Index("actualMaterialId")]
)
data class RunBomEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val originalMaterialId: Long? = null,
    val actualMaterialId: Long,
    val plannedQuantity: Double,
    val actualQuantity: Double? = null,
    val notes: String? = null,
)
