package com.example.zentra.domain.repository

import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.Ejercicio
import com.example.zentra.domain.model.RutinaUsuario

/**
 * Contrato del repositorio de rutinas de entrenamiento.
 * Abstrae el acceso al catálogo de ejercicios y a los planes personalizados del usuario.
 */
interface IRutinasRepositorio {

    /**
     * Obtiene ejercicios del catálogo filtrados por equipamiento y nivel.
     * Al ser una tabla pequeña (~68 filas), el filtrado se puede hacer en cliente.
     * @param equipos Lista de tipos de equipo disponibles para el usuario.
     * @param niveles Lista de niveles de dificultad acordes a la experiencia.
     * @return [Result] con la lista de [Ejercicio] o el error producido.
     */
    suspend fun obtenerEjercicios(equipos: List<String>, niveles: List<String>): Result<List<Ejercicio>>

    /**
     * Persiste una rutina completa: primero la cabecera y luego todos sus días.
     * @param rutina Cabecera del plan con objetivo, días y metadatos.
     * @param dias Lista de [DiaRutina] con los ejercicios de cada jornada.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun guardarRutina(rutina: RutinaUsuario, dias: List<DiaRutina>): Result<Unit>

    /**
     * Obtiene la rutina activa del usuario con todos sus días.
     * @param userId UUID del usuario autenticado.
     * @return [Result] con el par (cabecera, días) o null si no hay rutina activa.
     */
    suspend fun obtenerRutinaActiva(userId: String): Result<Pair<RutinaUsuario, List<DiaRutina>>?>

    /**
     * Marca como inactivas todas las rutinas del usuario.
     * Se llama antes de guardar una nueva rutina para que solo haya una activa.
     * @param userId UUID del usuario autenticado.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun desactivarRutinaActiva(userId: String): Result<Unit>
}
