package com.example.zentra.di

import com.example.zentra.data.remote.ConstantesRed
import com.example.zentra.data.remote.api.GeminiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee el cliente REST de Gemini.
 * Usa un Retrofit separado con la base URL de Gemini para no interferir
 * con el Retrofit de OpenFoodFacts ya configurado en ModuloRed.
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloGemini {

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(ConstantesRed.GEMINI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("gemini") retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)
}
