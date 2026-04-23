package com.example.zentra.ui.screens.calculadora

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * - Cargar el perfil físico del usuario desde Supabase.
 * - Calcular el objetivo calórico diario mediante Mifflin-St Jeor → TDEE → ajuste por objetivo.
 * - Distribuir los macronutrientes según los estándares de nutrición deportiva.
 * - Leer y crear el registro diario en `daily_macros`, y actualizarlo cuando se añaden ingestas.
 * - Gestionar el picker de recetas para los slots de ingesta.
 */
@HiltViewModel
class CalculadoraViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio,
    private val macrosRepositorio: IMacrosRepositorio,
    private val recetasRepositorio: IRecetasRepositorio
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoCalculadora())
    val estado: StateFlow<EstadoCalculadora> = _estado.asStateFlow()

    init {
        Log.d("CalculadoraViewModel", "ViewModel inicializado. Cargando datos nutricionales del día.")
        cargarDatosDelDia()
    }

    /**
     * Carga o recalcula todos los datos de la pantalla.
     * Obtiene el perfil, calcula los objetivos con la configuración actual y lee (o crea)
     * el registro nutricional de hoy en Supabase. Preserva las ingestas en memoria del turno actual.
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

                val hoy = LocalDate.now().toString()
                val macrosExistentes = macrosRepositorio.obtenerMacrosDelDia(userId, hoy).getOrNull()

                val macrosHoy = macrosExistentes ?: run {
                    Log.d("CalculadoraViewModel", "Sin registro para $hoy. Creando fila inicial en daily_macros.")
                    val nuevo = MacrosDiarios(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        fecha = hoy,
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

    /** Abre el picker de recetas para el slot indicado. Carga la biblioteca si aún no está en memoria. */
    fun abrirSlot(nombreSlot: String) {
        _estado.value = _estado.value.copy(slotActivo = nombreSlot)
        if (_estado.value.recetasDisponibles.isEmpty()) {
            cargarRecetasParaPicker()
        }
    }

    /** Cierra el picker de recetas sin añadir nada. */
    fun cerrarSlot() {
        _estado.value = _estado.value.copy(slotActivo = null)
    }

    /**
     * Añade una receta al slot activo, suma sus macros a los totales del día y
     * persiste los nuevos totales en Supabase de forma reactiva.
     * @param receta La receta seleccionada por el usuario en el picker.
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

            // Actualizamos el mapa de ingestas en memoria para reflejar el slot
            val mapaActualizado = estadoActual.ingestasDelDia.toMutableMap()
            mapaActualizado[slotNombre] = (mapaActualizado[slotNombre] ?: emptyList()) + receta

            // Actualización optimista: la UI responde antes de confirmar con Supabase
            _estado.value = estadoActual.copy(
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas,
                ingestasDelDia = mapaActualizado,
                slotActivo = null
            )

            Log.d("CalculadoraViewModel", "Receta '${receta.titulo}' añadida a '$slotNombre'. Total: $nuevoKcal kcal.")

            // Persistimos los nuevos totales acumulados en daily_macros
            val macrosActualizados = MacrosDiarios(
                id = estadoActual.macrosDiariosId,
                userId = userId,
                fecha = LocalDate.now().toString(),
                objetivoKcal = estadoActual.objetivoKcal,
                consumidoKcal = nuevoKcal,
                consumidoProteinasG = nuevasProteinas,
                consumidoCarbosG = nuevosCarbos,
                consumidoGrasasG = nuevasGrasas
            )
            macrosRepositorio.guardarMacrosDelDia(macrosActualizados).onFailure { e ->
                Log.e("CalculadoraViewModel", "Error al persistir macros del día tras añadir receta: ${e.message}")
            }
        }
    }

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

    /** Carga la biblioteca de recetas del usuario para mostrarlas en el picker de slots. */
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
     * Calcula el objetivo calórico diario y la distribución de macronutrientes.
     *
     * Proceso:
     * 1. TMB → Fórmula Mifflin-St Jeor diferenciada por sexo.
     * 2. TDEE → TMB × factor de actividad.
     * 3. Objetivo calórico → TDEE ajustado según el modificador del objetivo físico.
     * 4. Macros → 2g proteína/kg, 1g grasa/kg; el resto en carbohidratos.
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
 * Los totales consumidos se actualizan en tiempo real a medida que el usuario añade ingestas.
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
    // Nombre del slot cuyo picker está abierto; null = picker cerrado
    val slotActivo: String? = null,
    val recetasDisponibles: List<Receta> = emptyList(),
    val cargandoRecetas: Boolean = false,
    // Mapa en memoria de recetas añadidas por slot en la sesión actual (no persiste entre aperturas)
    val ingestasDelDia: Map<String, List<Receta>> = emptyMap()
)
