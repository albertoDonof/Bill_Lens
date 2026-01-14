package com.example.billlens.data.di

import com.example.billlens.data.network.BillLensApiService
import com.example.billlens.data.network.NetworkDataSource
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// app/src/main/java/com/example/billlens/data/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // L'IP 10.0.2.2 è l'alias per il localhost del tuo PC dall'emulatore Android
    //private const val BASE_URL = "http://10.0.2.2:5000/"
    //Per il telefono fisico:
    private const val BASE_URL = "http://10.154.75.78:5000/"


    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Formato atteso dal server
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): BillLensApiService {
        return retrofit.create(BillLensApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkDataSource(apiService: BillLensApiService): NetworkDataSource {
        return NetworkDataSource(apiService)
    }
}