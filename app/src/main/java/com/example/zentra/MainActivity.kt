package com.example.zentra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.zentra.ui.navigation.GrafoNavegacion
import com.example.zentra.ui.theme.ZentraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal y único punto de entrada de la interfaz de usuario.
 * La anotación @AndroidEntryPoint habilita la inyección de dependencias de Hilt en esta Activity.
 * Toda la lógica de navegación se delega al composable [GrafoNavegacion].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZentraTheme {
                GrafoNavegacion()
            }
        }
    }
}
