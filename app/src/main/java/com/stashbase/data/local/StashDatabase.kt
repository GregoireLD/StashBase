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
        BlueprintEntity::class,
        BlueprintBomEntryEntity::class,
        BlueprintStepEntity::class,
        RunEntity::class,
        RunBomEntryEntity::class,
        RunStepEntity::class,
        ShoppingListItemEntity::class,
        FinishedItemEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class StashDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun toolDao(): ToolDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun blueprintDao(): BlueprintDao
    abstract fun runDao(): RunDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun finishedItemDao(): FinishedItemDao

    companion object {
        const val DATABASE_NAME = "stashbase.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finished_items_projectId` ON `finished_items` (`projectId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. blueprints (from projects, without status)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `blueprints` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `category` TEXT,
                        `photoPath` TEXT,
                        `externalLinks` TEXT NOT NULL DEFAULT '[]',
                        `notes` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        `deadlineMillis` INTEGER
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `blueprints` SELECT `id`,`name`,`description`,`category`,`photoPath`,`externalLinks`,`notes`,`createdAtMillis`,`deadlineMillis` FROM `projects`")

                // 2. blueprint_bom_entries (from bom_entries)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `blueprint_bom_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blueprintId` INTEGER NOT NULL,
                        `materialId` INTEGER NOT NULL,
                        `requiredQuantity` REAL NOT NULL,
                        `notes` TEXT,
                        FOREIGN KEY(`blueprintId`) REFERENCES `blueprints`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`materialId`) REFERENCES `materials`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `blueprint_bom_entries` SELECT `id`,`projectId`,`materialId`,`requiredQuantity`,`notes` FROM `bom_entries`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blueprint_bom_entries_blueprintId` ON `blueprint_bom_entries` (`blueprintId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blueprint_bom_entries_materialId` ON `blueprint_bom_entries` (`materialId`)")

                // 3. blueprint_steps (new, empty)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `blueprint_steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blueprintId` INTEGER NOT NULL,
                        `stepOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `photoPath` TEXT,
                        `groupName` TEXT,
                        FOREIGN KEY(`blueprintId`) REFERENCES `blueprints`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blueprint_steps_blueprintId` ON `blueprint_steps` (`blueprintId`)")

                // 4. runs (new, empty)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `runs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blueprintId` INTEGER,
                        `name` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                        `notes` TEXT,
                        `photoPath` TEXT,
                        `startedAtMillis` INTEGER NOT NULL,
                        `completedAtMillis` INTEGER,
                        `deadlineMillis` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`blueprintId`) REFERENCES `blueprints`(`id`) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_blueprintId` ON `runs` (`blueprintId`)")

                // 5. run_bom_entries (new, empty)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `run_bom_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `runId` INTEGER NOT NULL,
                        `originalMaterialId` INTEGER,
                        `actualMaterialId` INTEGER NOT NULL,
                        `plannedQuantity` REAL NOT NULL,
                        `actualQuantity` REAL,
                        `notes` TEXT,
                        FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`actualMaterialId`) REFERENCES `materials`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_bom_entries_runId` ON `run_bom_entries` (`runId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_bom_entries_actualMaterialId` ON `run_bom_entries` (`actualMaterialId`)")

                // 6. run_steps (new, empty)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `run_steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `runId` INTEGER NOT NULL,
                        `blueprintStepId` INTEGER,
                        `stepOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `photoPath` TEXT,
                        `groupName` TEXT,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completionNotes` TEXT,
                        FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_steps_runId` ON `run_steps` (`runId`)")

                // 7. Recreate finished_items with runId (nullable) instead of projectId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `finished_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `runId` INTEGER,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL DEFAULT 1.0,
                        `unit` TEXT NOT NULL DEFAULT 'PIECE',
                        `status` TEXT NOT NULL DEFAULT 'PERSONAL_USE',
                        `sellingPrice` REAL,
                        `notes` TEXT,
                        `photoPath` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `finished_items_new` (`id`,`runId`,`name`,`quantity`,`unit`,`status`,`sellingPrice`,`notes`,`photoPath`,`createdAtMillis`) SELECT `id`,NULL,`name`,`quantity`,`unit`,`status`,`sellingPrice`,`notes`,`photoPath`,`createdAtMillis` FROM `finished_items`")
                db.execSQL("DROP TABLE `finished_items`")
                db.execSQL("ALTER TABLE `finished_items_new` RENAME TO `finished_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finished_items_runId` ON `finished_items` (`runId`)")

                // 8. Recreate shopping_list_items with sourceRunId instead of sourceProjectId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_list_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `quantity` REAL,
                        `unit` TEXT,
                        `isChecked` INTEGER NOT NULL DEFAULT 0,
                        `sourceRunId` INTEGER,
                        `sourceMaterialId` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `shopping_list_items_new` (`id`,`name`,`description`,`quantity`,`unit`,`isChecked`,`sourceRunId`,`sourceMaterialId`,`createdAtMillis`) SELECT `id`,`name`,`description`,`quantity`,`unit`,`isChecked`,NULL,`sourceMaterialId`,`createdAtMillis` FROM `shopping_list_items`")
                db.execSQL("DROP TABLE `shopping_list_items`")
                db.execSQL("ALTER TABLE `shopping_list_items_new` RENAME TO `shopping_list_items`")

                // 9. Drop old tables
                db.execSQL("DROP TABLE IF EXISTS `bom_entries`")
                db.execSQL("DROP TABLE IF EXISTS `projects`")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate blueprint_steps: replace groupName with parentStepId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `blueprint_steps_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blueprintId` INTEGER NOT NULL,
                        `parentStepId` INTEGER,
                        `stepOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `photoPath` TEXT,
                        FOREIGN KEY(`blueprintId`) REFERENCES `blueprints`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `blueprint_steps_new` (`id`,`blueprintId`,`parentStepId`,`stepOrder`,`title`,`description`,`photoPath`) SELECT `id`,`blueprintId`,NULL,`stepOrder`,`title`,`description`,`photoPath` FROM `blueprint_steps`")
                db.execSQL("DROP TABLE `blueprint_steps`")
                db.execSQL("ALTER TABLE `blueprint_steps_new` RENAME TO `blueprint_steps`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blueprint_steps_blueprintId` ON `blueprint_steps` (`blueprintId`)")

                // Recreate run_steps: replace groupName with parentStepId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `run_steps_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `runId` INTEGER NOT NULL,
                        `blueprintStepId` INTEGER,
                        `parentStepId` INTEGER,
                        `stepOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `photoPath` TEXT,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completionNotes` TEXT,
                        FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `run_steps_new` (`id`,`runId`,`blueprintStepId`,`parentStepId`,`stepOrder`,`title`,`description`,`photoPath`,`isCompleted`,`completionNotes`) SELECT `id`,`runId`,`blueprintStepId`,NULL,`stepOrder`,`title`,`description`,`photoPath`,`isCompleted`,`completionNotes` FROM `run_steps`")
                db.execSQL("DROP TABLE `run_steps`")
                db.execSQL("ALTER TABLE `run_steps_new` RENAME TO `run_steps`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_steps_runId` ON `run_steps` (`runId`)")
            }
        }
    }
}
