package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blueprint_steps",
    foreignKeys = [
        ForeignKey(entity = BlueprintEntity::class, parentColumns = ["id"], childColumns = ["blueprintId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("blueprintId")]
)
data class BlueprintStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blueprintId: Long,
    val parentStepId: Long? = null,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val photoPath: String? = null,
)
