package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_steps",
    foreignKeys = [
        ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["runId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("runId")]
)
data class RunStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val blueprintStepId: Long? = null,
    val parentStepId: Long? = null,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val photoPath: String? = null,
    val isCompleted: Boolean = false,
    val completionNotes: String? = null,
)
