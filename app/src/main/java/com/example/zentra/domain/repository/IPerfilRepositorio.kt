package com.example.zentra.domain.repository

import com.example.zentra.domain.model.Perfil

/**
 * Contrato del repositorio de perfiles de usuario.
 * Define las operaciones disponibles para la capa de dominio, sin acoplarse
 * a ningún detalle de implementación (Supabase, Room, etc.).
 */
interface IPerfilRepositorio {

    /**
     * Recupera el perfil de un usuario desde la fuente de datos.
     * @param userId UUID del usuario autenticado.
     * @return [Result] con el [Perfil] si la operación es exitosa, o el error en caso contrario.
     */
    suspend fun obtenerPerfil(userId: String): Result<Perfil>

    /**
     * Guarda o actualiza el perfil de un usuario en la fuente de datos.
     * @param perfil Objeto de dominio con los datos actualizados.
     * @return [Result] vacío si la operación es exitosa, o el error en caso contrario.
     */
    suspend fun guardarPerfil(perfil: Perfil): Result<Unit>
}
