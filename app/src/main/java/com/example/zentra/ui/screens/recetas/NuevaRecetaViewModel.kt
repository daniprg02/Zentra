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
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel de la pantalla de creación de recetas.
 *
 * Responsabilidades:
 * - Gestionar el formulario de nueva receta.
 * - Cargar las recetas guardadas del usuario para el selector de macros automático.
 * - Acumular los macros al seleccionar/deseleccionar recetas del historial.
 * - Persistir la receta en Supabase y señalizar la navegación con [EstadoNuevaReceta.guardado].
 */
@HiltViewModel
class NuevaRecetaViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val recetasRepositorio: IRecetasRepositorio
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoNuevaReceta())
    val estado: StateFlow<EstadoNuevaReceta> = _estado.asStateFlow()

    init {
        Log.d("NuevaRecetaViewModel", "ViewModel inicializado. Cargando datos iniciales.")
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val recetas = recetasRepositorio.obtenerRecetasDeUsuario(userId)
                    .getOrDefault(emptyList())

                // Extraer nombres de ingredientes de recetas anteriores para el autocompletado
                val alimentos = recetas
                    .flatMap { r ->
                        r.ingredientes.split("\n", ",").mapNotNull { linea ->
                            val raw = linea.trim()
                            if (raw.length < 2) null
                            else {
                                val pesoG = if (raw.contains("("))
                                    raw.substringAfter("(").substringBefore(")").removeSuffix("g").trim()
                                else ""
                                val sinPeso = if (raw.contains("(")) raw.substringBefore("(").trim() else raw
                                val unidades = sinPeso.takeWhile { it.isDigit() || it == '/' || it == '.' }.trim()
                                val nombre = sinPeso.removePrefix(unidades).trim()
                                if (nombre.length < 2) null
                                else AlimentoGuardado(nombre = nombre, unidades = unidades, pesoG = pesoG)
                            }
                        }
                    }
                    .distinctBy { it.nombre }
                    .sortedBy { it.nombre }

                val recetasOrdenadas = recetas.sortedByDescending { it.creadaEn }

                _estado.value = _estado.value.copy(
                    alimentosGuardados = alimentos,
                    recetasDisponibles = recetasOrdenadas
                )
                Log.d("NuevaRecetaViewModel", "${recetasOrdenadas.size} recetas y ${alimentos.size} alimentos cargados.")
            } catch (e: Exception) {
                Log.e("NuevaRecetaViewModel", "Error al cargar datos iniciales: ${e.message}")
            }
        }
    }

    // ─── Selector de recetas con acumulación de macros ────────────────────────

    /**
     * Alterna la selección de una receta del historial.
     * Al seleccionar, suma sus macros al formulario. Al deseleccionar, los resta.
     * Los campos de macro se actualizan en tiempo real para mostrar el total acumulado.
     */
    fun toggleRecetaSeleccionada(receta: Receta) {
        val estado = _estado.value
        val form = estado.formulario
        val yaSeleccionada = receta.id in estado.recetasSeleccionadas

        val pActual = form.proteinas.toFloatOrNull() ?: 0f
        val cActual = form.carbos.toFloatOrNull() ?: 0f
        val gActual = form.grasas.toFloatOrNull() ?: 0f

        val nuevasP: Float
        val nuevoC: Float
        val nuevaG: Float

        if (yaSeleccionada) {
            nuevasP = (pActual - receta.proteinasG).coerceAtLeast(0f)
            nuevoC = (cActual - receta.carbosG).coerceAtLeast(0f)
            nuevaG = (gActual - receta.grasasG).coerceAtLeast(0f)
        } else {
            nuevasP = pActual + receta.proteinasG
            nuevoC = cActual + receta.carbosG
            nuevaG = gActual + receta.grasasG
        }

        // Locale.ROOT garantiza punto decimal en cualquier idioma del dispositivo (evita "30,")
        val fmt: (Float) -> String = { v ->
            if (v == 0f) "" else String.format(Locale.ROOT, "%.1f", v).trimEnd('0').trimEnd('.')
        }

        _estado.value = estado.copy(
            formulario = form.copy(
                proteinas = fmt(nuevasP),
                carbos = fmt(nuevoC),
                grasas = fmt(nuevaG),
                error = null
            ),
            recetasSeleccionadas = if (yaSeleccionada) {
                estado.recetasSeleccionadas - receta.id
            } else {
                estado.recetasSeleccionadas + receta.id
            }
        )
        Log.d(
            "NuevaRecetaViewModel",
            "Receta '${receta.titulo}' ${if (yaSeleccionada) "deseleccionada" else "seleccionada"}. " +
            "Macros: P=${fmt(nuevasP)}g C=${fmt(nuevoC)}g G=${fmt(nuevaG)}g"
        )
    }

    // ─── Actualizadores de campos del formulario ──────────────────────────────

    fun actualizarTitulo(valor: String) {
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(titulo = valor, error = null))
    }

    fun agregarIngrediente() {
        val form = _estado.value.formulario
        if (form.ingredientes.size >= 10) return
        _estado.value = _estado.value.copy(formulario = form.copy(ingredientes = form.ingredientes + IngredienteFormulario()))
    }

    fun eliminarIngrediente(idx: Int) {
        val lista = _estado.value.formulario.ingredientes.toMutableList()
        if (lista.size > 1) lista.removeAt(idx)
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = lista))
    }

    fun actualizarUnidades(idx: Int, valor: String) {
        val lista = _estado.value.formulario.ingredientes.toMutableList()
        lista[idx] = lista[idx].copy(unidades = valor)
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = lista))
    }

    fun actualizarAlimento(idx: Int, valor: String) {
        val lista = _estado.value.formulario.ingredientes.toMutableList()
        lista[idx] = lista[idx].copy(alimento = valor)
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = lista))
    }

    fun actualizarPesoG(idx: Int, valor: String) {
        val lista = _estado.value.formulario.ingredientes.toMutableList()
        lista[idx] = lista[idx].copy(pesoG = valor)
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = lista))
    }

    fun usarAlimentoGuardado(alimento: AlimentoGuardado) {
        val lista = _estado.value.formulario.ingredientes.toMutableList()
        val idx = lista.indexOfFirst { it.alimento.isBlank() }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(
                alimento = alimento.nombre,
                unidades = alimento.unidades.ifEmpty { lista[idx].unidades },
                pesoG = alimento.pesoG.ifEmpty { lista[idx].pesoG }
            )
        } else {
            lista.add(IngredienteFormulario(alimento = alimento.nombre, unidades = alimento.unidades, pesoG = alimento.pesoG))
        }
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(ingredientes = lista))
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
     * Valida el formulario, calcula las kcal (P×4 + C×4 + G×9) y persiste la receta en Supabase.
     * Emite [EstadoNuevaReceta.guardado] = true para que la UI navegue de vuelta automáticamente.
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

                val ingredientesTexto = form.ingredientes
                    .filter { it.alimento.isNotBlank() }
                    .joinToString("\n") { ing ->
                        buildString {
                            if (ing.unidades.isNotBlank()) append("${ing.unidades} ")
                            append(ing.alimento.trim())
                            if (ing.pesoG.isNotBlank()) append(" (${ing.pesoG}g)")
                        }
                    }

                val nuevaReceta = Receta(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    titulo = form.titulo.trim(),
                    kcalTotales = kcalCalculadas,
                    proteinasG = proteinas,
                    carbosG = carbos,
                    grasasG = grasas,
                    ingredientes = ingredientesTexto,
                    fijada = false,
                    creadaEn = null
                )

                recetasRepositorio.guardarReceta(nuevaReceta).getOrThrow()
                Log.d("NuevaRecetaViewModel", "Receta '${nuevaReceta.titulo}' (${nuevaReceta.kcalTotales} kcal) guardada correctamente.")
                _estado.value = _estado.value.copy(guardado = true)
            } catch (e: Exception) {
                Log.e("NuevaRecetaViewModel", "Error al guardar la receta: ${e.message}")
                _estado.value = _estado.value.copy(
                    formulario = _estado.value.formulario.copy(guardando = false, error = "No se pudo guardar. Inténtalo de nuevo.")
                )
            }
        }
    }
}

/** Estado completo de la pantalla de creación de recetas. */
data class EstadoNuevaReceta(
    val formulario: FormularioNuevaReceta = FormularioNuevaReceta(),
    val alimentosGuardados: List<AlimentoGuardado> = emptyList(),
    val recetasDisponibles: List<Receta> = emptyList(),
    val recetasSeleccionadas: Set<String> = emptySet(),
    val guardado: Boolean = false
)
