package com.example.billlens.data.di

import com.example.billlens.data.repository.UserRepository
import com.example.billlens.data.repository.UserRepositoryImpl
import com.example.billlens.data.user.FakeUserDataSource
import com.example.billlens.data.user.UserDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    // Dice a Hilt: quando qualcuno chiede un UserRepository, fornisci UserRepositoryImpl
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    // Dice a Hilt: quando qualcuno chiede un UserDataSource, fornisci FakeUserDataSource
    // ATTENZIONE: Quando avrai una vera UserDataSource, cambierai solo questa riga!
    @Binds
    @Singleton
    abstract fun bindUserDataSource(impl: FakeUserDataSource): UserDataSource
}