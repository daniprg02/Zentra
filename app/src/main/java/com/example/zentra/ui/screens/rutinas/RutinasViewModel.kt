package com.example.zentra.ui.screens.rutinas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.EjercicioEnRutina
import com.example.zentra.domain.model.RutinaUsuario
import com.example.zentra.domain.repository.IRutinasRepositorio
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
 * ViewModel del módulo de Rutinas de Entrenamiento.
 *
 * Responsabilidades:
 * - Verificar si el usuario ya tiene un plan activo al iniciar la pantalla.
 * - Guiar al usuario por el cuestionario de tres pasos para personalizar su rutina.
 * - Aplicar el algoritmo de generación: split por días → filtrado de ejercicios →
 *   asignación de volumen según objetivo → persistencia en Supabase.
 * - Gestionar el flujo de creación de una nueva rutina sobre una existente.
 */
@HiltViewModel
class RutinasViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val rutinasRepositorio: IRutinasRepositorio
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoRutinas>(EstadoRutinas.Cargando)
    val estado: StateFlow<EstadoRutinas> = _estado.asStateFlow()

    init {
        Log.d("RutinasViewModel", "ViewModel inicializado. Buscando rutina activa.")
        cargarRutinaActiva()
    }

    /** Consulta Supabase para determinar si el usuario ya tiene un plan activo. */
    fun cargarRutinaActiva() {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Cargando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")
                val resultado = rutinasRepositorio.obtenerRutinaActiva(userId).getOrNull()
                _estado.value = if (resultado != null) {
                    val (cabecera, dias) = resultado
                    Log.d("RutinasViewModel", "Rutina activa encontrada: ${dias.size} días.")
                    EstadoRutinas.RutinaActiva(cabecera, dias)
                } else {
                    Log.d("RutinasViewModel", "No hay rutina activa. Mostrando pantalla de inicio.")
                    EstadoRutinas.SinRutina
                }
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al cargar la rutina activa: ${e.message}")
                _estado.value = EstadoRutinas.Error("No se pudo cargar tu plan. Comprueba tu conexión.")
            }
        }
    }

    /** Inicia el cuestionario desde el paso 1 con valores por defecto. */
    fun iniciarCuestionario() {
        _estado.value = EstadoRutinas.EnCuestionario(paso = 1, datos = DatosCuestionario())
        Log.d("RutinasViewModel", "Cuestionario iniciado en el paso 1.")
    }

    /** Avanza al paso siguiente o lanza la generación si estamos en el último paso. */
    fun siguientePaso() {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        if (actual.paso < 3) {
            _estado.value = actual.copy(paso = actual.paso + 1)
            Log.d("RutinasViewModel", "Cuestionario: avanzando al paso ${actual.paso + 1}.")
        } else {
            generarYGuardarRutina(actual.datos)
        }
    }

    /** Retrocede al paso anterior o vuelve a la pantalla de inicio si estamos en el paso 1. */
    fun anteriorPaso() {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        if (actual.paso > 1) {
            _estado.value = actual.copy(paso = actual.paso - 1)
        } else {
            _estado.value = EstadoRutinas.SinRutina
        }
    }

    /** Actualiza los datos del cuestionario manteniendo el paso actual. */
    fun actualizarDatos(nuevos: DatosCuestionario) {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        _estado.value = actual.copy(datos = nuevos)
    }

    /** Muestra el diálogo de confirmación para reemplazar la rutina activa. */
    fun pedirNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = true)
    }

    /** Cierra el diálogo de confirmación sin hacer nada. */
    fun cancelarNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = false)
    }

    /** Confirma la creación de una nueva rutina e inicia el cuestionario. */
    fun confirmarNuevaRutina() {
        iniciarCuestionario()
    }

    /**
     * Ejecuta el algoritmo de generación y persiste el resultado en Supabase.
     *
     * Proceso:
     * 1. Mapa experiencia → niveles permitidos.
     * 2. Mapa lugar → equipos disponibles.
     * 3. Carga y filtra ejercicios del catálogo.
     * 4. Aplica el split de días correspondiente.
     * 5. Para cada día, selecciona ejercicios (más en músculos prioritarios).
     * 6. Asigna series/reps según el objetivo del usuario.
     * 7. Desactiva la rutina anterior y guarda la nueva en Supabase.
     */
    private fun generarYGuardarRutina(datos: DatosCuestionario) {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Generando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val niveles = experienciaANiveles(datos.experiencia)
                val equipos = lugarAEquipos(datos.lugarEntrenamiento)

                Log.d("RutinasViewModel", "Generando rutina: ${datos.diasSemana}d, obj=${datos.objetivo}, niveles=$niveles, equipos=$equipos")

                val ejercicios = rutinasRepositorio.obtenerEjercicios(equipos, niveles)
                    .getOrDefault(emptyList())

                val porGrupo = ejercicios.groupBy { it.grupoMuscular }
                val split = obtenerSplit(datos.diasSemana)
                val (series, reps) = seriesRepsPorObjetivo(datos.objetivo)

                val rutinaId = UUID.randomUUID().toString()

                val dias = split.mapIndexed { index, (nombreDia, musculos) ->
                    val ejerciciosDelDia = musculos.flatMap { musculo ->
                        val pool = porGrupo[musculo] ?: emptyList()
                        val cantidad = if (musculo in datos.musculosPrioritarios) 3 else 2
                        pool.shuffled().take(cantidad.coerceAtMost(pool.size))
                    }
                    DiaRutina(
                        id = UUID.randomUUID().toString(),
                        rutinaId = rutinaId,
                        diaNumero = index + 1,
                        nombreDia = nombreDia,
                        ejercicios = ejerciciosDelDia.map { ej ->
                            EjercicioEnRutina(
                                ejercicioId = ej.id,
                                nombre = ej.nombre,
                                grupoMuscular = ej.grupoMuscular,
                                series = series,
                                repeticiones = reps
                            )
                        }
                    )
                }

                val rutina = RutinaUsuario(
                    id = rutinaId,
                    userId = userId,
                    objetivo = datos.objetivo,
                    diasSemana = datos.diasSemana,
                    activa = true,
                    creadaEn = null
                )

                rutinasRepositorio.desactivarRutinaActiva(userId)
                rutinasRepositorio.guardarRutina(rutina, dias).getOrThrow()

                Log.d("RutinasViewModel", "Rutina generada y guardada: $rutinaId con ${dias.size} días.")
                _estado.value = EstadoRutinas.RutinaActiva(rutina, dias)
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al generar la rutina: ${e.message}")
                _estado.value = EstadoRutinas.Error("No se pudo generar tu rutina. Inténtalo de nuevo.")
            }
        }
    }

    /** Traduce el nivel de experiencia del usuario a los niveles de dificultad permitidos. */
    private fun experienciaANiveles(experiencia: String): List<String> = when (experiencia) {
        "Nunca he entrenado", "Menos de 1 mes", "1 a 3 meses" -> listOf("Principiante")
        "3 a 6 meses", "6 a 12 meses" -> listOf("Principiante", "Intermedio")
        else -> listOf("Principiante", "Intermedio", "Avanzado")
    }

    /** Traduce el lugar de entrenamiento a los tipos de equipamiento disponibles. */
    private fun lugarAEquipos(lugar: String): List<String> = when (lugar) {
        "Casa" -> listOf("Calistenia", "Mancuernas")
        "Calle" -> listOf("Calistenia")
        "Gimnasio mediano" -> listOf("Barra", "Mancuernas", "Cable", "Calistenia")
        "Gimnasio pequeño" -> listOf("Mancuernas", "Cable", "Calistenia")
        "Mixto" -> listOf("Calistenia", "Mancuernas", "Barra", "Cable")
        else -> listOf("Barra", "Mancuernas", "Cable", "Máquina", "Calistenia") // Gimnasio grande
    }

    /**
     * Devuelve el split (lista de días con sus grupos musculares) según el número de días.
     * - 2 días: Full Body A/B
     * - 3 días: Push / Pull / Piernas
     * - 4 días: Upper A / Lower A / Upper B / Lower B
     * - 5 días: Torso dividido + Piernas
     * - 6 días: PPL × 2
     */
    private fun obtenerSplit(dias: Int): List<Pair<String, List<String>>> = when (dias) {
        2 -> listOf(
            "Full Body A" to listOf("Pecho", "Espalda", "Hombros", "Cuádriceps"),
            "Full Body B" to listOf("Espalda", "Isquiotibiales", "Glúteos", "Core")
        )
        3 -> listOf(
            "Push" to listOf("Pecho", "Hombros", "Tríceps"),
            "Pull" to listOf("Espalda", "Bíceps"),
            "Piernas" to listOf("Cuádriceps", "Isquiotibiales", "Glúteos", "Gemelos")
        )
        4 -> listOf(
            "Upper A" to listOf("Pecho", "Espalda", "Hombros"),
            "Lower A" to listOf("Cuádriceps", "Isquiotibiales", "Glúteos"),
            "Upper B" to listOf("Pecho", "Espalda", "Bíceps", "Tríceps"),
            "Lower B" to listOf("Cuádriceps", "Isquiotibiales", "Gemelos", "Core")
        )
        5 -> listOf(
            "Pecho + Tríceps" to listOf("Pecho", "Tríceps"),
            "Espalda + Bíceps" to listOf("Espalda", "Bíceps"),
            "Hombros + Core" to listOf("Hombros", "Core"),
            "Cuádriceps + Isquiotibiales" to listOf("Cuádriceps", "Isquiotibiales"),
            "Glúteos + Gemelos" to listOf("Glúteos", "Gemelos")
        )
        else -> listOf(
            "Push A" to listOf("Pecho", "Hombros", "Tríceps"),
            "Pull A" to listOf("Espalda", "Bíceps"),
            "Legs A" to listOf("Cuádriceps", "Isquiotibiales", "Glúteos"),
            "Push B" to listOf("Pecho", "Hombros", "Tríceps"),
            "Pull B" to listOf("Espalda", "Bíceps"),
            "Legs B" to listOf("Cuádriceps", "Isquiotibiales", "Gemelos", "Core")
        )
    }

    /**
     * Devuelve el volumen óptimo (series × repeticiones) según el objetivo calórico.
     * - Déficit: más reps para preservar músculo con menor carga articular.
     * - Mantenimiento: rango híbrido de fuerza-hipertrofia.
     * - Superávit: más peso y menos reps para estimular crecimiento máximo.
     */
    private fun seriesRepsPorObjetivo(objetivo: String): Pair<Int, String> = when (objetivo) {
        "Déficit" -> 3 to "12-15"
        "Superávit" -> 4 to "6-10"
        else -> 4 to "8-12"
    }
}

/** Estados posibles de la pantalla de Rutinas a lo largo de su ciclo de vida. */
sealed class EstadoRutinas {
    object Cargando : EstadoRutinas()
    object SinRutina : EstadoRutinas()
    data class EnCuestionario(val paso: Int, val datos: DatosCuestionario) : EstadoRutinas()
    object Generando : EstadoRutinas()
    data class RutinaActiva(
        val cabecera: RutinaUsuario,
        val dias: List<DiaRutina>,
        val mostrandoDialogoNueva: Boolean = false
    ) : EstadoRutinas()
    data class Error(val mensaje: String) : EstadoRutinas()
}

/**
 * Datos recogidos a lo largo del cuestionario de generación de rutinas.
 * Todos los campos tienen valores por defecto sensatos para minimizar la fricción del usuario.
 */
data class DatosCuestionario(
    val objetivo: String = "Mantenimiento",
    val diasSemana: Int = 3,
    val musculosPrioritarios: Set<String> = emptySet(),
    val experiencia: String = "Nunca he entrenado",
    val lugarEntrenamiento: String = "Gimnasio grande"
)
