package com.example.zentra.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zentra.ui.screens.auth.OnboardingViewModel.EstadoOnboarding

/**
 * Wizard de Onboarding de Zentra.
 * Recoge los datos físicos del usuario en 3 pasos para construir su perfil inicial.
 * Los datos recopilados aquí alimentan el cálculo de TMB (Mifflin-St Jeor) y el TDEE.
 *
 * Paso 1: Nombre y Apodo.
 * Paso 2: Sexo y Edad.
 * Paso 3: Altura, Peso y Sistema de medidas.
 *
 * @param onPerfilGuardado Callback invocado cuando el perfil se guarda correctamente en Supabase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaOnboarding(
    onPerfilGuardado: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val paso by viewModel.pasoActual.collectAsState()
    val formulario by viewModel.formulario.collectAsState()
    val estado by viewModel.estado.collectAsState()
    val totalPasos = OnboardingViewModel.TOTAL_PASOS

    LaunchedEffect(estado) {
        if (estado is EstadoOnboarding.Exitoso) onPerfilGuardado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paso ${paso + 1} de $totalPasos") },
                navigationIcon = {
                    if (paso > 0) {
                        IconButton(onClick = { viewModel.retrocederPaso() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al paso anterior")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Barra de progreso del wizard
            LinearProgressIndicator(
                progress = { (paso + 1).toFloat() / totalPasos },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Contenido animado que cambia entre pasos con un deslizamiento
            AnimatedContent(
                targetState = paso,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "animacion_paso_onboarding"
            ) { pasoVisible ->
                when (pasoVisible) {
                    0 -> PasoIdentidad(
                        nombre = formulario.nombre,
                        apodo = formulario.apodo,
                        onNombreChange = viewModel::actualizarNombre,
                        onApodoChange = viewModel::actualizarApodo
                    )
                    1 -> PasoPerfilPersonal(
                        sexo = formulario.sexo,
                        edad = formulario.edad,
                        onSexoChange = viewModel::actualizarSexo,
                        onEdadChange = viewModel::actualizarEdad
                    )
                    2 -> PasoMedidasFisicas(
                        alturaCm = formulario.alturaCm,
                        pesoKg = formulario.pesoKg,
                        sistema = formulario.preferenciaSistema,
                        onAlturaChange = viewModel::actualizarAltura,
                        onPesoChange = viewModel::actualizarPeso,
                        onSistemaChange = viewModel::actualizarSistema
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de error de validación
            val mensajeError = when (val e = estado) {
                is EstadoOnboarding.ErrorValidacion -> e.mensaje
                is EstadoOnboarding.Error -> e.mensaje
                else -> null
            }
            if (mensajeError != null) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botón de acción: Siguiente o Finalizar
            Button(
                onClick = {
                    if (paso < totalPasos - 1) viewModel.avanzarPaso()
                    else viewModel.guardarPerfil()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = estado !is EstadoOnboarding.Cargando
            ) {
                if (estado is EstadoOnboarding.Cargando) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    Text(
                        text = if (paso < totalPasos - 1) "Siguiente" else "Empezar",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Composables internos de cada paso ---

@Composable
private fun PasoIdentidad(
    nombre: String,
    apodo: String,
    onNombreChange: (String) -> Unit,
    onApodoChange: (String) -> Unit
) {
    Column {
        Text(
            text = "¿Cómo te llamamos?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Introduce tu nombre real y el apodo que aparecerá en la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = apodo,
            onValueChange = onApodoChange,
            label = { Text("Apodo") },
            supportingText = { Text("Este nombre se mostrará en tu perfil") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
    }
}

@Composable
private fun PasoPerfilPersonal(
    sexo: String,
    edad: String,
    onSexoChange: (String) -> Unit,
    onEdadChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Tu perfil personal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "El sexo y la edad determinan la fórmula de cálculo de tu metabolismo basal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Sexo biológico",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Selección de sexo con dos botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Masculino", "Femenino").forEach { opcion ->
                val seleccionado = sexo == opcion
                if (seleccionado) {
                    Button(
                        onClick = { onSexoChange(opcion) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(opcion) }
                } else {
                    OutlinedButton(
                        onClick = { onSexoChange(opcion) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(opcion) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = edad,
            onValueChange = { if (it.length <= 3) onEdadChange(it) },
            label = { Text("Edad") },
            suffix = { Text("años") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun PasoMedidasFisicas(
    alturaCm: String,
    pesoKg: String,
    sistema: String,
    onAlturaChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onSistemaChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Tus medidas físicas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Se almacenan siempre en kg y cm. El sistema de medidas solo afecta a cómo se muestran los datos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Sistema de medidas
        Text(
            text = "Sistema de medidas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("metrico" to "Métrico (kg/cm)", "imperial" to "Imperial (lb/ft)").forEach { (valor, etiqueta) ->
                val seleccionado = sistema == valor
                if (seleccionado) {
                    Button(
                        onClick = { onSistemaChange(valor) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(etiqueta, textAlign = TextAlign.Center) }
                } else {
                    OutlinedButton(
                        onClick = { onSistemaChange(valor) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(etiqueta, textAlign = TextAlign.Center) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = alturaCm,
                onValueChange = { if (it.length <= 3) onAlturaChange(it) },
                label = { Text("Altura") },
                suffix = { Text("cm") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = pesoKg,
                onValueChange = { if (it.length <= 5) onPesoChange(it) },
                label = { Text("Peso") },
                suffix = { Text("kg") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
