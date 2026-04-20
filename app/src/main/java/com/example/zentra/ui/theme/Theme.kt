package com.example.zentra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema de colores para modo oscuro. Es el preferido en una app deportiva de uso diario.
private val EsquemaColorOscuro = darkColorScheme(
    primary = ZentraAzulPrimarioClaro,
    onPrimary = Color(0xFF003580),
    primaryContainer = ZentraAzulContenedorOscuro,
    onPrimaryContainer = ZentraAzulContenedor,
    secondary = ZentraTealClaro,
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF004D45),
    onSecondaryContainer = Color(0xFFA7F3EB),
    tertiary = ZentraEnergiaClaro,
    onTertiary = Color(0xFF4A1900),
    background = ZentraFondoOscuro,
    onBackground = ZentraTextoSecundario,
    surface = ZentraSuperficieOscura,
    onSurface = ZentraTextoSecundario,
    surfaceVariant = ZentraSuperficieOscura2,
    onSurfaceVariant = Color(0xFFCACADE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF8E8EA8)
)

// Esquema de colores para modo claro.
private val EsquemaColorClaro = lightColorScheme(
    primary = ZentraAzulPrimario,
    onPrimary = ZentraBlanco,
    primaryContainer = ZentraAzulContenedor,
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = ZentraTeal,
    onSecondary = ZentraBlanco,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = ZentraEnergia,
    onTertiary = ZentraBlanco,
    background = Color(0xFFF8F9FF),
    onBackground = ZentraTextoOscuro,
    surface = ZentraBlanco,
    onSurface = ZentraTextoOscuro,
    surfaceVariant = Color(0xFFE8E8F4),
    onSurfaceVariant = Color(0xFF44445C),
    error = Color(0xFFBA1A1A),
    onError = ZentraBlanco,
    outline = Color(0xFF767690)
)

/**
 * Tema global de Zentra.
 * Se desactivan los colores dinámicos de Android 12+ para preservar la identidad visual de la marca.
 * @param temaOscuro Usa el esquema oscuro si es verdadero. Por defecto sigue la configuración del sistema.
 */
@Composable
fun ZentraTheme(
    temaOscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val esquemaColor = if (temaOscuro) EsquemaColorOscuro else EsquemaColorClaro

    MaterialTheme(
        colorScheme = esquemaColor,
        typography = Tipografia,
        content = content
    )
}
