package com.example.zentra.data.repository

import android.util.Log
import com.example.zentra.data.remote.dto.PerfilDto
import com.example.zentra.data.remote.dto.asDto
import com.example.zentra.domain.model.Perfil
import com.example.zentra.domain.repository.IPerfilRepositorio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

/**
 * Implementación del repositorio de perfiles que utiliza Supabase como fuente de datos remota.
 * Realiza las operaciones CRUD sobre la tabla `profiles` de la base de datos.
 */
class PerfilRepositorioImpl @Inject constructor(
    private val supabase: SupabaseClient
) : IPerfilRepositorio {

    override suspend fun obtenerPerfil(userId: String): Result<Perfil> {
        return try {
            val dto = supabase
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<PerfilDto>()
            Result.success(dto.asDominio())
        } catch (e: Exception) {
            Log.e("PerfilRepositorioImpl", "Error al recuperar el perfil del usuario $userId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guardarPerfil(perfil: Perfil): Result<Unit> {
        return try {
            supabase
                .from("profiles")
                .upsert(perfil.asDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PerfilRepositorioImpl", "Error al guardar el perfil del usuario ${perfil.id}: ${e.message}")
            Result.failure(e)
        }
    }
}
