package com.stashbase.di

import android.content.Context
import androidx.room.Room
import com.stashbase.data.local.StashDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StashDatabase =
        Room.databaseBuilder(context, StashDatabase::class.java, StashDatabase.DATABASE_NAME)
            .addMigrations(StashDatabase.MIGRATION_1_2, StashDatabase.MIGRATION_2_3, StashDatabase.MIGRATION_3_4)
            .build()

    @Provides fun provideMaterialDao(db: StashDatabase) = db.materialDao()
    @Provides fun provideToolDao(db: StashDatabase) = db.toolDao()
    @Provides fun provideStorageLocationDao(db: StashDatabase) = db.storageLocationDao()
    @Provides fun provideBlueprintDao(db: StashDatabase) = db.blueprintDao()
    @Provides fun provideRunDao(db: StashDatabase) = db.runDao()
    @Provides fun provideShoppingListDao(db: StashDatabase) = db.shoppingListDao()
    @Provides fun provideFinishedItemDao(db: StashDatabase) = db.finishedItemDao()
}
