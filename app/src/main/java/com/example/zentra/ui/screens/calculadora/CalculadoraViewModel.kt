package com.example.zentra.ui.screens.calculadora

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.data.local.buscarEnMasterlist
import com.example.zentra.data.remote.api.OpenFoodFactsService
import com.example.zentra.domain.model.MacrosDiarios
import com.example.zentra.domain.model.NivelActividad
import com.example.zentra.domain.model.ObjetivoFisico
import com.example.zentra.domain.model.Perfil
import com.example.zentra.domain.model.Receta
import com.example.zentra.domain.repository.IMacrosRepositorio
import com.example.zentra.domain.repository.IPerfilRepositorio
import com.example.zentra.domain.repository.IRecetasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel del módulo de Calculadora Dietética.
 *
 * Responsabilidades:
 * - Cargar el perfil físico del usuario y calcular el objetivo calórico diario.
 * - Leer y crear el registro `daily_macros` de la fecha visualizada.
 * - Gestionar los slots de ingesta: añadir y eliminar recetas con actualización optimista.
 * - Navegar entre fechas y bloquear la edición al consultar días anteriores (modo historial).
 * - Buscar alimentos en la API pública de OpenFoodFacts con debounce de 700 ms.
 */
@HiltViewModel
class CalculadoraViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio,
    private val macrosRepositorio: IMacrosRepositorio,
    private val recetasRepositorio: IRecetasRepositorio,
    private val foodService: OpenFoodFactsService
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoCalculadora())
    val estado: StateFlow<EstadoCalculadora> = _estado.asStateFlow()

    private var busquedaJob: Job? = null

    init {
        Log.d("CalculadoraViewModel", "ViewModel inicializado. Cargando datos nutricionales del día.")
        cargarDatosDelDia()
    }

    /**
     * Carga o recalcula todos los datos de la pantalla para la fecha actualmente seleccionada.
     * En modo historial no crea un registro nuevo si el día no tiene datos registrados.
     */
    fun cargarDatosDelDia() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = null)
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa. Reinicia la aplicación.")

                val perfil = perfilRepositorio.obtenerPerfil(userId).getOrThrow()
                Log.d("CalculadoraViewModel", "Perfil: ${perfil.apodo} | ${perfil.pesoKg}kg, ${perfil.alturaCm}cm, ${perfil.edad}a")

                val objetivos = calcularObjetivosNutricionales(
                    perfil = perfil,
                    nivel = _estado.value.nivelActividad,
                    objetivo = _estado.value.objetivo
                )

                val fecha = _estado.value.fechaVisualizando
                val esModoHistorial = _estado.value.esModoHistorial
                val macrosExistentes = macrosRepositorio.obtenerMacrosDelDia(userId, fecha).getOrNull()

                val macrosHoy = when {
                    macrosExistentes != null -> macrosExistentes
                    !esModoHistorial -> {
                        Log.d("CalculadoraViewModel", "Sin registro para $fecha. Creando fila inicial en daily_macros.")
                        val nuevo = MacrosDiarios(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            fecha = fecha,
                            objetivoKcal = objetivos.objetivoKcal,
                            consumidoKcal = 0,
                            consumidoProteinasG = 0f,
                            consumidoCarbosG = 0f,
                            consumidoGrasasG = 0f
                        )
                        macrosRepositorio.guardarMacrosDelDia(nuevo).onFailure { e ->
                            Log.e("CalculadoraViewModel", "Error al crear registro inicial: ${e.message}")
                        }
                        nuevo
                    }
                    else -> {
                        // Día pasado sin datos: mostramos ceros sin crear fila en BD
                        Log.d("CalculadoraViewModel", "Modo historial: sin registro para $fecha. Mostrando ceros.")
                        MacrosDiarios(
                            id = "",
                            userId = userId,
                            fecha = fecha,
                            objetivoKcal = objetivos.objetivoKcal,
                            consumidoKcal = 0,
                            consumidoProteinasG = 0f,
                            consumidoCarbosG = 0f,
                            consumidoGrasasG = 0f
                        )
                    }
                }

                _estado.value = _estado.value.copy(
                    cargando = false,
                    apodo = perfil.apodo,
                    macrosDiariosId = macrosHoy.id,
                    objetivoKcal = objetivos.objetivoKcal,
                    objetivoProteinasG = objetivos.proteinasG,
                    objetivoCarbosG = objetivos.carbosG,
                    objetivoGrasasG = objetivos.grasasG,
                    consumidoKcal = macrosHoy.consumidoKcal,
                    consumidoProteinasG = macrosHoy.consumidoProteinasG,
                    consumidoCarbosG = macrosHoy.consumidoCarbosG,
                    consumidoGrasasG = macrosHoy.consumidoGrasasG
                )
            } catch (e: Exception) {
                Log.e("CalculadoraViewModel", "Error al cargar los datos del día: ${e.message}")
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "No se pudieron cargar los datos. Comprueba tu conexión."
                )
            }
        }
    }

    // ─── Navegación de fechas ──────────────────────────────────────────────────

    /**
     * Cambia la fecha visualizada y recarga los datos.
     * Si la fecha es anterior a hoy activa el modo historial (solo lectura).
     */
    fun cambiarFecha(fecha: LocalDate) {
        val esHistorial = fecha.isBefore(LocalDate.now())
        _estado.value = _estado.value.copy(
            fechaVisualizando = fecha.toString(),
            esModoHistorial = esHistorial,
            ingestasDelDia = emptyMap(),
            busquedaTexto = "",
            resultadosBusqueda = emptyList(),
            buscandoAlimento = false
        )
        cargarDatosDelDia()
        Log.d("CalculadoraViewModel", "Fecha cambiada a $fecha. Historial=$esHistorial")
    }

    /** Vuelve a mostrar los datos del día de hoy saliendo del modo historial. */
    fun volverAHoy() {
        cambiarFecha(LocalDate.now())
    }

    // ─── Gestión de slots ─────────────────────────────────────────────────────

    /** Abre el picker de recetas para el slot indicado. El modo historial bloquea esta acción. */
    fun abrirSlot(nombreSlot: String) {
        if (_estado.value.esModoHistorial) return
        _estado.value = _estado.value.copy(
            slotActivo = nombreSlot,
            busquedaTexto = "",
            resultadosBusqueda = emptyList(),
            buscandoAlimento = false
        )
        if (_estado.value.recetasDisponibles.isEmpty()) {
            cargarRecetasParaPicker()
        }
    }

    /** Cierra el picker de recetas y limpia el estado de búsqueda. */
    fun cerrarSlot() {
        busquedaJob?.cancel()
        _estado.value = _estado.value.copy(
            slotActivo = null,
            busquedaTexto = "",
            resultadosLocales = emptyList(),
            resultadosBusqueda = emptyList(),
            buscandoAlimento = false
        )
    }

    /**
     * Añade una receta (propia o resultado de búsqueda) al slot activo,
     * suma sus macros a los totales del día y persiste en Supabase.
     */
    fun agregarRecetaASlot(receta: Receta) {
        viewModelScope.launch {
            val estadoActual = _estado.value
            val slotNombre = estadoActual.slotActivo ?: return@launch
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

            val nuevoKcal = estadoActual.consumidoKcal + receta.kcalTotales
            val nuevasProteinas = estadoActual.consumidoProteinasG + receta.proteinasG
            val nuevosCarbos = estadoActual.consumidoCarbosG + receta.carbosG
            val nuevasGrasas = estadoActual.consumidoGrasasG + receta.grasasG

            val mapaActualizado = estadoActual.ingestasDelDia.toMutableMap()
            mapaActualizado[slotNombre] = (mapaActualizado[slotNombre] ?: emptyList()) + receta

            _estado.value = estadoActual.copy(
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas,
                ingestasDelDia = mapaActualizado,
                slotActivo = null,
                busquedaTexto = "",
                resultadosBusqueda = emptyList()
            )

            Log.d("CalculadoraViewModel", "Receta '${receta.titulo}' añadida a '$slotNombre'. Total: $nuevoKcal kcal.")

            val macrosActualizados = MacrosDiarios(
                id = estadoActual.macrosDiariosId,
                userId = userId,
                fecha = estadoActual.fechaVisualizando,
                objetivoKcal = estadoActual.objetivoKcal,
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas
            )
            macrosRepositorio.guardarMacrosDelDia(macrosActualizados).onFailure { e ->
                Log.e("CalculadoraViewModel", "Error al persistir macros tras añadir receta: ${e.message}")
            }
        }
    }

    /**
     * Elimina una receta de un slot, resta sus macros de los totales del día
     * y actualiza el registro en Supabase.
     */
    fun eliminarRecetaDeSlot(nombreSlot: String, receta: Receta) {
        viewModelScope.launch {
            val estadoActual = _estado.value
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

            val mapaActualizado = estadoActual.ingestasDelDia.toMutableMap()
            val listaActualizada = (mapaActualizado[nombreSlot] ?: emptyList())
                .toMutableList().also { it.remove(receta) }
            if (listaActualizada.isEmpty()) mapaActualizado.remove(nombreSlot)
            else mapaActualizado[nombreSlot] = listaActualizada

            val nuevoKcal = (estadoActual.consumidoKcal - receta.kcalTotales).coerceAtLeast(0)
            val nuevasProteinas = (estadoActual.consumidoProteinasG - receta.proteinasG).coerceAtLeast(0f)
            val nuevosCarbos = (estadoActual.consumidoCarbosG - receta.carbosG).coerceAtLeast(0f)
            val nuevasGrasas = (estadoActual.consumidoGrasasG - receta.grasasG).coerceAtLeast(0f)

            _estado.value = estadoActual.copy(
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas,
                ingestasDelDia = mapaActualizado
            )

            Log.d("CalculadoraViewModel", "Receta '${receta.titulo}' eliminada de '$nombreSlot'. Total: $nuevoKcal kcal.")

            val macrosActualizados = MacrosDiarios(
                id = estadoActual.macrosDiariosId,
                userId = userId,
                fecha = estadoActual.fechaVisualizando,
                objetivoKcal = estadoActual.objetivoKcal,
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas
            )
            macrosRepositorio.guardarMacrosDelDia(macrosActualizados).onFailure { e ->
                Log.e("CalculadoraViewModel", "Error al persistir macros tras eliminar receta: ${e.message}")
            }
        }
    }

    // ─── Búsqueda OpenFoodFacts ────────────────────────────────────────────────

    /**
     * Actualiza el texto de búsqueda.
     * - Los resultados locales (masterlist) se calculan de forma inmediata.
     * - Los resultados de la API de OpenFoodFacts se lanzan con debounce de 700 ms.
     */
    fun actualizarBusqueda(query: String) {
        _estado.value = _estado.value.copy(
            busquedaTexto = query,
            resultadosBusqueda = emptyList(),
            resultadosLocales = buscarEnMasterlist(query)
        )
        busquedaJob?.cancel()
        if (query.isBlank()) {
            _estado.value = _estado.value.copy(buscandoAlimento = false, resultadosLocales = emptyList())
            return
        }
        busquedaJob = viewModelScope.launch {
            delay(700L)
            _estado.value = _estado.value.copy(buscandoAlimento = true)
            try {
                val respuesta = foodService.buscarProductos(query)
                val recetas = respuesta.products
                    ?.mapNotNull { it.aReceta() }
                    ?.distinctBy { it.titulo.lowercase() }
                    ?.take(10)
                    ?: emptyList()
                Log.d("CalculadoraViewModel", "${recetas.size} alimentos de API + ${_estado.value.resultadosLocales.size} locales para '$query'.")
                _estado.value = _estado.value.copy(resultadosBusqueda = recetas, buscandoAlimento = false)
            } catch (e: Exception) {
                Log.e("CalculadoraViewModel", "Error buscando '$query' en OpenFoodFacts: ${e.message}")
                _estado.value = _estado.value.copy(buscandoAlimento = false)
            }
        }
    }

    // ─── Configuración nutricional ────────────────────────────────────────────

    /** Cambia el nivel de actividad y recalcula los objetivos del día. */
    fun cambiarNivelActividad(nivel: NivelActividad) {
        Log.d("CalculadoraViewModel", "Nivel de actividad: ${nivel.etiqueta}")
        _estado.value = _estado.value.copy(nivelActividad = nivel)
        cargarDatosDelDia()
    }

    /** Cambia el objetivo físico y recalcula los objetivos del día. */
    fun cambiarObjetivo(objetivo: ObjetivoFisico) {
        Log.d("CalculadoraViewModel", "Objetivo físico: ${objetivo.etiqueta}")
        _estado.value = _estado.value.copy(objetivo = objetivo)
        cargarDatosDelDia()
    }

    // ─── Edición de cantidad de ingesta ──────────────────────────────────────────

    /** Abre el diálogo de edición de gramos para una ingesta concreta del slot. */
    fun abrirEdicionIngesta(slotNombre: String, receta: Receta) {
        _estado.value = _estado.value.copy(
            ingestaEditando = IngestaEditando(slotNombre = slotNombre, recetaOriginal = receta)
        )
    }

    /** Actualiza el texto del campo de gramos mientras el usuario escribe. */
    fun actualizarCantidadEdicion(texto: String) {
        val edicion = _estado.value.ingestaEditando ?: return
        _estado.value = _estado.value.copy(
            ingestaEditando = edicion.copy(cantidadTexto = texto.filter { it.isDigit() || it == '.' })
        )
    }

    /**
     * Aplica la cantidad editada escalando los macros con regla de tres (base = 100 g).
     * Reemplaza la receta original en el slot y persiste los nuevos totales en Supabase.
     */
    fun confirmarEdicionIngesta() {
        val edicion = _estado.value.ingestaEditando ?: return
        val cantidad = edicion.cantidadTexto.toFloatOrNull() ?: return
        if (cantidad <= 0f) return

        val factor = cantidad / 100f
        val recetaEscalada = edicion.recetaOriginal.copy(
            kcalTotales = (edicion.recetaOriginal.kcalTotales * factor).toInt(),
            proteinasG = edicion.recetaOriginal.proteinasG * factor,
            carbosG = edicion.recetaOriginal.carbosG * factor,
            grasasG = edicion.recetaOriginal.grasasG * factor
        )

        val estadoActual = _estado.value
        val mapaActualizado = estadoActual.ingestasDelDia.toMutableMap()
        val lista = (mapaActualizado[edicion.slotNombre] ?: emptyList()).toMutableList()
        val idx = lista.indexOf(edicion.recetaOriginal)
        if (idx >= 0) lista[idx] = recetaEscalada
        mapaActualizado[edicion.slotNombre] = lista

        val nuevoKcal = (estadoActual.consumidoKcal - edicion.recetaOriginal.kcalTotales + recetaEscalada.kcalTotales).coerceAtLeast(0)
        val nuevasProteinas = (estadoActual.consumidoProteinasG - edicion.recetaOriginal.proteinasG + recetaEscalada.proteinasG).coerceAtLeast(0f)
        val nuevosCarbos = (estadoActual.consumidoCarbosG - edicion.recetaOriginal.carbosG + recetaEscalada.carbosG).coerceAtLeast(0f)
        val nuevasGrasas = (estadoActual.consumidoGrasasG - edicion.recetaOriginal.grasasG + recetaEscalada.grasasG).coerceAtLeast(0f)

        _estado.value = estadoActual.copy(
            consumidoKcal = nuevoKcal,
            consumidoProteinasG = nuevasProteinas,
            consumidoCarbosG = nuevosCarbos,
            consumidoGrasasG = nuevasGrasas,
            ingestasDelDia = mapaActualizado,
            ingestaEditando = null
        )
        Log.d("CalculadoraViewModel", "Ingesta '${edicion.recetaOriginal.titulo}' ajustada a ${cantidad}g → ${recetaEscalada.kcalTotales} kcal.")

        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            macrosRepositorio.guardarMacrosDelDia(
                MacrosDiarios(
                    id = estadoActual.macrosDiariosId,
                    userId = userId,
                    fecha = estadoActual.fechaVisualizando,
                    objetivoKcal = estadoActual.objetivoKcal,
                    consumidoKcal = nuevoKcal,
                    consumidoProteinasG = nuevasProteinas,
                    consumidoCarbosG = nuevosCarbos,
                    consumidoGrasasG = nuevasGrasas
                )
            ).onFailure { e ->
                Log.e("CalculadoraViewModel", "Error al persistir macros tras editar ingesta: ${e.message}")
            }
        }
    }

    /** Cancela la edición de ingesta sin aplicar cambios. */
    fun cerrarEdicionIngesta() {
        _estado.value = _estado.value.copy(ingestaEditando = null)
    }

    /** Cierra la sesión del usuario en Supabase y señaliza la navegación de vuelta al login. */
    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                Log.d("CalculadoraViewModel", "Sesión cerrada correctamente.")
            } catch (e: Exception) {
                Log.e("CalculadoraViewModel", "Error al cerrar sesión: ${e.message}")
            } finally {
                _estado.value = _estado.value.copy(sesionCerrada = true)
            }
        }
    }

    /** Carga la biblioteca de recetas del usuario para mostrarlas en el picker. */
    private fun cargarRecetasParaPicker() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargandoRecetas = true)
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val recetas = recetasRepositorio.obtenerRecetasDeUsuario(userId).getOrDefault(emptyList())
                Log.d("CalculadoraViewModel", "${recetas.size} recetas cargadas para el picker.")
                _estado.value = _estado.value.copy(recetasDisponibles = recetas, cargandoRecetas = false)
            } catch (e: Exception) {
                Log.e("CalculadoraViewModel", "Error al cargar recetas para el picker: ${e.message}")
                _estado.value = _estado.value.copy(cargandoRecetas = false)
            }
        }
    }

    /**
     * Calcula el objetivo calórico y la distribución de macros.
     * TMB (Mifflin-St Jeor) → TDEE → ajuste por objetivo → macros (2g prot/kg, 1g grasa/kg).
     */
    private fun calcularObjetivosNutricionales(
        perfil: Perfil,
        nivel: NivelActividad,
        objetivo: ObjetivoFisico
    ): ResultadoCalculoNutricional {
        val tmb = if (perfil.sexo == "Masculino") {
            10f * perfil.pesoKg + 6.25f * perfil.alturaCm - 5f * perfil.edad + 5f
        } else {
            10f * perfil.pesoKg + 6.25f * perfil.alturaCm - 5f * perfil.edad - 161f
        }

        val tdee = tmb * nivel.factor
        val objetivoKcal = (tdee * (1f + objetivo.modificador)).toInt()

        val proteinasG = perfil.pesoKg * 2f
        val grasasG = perfil.pesoKg * 1f
        val kcalFijas = (proteinasG * 4f) + (grasasG * 9f)
        val carbosG = ((objetivoKcal - kcalFijas) / 4f).coerceAtLeast(0f)

        Log.d(
            "CalculadoraViewModel",
            "TMB=${"%.0f".format(tmb)} → TDEE=${"%.0f".format(tdee)} → Obj=$objetivoKcal kcal | " +
            "P:${"%.0f".format(proteinasG)}g C:${"%.0f".format(carbosG)}g G:${"%.0f".format(grasasG)}g"
        )

        return ResultadoCalculoNutricional(objetivoKcal, proteinasG, carbosG, grasasG)
    }

    private data class ResultadoCalculoNutricional(
        val objetivoKcal: Int,
        val proteinasG: Float,
        val carbosG: Float,
        val grasasG: Float
    )
}

/**
 * Estado completo de la pantalla de Calculadora Dietética.
 */
data class EstadoCalculadora(
    val cargando: Boolean = true,
    val error: String? = null,
    val apodo: String = "",
    val macrosDiariosId: String = "",
    val nivelActividad: NivelActividad = NivelActividad.MODERADO,
    val objetivo: ObjetivoFisico = ObjetivoFisico.MANTENIMIENTO,
    val objetivoKcal: Int = 0,
    val objetivoProteinasG: Float = 0f,
    val objetivoCarbosG: Float = 0f,
    val objetivoGrasasG: Float = 0f,
    val consumidoKcal: Int = 0,
    val consumidoProteinasG: Float = 0f,
    val consumidoCarbosG: Float = 0f,
    val consumidoGrasasG: Float = 0f,
    val slotActivo: String? = null,
    val recetasDisponibles: List<Receta> = emptyList(),
    val cargandoRecetas: Boolean = false,
    val ingestasDelDia: Map<String, List<Receta>> = emptyMap(),
    // Navegación de fechas (modo historial = solo lectura)
    val fechaVisualizando: String = java.time.LocalDate.now().toString(),
    val esModoHistorial: Boolean = false,
    // Búsqueda de alimentos: texto, resultados locales (inmediatos) y de API (con debounce)
    val busquedaTexto: String = "",
    val resultadosLocales: List<Receta> = emptyList(),
    val resultadosBusqueda: List<Receta> = emptyList(),
    val buscandoAlimento: Boolean = false,
    val sesionCerrada: Boolean = false,
    val ingestaEditando: IngestaEditando? = null
)

data class IngestaEditando(
    val slotNombre: String,
    val recetaOriginal: Receta,
    val cantidadTexto: String = "100"
)
