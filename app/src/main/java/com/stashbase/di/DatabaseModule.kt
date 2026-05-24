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
            .build()

    @Provides
    fun provideMaterialDao(db: StashDatabase) = db.materialDao()

    @Provides
    fun provideToolDao(db: StashDatabase) = db.toolDao()

    @Provides
    fun provideStorageLocationDao(db: StashDatabase) = db.storageLocationDao()

    @Provides
    fun provideProjectDao(db: StashDatabase) = db.projectDao()

    @Provides
    fun provideShoppingListDao(db: StashDatabase) = db.shoppingListDao()
}
