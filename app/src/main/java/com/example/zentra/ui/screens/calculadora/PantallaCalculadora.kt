package com.example.zentra.ui.screens.calculadora

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
 * Pantalla principal del módulo de Calculadora Dietética.
 * Mostrará el anillo de progreso calórico, el desglose de macros y los slots de ingestas del día.
 * TODO: Implementar la UI completa según el diseño de la calculadora dietética inteligente.
 */
@Composable
fun PantallaCalculadora(
    viewModel: CalculadoraViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Módulo de Calculadora Dietética",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaCalculadora() {
    ZentraTheme(temaOscuro = true) {
        PantallaCalculadora()
    }
}
