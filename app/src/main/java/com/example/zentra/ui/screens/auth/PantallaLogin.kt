package com.example.zentra.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zentra.ui.theme.ZentraTheme

/**
 * Pantalla de autenticación de Zentra.
 * Muestra el logo de la app, un acceso principal con Google y una opción secundaria con email.
 * TODO: Integrar animación Lottie en la cabecera (splash/logo animado).
 * @param onLoginExitoso Callback invocado cuando la autenticación se completa con éxito.
 */
@Composable
fun PantallaLogin(
    onLoginExitoso: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val estado by viewModel.estadoLogin.collectAsState()

    // Navegamos al menú principal en cuanto el login es exitoso
    LaunchedEffect(estado) {
        if (estado is LoginViewModel.EstadoLogin.Exitoso) {
            onLoginExitoso()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nombre de la aplicación como placeholder del logo/Lottie
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

        Spacer(modifier = Modifier.height(72.dp))

        // Botón principal de acceso: Google OAuth
        Button(
            onClick = { viewModel.iniciarSesionConGoogle() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = estado !is LoginViewModel.EstadoLogin.Cargando
        ) {
            Text(
                text = "Continuar con Google",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón secundario: Email/Contraseña
        OutlinedButton(
            onClick = { viewModel.iniciarSesionConEmail("test@zentra.app", "temporal") },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = estado !is LoginViewModel.EstadoLogin.Cargando
        ) {
            Text(
                text = "Entrar con Email",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Indicadores de estado de la operación
        when (val estadoActual = estado) {
            is LoginViewModel.EstadoLogin.Cargando -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is LoginViewModel.EstadoLogin.Error -> {
                Text(
                    text = estadoActual.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaLogin() {
    ZentraTheme(temaOscuro = true) {
        PantallaLogin(onLoginExitoso = {})
    }
}
