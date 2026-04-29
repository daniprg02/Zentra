package com.example.zentra.ui.screens.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zentra.data.local.ZentraCacheManager
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
 *
 * Flujo de decisión:
 * 1. Sesión activa + perfil en BD       → Principal
 * 2. Sesión activa + perfil en caché    → Principal (sin conexión)
 * 3. Sin sesión + perfil en caché       → Principal (token expirado, acceso offline)
 * 4. Sin sesión + sin caché             → Login
 * 5. Sesión activa pero sin perfil      → Onboarding
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val perfilRepositorio: IPerfilRepositorio,
    private val cacheManager: ZentraCacheManager
) : ViewModel() {

    private val _destino = MutableStateFlow<DestinoPantalla?>(null)
    val destino: StateFlow<DestinoPantalla?> = _destino.asStateFlow()

    init {
        verificarSesion()
    }

    private fun verificarSesion() {
        viewModelScope.launch {
            delay(800)
            try {
                val usuario = supabase.auth.currentUserOrNull()

                if (usuario == null) {
                    // Sin sesión en memoria → comprobar si hay perfil cacheado para acceso offline
                    val perfilCacheado = cacheManager.cargarPerfil()
                    _destino.value = if (perfilCacheado != null) {
                        Log.d("SplashViewModel", "Sin sesión activa pero perfil cacheado encontrado. Acceso offline.")
                        DestinoPantalla.Principal
                    } else {
                        Log.d("SplashViewModel", "Sin sesión ni caché. Redirigiendo al Login.")
                        DestinoPantalla.Login
                    }
                    return@launch
                }

                Log.d("SplashViewModel", "Sesión activa: ${usuario.id}. Verificando perfil...")

                // Comprobar perfil online; si falla (sin red), usar caché
                var errorDeRed = false
                val tienePerfil = try {
                    val resultado = perfilRepositorio.obtenerPerfil(usuario.id).isSuccess
                    Log.d("SplashViewModel", "Verificación online del perfil: $resultado")
                    resultado
                } catch (e: Exception) {
                    Log.w("SplashViewModel", "No se pudo verificar perfil en red. Comprobando caché.")
                    errorDeRed = true
                    cacheManager.cargarPerfil() != null
                }

                _destino.value = when {
                    tienePerfil -> {
                        Log.d("SplashViewModel", "Perfil encontrado. Redirigiendo al menú principal.")
                        DestinoPantalla.Principal
                    }
                    errorDeRed -> {
                        // Error de red y sin caché: no redirigir al Onboarding para evitar re-registro accidental
                        Log.d("SplashViewModel", "Error de red sin caché. Redirigiendo al Login.")
                        DestinoPantalla.Login
                    }
                    else -> {
                        Log.d("SplashViewModel", "Sin perfil completado. Redirigiendo al Onboarding.")
                        DestinoPantalla.Onboarding
                    }
                }
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Error al verificar la sesión: ${e.message}")
                // Ante cualquier error de red, si hay perfil cacheado → acceso offline
                val perfilCacheado = cacheManager.cargarPerfil()
                _destino.value = if (perfilCacheado != null) {
                    Log.d("SplashViewModel", "Error de red. Usando caché para acceso offline.")
                    DestinoPantalla.Principal
                } else {
                    DestinoPantalla.Login
                }
            }
        }
    }

    sealed class DestinoPantalla {
        data object Login : DestinoPantalla()
        data object Onboarding : DestinoPantalla()
        data object Principal : DestinoPantalla()
    }
}
