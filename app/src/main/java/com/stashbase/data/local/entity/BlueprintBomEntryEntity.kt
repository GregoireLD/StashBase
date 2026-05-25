package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blueprint_bom_entries",
    foreignKeys = [
        ForeignKey(entity = BlueprintEntity::class, parentColumns = ["id"], childColumns = ["blueprintId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("blueprintId"), Index("materialId")]
)
data class BlueprintBomEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blueprintId: Long,
    val materialId: Long,
    val requiredQuantity: Double,
    val notes: String? = null,
)
