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
 * - Verificar si el usuario tiene un plan activo al iniciar.
 * - Guiar el cuestionario de 3 pasos para generar una rutina personalizada.
 * - Aplicar el algoritmo: split por días → filtrar ejercicios → asignar volumen → persistir.
 * - Listar todas las rutinas guardadas y permitir activar una anterior o eliminar con confirmación.
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

    // ─── Carga inicial ────────────────────────────────────────────────────────

    /**
     * Consulta Supabase para determinar si el usuario tiene un plan activo.
     * También carga el historial completo de rutinas para el gestor.
     */
    fun cargarRutinaActiva() {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Cargando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val activa = rutinasRepositorio.obtenerRutinaActiva(userId).getOrNull()
                val todas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())

                _estado.value = if (activa != null) {
                    val (cabecera, dias) = activa
                    Log.d("RutinasViewModel", "Rutina activa: ${dias.size} días. Total guardadas: ${todas.size}.")
                    EstadoRutinas.RutinaActiva(cabecera = cabecera, dias = dias, todasLasRutinas = todas)
                } else {
                    Log.d("RutinasViewModel", "Sin rutina activa. Guardadas: ${todas.size}.")
                    EstadoRutinas.SinRutina(todasLasRutinas = todas)
                }
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al cargar la rutina activa: ${e.message}")
                _estado.value = EstadoRutinas.Error("No se pudo cargar tu plan. Comprueba tu conexión.")
            }
        }
    }

    // ─── Cuestionario ─────────────────────────────────────────────────────────

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
            cargarRutinaActiva()
        }
    }

    /** Actualiza los datos del cuestionario manteniendo el paso actual. */
    fun actualizarDatos(nuevos: DatosCuestionario) {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        _estado.value = actual.copy(datos = nuevos)
    }

    // ─── Gestión de la rutina activa ──────────────────────────────────────────

    /** Muestra el diálogo de confirmación para crear una nueva rutina. */
    fun pedirNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = true)
    }

    /** Cierra el diálogo de nueva rutina sin hacer nada. */
    fun cancelarNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = false)
    }

    /** Confirma la creación de una nueva rutina e inicia el cuestionario. */
    fun confirmarNuevaRutina() {
        iniciarCuestionario()
    }

    // ─── Gestión del historial de rutinas ────────────────────────────────────

    /**
     * Activa una rutina guardada anteriormente.
     * Desactiva la actual, marca la seleccionada como activa y carga sus días.
     */
    fun activarRutina(rutina: RutinaUsuario) {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Cargando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                rutinasRepositorio.desactivarRutinaActiva(userId).getOrThrow()
                rutinasRepositorio.marcarRutinaActiva(rutina.id).getOrThrow()

                val dias = rutinasRepositorio.obtenerDiasDeRutina(rutina.id).getOrThrow()
                val todas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())

                Log.d("RutinasViewModel", "Rutina '${rutina.id}' activada. ${dias.size} días cargados.")
                _estado.value = EstadoRutinas.RutinaActiva(
                    cabecera = rutina.copy(activa = true),
                    dias = dias,
                    todasLasRutinas = todas
                )
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al activar rutina: ${e.message}")
                cargarRutinaActiva()
            }
        }
    }

    /** Abre el diálogo de confirmación para eliminar una rutina concreta. */
    fun pedirEliminarRutina(rutina: RutinaUsuario) {
        val actual = _estado.value
        _estado.value = when (actual) {
            is EstadoRutinas.RutinaActiva -> actual.copy(rutinaParaEliminar = rutina)
            is EstadoRutinas.SinRutina -> actual.copy(rutinaParaEliminar = rutina)
            else -> actual
        }
    }

    /** Cierra el diálogo de eliminación sin hacer nada. */
    fun cancelarEliminarRutina() {
        val actual = _estado.value
        _estado.value = when (actual) {
            is EstadoRutinas.RutinaActiva -> actual.copy(rutinaParaEliminar = null)
            is EstadoRutinas.SinRutina -> actual.copy(rutinaParaEliminar = null)
            else -> actual
        }
    }

    /** Ejecuta la eliminación permanente de la rutina confirmada y recarga el estado. */
    fun confirmarEliminarRutina() {
        val rutinaAEliminar = when (val actual = _estado.value) {
            is EstadoRutinas.RutinaActiva -> actual.rutinaParaEliminar
            is EstadoRutinas.SinRutina -> actual.rutinaParaEliminar
            else -> null
        } ?: return

        viewModelScope.launch {
            try {
                rutinasRepositorio.eliminarRutina(rutinaAEliminar.id).getOrThrow()
                Log.d("RutinasViewModel", "Rutina '${rutinaAEliminar.id}' eliminada.")
                cargarRutinaActiva()
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al eliminar rutina: ${e.message}")
                cancelarEliminarRutina()
            }
        }
    }

    // ─── Algoritmo de generación ──────────────────────────────────────────────

    /**
     * Ejecuta el algoritmo de generación y persiste el resultado en Supabase.
     *
     * Proceso:
     * 1. Experiencia → niveles permitidos.
     * 2. Lugar → equipos disponibles.
     * 3. Carga y filtra ejercicios del catálogo.
     * 4. Aplica el split de días correspondiente.
     * 5. Selecciona ejercicios (más en músculos prioritarios).
     * 6. Asigna series/reps según el objetivo.
     * 7. Desactiva rutina anterior y guarda la nueva.
     */
    private fun generarYGuardarRutina(datos: DatosCuestionario) {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Generando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val niveles = experienciaANiveles(datos.experiencia)
                val equipos = lugarAEquipos(datos.lugarEntrenamiento)

                Log.d("RutinasViewModel", "Generando: ${datos.diasSemana}d, obj=${datos.objetivo}, niveles=$niveles, equipos=$equipos")

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

                val todas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())
                Log.d("RutinasViewModel", "Rutina $rutinaId generada y guardada.")
                _estado.value = EstadoRutinas.RutinaActiva(rutina, dias, todasLasRutinas = todas)
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al generar la rutina: ${e.message}")
                _estado.value = EstadoRutinas.Error("No se pudo generar tu rutina. Inténtalo de nuevo.")
            }
        }
    }

    // ─── Mapas auxiliares del algoritmo ──────────────────────────────────────

    private fun experienciaANiveles(experiencia: String): List<String> = when (experiencia) {
        "Nunca he entrenado", "Menos de 1 mes", "1 a 3 meses" -> listOf("Principiante")
        "3 a 6 meses", "6 a 12 meses" -> listOf("Principiante", "Intermedio")
        else -> listOf("Principiante", "Intermedio", "Avanzado")
    }

    private fun lugarAEquipos(lugar: String): List<String> = when (lugar) {
        "Casa" -> listOf("Calistenia", "Mancuernas")
        "Calle" -> listOf("Calistenia")
        "Gimnasio mediano" -> listOf("Barra", "Mancuernas", "Cable", "Calistenia")
        "Gimnasio pequeño" -> listOf("Mancuernas", "Cable", "Calistenia")
        "Mixto" -> listOf("Calistenia", "Mancuernas", "Barra", "Cable")
        else -> listOf("Barra", "Mancuernas", "Cable", "Máquina", "Calistenia")
    }

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

    private fun seriesRepsPorObjetivo(objetivo: String): Pair<Int, String> = when (objetivo) {
        "Déficit" -> 3 to "12-15"
        "Superávit" -> 4 to "6-10"
        else -> 4 to "8-12"
    }
}

// ─── Estados ──────────────────────────────────────────────────────────────────

sealed class EstadoRutinas {
    object Cargando : EstadoRutinas()

    /** No hay rutina activa, pero puede haber rutinas guardadas anteriores. */
    data class SinRutina(
        val todasLasRutinas: List<RutinaUsuario> = emptyList(),
        val rutinaParaEliminar: RutinaUsuario? = null
    ) : EstadoRutinas()

    data class EnCuestionario(val paso: Int, val datos: DatosCuestionario) : EstadoRutinas()

    object Generando : EstadoRutinas()

    data class RutinaActiva(
        val cabecera: RutinaUsuario,
        val dias: List<DiaRutina>,
        val todasLasRutinas: List<RutinaUsuario> = emptyList(),
        val mostrandoDialogoNueva: Boolean = false,
        val rutinaParaEliminar: RutinaUsuario? = null
    ) : EstadoRutinas()

    data class Error(val mensaje: String) : EstadoRutinas()
}

data class DatosCuestionario(
    val objetivo: String = "Mantenimiento",
    val diasSemana: Int = 3,
    val musculosPrioritarios: Set<String> = emptySet(),
    val experiencia: String = "Nunca he entrenado",
    val lugarEntrenamiento: String = "Gimnasio grande"
)
