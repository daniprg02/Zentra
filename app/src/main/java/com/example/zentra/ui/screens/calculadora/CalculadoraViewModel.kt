package com.example.zentra.ui.screens.calculadora

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.model.MacrosDiarios
import com.example.zentra.domain.model.NivelActividad
import com.example.zentra.domain.model.ObjetivoFisico
import com.example.zentra.domain.model.Perfil
import com.example.zentra.domain.repository.IMacrosRepositorio
import com.example.zentra.domain.repository.IPerfilRepositorio
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
 * - Leer y crear el registro diario de ingestas en la tabla `daily_macros`.
 * - Recalcular en tiempo real cuando el usuario cambia el nivel de actividad o el objetivo físico.
 */
@HiltViewModel
class CalculadoraViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio,
    private val macrosRepositorio: IMacrosRepositorio
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoCalculadora())
    val estado: StateFlow<EstadoCalculadora> = _estado.asStateFlow()

    init {
        Log.d("CalculadoraViewModel", "ViewModel inicializado. Cargando datos nutricionales del día.")
        cargarDatosDelDia()
    }

    /**
     * Carga o recalcula todos los datos de la pantalla principal de la calculadora.
     * Obtiene el perfil del usuario, calcula los objetivos con la configuración actual
     * y lee (o crea si no existe) el registro nutricional de hoy en Supabase.
     */
    fun cargarDatosDelDia() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = null)
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa. Reinicia la aplicación.")

                val perfil = perfilRepositorio.obtenerPerfil(userId).getOrThrow()
                Log.d("CalculadoraViewModel", "Perfil cargado: ${perfil.apodo} | ${perfil.pesoKg}kg, ${perfil.alturaCm}cm, ${perfil.edad}a, ${perfil.sexo}")

                // Los objetivos siempre se recalculan frescos; el valor guardado en BD puede
                // quedar obsoleto si el usuario cambia su perfil físico o su configuración.
                val objetivos = calcularObjetivosNutricionales(
                    perfil = perfil,
                    nivel = _estado.value.nivelActividad,
                    objetivo = _estado.value.objetivo
                )

                val hoy = LocalDate.now().toString()
                val macrosGuardados = macrosRepositorio.obtenerMacrosDelDia(userId, hoy).getOrNull()

                if (macrosGuardados == null) {
                    // Primera apertura del día: inicializamos la fila en Supabase con valores a cero.
                    Log.d("CalculadoraViewModel", "Sin registro nutricional para $hoy. Creando fila inicial en daily_macros.")
                    val registroInicial = MacrosDiarios(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        fecha = hoy,
                        objetivoKcal = objetivos.objetivoKcal,
                        consumidoKcal = 0,
                        consumidoProteinasG = 0f,
                        consumidoCarbosG = 0f,
                        consumidoGrasasG = 0f
                    )
                    macrosRepositorio.guardarMacrosDelDia(registroInicial).onFailure { e ->
                        Log.e("CalculadoraViewModel", "No se pudo crear el registro inicial en Supabase: ${e.message}")
                    }
                }

                _estado.value = _estado.value.copy(
                    cargando = false,
                    apodo = perfil.apodo,
                    objetivoKcal = objetivos.objetivoKcal,
                    objetivoProteinasG = objetivos.proteinasG,
                    objetivoCarbosG = objetivos.carbosG,
                    objetivoGrasasG = objetivos.grasasG,
                    consumidoKcal = macrosGuardados?.consumidoKcal ?: 0,
                    consumidoProteinasG = macrosGuardados?.consumidoProteinasG ?: 0f,
                    consumidoCarbosG = macrosGuardados?.consumidoCarbosG ?: 0f,
                    consumidoGrasasG = macrosGuardados?.consumidoGrasasG ?: 0f
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

    /**
     * Actualiza el nivel de actividad seleccionado y recalcula los objetivos nutricionales.
     * @param nivel Nuevo nivel de actividad elegido por el usuario.
     */
    fun cambiarNivelActividad(nivel: NivelActividad) {
        Log.d("CalculadoraViewModel", "Nivel de actividad cambiado a: ${nivel.etiqueta} (factor ${nivel.factor})")
        _estado.value = _estado.value.copy(nivelActividad = nivel)
        cargarDatosDelDia()
    }

    /**
     * Actualiza el objetivo físico seleccionado y recalcula los objetivos nutricionales.
     * @param objetivo Nuevo objetivo físico elegido por el usuario.
     */
    fun cambiarObjetivo(objetivo: ObjetivoFisico) {
        Log.d("CalculadoraViewModel", "Objetivo físico cambiado a: ${objetivo.etiqueta} (modificador ${objetivo.modificador})")
        _estado.value = _estado.value.copy(objetivo = objetivo)
        cargarDatosDelDia()
    }

    /**
     * Calcula el objetivo calórico diario y la distribución de macronutrientes.
     *
     * Proceso de cálculo:
     * 1. TMB (Tasa Metabólica Basal) → Fórmula Mifflin-St Jeor diferenciada por sexo.
     * 2. TDEE (Gasto Energético Total Diario) → TMB × factor de actividad.
     * 3. Objetivo calórico → TDEE ajustado según el modificador del objetivo físico.
     * 4. Macros → 2g proteína/kg, 1g grasa/kg; el resto de calorías se destina a carbohidratos.
     *
     * @param perfil Datos físicos del usuario almacenados en la tabla `profiles`.
     * @param nivel Factor de actividad seleccionado por el usuario.
     * @param objetivo Modificador calórico según el objetivo físico del usuario.
     * @return [ResultadoCalculoNutricional] con las kcal y gramos objetivo de cada macronutriente.
     */
    private fun calcularObjetivosNutricionales(
        perfil: Perfil,
        nivel: NivelActividad,
        objetivo: ObjetivoFisico
    ): ResultadoCalculoNutricional {
        // Paso 1: TMB con Mifflin-St Jeor
        val tmb = if (perfil.sexo == "Masculino") {
            10f * perfil.pesoKg + 6.25f * perfil.alturaCm - 5f * perfil.edad + 5f
        } else {
            10f * perfil.pesoKg + 6.25f * perfil.alturaCm - 5f * perfil.edad - 161f
        }

        // Paso 2: TDEE aplicando el factor de actividad física
        val tdee = tmb * nivel.factor

        // Paso 3: Ajuste calórico según el objetivo físico del usuario
        val objetivoKcal = (tdee * (1f + objetivo.modificador)).toInt()

        // Paso 4: Distribución de macronutrientes prioritaria por masa muscular
        val proteinasG = perfil.pesoKg * 2f           // 2g/kg: preserva o construye músculo
        val grasasG = perfil.pesoKg * 1f              // 1g/kg: función hormonal y articular
        val kcalFijas = (proteinasG * 4f) + (grasasG * 9f)
        val carbosG = ((objetivoKcal - kcalFijas) / 4f).coerceAtLeast(0f)

        Log.d(
            "CalculadoraViewModel",
            "Cálculo: TMB=${"%.0f".format(tmb)} → TDEE=${"%.0f".format(tdee)} → Objetivo=$objetivoKcal kcal | " +
            "P:${"%.0f".format(proteinasG)}g | C:${"%.0f".format(carbosG)}g | G:${"%.0f".format(grasasG)}g"
        )

        return ResultadoCalculoNutricional(
            objetivoKcal = objetivoKcal,
            proteinasG = proteinasG,
            carbosG = carbosG,
            grasasG = grasasG
        )
    }

    /** Contenedor interno para el resultado del cálculo nutricional. */
    private data class ResultadoCalculoNutricional(
        val objetivoKcal: Int,
        val proteinasG: Float,
        val carbosG: Float,
        val grasasG: Float
    )
}

/**
 * Estado de la UI de la pantalla de Calculadora Dietética.
 * Todos los valores numéricos están en unidades del sistema métrico (kcal y gramos).
 */
data class EstadoCalculadora(
    val cargando: Boolean = true,
    val error: String? = null,
    val apodo: String = "",
    val nivelActividad: NivelActividad = NivelActividad.MODERADO,
    val objetivo: ObjetivoFisico = ObjetivoFisico.MANTENIMIENTO,
    val objetivoKcal: Int = 0,
    val objetivoProteinasG: Float = 0f,
    val objetivoCarbosG: Float = 0f,
    val objetivoGrasasG: Float = 0f,
    val consumidoKcal: Int = 0,
    val consumidoProteinasG: Float = 0f,
    val consumidoCarbosG: Float = 0f,
    val consumidoGrasasG: Float = 0f
)
