package com.stashbase.domain.model

data class FinishedItem(
    val id: Long = 0,
    val projectId: Long,
    val projectName: String = "",
    val name: String,
    val quantity: Double = 1.0,
    val unit: MaterialUnit = MaterialUnit.PIECE,
    val status: FinishedItemStatus = FinishedItemStatus.PERSONAL_USE,
    val sellingPrice: Double? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
