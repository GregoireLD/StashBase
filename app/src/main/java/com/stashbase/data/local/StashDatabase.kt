package com.stashbase.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stashbase.data.local.dao.*
import com.stashbase.data.local.entity.*

@Database(
    entities = [
        MaterialEntity::class,
        ToolEntity::class,
        StorageLocationEntity::class,
        ProjectEntity::class,
        BomEntryEntity::class,
        ShoppingListItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StashDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun toolDao(): ToolDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun projectDao(): ProjectDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {
        const val DATABASE_NAME = "stashbase.db"
    }
}
