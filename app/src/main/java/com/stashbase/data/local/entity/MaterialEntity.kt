package com.stashbase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "materials",
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
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val brand: String? = null,
    val color: String? = null,
    val colorCode: String? = null,
    val quantity: Double,
    val unit: String,
    val locationId: Long? = null,
    val purchasePrice: Double? = null,
    val purchaseDateMillis: Long? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val tags: String = "",
    val lowStockThreshold: Double? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
