package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.MacrosDiarios
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la tabla `daily_macros` de Supabase.
 * Registra el estado nutricional del usuario para cada día calendario.
 */
@Serializable
data class MacrosDiariosDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val fecha: String = "",
    @SerialName("objetivo_kcal") val objetivoKcal: Int = 0,
    @SerialName("consumido_kcal") val consumidoKcal: Int = 0,
    @SerialName("consumido_proteinas") val consumidoProteinasG: Float = 0f,
    @SerialName("consumido_carbos") val consumidoCarbosG: Float = 0f,
    @SerialName("consumido_grasas") val consumidoGrasasG: Float = 0f
) {
    /** Convierte este DTO al modelo de dominio [MacrosDiarios]. */
    fun asDominio() = MacrosDiarios(
        id = id,
        userId = userId,
        fecha = fecha,
        objetivoKcal = objetivoKcal,
        consumidoKcal = consumidoKcal,
        consumidoProteinasG = consumidoProteinasG,
        consumidoCarbosG = consumidoCarbosG,
        consumidoGrasasG = consumidoGrasasG
    )
}

/** Convierte un [MacrosDiarios] de dominio a su representación como DTO para Supabase. */
fun MacrosDiarios.asDto() = MacrosDiariosDto(
    id = id,
    userId = userId,
    fecha = fecha,
    objetivoKcal = objetivoKcal,
    consumidoKcal = consumidoKcal,
    consumidoProteinasG = consumidoProteinasG,
    consumidoCarbosG = consumidoCarbosG,
    consumidoGrasasG = consumidoGrasasG
)
