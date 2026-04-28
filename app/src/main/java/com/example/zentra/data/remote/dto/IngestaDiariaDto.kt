package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.IngestaSlot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la tabla `daily_intakes` de Supabase.
 * Persiste cada alimento añadido a un slot de ingesta diaria junto con
 * sus macros base (100 g) y los gramos actuales elegidos por el usuario.
 */
@Serializable
data class IngestaDiariaDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val fecha: String = "",
    val slot: String = "",
    @SerialName("receta_titulo") val recetaTitulo: String = "",
    val gramos: Float = 100f,
    @SerialName("base_kcal") val baseKcal: Int = 0,
    @SerialName("base_proteinas_g") val baseProteinasG: Float = 0f,
    @SerialName("base_carbos_g") val baseCarbosG: Float = 0f,
    @SerialName("base_grasas_g") val baseGrasasG: Float = 0f,
    val orden: Int = 0,
    @SerialName("created_at") val creadoEn: String? = null
) {
    /** Convierte este DTO al modelo de dominio [IngestaSlot]. */
    fun asDominio() = IngestaSlot(
        dbId = id,
        recetaTitulo = recetaTitulo,
        gramos = gramos,
        baseKcal = baseKcal,
        baseProteinasG = baseProteinasG,
        baseCarbosG = baseCarbosG,
        baseGrasasG = baseGrasasG,
        orden = orden
    )
}

/**
 * Convierte un [IngestaSlot] de dominio a su representación como DTO para Supabase.
 * Requiere los campos de contexto (userId, fecha, slotNombre) que el slot no almacena.
 */
fun IngestaSlot.asDto(userId: String, fecha: String, slotNombre: String) = IngestaDiariaDto(
    id = dbId,
    userId = userId,
    fecha = fecha,
    slot = slotNombre,
    recetaTitulo = recetaTitulo,
    gramos = gramos,
    baseKcal = baseKcal,
    baseProteinasG = baseProteinasG,
    baseCarbosG = baseCarbosG,
    baseGrasasG = baseGrasasG,
    orden = orden
)
