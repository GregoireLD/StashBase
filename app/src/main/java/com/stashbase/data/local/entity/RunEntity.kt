package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    foreignKeys = [
        ForeignKey(entity = BlueprintEntity::class, parentColumns = ["id"], childColumns = ["blueprintId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("blueprintId")]
)
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blueprintId: Long? = null,
    val name: String,
    val status: String = "IN_PROGRESS",
    val notes: String? = null,
    val photoPath: String? = null,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
    val deadlineMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
