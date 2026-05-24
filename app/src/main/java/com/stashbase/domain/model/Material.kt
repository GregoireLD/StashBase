package com.stashbase.domain.model

data class Material(
    val id: Long = 0,
    val name: String,
    val type: MaterialType,
    val brand: String? = null,
    val color: String? = null,
    val colorCode: String? = null,
    val quantity: Double,
    val unit: MaterialUnit,
    val locationId: Long? = null,
    val locationName: String? = null,
    val purchasePrice: Double? = null,
    val purchaseDateMillis: Long? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val tags: List<String> = emptyList(),
    val lowStockThreshold: Double? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val allocatedQuantity: Double = 0.0

    val availableQuantity: Double get() = quantity - allocatedQuantity

    val isLowStock: Boolean get() = lowStockThreshold != null && availableQuantity <= lowStockThreshold
}
