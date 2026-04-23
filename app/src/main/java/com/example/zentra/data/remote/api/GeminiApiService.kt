package com.example.zentra.data.remote.api

import com.example.zentra.data.remote.dto.PeticionGemini
import com.example.zentra.data.remote.dto.RespuestaGeminiApi
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Servicio Retrofit para la API REST de Gemini (Google Generative Language API).
 * Base URL: https://generativelanguage.googleapis.com/
 * Documentación: https://ai.google.dev/api/generate-content
 */
interface GeminiApiService {

    /**
     * Envía un prompt al modelo indicado y devuelve el texto generado.
     * @param modelo Nombre del modelo, ej: "gemini-2.0-flash-lite"
     * @param apiKey Clave de la API de Gemini (desde BuildConfig)
     * @param peticion Cuerpo de la petición con el prompt y configuración de generación
     */
    @POST("v1beta/models/{modelo}:generateContent")
    suspend fun generarContenido(
        @Path("modelo") modelo: String,
        @Query("key") apiKey: String,
        @Body peticion: PeticionGemini
    ): RespuestaGeminiApi
}
