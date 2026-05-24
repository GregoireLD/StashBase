package com.stashbase.domain.repository

import com.stashbase.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    fun getAll(): Flow<List<ShoppingListItem>>
    fun getUnchecked(): Flow<List<ShoppingListItem>>
    suspend fun upsert(item: ShoppingListItem): Long
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun delete(id: Long)
    suspend fun deleteChecked()
    suspend fun generateFromProjectDeficits(projectId: Long)
}
