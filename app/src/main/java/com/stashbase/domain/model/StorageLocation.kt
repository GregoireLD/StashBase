package com.stashbase.domain.model

data class StorageLocation(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val parentLocationId: Long? = null,
    val parentLocationName: String? = null,
    val colorHex: String? = null,
    val children: List<StorageLocation> = emptyList(),
) {
    val fullPath: String get() = if (parentLocationName != null) "$parentLocationName › $name" else name
}
