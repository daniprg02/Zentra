package com.example.zentra.ui.screens.rutinas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zentra.ui.theme.ZentraTheme

/**
 * Pantalla principal del módulo de Rutinas.
 * Mostrará el plan de entrenamiento activo del usuario y permitirá generar nuevas rutinas
 * personalizadas mediante el cuestionario de parámetros físicos.
 * TODO: Implementar la UI completa según el diseño del módulo de rutinas.
 */
@Composable
fun PantallaRutinas(
    viewModel: RutinasViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Módulo de Rutinas",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaRutinas() {
    ZentraTheme(temaOscuro = true) {
        PantallaRutinas()
    }
}
