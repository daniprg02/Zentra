package com.example.zentra.domain.model

/**
 * Ejercicio asignado a un día concreto de una rutina, con volumen personalizado.
 *
 * @param ejercicioId UUID del ejercicio en el catálogo global.
 * @param nombre Nombre del ejercicio copiado del catálogo para mostrar sin JOIN.
 * @param grupoMuscular Grupo muscular del ejercicio.
 * @param series Número de series asignadas según el objetivo del usuario.
 * @param repeticiones Rango de repeticiones (ej: "8-12", "12-15", "6-10").
 */
data class EjercicioEnRutina(
    val ejercicioId: String,
    val nombre: String,
    val grupoMuscular: String,
    val series: Int,
    val repeticiones: String
)

/**
 * Modelo de dominio de un día dentro de un plan de entrenamiento activo.
 *
 * @param id UUID único del día generado en cliente.
 * @param rutinaId UUID de la [RutinaUsuario] a la que pertenece.
 * @param diaNumero Posición ordinal del día dentro de la semana (1..6).
 * @param nombreDia Etiqueta descriptiva del bloque (ej: "Push", "Piernas", "Full Body A").
 * @param ejercicios Lista de ejercicios asignados a este día con su volumen.
 */
data class DiaRutina(
    val id: String,
    val rutinaId: String,
    val diaNumero: Int,
    val nombreDia: String,
    val ejercicios: List<EjercicioEnRutina>
)
