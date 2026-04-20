package com.example.zentra.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de autenticación.
 * Gestiona los flujos de login con Google y con email/contraseña mediante Supabase Auth.
 * Expone el estado de la operación para que la UI pueda reaccionar de forma reactiva.
 */
@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _estadoLogin = MutableStateFlow<EstadoLogin>(EstadoLogin.Inactivo)
    val estadoLogin: StateFlow<EstadoLogin> = _estadoLogin.asStateFlow()

    /**
     * Inicia el flujo de autenticación OAuth con Google vía Supabase Auth.
     * TODO: Implementar cuando se configure el proveedor Google en el panel de Supabase.
     */
    fun iniciarSesionConGoogle() {
        viewModelScope.launch {
            _estadoLogin.value = EstadoLogin.Cargando
            Log.d("LoginViewModel", "Iniciando flujo de autenticación OAuth con Google...")
            // TODO: supabase.auth.signInWith(Google)
            _estadoLogin.value = EstadoLogin.Exitoso
        }
    }

    /**
     * Inicia sesión con credenciales de email y contraseña mediante Supabase Auth.
     * @param email Correo electrónico del usuario.
     * @param contrasena Contraseña del usuario.
     * TODO: Implementar cuando se configure la autenticación por email en Supabase.
     */
    fun iniciarSesionConEmail(email: String, contrasena: String) {
        viewModelScope.launch {
            _estadoLogin.value = EstadoLogin.Cargando
            Log.d("LoginViewModel", "Iniciando sesión con email: $email")
            // TODO: supabase.auth.signInWith(Email) { this.email = email; password = contrasena }
            _estadoLogin.value = EstadoLogin.Exitoso
        }
    }

    /** Representa los posibles estados de la operación de autenticación. */
    sealed class EstadoLogin {
        data object Inactivo : EstadoLogin()
        data object Cargando : EstadoLogin()
        data object Exitoso : EstadoLogin()
        data class Error(val mensaje: String) : EstadoLogin()
    }
}
