package com.example.zentra.data.repository

import android.util.Log
import com.example.zentra.data.remote.dto.MacrosDiariosDto
import com.example.zentra.data.remote.dto.asDto
import com.example.zentra.domain.model.MacrosDiarios
import com.example.zentra.domain.repository.IMacrosRepositorio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

/**
 * Implementación del repositorio de macros diarios sobre Supabase.
 * Opera sobre la tabla `daily_macros` filtrando por usuario y fecha.
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
}
