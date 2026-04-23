package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.Receta
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * DTO raíz de la respuesta de búsqueda de OpenFoodFacts.
 * El campo `products` puede ser nulo si no hay resultados o la API falla.
 */
data class RespuestaBusquedaAlimentos(
    val products: List<ProductoAlimentoDto>?
)

/** Producto individual devuelto por la búsqueda de OpenFoodFacts. */
data class ProductoAlimentoDto(
    @SerializedName("product_name") val nombre: String?,
    @SerializedName("serving_size") val tamanoRacion: String?,
    val nutriments: NutrimentosDto?
) {
    /**
     * Convierte el producto a una [Receta] temporal (sin persistir en BD).
     * Usa valores por ración si existen; cae a valores por 100 g si no.
     * Devuelve null si faltan datos mínimos para calcular las calorías.
     */
    fun aReceta(): Receta? {
        val titulo = nombre?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val nut = nutriments ?: return null

        val kcal = (nut.kcalRacion ?: nut.kcal100g)?.toFloat() ?: return null
        if (kcal <= 0f) return null

        val proteinas = (nut.proteinasRacion ?: nut.proteinas100g)?.toFloat() ?: 0f
        val carbos = (nut.carbosRacion ?: nut.carbos100g)?.toFloat() ?: 0f
        val grasas = (nut.grasasRacion ?: nut.grasas100g)?.toFloat() ?: 0f

        return Receta(
            id = UUID.randomUUID().toString(),
            userId = "",
            titulo = titulo,
            kcalTotales = kcal.toInt(),
            proteinasG = proteinas,
            carbosG = carbos,
            grasasG = grasas,
            ingredientes = tamanoRacion ?: "",
            fijada = false,
            creadaEn = null
        )
    }
}

/** Tabla de nutrientes del producto, todos opcionales para gestionar datos incompletos. */
data class NutrimentosDto(
    @SerializedName("energy-kcal_serving") val kcalRacion: Double?,
    @SerializedName("proteins_serving") val proteinasRacion: Double?,
    @SerializedName("carbohydrates_serving") val carbosRacion: Double?,
    @SerializedName("fat_serving") val grasasRacion: Double?,
    @SerializedName("energy-kcal_100g") val kcal100g: Double?,
    @SerializedName("proteins_100g") val proteinas100g: Double?,
    @SerializedName("carbohydrates_100g") val carbos100g: Double?,
    @SerializedName("fat_100g") val grasas100g: Double?
)
