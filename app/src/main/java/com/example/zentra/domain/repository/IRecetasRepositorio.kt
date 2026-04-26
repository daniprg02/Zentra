package com.example.zentra.domain.repository

import com.example.zentra.domain.model.Receta
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de recetas.
 * Abstrae el acceso a los datos del gestor de recetas, tanto personales del usuario
 * como del catálogo predefinido de la base de datos.
 */
interface IRecetasRepositorio {

    /**
     * Flujo que emite una señal cada vez que se guarda o elimina una receta.
     * Permite a otros módulos (p. ej. Calculadora) reaccionar a los cambios sin acoplamiento directo.
     */
    val notificaciones: Flow<Unit>

    /**
     * Obtiene todas las recetas del usuario.
     * @param userId UUID del usuario autenticado.
     * @return [Result] con la lista de [Receta] o el error producido.
     */
    suspend fun obtenerRecetasDeUsuario(userId: String): Result<List<Receta>>

    /**
     * Guarda una nueva receta en la base de datos del usuario.
     * @param receta La receta a persistir.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun guardarReceta(receta: Receta): Result<Unit>

    /**
     * Elimina una receta de la base de datos.
     * @param recetaId UUID de la receta a eliminar.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun eliminarReceta(recetaId: String): Result<Unit>
}
