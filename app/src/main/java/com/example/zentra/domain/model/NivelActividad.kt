package com.example.zentra.domain.model

/**
 * Niveles de actividad física para el cálculo del TDEE (Gasto Energético Total Diario).
 * Cada nivel aplica un factor multiplicador sobre la TMB según los estándares de
 * la ecuación de Harris-Benedict revisada.
 *
 * @param factor Multiplicador aplicado sobre la TMB para estimar el gasto real diario.
 * @param etiqueta Texto descriptivo para mostrar al usuario en la interfaz.
 */
enum class NivelActividad(val factor: Float, val etiqueta: String) {
    SEDENTARIO(1.2f, "Sedentario"),
    LIGERO(1.375f, "Poco activo"),
    MODERADO(1.55f, "Moderado"),
    ACTIVO(1.725f, "Muy activo"),
    ATLETA(1.9f, "Atleta")
}
