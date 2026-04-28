package com.example.zentra.data.repository

import android.util.Log
import com.example.zentra.data.remote.dto.IngestaDiariaDto
import com.example.zentra.data.remote.dto.MacrosDiariosDto
import com.example.zentra.data.remote.dto.asDto
import com.example.zentra.domain.model.IngestaSlot
import com.example.zentra.domain.model.MacrosDiarios
import com.example.zentra.domain.repository.IMacrosRepositorio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import javax.inject.Inject

/**
 * Implementación del repositorio de macros diarios sobre Supabase.
 * Opera sobre `daily_macros` para los totales del día y sobre `daily_intakes`
 * para las ingestas detalladas por slot, filtrando siempre por usuario y fecha.
 */
class MacrosRepositorioImpl @Inject constructor(
    private val supabase: SupabaseClient
) : IMacrosRepositorio {

    override suspend fun obtenerMacrosDelDia(userId: String, fecha: String): Result<MacrosDiarios> {
        return try {
            val dto = supabase
                .from("daily_macros")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("fecha", fecha)
                    }
                }
                .decodeSingle<MacrosDiariosDto>()
            Result.success(dto.asDominio())
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al obtener los macros del día $fecha para el usuario $userId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guardarMacrosDelDia(macrosDiarios: MacrosDiarios): Result<Unit> {
        return try {
            supabase
                .from("daily_macros")
                .upsert(macrosDiarios.asDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al guardar los macros del día ${macrosDiarios.fecha}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun obtenerIngestasDelDia(userId: String, fecha: String): Result<Map<String, List<IngestaSlot>>> {
        return try {
            val dtos = supabase
                .from("daily_intakes")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("fecha", fecha)
                    }
                }
                .decodeList<IngestaDiariaDto>()
            val mapa = dtos
                .groupBy { it.slot }
                .mapValues { (_, lista) -> lista.sortedBy { it.orden }.map { it.asDominio() } }
            Log.d("MacrosRepositorioImpl", "${dtos.size} ingestas cargadas para $fecha.")
            Result.success(mapa)
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al obtener las ingestas del día $fecha: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guardarIngesta(
        userId: String,
        fecha: String,
        slotNombre: String,
        slot: IngestaSlot
    ): Result<IngestaSlot> {
        return try {
            val id = if (slot.dbId.isBlank()) UUID.randomUUID().toString() else slot.dbId
            val dto = slot.copy(dbId = id).asDto(userId, fecha, slotNombre)
            supabase.from("daily_intakes").upsert(dto)
            Log.d("MacrosRepositorioImpl", "Ingesta '${slot.recetaTitulo}' guardada con id=$id.")
            Result.success(slot.copy(dbId = id))
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al guardar la ingesta '${slot.recetaTitulo}': ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun eliminarIngesta(dbId: String): Result<Unit> {
        return try {
            supabase
                .from("daily_intakes")
                .delete {
                    filter { eq("id", dbId) }
                }
            Log.d("MacrosRepositorioImpl", "Ingesta $dbId eliminada.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al eliminar la ingesta $dbId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun eliminarIngestasDelDia(userId: String, fecha: String): Result<Unit> {
        return try {
            supabase
                .from("daily_intakes")
                .delete {
                    filter {
                        eq("user_id", userId)
                        eq("fecha", fecha)
                    }
                }
            Log.d("MacrosRepositorioImpl", "Todas las ingestas del día $fecha eliminadas.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MacrosRepositorioImpl", "Error al eliminar las ingestas del día $fecha: ${e.message}")
            Result.failure(e)
        }
    }
}
