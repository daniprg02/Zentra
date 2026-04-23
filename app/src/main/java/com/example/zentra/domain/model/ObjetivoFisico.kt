package com.example.zentra.domain.model

/**
 * Objetivos físicos del usuario que determinan el ajuste calórico sobre el TDEE.
 * El modificador se aplica como factor porcentual sobre el gasto energético total calculado.
 *
 * @param modificador Ajuste porcentual: -0.15 para déficit, 0 para mantenimiento, +0.10 para superávit.
 * @param etiqueta Texto descriptivo para los selectores de la interfaz.
 */
enum class ObjetivoFisico(val modificador: Float, val etiqueta: String) {
    DEFICIT(-0.15f, "Déficit"),
    MANTENIMIENTO(0f, "Mantenim."),
    SUPERAVIT(0.10f, "Superávit")
}
