package com.example.zentra.ui.screens.recetas

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
 * Pantalla principal del módulo de Gestor de Recetas.
 * Mostrará la biblioteca de recetas del usuario, la barra de búsqueda y el carrusel
 * de favoritas, junto con el FAB para añadir nuevas recetas.
 * TODO: Implementar la UI completa según el diseño del gestor de recetas.
 */
@Composable
fun PantallaRecetas(
    viewModel: RecetasViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Módulo de Recetas",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaRecetas() {
    ZentraTheme(temaOscuro = true) {
        PantallaRecetas()
    }
}
