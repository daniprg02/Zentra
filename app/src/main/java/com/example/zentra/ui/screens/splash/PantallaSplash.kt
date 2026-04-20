package com.example.zentra.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zentra.ui.screens.splash.SplashViewModel.DestinoPantalla

/**
 * Pantalla de carga inicial de Zentra.
 * Verifica en segundo plano si el usuario tiene una sesión activa y redirige
 * automáticamente al destino correcto sin interacción del usuario.
 * TODO: Sustituir el nombre de texto por una animación Lottie del logo.
 *
 * @param onNavegacionLogin Callback para ir a la pantalla de login.
 * @param onNavegacionOnboarding Callback para ir al wizard de alta de usuario.
 * @param onNavegacionPrincipal Callback para ir al menú principal.
 */
@Composable
fun PantallaSplash(
    onNavegacionLogin: () -> Unit,
    onNavegacionOnboarding: () -> Unit,
    onNavegacionPrincipal: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destino by viewModel.destino.collectAsState()

    // Navegamos en cuanto el ViewModel determina el destino correcto
    LaunchedEffect(destino) {
        when (destino) {
            is DestinoPantalla.Login -> onNavegacionLogin()
            is DestinoPantalla.Onboarding -> onNavegacionOnboarding()
            is DestinoPantalla.Principal -> onNavegacionPrincipal()
            null -> { /* Cargando, no hacemos nada todavía */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZENTRA",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Entrena. Nutre. Evoluciona.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
