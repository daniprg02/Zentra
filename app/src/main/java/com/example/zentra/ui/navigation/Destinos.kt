package com.example.zentra.ui.navigation

/**
 * Contrato de rutas de navegación de Zentra.
 * Centraliza todas las rutas en un único lugar para evitar cadenas de texto dispersas.
 * Cada objeto representa una pantalla distinta en el grafo de navegación.
 */
sealed class Destinos(val ruta: String) {
    data object Login : Destinos("login")
    data object Rutinas : Destinos("rutinas")
    data object Calculadora : Destinos("calculadora")
    data object Recetas : Destinos("recetas")
}
