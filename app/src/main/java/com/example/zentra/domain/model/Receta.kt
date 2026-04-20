package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa una receta del módulo de alimentación.
 * Almacena los macronutrientes totales para que la calculadora dietética pueda
 * volcarlos directamente en los slots de ingesta diaria.
 *
 * @param id UUID único de la receta.
 * @param userId UUID del usuario propietario de la receta.
 * @param titulo Nombre descriptivo de la receta.
 * @param kcalTotales Calorías totales de la receta completa.
 * @param proteinasG Gramos de proteína de la receta completa.
 * @param carbosG Gramos de carbohidratos de la receta completa.
 * @param grasasG Gramos de grasa de la receta completa.
 * @param ingredientes Lista de ingredientes en formato texto libre o JSON serializado.
 */
data class Receta(
    val id: String,
    val userId: String,
    val titulo: String,
    val kcalTotales: Int,
    val proteinasG: Float,
    val carbosG: Float,
    val grasasG: Float,
    val ingredientes: String
)
