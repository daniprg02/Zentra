package com.example.zentra.ui.screens.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.domain.repository.IPerfilRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del Splash Screen.
 * Comprueba si existe una sesión activa en memoria al arrancar la app y redirige
 * al usuario al destino correcto sin que tenga que hacer nada.
 *
 * Flujo:
 * - Sin sesión activa → Pantalla de Login.
 * - Con sesión pero sin perfil → Onboarding.
 * - Con sesión y perfil completo → Menú principal (Rutinas).
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio
) : ViewModel() {

    private val _destino = MutableStateFlow<DestinoPantalla?>(null)
    val destino: StateFlow<DestinoPantalla?> = _destino.asStateFlow()

    init {
        verificarSesion()
    }

    private fun verificarSesion() {
        viewModelScope.launch {
            // Pausa mínima para que el splash sea visible
            delay(800)
            try {
                val usuario = supabase.auth.currentUserOrNull()
                if (usuario == null) {
                    Log.d("SplashViewModel", "No hay sesión activa. Redirigiendo al Login.")
                    _destino.value = DestinoPantalla.Login
                    return@launch
                }
                Log.d("SplashViewModel", "Sesión activa para el usuario: ${usuario.id}. Comprobando perfil...")
                val tienePerfil = perfilRepositorio.obtenerPerfil(usuario.id).isSuccess
                _destino.value = if (tienePerfil) {
                    Log.d("SplashViewModel", "Perfil encontrado. Redirigiendo al menú principal.")
                    DestinoPantalla.Principal
                } else {
                    Log.d("SplashViewModel", "Sin perfil completado. Redirigiendo al Onboarding.")
                    DestinoPantalla.Onboarding
                }
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Error al verificar la sesión: ${e.message}")
                _destino.value = DestinoPantalla.Login
            }
        }
    }

    sealed class DestinoPantalla {
        data object Login : DestinoPantalla()
        data object Onboarding : DestinoPantalla()
        data object Principal : DestinoPantalla()
    }
}
