package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_locations",
    foreignKeys = [
        ForeignKey(
            entity = StorageLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentLocationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentLocationId")]
)
data class StorageLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val parentLocationId: Long? = null,
    val colorHex: String? = null,
)
