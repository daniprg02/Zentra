package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.Receta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la tabla `recipes` de Supabase.
 * Los valores por defecto permiten deserializar respuestas parciales sin errores.
 */
@Serializable
data class RecetaDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val titulo: String = "",
    @SerialName("kcal_totales") val kcalTotales: Int = 0,
    @SerialName("proteinas_g") val proteinasG: Float = 0f,
    @SerialName("carbos_g") val carbosG: Float = 0f,
    @SerialName("grasas_g") val grasasG: Float = 0f,
    val ingredientes: String = "",
    val fijada: Boolean = false,
    // Nullable: al insertar enviamos null para que Supabase aplique DEFAULT now()
    @SerialName("creada_en") val creadaEn: String? = null
) {
    /** Convierte este DTO al modelo de dominio [Receta]. */
    fun asDominio() = Receta(
        id = id,
        userId = userId,
        titulo = titulo,
        kcalTotales = kcalTotales,
        proteinasG = proteinasG,
        carbosG = carbosG,
        grasasG = grasasG,
        ingredientes = ingredientes,
        fijada = fijada,
        creadaEn = creadaEn
    )
}

/** Convierte una [Receta] de dominio a su representación como DTO para Supabase. */
fun Receta.asDto() = RecetaDto(
    id = id,
    userId = userId,
    titulo = titulo,
    kcalTotales = kcalTotales,
    proteinasG = proteinasG,
    carbosG = carbosG,
    grasasG = grasasG,
    ingredientes = ingredientes,
    fijada = fijada,
    creadaEn = creadaEn
)
