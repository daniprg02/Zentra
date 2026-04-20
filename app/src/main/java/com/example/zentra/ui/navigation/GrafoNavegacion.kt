package com.example.zentra.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zentra.ui.screens.auth.PantallaLogin
import com.example.zentra.ui.screens.calculadora.PantallaCalculadora
import com.example.zentra.ui.screens.recetas.PantallaRecetas
import com.example.zentra.ui.screens.rutinas.PantallaRutinas

/**
 * Define el grafo de navegación principal de la aplicación.
 * Gestiona la transición entre la pantalla de login y las tres secciones principales,
 * mostrando la barra de navegación inferior únicamente en las pantallas del menú principal.
 */
@Composable
fun GrafoNavegacion() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // Rutas en las que se muestra la barra de navegación inferior
    val rutasPrincipales = listOf(
        Destinos.Rutinas.ruta,
        Destinos.Calculadora.ruta,
        Destinos.Recetas.ruta
    )

    Scaffold(
        bottomBar = {
            if (rutaActual in rutasPrincipales) {
                BarraNavegacionInferior(
                    navController = navController,
                    rutaActual = rutaActual
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destinos.Login.ruta,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destinos.Login.ruta) {
                PantallaLogin(
                    onLoginExitoso = {
                        // Al autenticarse, navegamos al módulo principal eliminando el login del stack
                        navController.navigate(Destinos.Rutinas.ruta) {
                            popUpTo(Destinos.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destinos.Rutinas.ruta) {
                PantallaRutinas()
            }
            composable(Destinos.Calculadora.ruta) {
                PantallaCalculadora()
            }
            composable(Destinos.Recetas.ruta) {
                PantallaRecetas()
            }
        }
    }
}

/**
 * Barra de navegación inferior con los tres módulos principales de Zentra.
 * @param navController Controlador de navegación para gestionar los saltos entre tabs.
 * @param rutaActual Ruta en pantalla para marcar el tab seleccionado.
 */
@Composable
private fun BarraNavegacionInferior(
    navController: NavController,
    rutaActual: String?
) {
    val elementos = listOf(
        ElementoNavegacion(Destinos.Rutinas.ruta, "Rutinas", Icons.Default.FitnessCenter),
        ElementoNavegacion(Destinos.Calculadora.ruta, "Calculadora", Icons.Default.BarChart),
        ElementoNavegacion(Destinos.Recetas.ruta, "Recetas", Icons.Default.MenuBook)
    )

    NavigationBar {
        elementos.forEach { elemento ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = elemento.icono,
                        contentDescription = elemento.etiqueta
                    )
                },
                label = { Text(text = elemento.etiqueta) },
                selected = rutaActual == elemento.ruta,
                onClick = {
                    if (rutaActual != elemento.ruta) {
                        navController.navigate(elemento.ruta) {
                            // Mantenemos un único elemento en el back stack del tab raíz
                            popUpTo(Destinos.Rutinas.ruta) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Modelo interno que representa un elemento de la barra de navegación inferior.
 */
private data class ElementoNavegacion(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector
)
