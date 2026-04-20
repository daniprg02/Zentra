package com.example.zentra.data.remote.dto

import com.example.zentra.domain.model.Perfil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO (Data Transfer Object) para la tabla `profiles` de Supabase.
 * Los nombres de los campos con @SerialName coinciden exactamente con las columnas de la BD.
 * Los valores por defecto son necesarios para que Supabase pueda deserializar respuestas parciales.
 */
@Serializable
data class PerfilDto(
    val id: String = "",
    val nombre: String = "",
    val apodo: String = "",
    val edad: Int = 0,
    @SerialName("altura_cm") val alturaCm: Int = 0,
    @SerialName("peso_kg") val pesoKg: Float = 0f,
    @SerialName("preferencia_sistema") val preferenciaSistema: String = "metrico",
    val sexo: String = "",
    // Nullable: si enviamos null, Supabase aplica el DEFAULT now() de la columna.
    // Enviar "" (vacío) causaría un error de parseo en PostgreSQL al intentar convertirlo a timestamp.
    @SerialName("creado_en") val creadoEn: String? = null
) {
    /** Convierte este DTO al modelo de dominio [Perfil]. */
    fun asDominio() = Perfil(
        id = id,
        nombre = nombre,
        apodo = apodo,
        edad = edad,
        alturaCm = alturaCm,
        pesoKg = pesoKg,
        preferenciaSistema = preferenciaSistema,
        sexo = sexo,
        creadoEn = creadoEn
    )
}

/** Convierte un [Perfil] de dominio a su representación como DTO para Supabase. */
fun Perfil.asDto() = PerfilDto(
    id = id,
    nombre = nombre,
    apodo = apodo,
    edad = edad,
    alturaCm = alturaCm,
    pesoKg = pesoKg,
    preferenciaSistema = preferenciaSistema,
    sexo = sexo,
    creadoEn = creadoEn  // null → Supabase usa DEFAULT now(); valor real → lo preserva al hacer update
)
