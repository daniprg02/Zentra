package com.example.zentra.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.repository.IPerfilRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de autenticación.
 * Gestiona los flujos de inicio de sesión y registro mediante Supabase Auth (Email/Contraseña).
 * Tras autenticar, comprueba si el usuario ya tiene perfil para redirigirlo correctamente.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio,
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("zentra_credenciales", Context.MODE_PRIVATE)

    val emailGuardado: String get() = prefs.getString("email", "") ?: ""
    val contrasenaGuardada: String get() = prefs.getString("contrasena", "") ?: ""
    val recordarGuardado: Boolean get() = prefs.getBoolean("recordar", false)

    private val _estado = MutableStateFlow<EstadoLogin>(EstadoLogin.Inactivo)
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    // Controla si el formulario está en modo "Iniciar sesión" o "Registrarse"
    private val _modo = MutableStateFlow(ModoLogin.INICIAR_SESION)
    val modo: StateFlow<ModoLogin> = _modo.asStateFlow()

    /** Alterna entre los modos de login y registro, limpiando el estado de error previo. */
    fun toggleModo() {
        _modo.value = if (_modo.value == ModoLogin.INICIAR_SESION) ModoLogin.REGISTRARSE else ModoLogin.INICIAR_SESION
        _estado.value = EstadoLogin.Inactivo
    }

    /**
     * Autentica al usuario con email y contraseña mediante Supabase Auth.
     * Si el login es correcto, comprueba si el usuario ya tiene perfil creado.
     * @param email Correo electrónico del usuario.
     * @param contrasena Contraseña del usuario.
     */
    fun iniciarSesion(email: String, contrasena: String, recordar: Boolean = false) {
        if (!validarCampos(email, contrasena)) return
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = contrasena
                }
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No se pudo recuperar el ID del usuario tras el login")
                Log.d("LoginViewModel", "Login correcto para el usuario: $userId")

                if (recordar) {
                    prefs.edit().putString("email", email.trim()).putString("contrasena", contrasena).putBoolean("recordar", true).apply()
                } else {
                    prefs.edit().remove("email").remove("contrasena").putBoolean("recordar", false).apply()
                }

                val tienePerfil = perfilRepositorio.obtenerPerfil(userId).isSuccess
                _estado.value = if (tienePerfil) EstadoLogin.ExitosoConPerfil else EstadoLogin.ExitosoSinPerfil
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al iniciar sesión: ${e.message}")
                _estado.value = EstadoLogin.Error(traducirError(e))
            }
        }
    }

    /** Limpia cualquier mensaje de error o confirmación cuando el usuario empieza a escribir. */
    fun limpiarMensaje() {
        if (_estado.value is EstadoLogin.Error || _estado.value is EstadoLogin.ResetEnviado) {
            _estado.value = EstadoLogin.Inactivo
        }
    }

    /**
     * Registra un nuevo usuario con email y contraseña mediante Supabase Auth.
     * Tras el registro, el usuario siempre va al Onboarding para completar su perfil físico.
     * NOTA: Asegúrate de tener desactivada la confirmación de email en el panel de Supabase
     * (Authentication → Settings → Disable email confirmations) para desarrollo.
     * @param email Correo electrónico del nuevo usuario.
     * @param contrasena Contraseña elegida (mínimo 6 caracteres, requisito de Supabase).
     */
    fun registrarse(email: String, contrasena: String) {
        if (!validarCampos(email, contrasena)) return
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = contrasena
                }

                // signUpWith crea la cuenta pero, si la confirmación de email está habilitada
                // en Supabase, NO establece una sesión activa en el cliente.
                // Para garantizar que hay sesión antes de ir al Onboarding, hacemos signIn
                // explícito si currentUserOrNull() sigue siendo null.
                if (supabase.auth.currentUserOrNull() == null) {
                    Log.d("LoginViewModel", "signUpWith completado sin sesión. Intentando signIn explícito...")
                    supabase.auth.signInWith(Email) {
                        this.email = email.trim()
                        this.password = contrasena
                    }
                }

                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("No se pudo establecer la sesión tras el registro. Comprueba si la confirmación de email está desactivada en Supabase.")
                Log.d("LoginViewModel", "Registro y sesión establecidos correctamente para: $userId")
                _estado.value = EstadoLogin.ExitosoSinPerfil
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al registrarse: ${e.message}")
                _estado.value = EstadoLogin.Error(traducirError(e))
            }
        }
    }

    /**
     * Valida los campos del formulario antes de lanzar la petición a Supabase.
     * Establece el estado de error si algún campo no cumple los requisitos.
     */
    private fun validarCampos(email: String, contrasena: String): Boolean {
        return when {
            email.isBlank() -> {
                _estado.value = EstadoLogin.Error("El email no puede estar vacío.")
                false
            }
            !email.contains("@") -> {
                _estado.value = EstadoLogin.Error("Introduce un email válido.")
                false
            }
            contrasena.length < 6 -> {
                _estado.value = EstadoLogin.Error("La contraseña debe tener al menos 6 caracteres.")
                false
            }
            else -> true
        }
    }

    /**
     * Envía un email de recuperación de contraseña a la dirección indicada.
     * Muestra un error si el campo está vacío; de lo contrario, transiciona a ResetEnviado.
     */
    fun resetContrasena(email: String) {
        if (email.isBlank()) {
            _estado.value = EstadoLogin.Error("Introduce tu email para recuperar la contraseña.")
            return
        }
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            try {
                supabase.auth.resetPasswordForEmail(email.trim())
                Log.d("LoginViewModel", "Email de recuperación enviado a: ${email.trim()}")
                _estado.value = EstadoLogin.ResetEnviado
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al enviar reset de contraseña: ${e.message}")
                _estado.value = EstadoLogin.Error("No se pudo enviar el correo. Inténtalo de nuevo.")
            }
        }
    }

    /** Convierte las excepciones de Supabase en mensajes comprensibles para el usuario. */
    private fun traducirError(e: Exception): String = when {
        e.message?.contains("Invalid login credentials") == true -> "Email o contraseña incorrectos."
        e.message?.contains("already registered") == true -> "Este email ya tiene una cuenta."
        e.message?.contains("Unable to connect") == true ||
        e.message?.contains("network") == true -> "Sin conexión. Comprueba tu internet."
        else -> "Error inesperado. Inténtalo de nuevo."
    }

    /** Indica si el formulario está en modo login o registro. */
    enum class ModoLogin { INICIAR_SESION, REGISTRARSE }

    sealed class EstadoLogin {
        data object Inactivo : EstadoLogin()
        data object Cargando : EstadoLogin()
        /** Login correcto y el usuario ya tiene perfil físico completado. */
        data object ExitosoConPerfil : EstadoLogin()
        /** Login/registro correcto pero el usuario aún no ha completado el Onboarding. */
        data object ExitosoSinPerfil : EstadoLogin()
        /** Email de recuperación de contraseña enviado correctamente. */
        data object ResetEnviado : EstadoLogin()
        data class Error(val mensaje: String) : EstadoLogin()
    }
}
