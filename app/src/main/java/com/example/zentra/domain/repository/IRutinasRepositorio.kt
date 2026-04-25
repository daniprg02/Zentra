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

    /**
     * Obtiene todas las rutinas del usuario ordenadas de más reciente a más antigua.
     * @param userId UUID del usuario autenticado.
     * @return [Result] con la lista completa de [RutinaUsuario].
     */
    suspend fun obtenerTodasLasRutinas(userId: String): Result<List<RutinaUsuario>>

    /**
     * Obtiene los días de una rutina concreta, ordenados por número de día.
     * Se usa para cargar los días al activar una rutina anterior.
     * @param rutinaId UUID de la rutina.
     * @return [Result] con la lista de [DiaRutina].
     */
    suspend fun obtenerDiasDeRutina(rutinaId: String): Result<List<DiaRutina>>

    /**
     * Marca una rutina concreta como activa (sin tocar las demás).
     * Debe llamarse tras [desactivarRutinaActiva] para garantizar unicidad.
     * @param rutinaId UUID de la rutina a activar.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun marcarRutinaActiva(rutinaId: String): Result<Unit>

    /**
     * Elimina permanentemente una rutina y sus días (cascade en BD).
     * @param rutinaId UUID de la rutina a eliminar.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun eliminarRutina(rutinaId: String): Result<Unit>

    /**
     * Actualiza la lista de ejercicios de un día concreto de una rutina.
     * Se usa al editar series/repeticiones o al sustituir un ejercicio por IA.
     * @param dia [DiaRutina] con la lista de ejercicios ya modificada.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun actualizarDiaRutina(dia: DiaRutina): Result<Unit>
}
