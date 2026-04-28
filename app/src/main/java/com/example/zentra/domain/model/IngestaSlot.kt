package com.example.zentra.domain.model

/**
 * Representa una unidad de comida añadida a un slot de ingesta diaria.
 *
 * Los macros base se guardan siempre referenciados a 100 g, independientemente
 * de los gramos actuales elegidos por el usuario. Así la edición de cantidad
 * siempre escala desde la misma referencia y nunca acumula errores de redondeo.
 *
 * @param dbId UUID de la fila en `daily_intakes`. Vacío ("") mientras no se ha persistido.
 * @param recetaTitulo Nombre del alimento o receta.
 * @param gramos Cantidad actual que el usuario ha introducido.
 * @param baseKcal Calorías de referencia para 100 g.
 * @param baseProteinasG Proteínas de referencia para 100 g.
 * @param baseCarbosG Carbohidratos de referencia para 100 g.
 * @param baseGrasasG Grasas de referencia para 100 g.
 * @param orden Posición dentro del slot (0-based) para respetar el orden de inserción.
 */
data class IngestaSlot(
    val dbId: String = "",
    val recetaTitulo: String,
    val gramos: Float = 100f,
    val baseKcal: Int,
    val baseProteinasG: Float,
    val baseCarbosG: Float,
    val baseGrasasG: Float,
    val orden: Int = 0
) {
    private val factor: Float get() = gramos / 100f

    val kcalActual: Int get() = (baseKcal * factor).toInt()
    val proteinasActual: Float get() = baseProteinasG * factor
    val carbosActual: Float get() = baseCarbosG * factor
    val grasasActual: Float get() = baseGrasasG * factor
}
