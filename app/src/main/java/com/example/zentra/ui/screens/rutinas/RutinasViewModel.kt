package com.example.zentra.ui.screens.rutinas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.data.local.ListaEjerciciosLocal
import com.example.zentra.data.local.ZentraCacheManager
import com.example.zentra.data.remote.dto.PerfilDto
import com.example.zentra.data.remote.gemini.GeminiGeneradorRutinas
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.Ejercicio
import com.example.zentra.domain.model.EjercicioEnRutina
import com.example.zentra.domain.model.RutinaUsuario
import com.example.zentra.domain.repository.IRutinasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
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
 * - Llamar a Gemini para generar la rutina con IA; si falla, usa el algoritmo local.
 * - Respetar los límites de ejercicios por día según la experiencia del usuario.
 * - Gestionar el historial (máximo 10 rutinas), activación y eliminación.
 * - Permitir editar series/repeticiones y sustituir ejercicios por IA.
 * - Cambiar el grupo muscular de un ejercicio sin repetir los ya presentes en el día.
 * - Funcionar en modo sin conexión mostrando la rutina cacheada.
 */
@HiltViewModel
class RutinasViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val rutinasRepositorio: IRutinasRepositorio,
    private val geminiGenerador: GeminiGeneradorRutinas,
    private val cacheManager: ZentraCacheManager
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoRutinas>(EstadoRutinas.Cargando)
    val estado: StateFlow<EstadoRutinas> = _estado.asStateFlow()

    private var sexoUsuario = "Masculino"
    private var sexoCargado = false

    init {
        Log.d("RutinasViewModel", "ViewModel inicializado. Buscando rutina activa.")
        cargarRutinaActiva()
    }

    // ─── Carga inicial ────────────────────────────────────────────────────────

    fun cargarRutinaActiva() {
        viewModelScope.launch {
            _estado.value = EstadoRutinas.Cargando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                if (!sexoCargado) {
                    sexoUsuario = try {
                        supabase.from("profiles")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<PerfilDto>()
                            ?.sexo?.takeIf { it.isNotBlank() } ?: "Masculino"
                    } catch (e: Exception) { "Masculino" }
                    sexoCargado = true
                    Log.d("RutinasViewModel", "Sexo del perfil cargado: $sexoUsuario")
                }

                val activa = rutinasRepositorio.obtenerRutinaActiva(userId).getOrNull()
                val todas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())

                _estado.value = if (activa != null) {
                    val (cabecera, dias) = activa
                    cacheManager.guardarRutinaActiva(cabecera, dias)
                    Log.d("RutinasViewModel", "Rutina activa: ${dias.size} días. Total guardadas: ${todas.size}.")
                    EstadoRutinas.RutinaActiva(
                        cabecera = cabecera,
                        dias = dias,
                        todasLasRutinas = todas,
                        sexo = sexoUsuario,
                        esRutinaBasica = !cabecera.generadaConIA
                    )
                } else {
                    Log.d("RutinasViewModel", "Sin rutina activa. Guardadas: ${todas.size}.")
                    EstadoRutinas.SinRutina(todasLasRutinas = todas)
                }
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al cargar la rutina activa: ${e.message}")
                val cacheada = cacheManager.cargarRutinaActiva()
                _estado.value = if (cacheada != null) {
                    val (cabecera, dias) = cacheada
                    Log.d("RutinasViewModel", "Sin conexión: mostrando rutina cacheada (${dias.size} días).")
                    EstadoRutinas.RutinaActiva(cabecera = cabecera, dias = dias, sexo = sexoUsuario, sinConexion = true)
                } else {
                    Log.d("RutinasViewModel", "Sin conexión y sin caché. Mostrando estado vacío.")
                    EstadoRutinas.SinRutina(sinConexion = true)
                }
            }
        }
    }

    // ─── Cuestionario ─────────────────────────────────────────────────────────

    fun iniciarCuestionario() {
        _estado.value = EstadoRutinas.EnCuestionario(paso = 1, datos = DatosCuestionario(generarConIA = true), sexo = sexoUsuario)
        Log.d("RutinasViewModel", "Cuestionario IA iniciado en el paso 1.")
    }

    fun iniciarCuestionarioLocal() {
        _estado.value = EstadoRutinas.EnCuestionario(paso = 1, datos = DatosCuestionario(generarConIA = false), sexo = sexoUsuario)
        Log.d("RutinasViewModel", "Cuestionario rutina básica iniciado en el paso 1.")
    }

    fun siguientePaso() {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        // La rutina básica tiene 3 pasos (sin lesiones); la IA tiene 4
        val maxPasos = if (actual.datos.generarConIA) 4 else 3
        if (actual.paso < maxPasos) {
            _estado.value = actual.copy(paso = actual.paso + 1)
        } else {
            generarYGuardarRutina(actual.datos)
        }
    }

    fun anteriorPaso() {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        if (actual.paso > 1) {
            _estado.value = actual.copy(paso = actual.paso - 1)
        } else {
            cargarRutinaActiva()
        }
    }

    fun actualizarDatos(nuevos: DatosCuestionario) {
        val actual = _estado.value as? EstadoRutinas.EnCuestionario ?: return
        _estado.value = actual.copy(datos = nuevos)
    }

    // ─── Gestión de la rutina activa ──────────────────────────────────────────

    fun pedirNuevaRutinaIA() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = true, nuevaRutinaConIA = true)
        Log.d("RutinasViewModel", "Solicitud de nueva rutina con IA.")
    }

    fun pedirNuevaRutinaLocal() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = true, nuevaRutinaConIA = false)
        Log.d("RutinasViewModel", "Solicitud de nueva plantilla local.")
    }

    fun cancelarNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(mostrandoDialogoNueva = false)
    }

    fun confirmarNuevaRutina() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        if (actual.nuevaRutinaConIA) iniciarCuestionario() else iniciarCuestionarioLocal()
    }

    // ─── Edición de ejercicios ─────────────────────────────────────────────────

    fun iniciarEdicionEjercicio(diaNumero: Int, ejercicioIdx: Int) {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        val dia = actual.dias.find { it.diaNumero == diaNumero } ?: return
        val ejercicio = dia.ejercicios.getOrNull(ejercicioIdx) ?: return
        // Grupos ocupados por otros ejercicios del mismo día (excluye el ejercicio actual)
        val gruposOcupados = dia.ejercicios
            .filterIndexed { i, _ -> i != ejercicioIdx }
            .map { it.grupoMuscular }
            .distinct()
        _estado.value = actual.copy(
            ejercicioEditando = EjercicioEditando(
                diaNumero = diaNumero,
                ejercicioIdx = ejercicioIdx,
                series = ejercicio.series,
                repeticiones = ejercicio.repeticiones,
                nombreEjercicio = ejercicio.nombre,
                grupoMuscular = ejercicio.grupoMuscular,
                gruposOcupadosEnDia = gruposOcupados
            )
        )
    }

    fun cancelarEdicionEjercicio() {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        _estado.value = actual.copy(ejercicioEditando = null)
    }

    fun guardarEdicionEjercicio(series: Int, repeticiones: String) {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        val edicion = actual.ejercicioEditando ?: return
        viewModelScope.launch {
            try {
                val dia = actual.dias.find { it.diaNumero == edicion.diaNumero } ?: return@launch
                val nuevosEjercicios = dia.ejercicios.toMutableList()
                nuevosEjercicios[edicion.ejercicioIdx] = nuevosEjercicios[edicion.ejercicioIdx].copy(
                    series = series.coerceIn(1, 6),
                    repeticiones = repeticiones.ifBlank { "8-12" }
                )
                val diaActualizado = dia.copy(ejercicios = nuevosEjercicios)
                rutinasRepositorio.actualizarDiaRutina(diaActualizado).getOrThrow()
                val nuevosDias = actual.dias.map { if (it.diaNumero == edicion.diaNumero) diaActualizado else it }
                Log.d("RutinasViewModel", "Ejercicio editado: '${edicion.nombreEjercicio}' → ${series}×${repeticiones}.")
                _estado.value = actual.copy(dias = nuevosDias, ejercicioEditando = null)
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al guardar la edición del ejercicio: ${e.message}")
                _estado.value = actual.copy(ejercicioEditando = null)
            }
        }
    }

    /**
     * Sustituye un ejercicio por otro del mismo grupo muscular generado con IA.
     * Excluye los ejercicios ya presentes en el día para evitar repeticiones.
     */
    fun sustituirEjercicioConIA(diaNumero: Int, ejercicioIdx: Int) {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        viewModelScope.launch {
            _estado.value = actual.copy(sustitucionEnCurso = diaNumero to ejercicioIdx)
            try {
                val dia = actual.dias.find { it.diaNumero == diaNumero } ?: run {
                    _estado.value = actual.copy(sustitucionEnCurso = null); return@launch
                }
                val ejercicioActual = dia.ejercicios.getOrNull(ejercicioIdx) ?: run {
                    _estado.value = actual.copy(sustitucionEnCurso = null); return@launch
                }
                val idsEnDia = dia.ejercicios.map { it.ejercicioId }.toSet()

                val todos = rutinasRepositorio.obtenerEjercicios(
                    equipos = listOf("Barra", "Mancuernas", "Cable", "Máquina", "Calistenia"),
                    niveles = listOf("Principiante", "Intermedio", "Avanzado")
                ).getOrDefault(emptyList())

                val candidatos = todos.filter {
                    it.grupoMuscular == ejercicioActual.grupoMuscular && it.id !in idsEnDia
                }

                if (candidatos.isEmpty()) {
                    Log.d("RutinasViewModel", "Sin candidatos para sustituir '${ejercicioActual.nombre}'.")
                    _estado.value = actual.copy(sustitucionEnCurso = null)
                    return@launch
                }

                val sustituto = geminiGenerador.generarSustituto(
                    ejercicioActual = ejercicioActual.nombre,
                    grupoMuscular = ejercicioActual.grupoMuscular,
                    candidatos = candidatos
                ) ?: candidatos.random()

                val nuevosEjercicios = dia.ejercicios.toMutableList()
                nuevosEjercicios[ejercicioIdx] = EjercicioEnRutina(
                    ejercicioId = sustituto.id,
                    nombre = sustituto.nombre,
                    grupoMuscular = sustituto.grupoMuscular,
                    series = ejercicioActual.series,
                    repeticiones = ejercicioActual.repeticiones
                )
                val diaActualizado = dia.copy(ejercicios = nuevosEjercicios)
                rutinasRepositorio.actualizarDiaRutina(diaActualizado).getOrThrow()
                val nuevosDias = actual.dias.map { if (it.diaNumero == diaNumero) diaActualizado else it }
                Log.d("RutinasViewModel", "'${ejercicioActual.nombre}' sustituido por '${sustituto.nombre}'.")
                _estado.value = actual.copy(dias = nuevosDias, sustitucionEnCurso = null)
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al sustituir ejercicio: ${e.message}")
                _estado.value = actual.copy(sustitucionEnCurso = null)
            }
        }
    }

    /**
     * Cambia el grupo muscular de un ejercicio y genera uno nuevo de ese grupo con IA.
     * Permite cualquier grupo destino; la unicidad se garantiza a nivel de ejercicio (id),
     * no de grupo, por lo que el mismo día puede tener varios ejercicios del mismo músculo.
     */
    fun cambiarGrupoMuscularEjercicio(nuevoGrupo: String) {
        val actual = _estado.value as? EstadoRutinas.RutinaActiva ?: return
        val edicion = actual.ejercicioEditando ?: return
        viewModelScope.launch {
            // Cerramos el diálogo y activamos el spinner antes de la llamada a red.
            _estado.value = actual.copy(
                sustitucionEnCurso = edicion.diaNumero to edicion.ejercicioIdx,
                ejercicioEditando = null
            )
            try {
                // A partir de aquí leemos el estado actual para no sobreescribir ejercicioEditando = null.
                val estadoVivo = _estado.value as? EstadoRutinas.RutinaActiva ?: return@launch
                val dia = estadoVivo.dias.find { it.diaNumero == edicion.diaNumero } ?: run {
                    _estado.value = estadoVivo.copy(sustitucionEnCurso = null); return@launch
                }
                val ejercicioActual = dia.ejercicios.getOrNull(edicion.ejercicioIdx) ?: run {
                    _estado.value = estadoVivo.copy(sustitucionEnCurso = null); return@launch
                }
                // Excluimos únicamente los ejercicios ya presentes en el día (por id), no por grupo.
                val idsEnDia = dia.ejercicios.map { it.ejercicioId }.toSet()

                val todos = rutinasRepositorio.obtenerEjercicios(
                    equipos = listOf("Barra", "Mancuernas", "Cable", "Máquina", "Calistenia"),
                    niveles = listOf("Principiante", "Intermedio", "Avanzado")
                ).getOrDefault(emptyList())

                val candidatos = todos.filter {
                    it.grupoMuscular == nuevoGrupo && it.id !in idsEnDia
                }

                if (candidatos.isEmpty()) {
                    Log.d("RutinasViewModel", "Sin candidatos para el grupo '$nuevoGrupo'. Sin cambios.")
                    _estado.value = estadoVivo.copy(sustitucionEnCurso = null)
                    return@launch
                }

                val elegido = geminiGenerador.generarSustituto(
                    ejercicioActual = ejercicioActual.nombre,
                    grupoMuscular = nuevoGrupo,
                    candidatos = candidatos
                ) ?: candidatos.random()

                val nuevosEjercicios = dia.ejercicios.toMutableList()
                nuevosEjercicios[edicion.ejercicioIdx] = EjercicioEnRutina(
                    ejercicioId = elegido.id,
                    nombre = elegido.nombre,
                    grupoMuscular = elegido.grupoMuscular,
                    series = ejercicioActual.series,
                    repeticiones = ejercicioActual.repeticiones
                )
                val diaActualizado = dia.copy(ejercicios = nuevosEjercicios)
                rutinasRepositorio.actualizarDiaRutina(diaActualizado).getOrThrow()
                val nuevosDias = estadoVivo.dias.map { if (it.diaNumero == edicion.diaNumero) diaActualizado else it }
                Log.d("RutinasViewModel", "Grupo cambiado: '${ejercicioActual.grupoMuscular}' → '$nuevoGrupo'. Ejercicio: '${elegido.nombre}'.")
                _estado.value = estadoVivo.copy(dias = nuevosDias, sustitucionEnCurso = null)
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al cambiar grupo muscular: ${e.message}")
                val estadoVivo = _estado.value as? EstadoRutinas.RutinaActiva ?: return@launch
                _estado.value = estadoVivo.copy(sustitucionEnCurso = null)
            }
        }
    }

    // ─── Gestión del historial de rutinas ────────────────────────────────────

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

                val nuevaCabecera = rutina.copy(activa = true)
                cacheManager.guardarRutinaActiva(nuevaCabecera, dias)
                Log.d("RutinasViewModel", "Rutina '${rutina.id}' activada. ${dias.size} días cargados.")
                _estado.value = EstadoRutinas.RutinaActiva(
                    cabecera = nuevaCabecera,
                    dias = dias,
                    todasLasRutinas = todas,
                    sexo = sexoUsuario
                )
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al activar rutina: ${e.message}")
                cargarRutinaActiva()
            }
        }
    }

    fun pedirEliminarRutina(rutina: RutinaUsuario) {
        val actual = _estado.value
        _estado.value = when (actual) {
            is EstadoRutinas.RutinaActiva -> actual.copy(rutinaParaEliminar = rutina)
            is EstadoRutinas.SinRutina -> actual.copy(rutinaParaEliminar = rutina)
            else -> actual
        }
    }

    fun cancelarEliminarRutina() {
        val actual = _estado.value
        _estado.value = when (actual) {
            is EstadoRutinas.RutinaActiva -> actual.copy(rutinaParaEliminar = null)
            is EstadoRutinas.SinRutina -> actual.copy(rutinaParaEliminar = null)
            else -> actual
        }
    }

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

    // ─── Generación de rutina ────────────────────────────────────────────────

    /**
     * Genera y guarda una nueva rutina.
     * - Si [DatosCuestionario.generarConIA] = true: intenta Gemini (con fallback local si falla).
     * - Si [DatosCuestionario.generarConIA] = false: usa directamente [ListaEjerciciosLocal] sin red.
     */
    private fun generarYGuardarRutina(datos: DatosCuestionario) {
        viewModelScope.launch {
            _estado.value = if (datos.generarConIA) {
                EstadoRutinas.Generando("Consultando IA para personalizar tu rutina...", esIA = true)
            } else {
                EstadoRutinas.Generando("Creando tu rutina básica...", esIA = false)
            }

            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa.")

                val todasLasRutinas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())
                if (todasLasRutinas.size >= 10) {
                    _estado.value = EstadoRutinas.Error(
                        "Has alcanzado el límite de 10 rutinas guardadas. Elimina alguna para poder crear una nueva."
                    )
                    return@launch
                }

                val niveles = experienciaANiveles(datos.experiencia)
                val equipos = lugarAEquipos(datos.lugarEntrenamiento, datos.lugaresSeleccionados)
                val rutinaId = UUID.randomUUID().toString()

                Log.d("RutinasViewModel", "Generando (IA=${datos.generarConIA}): ${datos.diasSemana}d, obj=${datos.objetivo}, equipos=$equipos")

                val dias = if (datos.generarConIA) {
                    // Camino IA: ejercicios de Supabase + Gemini, fallback local si no hay red
                    val ejerciciosRed = rutinasRepositorio.obtenerEjercicios(equipos, niveles).getOrDefault(emptyList())
                    val ejerciciosBase = ejerciciosRed.ifEmpty {
                        Log.d("RutinasViewModel", "Sin ejercicios en red, usando lista local como base.")
                        ListaEjerciciosLocal.obtenerEjercicios(equipos, niveles)
                    }
                    geminiGenerador.generarRutina(datos, ejerciciosBase, rutinaId)
                        ?: run {
                            Log.d("RutinasViewModel", "Gemini no disponible. Fallback al algoritmo local.")
                            _estado.value = EstadoRutinas.Generando("Generando tu rutina...", esIA = false)
                            generarDiasLocalmente(datos, ejerciciosBase, rutinaId)
                        }
                } else {
                    // Camino local: lista hardcodeada, sin llamadas a red
                    val ejerciciosLocales = ListaEjerciciosLocal.obtenerEjercicios(equipos, niveles)
                    Log.d("RutinasViewModel", "Usando ${ejerciciosLocales.size} ejercicios locales.")
                    generarDiasLocalmente(datos, ejerciciosLocales, rutinaId)
                }

                val rutina = RutinaUsuario(
                    id = rutinaId,
                    userId = userId,
                    objetivo = datos.objetivo,
                    diasSemana = datos.diasSemana,
                    activa = true,
                    creadaEn = null,
                    generadaConIA = datos.generarConIA
                )

                rutinasRepositorio.desactivarRutinaActiva(userId)
                rutinasRepositorio.guardarRutina(rutina, dias).getOrThrow()
                cacheManager.guardarRutinaActiva(rutina, dias)

                val todas = rutinasRepositorio.obtenerTodasLasRutinas(userId).getOrDefault(emptyList())
                Log.d("RutinasViewModel", "Rutina $rutinaId guardada con ${dias.size} días. EsBasica=${!datos.generarConIA}")
                _estado.value = EstadoRutinas.RutinaActiva(
                    rutina, dias,
                    todasLasRutinas = todas,
                    sexo = sexoUsuario,
                    esRutinaBasica = !datos.generarConIA
                )
            } catch (e: Exception) {
                Log.e("RutinasViewModel", "Error al generar la rutina: ${e.message}")
                _estado.value = EstadoRutinas.Error("No se pudo generar tu rutina. Inténtalo de nuevo.")
            }
        }
    }

    // ─── Algoritmo local (fallback) ───────────────────────────────────────────

    private fun generarDiasLocalmente(
        datos: DatosCuestionario,
        ejercicios: List<Ejercicio>,
        rutinaId: String
    ): List<DiaRutina> {
        val porGrupo = ejercicios.groupBy { it.grupoMuscular }
        val split = obtenerSplit(datos.diasSemana)
        val (series, reps) = seriesRepsPorObjetivo(datos.objetivo)
        val (minEj, maxEj) = experienciaAMinMax(datos.experiencia)

        return split.mapIndexed { index, (nombreDia, musculos) ->
            val ejerciciosCombinados = musculos.flatMap { musculo ->
                val pool = porGrupo[musculo] ?: emptyList()
                val cantidad = if (musculo in datos.musculosPrioritarios) 3 else 2
                pool.shuffled().take(cantidad.coerceAtMost(pool.size))
            }
            val ejerciciosDelDia = when {
                ejerciciosCombinados.size > maxEj -> ejerciciosCombinados.take(maxEj)
                ejerciciosCombinados.size < minEj && ejercicios.size >= minEj -> {
                    val extra = ejercicios.filter { it !in ejerciciosCombinados }.shuffled()
                    (ejerciciosCombinados + extra).take(minEj)
                }
                else -> ejerciciosCombinados
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
    }

    // ─── Mapas auxiliares ──────────────────────────────────────────────────────

    private fun experienciaANiveles(experiencia: String): List<String> = when (experiencia) {
        "Nunca he entrenado", "Menos de 1 mes", "1 a 3 meses" -> listOf("Principiante")
        "3 a 6 meses", "6 a 12 meses" -> listOf("Principiante", "Intermedio")
        else -> listOf("Principiante", "Intermedio", "Avanzado")
    }

    internal fun experienciaAMinMax(experiencia: String): Pair<Int, Int> = when (experiencia) {
        "Nunca he entrenado", "Menos de 1 mes" -> 3 to 4
        "1 a 3 meses", "3 a 6 meses" -> 4 to 5
        "6 a 12 meses" -> 5 to 6
        else -> 6 to 7
    }

    private fun lugarBaseAEquipos(lugar: String): List<String> = when (lugar) {
        "Casa" -> listOf("Calistenia", "Mancuernas")
        "Calle" -> listOf("Calistenia")
        "Gimnasio mediano" -> listOf("Barra", "Mancuernas", "Cable", "Calistenia")
        "Gimnasio pequeño" -> listOf("Mancuernas", "Cable", "Calistenia")
        else -> listOf("Barra", "Mancuernas", "Cable", "Máquina", "Calistenia")
    }

    private fun lugarAEquipos(lugar: String, lugaresSeleccionados: List<String>): List<String> {
        return if (lugar == "Mixto") {
            lugaresSeleccionados
                .flatMap { lugarBaseAEquipos(it) }
                .distinct()
                .ifEmpty { listOf("Calistenia", "Mancuernas", "Barra", "Cable") }
        } else {
            lugarBaseAEquipos(lugar)
        }
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

    data class SinRutina(
        val todasLasRutinas: List<RutinaUsuario> = emptyList(),
        val rutinaParaEliminar: RutinaUsuario? = null,
        val sinConexion: Boolean = false
    ) : EstadoRutinas()

    data class EnCuestionario(
        val paso: Int,
        val datos: DatosCuestionario,
        val sexo: String = "Masculino"
    ) : EstadoRutinas()

    data class Generando(
        val mensaje: String = "Generando tu rutina...",
        val esIA: Boolean = true
    ) : EstadoRutinas()

    data class RutinaActiva(
        val cabecera: RutinaUsuario,
        val dias: List<DiaRutina>,
        val todasLasRutinas: List<RutinaUsuario> = emptyList(),
        val sexo: String = "Masculino",
        val mostrandoDialogoNueva: Boolean = false,
        val nuevaRutinaConIA: Boolean = true,
        val rutinaParaEliminar: RutinaUsuario? = null,
        val ejercicioEditando: EjercicioEditando? = null,
        val sustitucionEnCurso: Pair<Int, Int>? = null,
        val sinConexion: Boolean = false,
        val esRutinaBasica: Boolean = false
    ) : EstadoRutinas()

    data class Error(val mensaje: String) : EstadoRutinas()
}

/**
 * Estado del diálogo de edición de un ejercicio concreto.
 * @param grupoMuscular Grupo muscular actual del ejercicio (para mostrar en el diálogo).
 * @param gruposOcupadosEnDia Grupos ya presentes en ese día por otros ejercicios (excluye el actual).
 */
data class EjercicioEditando(
    val diaNumero: Int,
    val ejercicioIdx: Int,
    val series: Int,
    val repeticiones: String,
    val nombreEjercicio: String,
    val grupoMuscular: String = "",
    val gruposOcupadosEnDia: List<String> = emptyList()
)

data class DatosCuestionario(
    val objetivo: String = "Mantenimiento",
    val diasSemana: Int = 3,
    val musculosPrioritarios: Set<String> = emptySet(),
    val experiencia: String = "Nunca he entrenado",
    val lugarEntrenamiento: String = "Gimnasio grande",
    val materialDisponible: String = "",
    val lugaresSeleccionados: List<String> = emptyList(),
    val lesiones: String = "",
    val generarConIA: Boolean = true
)
