package com.stashbase.data.local.dao

import androidx.room.*
import com.stashbase.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY isChecked ASC, createdAtMillis DESC")
    fun getAll(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0 ORDER BY createdAtMillis DESC")
    fun getUnchecked(): Flow<List<ShoppingListItemEntity>>

    @Upsert
    suspend fun upsert(item: ShoppingListItemEntity): Long

    @Query("UPDATE shopping_list_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE isChecked = 1")
    suspend fun deleteChecked()

    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 1")
    suspend fun getChecked(): List<ShoppingListItemEntity>
}
