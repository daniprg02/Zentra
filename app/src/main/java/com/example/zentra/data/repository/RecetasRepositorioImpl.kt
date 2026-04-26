package com.example.zentra.data.repository

import android.util.Log
import com.example.zentra.data.remote.dto.RecetaDto
import com.example.zentra.data.remote.dto.asDto
import com.example.zentra.domain.model.Receta
import com.example.zentra.domain.repository.IRecetasRepositorio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/**
 * Implementación del repositorio de recetas que utiliza Supabase como fuente de datos remota.
 * Opera sobre la tabla `recipes` filtrando siempre por el usuario autenticado.
 *
 * Al ser un @Singleton, el [MutableSharedFlow] actúa como bus de eventos compartido:
 * cualquier ViewModel que lo observe recibe la señal cuando se produce una mutación.
 */
class RecetasRepositorioImpl @Inject constructor(
    private val supabase: SupabaseClient
) : IRecetasRepositorio {

    private val _notificaciones = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    override val notificaciones: Flow<Unit> = _notificaciones

    override suspend fun obtenerRecetasDeUsuario(userId: String): Result<List<Receta>> {
        return try {
            val dtos = supabase
                .from("recipes")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RecetaDto>()
            Result.success(dtos.map { it.asDominio() })
        } catch (e: Exception) {
            Log.e("RecetasRepositorioImpl", "Error al obtener las recetas del usuario $userId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guardarReceta(receta: Receta): Result<Unit> {
        return try {
            supabase
                .from("recipes")
                .upsert(receta.asDto())
            _notificaciones.tryEmit(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RecetasRepositorioImpl", "Error al guardar la receta '${receta.titulo}': ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun eliminarReceta(recetaId: String): Result<Unit> {
        return try {
            supabase
                .from("recipes")
                .delete {
                    filter { eq("id", recetaId) }
                }
            _notificaciones.tryEmit(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RecetasRepositorioImpl", "Error al eliminar la receta $recetaId: ${e.message}")
            Result.failure(e)
        }
    }
}
