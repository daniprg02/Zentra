package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa el registro nutricional de un día concreto.
 * La calculadora dietética lee y escribe en este modelo para actualizar el progreso
 * en tiempo real a medida que el usuario añade ingestas a los slots del día.
 *
 * @param id UUID del registro.
 * @param userId UUID del usuario propietario.
 * @param fecha Fecha del registro en formato "YYYY-MM-DD".
 * @param objetivoKcal Calorías objetivo del día, calculadas con TDEE + ajuste de objetivo.
 * @param consumidoKcal Calorías consumidas hasta el momento.
 * @param consumidoProteinasG Proteínas consumidas en gramos.
 * @param consumidoCarbosG Carbohidratos consumidos en gramos.
 * @param consumidoGrasasG Grasas consumidas en gramos.
 */
data class MacrosDiarios(
    val id: String,
    val userId: String,
    val fecha: String,
    val objetivoKcal: Int,
    val consumidoKcal: Int,
    val consumidoProteinasG: Float,
    val consumidoCarbosG: Float,
    val consumidoGrasasG: Float
)
