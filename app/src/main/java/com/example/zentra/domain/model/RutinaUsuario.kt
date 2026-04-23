package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa la cabecera del plan de entrenamiento activo del usuario.
 * Se vincula con [DiasRutina] para obtener el detalle de ejercicios de cada jornada.
 *
 * @param id UUID único de la rutina.
 * @param userId UUID del usuario propietario.
 * @param objetivo "Déficit", "Mantenimiento" o "Superávit". Determina el ajuste calórico del TDEE.
 * @param diasSemana Número de días de entrenamiento semanales configurados.
 * @param activa Si es true, esta rutina es el plan vigente del usuario.
 */
data class RutinaUsuario(
    val id: String,
    val userId: String,
    val objetivo: String,
    val diasSemana: Int,
    val activa: Boolean,
    val creadaEn: String? = null
)
