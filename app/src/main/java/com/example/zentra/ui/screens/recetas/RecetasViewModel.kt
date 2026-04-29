package com.example.zentra.ui.screens.recetas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.data.local.MonitorConectividad
import com.example.zentra.data.local.ZentraCacheManager
import com.example.zentra.domain.model.Receta
import com.example.zentra.domain.repository.IRecetasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del módulo de Gestor de Recetas.
 *
 * Responsabilidades:
 * - Cargar y filtrar la biblioteca de recetas del usuario desde Supabase.
 * - Manejar el pin (fijado) y la eliminación de recetas.
 * - Reaccionar a cambios externos (NuevaRecetaViewModel) a través del flujo de notificaciones
 *   del repositorio para mantener la lista siempre actualizada sin mostrar un spinner.
 */
@HiltViewModel
class RecetasViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val recetasRepositorio: IRecetasRepositorio,
    private val cacheManager: ZentraCacheManager,
    private val monitorConectividad: MonitorConectividad
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRecetas())
    val estado: StateFlow<EstadoRecetas> = _estado.asStateFlow()

    init {
        Log.d("RecetasViewModel", "ViewModel inicializado. Cargando biblioteca de recetas.")
        cargarRecetas()
        observarNotificaciones()
        observarConectividad()
    }

    /**
     * Carga todas las recetas del usuario desde Supabase.
     * Las ordena con las fijadas primero y, dentro de cada grupo, por fecha de creación descendente.
     */
    fun cargarRecetas() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = null)
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val recetas = recetasRepositorio.obtenerRecetasDeUsuario(userId).getOrThrow()
                val ordenadas = recetas.sortedWith(
                    compareByDescending<Receta> { it.fijada }.thenByDescending { it.creadaEn }
                )
                cacheManager.guardarRecetas(ordenadas)
                Log.d("RecetasViewModel", "${ordenadas.size} recetas cargadas para el usuario.")
                _estado.value = _estado.value.copy(cargando = false, recetas = ordenadas, sinConexion = false)
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al cargar las recetas: ${e.message}")

                // sinConexion solo es true si el dispositivo no tiene red real
                val sinRed = !monitorConectividad.hayConexion()
                val recetasCacheadas = cacheManager.cargarRecetas()
                if (recetasCacheadas.isNotEmpty()) {
                    Log.d("RecetasViewModel", "Usando ${recetasCacheadas.size} recetas cacheadas. Sin red: $sinRed")
                    _estado.value = _estado.value.copy(cargando = false, recetas = recetasCacheadas, sinConexion = sinRed)
                } else {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "No se pudo cargar la biblioteca. Comprueba tu conexión."
                    )
                }
            }
        }
    }

    /** Actualiza el texto de búsqueda. El filtrado se aplica en la capa de UI. */
    fun actualizarFiltro(query: String) {
        _estado.value = _estado.value.copy(filtro = query)
    }

    /**
     * Elimina una receta de Supabase y la borra de la lista local sin recargar toda la BD.
     * Si la operación falla, recarga la lista para restaurar el estado correcto.
     */
    fun eliminarReceta(receta: Receta) {
        viewModelScope.launch {
            try {
                recetasRepositorio.eliminarReceta(receta.id).getOrThrow()
                Log.d("RecetasViewModel", "Receta '${receta.titulo}' eliminada.")
                _estado.value = _estado.value.copy(
                    recetas = _estado.value.recetas.filter { it.id != receta.id }
                )
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar la receta '${receta.titulo}': ${e.message}")
                cargarRecetas()
            }
        }
    }

    /**
     * Alterna el estado de fijado de una receta.
     * Actualiza la lista local de forma optimista y revierte en caso de error.
     */
    fun toggleFijada(receta: Receta) {
        viewModelScope.launch {
            val actualizada = receta.copy(fijada = !receta.fijada)
            try {
                val listaTemporal = _estado.value.recetas.map { if (it.id == receta.id) actualizada else it }
                    .sortedWith(compareByDescending<Receta> { it.fijada }.thenByDescending { it.creadaEn })
                _estado.value = _estado.value.copy(recetas = listaTemporal)

                recetasRepositorio.guardarReceta(actualizada).getOrThrow()
                Log.d("RecetasViewModel", "Receta '${receta.titulo}' ${if (actualizada.fijada) "fijada" else "desfijada"}.")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al actualizar el pin de '${receta.titulo}': ${e.message}")
                cargarRecetas()
            }
        }
    }

    /**
     * Escucha el flujo de notificaciones del repositorio.
     * Cuando NuevaRecetaViewModel guarda una receta, este método recarga la lista
     * de forma silenciosa (sin spinner) para reflejar el cambio al volver a esta pantalla.
     */
    private fun observarNotificaciones() {
        viewModelScope.launch {
            recetasRepositorio.notificaciones.collect {
                Log.d("RecetasViewModel", "Notificación recibida. Recargando lista en silencio.")
                recargarSilencioso()
            }
        }
    }

    /** Recarga automáticamente cuando la conexión se restaura estando en modo offline. */
    private fun observarConectividad() {
        viewModelScope.launch {
            monitorConectividad.observarConectividad().collect { hayConexion ->
                if (hayConexion && _estado.value.sinConexion) {
                    Log.d("RecetasViewModel", "Conexión restaurada. Recargando recetas.")
                    cargarRecetas()
                }
            }
        }
    }

    private fun recargarSilencioso() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val recetas = recetasRepositorio.obtenerRecetasDeUsuario(userId)
                    .getOrDefault(_estado.value.recetas)
                val ordenadas = recetas.sortedWith(
                    compareByDescending<Receta> { it.fijada }.thenByDescending { it.creadaEn }
                )
                _estado.value = _estado.value.copy(recetas = ordenadas)
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error en recarga silenciosa: ${e.message}")
            }
        }
    }
}

/** Estado de la pantalla del Gestor de Recetas. */
data class EstadoRecetas(
    val cargando: Boolean = true,
    val error: String? = null,
    val recetas: List<Receta> = emptyList(),
    val filtro: String = "",
    val sinConexion: Boolean = false
)

/** Ingrediente con sus tres campos editables: cantidad, nombre y peso en gramos. */
data class IngredienteFormulario(
    val unidades: String = "",
    val alimento: String = "",
    val pesoG: String = ""
)

/** Estado del formulario de creación de una receta nueva. */
data class FormularioNuevaReceta(
    val titulo: String = "",
    val ingredientes: List<IngredienteFormulario> = listOf(IngredienteFormulario()),
    val proteinas: String = "",
    val carbos: String = "",
    val grasas: String = "",
    val guardando: Boolean = false,
    val error: String? = null
)

/** Referencia a un alimento ya usado en recetas anteriores, para sugerencias de autocompletado. */
data class AlimentoGuardado(
    val nombre: String,
    val unidades: String = "",
    val pesoG: String = ""
)
