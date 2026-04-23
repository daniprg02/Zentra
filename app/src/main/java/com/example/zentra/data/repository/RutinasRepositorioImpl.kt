package com.example.zentra.data.repository

import android.util.Log
import com.example.zentra.data.remote.dto.DiaRutinaDto
import com.example.zentra.data.remote.dto.EjercicioDto
import com.example.zentra.data.remote.dto.RutinaUsuarioDto
import com.example.zentra.data.remote.dto.asDto
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.Ejercicio
import com.example.zentra.domain.model.RutinaUsuario
import com.example.zentra.domain.repository.IRutinasRepositorio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

/**
 * Implementación del repositorio de rutinas que usa Supabase como fuente de datos.
 * Opera sobre las tablas `exercises_master`, `user_routines` y `routine_days`.
 */
class RutinasRepositorioImpl @Inject constructor(
    private val supabase: SupabaseClient
) : IRutinasRepositorio {

    override suspend fun obtenerEjercicios(equipos: List<String>, niveles: List<String>): Result<List<Ejercicio>> {
        return try {
            // Carga todos los ejercicios del catálogo y filtra en cliente (tabla pequeña, ~68 filas)
            val todos = supabase
                .from("exercises_master")
                .select()
                .decodeList<EjercicioDto>()
            val filtrados = todos.filter { it.equipo in equipos && it.nivel in niveles }
            Log.d("RutinasRepositorioImpl", "${filtrados.size} ejercicios disponibles con equipos=$equipos y niveles=$niveles.")
            Result.success(filtrados.map { it.asDominio() })
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al obtener ejercicios del catálogo: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guardarRutina(rutina: RutinaUsuario, dias: List<DiaRutina>): Result<Unit> {
        return try {
            supabase.from("user_routines").upsert(rutina.asDto())
            // Inserción individual de cada día para garantizar compatibilidad con el JSONB de ejercicios
            dias.forEach { dia ->
                supabase.from("routine_days").insert(dia.asDto())
            }
            Log.d("RutinasRepositorioImpl", "Rutina '${rutina.id}' con ${dias.size} días guardada correctamente.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al guardar la rutina: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun obtenerRutinaActiva(userId: String): Result<Pair<RutinaUsuario, List<DiaRutina>>?> {
        return try {
            val rutinaDtos = supabase
                .from("user_routines")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("activa", true)
                    }
                }
                .decodeList<RutinaUsuarioDto>()

            val rutinaDto = rutinaDtos.firstOrNull() ?: return Result.success(null)
            val rutina = rutinaDto.asDominio()

            val dias = supabase
                .from("routine_days")
                .select {
                    filter { eq("routine_id", rutina.id) }
                }
                .decodeList<DiaRutinaDto>()
                .sortedBy { it.diaNumero }
                .map { it.asDominio() }

            Log.d("RutinasRepositorioImpl", "Rutina activa cargada: ${dias.size} días.")
            Result.success(rutina to dias)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al obtener la rutina activa: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun desactivarRutinaActiva(userId: String): Result<Unit> {
        return try {
            supabase.from("user_routines").update({
                set("activa", false)
            }) {
                filter {
                    eq("user_id", userId)
                    eq("activa", true)
                }
            }
            Log.d("RutinasRepositorioImpl", "Rutinas anteriores del usuario $userId desactivadas.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al desactivar rutinas del usuario: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun obtenerTodasLasRutinas(userId: String): Result<List<RutinaUsuario>> {
        return try {
            val rutinas = supabase
                .from("user_routines")
                .select { filter { eq("user_id", userId) } }
                .decodeList<RutinaUsuarioDto>()
                .sortedByDescending { it.creadaEn }
                .map { it.asDominio() }
            Log.d("RutinasRepositorioImpl", "${rutinas.size} rutinas totales cargadas para el usuario $userId.")
            Result.success(rutinas)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al obtener todas las rutinas: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun obtenerDiasDeRutina(rutinaId: String): Result<List<DiaRutina>> {
        return try {
            val dias = supabase
                .from("routine_days")
                .select { filter { eq("routine_id", rutinaId) } }
                .decodeList<DiaRutinaDto>()
                .sortedBy { it.diaNumero }
                .map { it.asDominio() }
            Log.d("RutinasRepositorioImpl", "${dias.size} días cargados para la rutina $rutinaId.")
            Result.success(dias)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al obtener días de la rutina $rutinaId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun marcarRutinaActiva(rutinaId: String): Result<Unit> {
        return try {
            supabase.from("user_routines").update({
                set("activa", true)
            }) {
                filter { eq("id", rutinaId) }
            }
            Log.d("RutinasRepositorioImpl", "Rutina $rutinaId marcada como activa.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al marcar rutina activa $rutinaId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun eliminarRutina(rutinaId: String): Result<Unit> {
        return try {
            // Los routine_days se eliminan en cascada por la FK ON DELETE CASCADE
            supabase.from("user_routines").delete {
                filter { eq("id", rutinaId) }
            }
            Log.d("RutinasRepositorioImpl", "Rutina $rutinaId eliminada (días en cascada).")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RutinasRepositorioImpl", "Error al eliminar la rutina $rutinaId: ${e.message}")
            Result.failure(e)
        }
    }
}
