package com.example.zentra.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zentra.ui.screens.auth.LoginViewModel.EstadoLogin
import com.example.zentra.ui.screens.auth.LoginViewModel.ModoLogin

/**
 * Pantalla de autenticación de Zentra.
 * Permite al usuario iniciar sesión o registrarse con email y contraseña.
 * Alterna entre ambos modos con un botón inferior sin necesidad de navegar a otra pantalla.
 *
 * @param onLoginConPerfil Callback cuando el usuario se autentica y ya tiene perfil completo.
 * @param onLoginSinPerfil Callback cuando el usuario se autentica pero aún no tiene perfil.
 */
@Composable
fun PantallaLogin(
    onLoginConPerfil: () -> Unit,
    onLoginSinPerfil: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val modo by viewModel.modo.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }

    // Navegamos al destino correcto cuando la autenticación es exitosa
    LaunchedEffect(estado) {
        when (estado) {
            is EstadoLogin.ExitosoConPerfil -> onLoginConPerfil()
            is EstadoLogin.ExitosoSinPerfil -> onLoginSinPerfil()
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cabecera con el nombre de la app
        Text(
            text = "ZENTRA",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (modo == ModoLogin.INICIAR_SESION) "Bienvenido de nuevo" else "Crea tu cuenta",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Campo: Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = estado !is EstadoLogin.Cargando
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo: Contraseña
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                    Icon(
                        imageVector = if (contrasenaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (modo == ModoLogin.INICIAR_SESION) viewModel.iniciarSesion(email, contrasena)
                    else viewModel.registrarse(email, contrasena)
                }
            ),
            enabled = estado !is EstadoLogin.Cargando
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón principal de acción
        Button(
            onClick = {
                focusManager.clearFocus()
                if (modo == ModoLogin.INICIAR_SESION) viewModel.iniciarSesion(email, contrasena)
                else viewModel.registrarse(email, contrasena)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = estado !is EstadoLogin.Cargando
        ) {
            if (estado is EstadoLogin.Cargando) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (modo == ModoLogin.INICIAR_SESION) "Iniciar sesión" else "Registrarse",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Mensaje de error
        if (estado is EstadoLogin.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (estado as EstadoLogin.Error).mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enlace para alternar entre login y registro
        TextButton(onClick = { viewModel.toggleModo() }) {
            Text(
                text = if (modo == ModoLogin.INICIAR_SESION)
                    "¿No tienes cuenta? Regístrate"
                else
                    "¿Ya tienes cuenta? Inicia sesión",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
