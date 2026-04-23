package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa una receta del módulo de alimentación.
 * Almacena los macronutrientes totales para que la calculadora dietética pueda
 * volcarlos directamente en los slots de ingesta diaria.
 *
 * @param id UUID único de la receta.
 * @param userId UUID del usuario propietario de la receta.
 * @param titulo Nombre descriptivo de la receta.
 * @param kcalTotales Calorías totales calculadas a partir de los macros (P×4 + C×4 + G×9).
 * @param proteinasG Gramos de proteína de la receta completa.
 * @param carbosG Gramos de carbohidratos de la receta completa.
 * @param grasasG Gramos de grasa de la receta completa.
 * @param ingredientes Lista de ingredientes en texto libre.
 * @param fijada Si es true, la receta aparece fijada al inicio de la biblioteca.
 * @param creadaEn Timestamp de creación en formato ISO 8601, rellenado por Supabase.
 */
data class Receta(
    val id: String,
    val userId: String,
    val titulo: String,
    val kcalTotales: Int,
    val proteinasG: Float,
    val carbosG: Float,
    val grasasG: Float,
    val ingredientes: String,
    val fijada: Boolean = false,
    val creadaEn: String? = null
)
