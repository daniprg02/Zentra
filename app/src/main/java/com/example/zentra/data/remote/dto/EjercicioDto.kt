package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.Ejercicio
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la tabla `exercises_master` de Supabase.
 * Catálogo de ejercicios compartido por todos los usuarios, de solo lectura.
 */
@Serializable
data class EjercicioDto(
    val id: String = "",
    val nombre: String = "",
    @SerialName("grupo_muscular") val grupoMuscular: String = "",
    val equipo: String = "",
    val nivel: String = ""
) {
    fun asDominio() = Ejercicio(
        id = id,
        nombre = nombre,
        grupoMuscular = grupoMuscular,
        equipo = equipo,
        nivel = nivel
    )
}
