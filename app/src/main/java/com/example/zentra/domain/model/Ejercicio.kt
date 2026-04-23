package com.example.zentra.domain.model

/**
 * Modelo de dominio que representa un ejercicio del catálogo global.
 * Los ejercicios del catálogo son compartidos por todos los usuarios y nunca se modifican.
 *
 * @param id UUID del ejercicio generado por Supabase.
 * @param nombre Nombre descriptivo del ejercicio (ej: "Press de banca plano").
 * @param grupoMuscular Músculo principal trabajado (ej: "Pecho", "Espalda").
 * @param equipo Tipo de equipamiento necesario (ej: "Barra", "Mancuernas", "Calistenia").
 * @param nivel Nivel de dificultad: "Principiante", "Intermedio" o "Avanzado".
 */
data class Ejercicio(
    val id: String,
    val nombre: String,
    val grupoMuscular: String,
    val equipo: String,
    val nivel: String
)
