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
import com.example.zentra.ui.screens.auth.PantallaOnboarding
import com.example.zentra.ui.screens.calculadora.PantallaCalculadora
import com.example.zentra.ui.screens.recetas.PantallaRecetas
import com.example.zentra.ui.screens.rutinas.PantallaRutinas
import com.example.zentra.ui.screens.splash.PantallaSplash

/**
 * Grafo de navegación principal de Zentra.
 * Arranca en el Splash, que verifica la sesión y redirige automáticamente.
 * La barra de navegación inferior solo se muestra en los tres módulos principales.
 */
@Composable
fun GrafoNavegacion() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    val rutasPrincipales = listOf(
        Destinos.Rutinas.ruta,
        Destinos.Calculadora.ruta,
        Destinos.Recetas.ruta
    )

    Scaffold(
        bottomBar = {
            if (rutaActual in rutasPrincipales) {
                BarraNavegacionInferior(navController = navController, rutaActual = rutaActual)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destinos.Splash.ruta,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Splash: punto de entrada, comprueba sesión y redirige
            composable(Destinos.Splash.ruta) {
                PantallaSplash(
                    onNavegacionLogin = {
                        navController.navigate(Destinos.Login.ruta) {
                            popUpTo(Destinos.Splash.ruta) { inclusive = true }
                        }
                    },
                    onNavegacionOnboarding = {
                        navController.navigate(Destinos.Onboarding.ruta) {
                            popUpTo(Destinos.Splash.ruta) { inclusive = true }
                        }
                    },
                    onNavegacionPrincipal = {
                        navController.navigate(Destinos.Rutinas.ruta) {
                            popUpTo(Destinos.Splash.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // Login: autenticación con email/contraseña
            composable(Destinos.Login.ruta) {
                PantallaLogin(
                    onLoginConPerfil = {
                        navController.navigate(Destinos.Rutinas.ruta) {
                            popUpTo(Destinos.Login.ruta) { inclusive = true }
                        }
                    },
                    onLoginSinPerfil = {
                        navController.navigate(Destinos.Onboarding.ruta) {
                            popUpTo(Destinos.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding: wizard de alta de datos físicos del usuario
            composable(Destinos.Onboarding.ruta) {
                PantallaOnboarding(
                    onPerfilGuardado = {
                        navController.navigate(Destinos.Rutinas.ruta) {
                            popUpTo(Destinos.Onboarding.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // Menú principal: tres módulos con barra de navegación inferior
            composable(Destinos.Rutinas.ruta) { PantallaRutinas() }
            composable(Destinos.Calculadora.ruta) { PantallaCalculadora() }
            composable(Destinos.Recetas.ruta) { PantallaRecetas() }
        }
    }
}

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
                icon = { Icon(elemento.icono, contentDescription = elemento.etiqueta) },
                label = { Text(elemento.etiqueta) },
                selected = rutaActual == elemento.ruta,
                onClick = {
                    if (rutaActual != elemento.ruta) {
                        navController.navigate(elemento.ruta) {
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

private data class ElementoNavegacion(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector
)
