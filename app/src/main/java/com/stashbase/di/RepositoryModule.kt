package com.stashbase.di

import com.stashbase.data.repository.*
import com.stashbase.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindMaterialRepository(impl: MaterialRepositoryImpl): MaterialRepository

    @Binds @Singleton
    abstract fun bindToolRepository(impl: ToolRepositoryImpl): ToolRepository

    @Binds @Singleton
    abstract fun bindStorageLocationRepository(impl: StorageLocationRepositoryImpl): StorageLocationRepository

    @Binds @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds @Singleton
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): ShoppingListRepository

    @Binds @Singleton
    abstract fun bindFinishedItemRepository(impl: FinishedItemRepositoryImpl): FinishedItemRepository
}
