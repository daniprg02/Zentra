package com.example.zentra.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.model.Perfil
import com.example.zentra.domain.repository.IPerfilRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del wizard de Onboarding.
 * Gestiona el estado de los 3 pasos del formulario de alta y persiste el perfil
 * del usuario en Supabase al finalizar. Los datos físicos son la base de todos
 * los cálculos de TMB, TDEE y distribución de macros de la aplicación.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio
) : ViewModel() {

    companion object {
        const val TOTAL_PASOS = 3
    }

    private val _pasoActual = MutableStateFlow(0)
    val pasoActual: StateFlow<Int> = _pasoActual.asStateFlow()

    private val _formulario = MutableStateFlow(FormularioPerfil())
    val formulario: StateFlow<FormularioPerfil> = _formulario.asStateFlow()

    private val _estado = MutableStateFlow<EstadoOnboarding>(EstadoOnboarding.Inactivo)
    val estado: StateFlow<EstadoOnboarding> = _estado.asStateFlow()

    // --- Actualización de campos del formulario ---

    fun actualizarNombre(valor: String) = _formulario.update { it.copy(nombre = valor) }
    fun actualizarApodo(valor: String) = _formulario.update { it.copy(apodo = valor) }
    fun actualizarSexo(valor: String) = _formulario.update { it.copy(sexo = valor) }
    fun actualizarEdad(valor: String) = _formulario.update { it.copy(edad = valor) }
    fun actualizarAltura(valor: String) = _formulario.update { it.copy(alturaCm = valor) }
    fun actualizarPeso(valor: String) = _formulario.update { it.copy(pesoKg = valor) }
    fun actualizarSistema(valor: String) = _formulario.update { it.copy(preferenciaSistema = valor) }

    // --- Navegación entre pasos ---

    /** Avanza al siguiente paso si el actual supera la validación. */
    fun avanzarPaso() {
        val error = validarPasoActual()
        if (error != null) {
            _estado.value = EstadoOnboarding.ErrorValidacion(error)
            return
        }
        _estado.value = EstadoOnboarding.Inactivo
        if (_pasoActual.value < TOTAL_PASOS - 1) _pasoActual.value++
    }

    /** Retrocede al paso anterior. */
    fun retrocederPaso() {
        _estado.value = EstadoOnboarding.Inactivo
        if (_pasoActual.value > 0) _pasoActual.value--
    }

    /**
     * Valida los campos del paso actual antes de avanzar.
     * @return Mensaje de error si hay campos inválidos, null si todo es correcto.
     */
    private fun validarPasoActual(): String? {
        val f = _formulario.value
        return when (_pasoActual.value) {
            0 -> when {
                f.nombre.isBlank() -> "El nombre no puede estar vacío."
                f.apodo.isBlank() -> "El apodo no puede estar vacío."
                else -> null
            }
            1 -> when {
                f.sexo.isBlank() -> "Selecciona tu sexo para continuar."
                f.edad.toIntOrNull() == null || f.edad.toInt() < 10 || f.edad.toInt() > 100 ->
                    "Introduce una edad válida (entre 10 y 100 años)."
                else -> null
            }
            2 -> when {
                f.alturaCm.toIntOrNull() == null || f.alturaCm.toInt() < 100 || f.alturaCm.toInt() > 250 ->
                    "Introduce una altura válida en centímetros (100-250 cm)."
                f.pesoKg.toFloatOrNull() == null || f.pesoKg.toFloat() < 30f || f.pesoKg.toFloat() > 300f ->
                    "Introduce un peso válido en kilogramos (30-300 kg)."
                else -> null
            }
            else -> null
        }
    }

    /**
     * Guarda el perfil del usuario en Supabase al completar el último paso del wizard.
     * Construye el objeto [Perfil] con los datos recopilados y lo persiste.
     */
    fun guardarPerfil() {
        val error = validarPasoActual()
        if (error != null) {
            _estado.value = EstadoOnboarding.ErrorValidacion(error)
            return
        }
        viewModelScope.launch {
            _estado.value = EstadoOnboarding.Cargando
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No hay sesión activa al intentar guardar el perfil")

                val f = _formulario.value
                val perfil = Perfil(
                    id = userId,
                    nombre = f.nombre.trim(),
                    apodo = f.apodo.trim(),
                    edad = f.edad.toInt(),
                    alturaCm = f.alturaCm.toInt(),
                    pesoKg = f.pesoKg.toFloat(),
                    preferenciaSistema = f.preferenciaSistema,
                    sexo = f.sexo,
                    creadoEn = null  // null → Supabase aplica el DEFAULT now() de la columna
                )
                Log.d("OnboardingViewModel", "Guardando perfil para el usuario: $userId")
                perfilRepositorio.guardarPerfil(perfil).getOrThrow()
                Log.d("OnboardingViewModel", "Perfil guardado correctamente.")
                _estado.value = EstadoOnboarding.Exitoso
            } catch (e: Exception) {
                Log.e("OnboardingViewModel", "Error al guardar el perfil del usuario: ${e.message}")
                _estado.value = EstadoOnboarding.Error("No se pudo guardar el perfil. Inténtalo de nuevo.")
            }
        }
    }

    /**
     * Datos del formulario de alta de usuario.
     * Todos los campos numéricos se guardan como String durante la edición para
     * no interrumpir la escritura; se convierten a su tipo real al guardar.
     */
    data class FormularioPerfil(
        val nombre: String = "",
        val apodo: String = "",
        val sexo: String = "",
        val edad: String = "",
        val alturaCm: String = "",
        val pesoKg: String = "",
        val preferenciaSistema: String = "metrico"
    )

    sealed class EstadoOnboarding {
        data object Inactivo : EstadoOnboarding()
        data object Cargando : EstadoOnboarding()
        data object Exitoso : EstadoOnboarding()
        data class ErrorValidacion(val mensaje: String) : EstadoOnboarding()
        data class Error(val mensaje: String) : EstadoOnboarding()
    }
}
