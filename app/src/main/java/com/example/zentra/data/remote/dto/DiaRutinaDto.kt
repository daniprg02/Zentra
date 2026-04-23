package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.EjercicioEnRutina
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representación JSON de un ejercicio dentro de un día de rutina.
 * Se almacena como objeto dentro del array JSONB del campo `ejercicios` en `routine_days`.
 */
@Serializable
data class EjercicioEnRutinaJson(
    @SerialName("ejercicio_id") val ejercicioId: String = "",
    val nombre: String = "",
    @SerialName("grupo_muscular") val grupoMuscular: String = "",
    val series: Int = 0,
    val repeticiones: String = ""
) {
    fun asDominio() = EjercicioEnRutina(
        ejercicioId = ejercicioId,
        nombre = nombre,
        grupoMuscular = grupoMuscular,
        series = series,
        repeticiones = repeticiones
    )
}

fun EjercicioEnRutina.asJson() = EjercicioEnRutinaJson(
    ejercicioId = ejercicioId,
    nombre = nombre,
    grupoMuscular = grupoMuscular,
    series = series,
    repeticiones = repeticiones
)

/**
 * DTO para la tabla `routine_days` de Supabase.
 * Cada fila representa un día de entrenamiento con su lista de ejercicios en JSONB.
 */
@Serializable
data class DiaRutinaDto(
    val id: String = "",
    @SerialName("routine_id") val routineId: String = "",
    @SerialName("dia_numero") val diaNumero: Int = 0,
    @SerialName("nombre_dia") val nombreDia: String = "",
    val ejercicios: List<EjercicioEnRutinaJson> = emptyList()
) {
    fun asDominio() = DiaRutina(
        id = id,
        rutinaId = routineId,
        diaNumero = diaNumero,
        nombreDia = nombreDia,
        ejercicios = ejercicios.map { it.asDominio() }
    )
}

fun DiaRutina.asDto() = DiaRutinaDto(
    id = id,
    routineId = rutinaId,
    diaNumero = diaNumero,
    nombreDia = nombreDia,
    ejercicios = ejercicios.map { it.asJson() }
)
