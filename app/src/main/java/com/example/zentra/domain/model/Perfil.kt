package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa el perfil físico del usuario.
 * Es la fuente de verdad interna de la app, independiente de la representación en base de datos.
 * Se mapea desde [com.example.zentra.data.remote.dto.PerfilDto].
 *
 * @param id UUID del usuario, vinculado a auth.users de Supabase.
 * @param nombre Nombre real del usuario.
 * @param apodo Nombre de usuario visible en la interfaz.
 * @param edad Edad en años, usada en el cálculo de la TMB (Mifflin-St Jeor).
 * @param alturaCm Altura siempre almacenada en centímetros (fuente de verdad).
 * @param pesoKg Peso siempre almacenado en kilogramos (fuente de verdad).
 * @param preferenciaSistema Unidad de visualización: "metrico" (kg/cm) o "imperial" (lb/ft).
 * @param sexo "Masculino" o "Femenino", determina la variante de la fórmula TMB.
 * @param creadoEn Timestamp de registro, en formato ISO 8601.
 */
data class Perfil(
    val id: String,
    val nombre: String,
    val apodo: String,
    val edad: Int,
    val alturaCm: Int,
    val pesoKg: Float,
    val preferenciaSistema: String,
    val sexo: String,
    // Nullable porque lo rellena Supabase automáticamente con now() al insertar
    val creadoEn: String?
)
