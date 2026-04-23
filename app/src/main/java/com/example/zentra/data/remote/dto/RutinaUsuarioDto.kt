package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.RutinaUsuario
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la tabla `user_routines` de Supabase.
 * Representa la cabecera del plan de entrenamiento activo del usuario.
 */
@Serializable
data class RutinaUsuarioDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val objetivo: String = "",
    @SerialName("dias_semana") val diasSemana: Int = 0,
    val activa: Boolean = true,
    @SerialName("creada_en") val creadaEn: String? = null
) {
    fun asDominio() = RutinaUsuario(
        id = id,
        userId = userId,
        objetivo = objetivo,
        diasSemana = diasSemana,
        activa = activa,
        creadaEn = creadaEn
    )
}

fun RutinaUsuario.asDto() = RutinaUsuarioDto(
    id = id,
    userId = userId,
    objetivo = objetivo,
    diasSemana = diasSemana,
    activa = activa,
    creadaEn = creadaEn
)
