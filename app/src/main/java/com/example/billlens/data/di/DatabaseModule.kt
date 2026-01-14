package com.example.billlens.data.di

import android.content.Context
import com.example.billlens.data.local.AppDatabase
import com.example.billlens.data.local.ExpenseDao
import com.example.billlens.data.local.LocalDataSource
import com.example.billlens.data.local.UserDao
import com.example.billlens.data.local.UserLocalDataSource
import com.example.billlens.data.repository.ExpenseRepository
import com.example.billlens.data.repository.ExpenseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


// Modificato in "abstract class" per usare @Binds
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository
}

// Puoi tenere il resto in un modulo separato o nello stesso file.
// Per chiarezza, è comune avere un modulo per i repository e uno per i database/network.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideLocalDataSource(expenseDao: ExpenseDao): LocalDataSource {
        return LocalDataSource(expenseDao)
    }

    @Provides
    @Singleton
    fun provideUserLocalDataSource(userDao: UserDao): UserLocalDataSource {
        return UserLocalDataSource(userDao)
    }
}