package com.example.zentra.di

import com.example.zentra.data.remote.ConstantesRed
import com.example.zentra.data.remote.api.OpenFoodFactsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Módulo de Hilt responsable de proveer las dependencias de red de la aplicación.
 * Incluye el cliente de Supabase (BDD + Auth + Storage) y el cliente Retrofit
 * para la API nutricional externa (OpenFoodFacts).
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloRed {

    /**
     * Proporciona la instancia única del cliente de Supabase configurada con los
     * plugins de autenticación, base de datos (PostgREST) y almacenamiento.
     * Recuerda configurar [ConstantesRed.SUPABASE_URL] y [ConstantesRed.SUPABASE_ANON_KEY]
     * con los valores reales de tu proyecto antes de compilar.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = ConstantesRed.SUPABASE_URL,
        supabaseKey = ConstantesRed.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    /**
     * Proporciona el cliente OkHttp con un interceptor de logs activo solo en debug.
     * Permite inspeccionar todas las peticiones HTTP en Logcat durante el desarrollo.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // Solo registramos el body completo en builds de debug para no exponer datos en producción
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    /**
     * Proporciona la instancia de Retrofit para consumir la API nutricional externa (OpenFoodFacts).
     * @param okHttpClient Cliente HTTP configurado con logging.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(ConstantesRed.API_NUTRICIONAL_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Proporciona la implementación de [OpenFoodFactsService] generada por Retrofit.
     * @param retrofit Instancia de Retrofit ya configurada con la base URL y el conversor.
     */
    @Provides
    @Singleton
    fun provideOpenFoodFactsService(retrofit: Retrofit): OpenFoodFactsService =
        retrofit.create(OpenFoodFactsService::class.java)
}
