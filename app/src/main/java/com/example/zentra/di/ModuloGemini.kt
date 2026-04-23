package com.example.zentra.di

import com.example.zentra.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuloGemini {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
}
