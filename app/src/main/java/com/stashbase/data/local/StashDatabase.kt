package com.stashbase.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        FinishedItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class StashDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun toolDao(): ToolDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun projectDao(): ProjectDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun finishedItemDao(): FinishedItemDao

    companion object {
        const val DATABASE_NAME = "stashbase.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `finished_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL DEFAULT 1.0,
                        `unit` TEXT NOT NULL DEFAULT 'PIECE',
                        `status` TEXT NOT NULL DEFAULT 'PERSONAL_USE',
                        `sellingPrice` REAL,
                        `notes` TEXT,
                        `photoPath` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finished_items_projectId` ON `finished_items` (`projectId`)")
            }
        }
    }
}
