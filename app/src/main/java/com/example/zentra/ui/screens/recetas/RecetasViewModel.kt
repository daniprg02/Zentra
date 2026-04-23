package com.example.zentra.ui.screens.recetas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.model.Receta
import com.example.zentra.domain.repository.IRecetasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel del módulo de Gestor de Recetas.
 *
 * Responsabilidades:
 * - Cargar y filtrar la biblioteca de recetas del usuario desde Supabase.
 * - Gestionar el formulario de creación manual de recetas.
 * - Manejar el pin (fijado) y la eliminación de recetas.
 * - Calcular las kcal automáticamente a partir de los macros introducidos.
 */
@HiltViewModel
class RecetasViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val recetasRepositorio: IRecetasRepositorio
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRecetas())
    val estado: StateFlow<EstadoRecetas> = _estado.asStateFlow()

    init {
        Log.d("RecetasViewModel", "ViewModel inicializado. Cargando biblioteca de recetas.")
        cargarRecetas()
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
                Log.d("RecetasViewModel", "${ordenadas.size} recetas cargadas para el usuario.")
                _estado.value = _estado.value.copy(cargando = false, recetas = ordenadas)
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al cargar las recetas: ${e.message}")
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "No se pudo cargar la biblioteca. Comprueba tu conexión."
                )
            }
        }
    }

    /** Actualiza el texto de búsqueda. El filtrado se aplica en la capa de UI. */
    fun actualizarFiltro(query: String) {
        _estado.value = _estado.value.copy(filtro = query)
    }

    /** Abre el formulario de creación con todos los campos limpios. */
    fun mostrarFormulario() {
        _estado.value = _estado.value.copy(
            mostrandoFormulario = true,
            formulario = FormularioNuevaReceta()
        )
    }

    /** Cierra el formulario descartando cualquier dato sin guardar. */
    fun ocultarFormulario() {
        _estado.value = _estado.value.copy(mostrandoFormulario = false)
    }

    // Actualizadores individuales de los campos del formulario
    fun actualizarTitulo(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(titulo = valor, error = null))
    }

    fun actualizarIngredientes(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = valor))
    }

    fun actualizarProteinas(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(proteinas = valor, error = null))
    }

    fun actualizarCarbos(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(carbos = valor, error = null))
    }

    fun actualizarGrasas(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(grasas = valor, error = null))
    }

    /**
     * Valida el formulario, calcula las kcal a partir de los macros y persiste la receta en Supabase.
     * Las kcal no se introducen manualmente: se calculan siempre como P×4 + C×4 + G×9.
     */
    fun guardarNuevaReceta() {
        viewModelScope.launch {
            val form = _estado.value.formulario

            if (form.titulo.isBlank()) {
                _estado.value = _estado.value.copy(formulario = form.copy(error = "El título no puede estar vacío."))
                return@launch
            }

            val proteinas = form.proteinas.toFloatOrNull()
            val carbos = form.carbos.toFloatOrNull()
            val grasas = form.grasas.toFloatOrNull()

            if (proteinas == null || carbos == null || grasas == null) {
                _estado.value = _estado.value.copy(formulario = form.copy(error = "Introduce valores numéricos válidos en los macros."))
                return@launch
            }
            if (proteinas < 0f || carbos < 0f || grasas < 0f) {
                _estado.value = _estado.value.copy(formulario = form.copy(error = "Los macros no pueden ser negativos."))
                return@launch
            }

            _estado.value = _estado.value.copy(formulario = form.copy(guardando = true, error = null))

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val kcalCalculadas = ((proteinas * 4f) + (carbos * 4f) + (grasas * 9f)).toInt()

                val nuevaReceta = Receta(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    titulo = form.titulo.trim(),
                    kcalTotales = kcalCalculadas,
                    proteinasG = proteinas,
                    carbosG = carbos,
                    grasasG = grasas,
                    ingredientes = form.ingredientes.trim(),
                    fijada = false,
                    creadaEn = null
                )

                recetasRepositorio.guardarReceta(nuevaReceta).getOrThrow()
                Log.d("RecetasViewModel", "Receta '${nuevaReceta.titulo}' (${nuevaReceta.kcalTotales} kcal) guardada correctamente.")

                ocultarFormulario()
                cargarRecetas()
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al guardar la receta: ${e.message}")
                _estado.value = _estado.value.copy(
                    formulario = _estado.value.formulario.copy(guardando = false, error = "No se pudo guardar. Inténtalo de nuevo.")
                )
            }
        }
    }

    /**
     * Elimina una receta de Supabase y la borra de la lista local sin recargar toda la BD.
     * @param receta La receta a eliminar.
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
                // Recargamos para restaurar el estado correcto si el swipe quedó en estado inconsistente
                cargarRecetas()
            }
        }
    }

    /**
     * Alterna el estado de fijado de una receta. Las recetas fijadas aparecen en el carrusel superior.
     * Actualiza la lista local inmediatamente para que la UI responda sin esperar la red.
     * @param receta La receta cuyo pin se quiere alternar.
     */
    fun toggleFijada(receta: Receta) {
        viewModelScope.launch {
            val actualizada = receta.copy(fijada = !receta.fijada)
            try {
                // Actualización optimista: reflejamos el cambio en la UI antes de confirmar con Supabase
                val listaTemporal = _estado.value.recetas.map { if (it.id == receta.id) actualizada else it }
                    .sortedWith(compareByDescending<Receta> { it.fijada }.thenByDescending { it.creadaEn })
                _estado.value = _estado.value.copy(recetas = listaTemporal)

                recetasRepositorio.guardarReceta(actualizada).getOrThrow()
                Log.d("RecetasViewModel", "Receta '${receta.titulo}' ${if (actualizada.fijada) "fijada" else "desfijada"}.")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al actualizar el pin de '${receta.titulo}': ${e.message}")
                // Revertimos el cambio optimista si Supabase falla
                cargarRecetas()
            }
        }
    }
}

/**
 * Estado completo de la pantalla del Gestor de Recetas.
 */
data class EstadoRecetas(
    val cargando: Boolean = true,
    val error: String? = null,
    val recetas: List<Receta> = emptyList(),
    val filtro: String = "",
    val mostrandoFormulario: Boolean = false,
    val formulario: FormularioNuevaReceta = FormularioNuevaReceta()
)

/**
 * Estado del formulario de creación manual de recetas.
 * Todos los campos numéricos se almacenan como String para permitir entrada parcial sin errores de parseo.
 */
data class FormularioNuevaReceta(
    val titulo: String = "",
    val ingredientes: String = "",
    val proteinas: String = "",
    val carbos: String = "",
    val grasas: String = "",
    val guardando: Boolean = false,
    val error: String? = null
)
