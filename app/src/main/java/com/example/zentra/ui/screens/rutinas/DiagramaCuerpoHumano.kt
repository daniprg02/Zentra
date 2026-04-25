package com.example.zentra.ui.screens.rutinas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zentra.R
import kotlinx.coroutines.launch

/**
 * Colores identificativos por grupo muscular. Compartidos con la leyenda del diagrama.
 */
val COLORES_MUSCULARES: Map<String, Color> = mapOf(
    "Pecho"          to Color(0xFFEF5350),
    "Espalda"        to Color(0xFF42A5F5),
    "Hombros"        to Color(0xFFFFB300),
    "Bíceps"         to Color(0xFFAB47BC),
    "Tríceps"        to Color(0xFF5C6BC0),
    "Core"           to Color(0xFFFDD835),
    "Cuádriceps"     to Color(0xFF66BB6A),
    "Isquiotibiales" to Color(0xFF26A69A),
    "Glúteos"        to Color(0xFFEC407A),
    "Gemelos"        to Color(0xFF26C6DA)
)

// Posiciones de los puntos musculares como fracción (x, y) del contenedor, vista frontal.
private val POSICIONES_FRENTE: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"    to listOf(0.27f to 0.21f, 0.73f to 0.21f),
    "Pecho"      to listOf(0.50f to 0.30f),
    "Bíceps"     to listOf(0.19f to 0.38f, 0.81f to 0.38f),
    "Core"       to listOf(0.50f to 0.46f),
    "Cuádriceps" to listOf(0.37f to 0.64f, 0.63f to 0.64f),
    "Gemelos"    to listOf(0.37f to 0.83f, 0.63f to 0.83f)
)

// Posiciones de los puntos musculares, vista trasera.
private val POSICIONES_ESPALDA: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"        to listOf(0.27f to 0.21f, 0.73f to 0.21f),
    "Espalda"        to listOf(0.50f to 0.34f),
    "Tríceps"        to listOf(0.19f to 0.38f, 0.81f to 0.38f),
    "Glúteos"        to listOf(0.40f to 0.56f, 0.60f to 0.56f),
    "Isquiotibiales" to listOf(0.37f to 0.67f, 0.63f to 0.67f),
    "Gemelos"        to listOf(0.37f to 0.83f, 0.63f to 0.83f)
)

/**
 * Diagrama anatómico del cuerpo humano con imágenes PNG reales.
 * Superpone círculos de colores sobre cada grupo muscular activo.
 * Permite deslizar (o pulsar los botones) para cambiar entre vista frontal y trasera.
 *
 * @param musculosFrecuencia Mapa de grupo muscular → intensidad (0 = inactivo, ≥1 = activo).
 *   En el paso de selección, usa valor 5 para los músculos prioritarios elegidos.
 *   En la pantalla de rutina activa, usa el conteo real de ejercicios por grupo.
 * @param sexo "Masculino" o "Femenino" para seleccionar la imagen correspondiente.
 */
@Composable
fun DiagramaCuerpoHumano(
    musculosFrecuencia: Map<String, Int>,
    sexo: String,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val esMasculino = sexo != "Femenino"

    val frenteRes = if (esMasculino) R.drawable.hombre_delante else R.drawable.mujer_delante
    val espaldaRes = if (esMasculino) R.drawable.hombre_detras else R.drawable.mujer_detras

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Músculos trabajados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.65f)) {
            SegmentedButton(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Frente") }
            SegmentedButton(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Espalda") }
        }

        Spacer(Modifier.height(12.dp))

        // Imagen del cuerpo con círculos de músculos superpuestos mediante Canvas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(0.43f)
        ) { pagina ->
            val imageRes = if (pagina == 0) frenteRes else espaldaRes
            val posiciones = if (pagina == 0) POSICIONES_FRENTE else POSICIONES_ESPALDA

            Box {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = if (pagina == 0) "Vista frontal" else "Vista trasera",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val W = size.width
                    val H = size.height
                    val radio = W * 0.065f

                    posiciones.forEach { (musculo, puntos) ->
                        val frecuencia = musculosFrecuencia[musculo] ?: 0
                        if (frecuencia > 0) {
                            val color = COLORES_MUSCULARES[musculo] ?: Color.Gray
                            // Alpha progresivo según frecuencia de ejercicios (min 0.72, max 1.0)
                            val alpha = (0.72f + frecuencia.coerceAtMost(4) * 0.07f).coerceAtMost(1f)
                            puntos.forEach { (xFrac, yFrac) ->
                                val cx = W * xFrac
                                val cy = H * yFrac
                                // Halo blanco semitransparente para contraste sobre fondo oscuro
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.5f),
                                    radius = radio + 3.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = color.copy(alpha = alpha),
                                    radius = radio,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Indicadores de página
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == idx) 8.dp else 5.dp)
                        .background(
                            color = if (pagerState.currentPage == idx)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        // Leyenda: muestra únicamente los grupos musculares activos con su color
        val musculosActivos = COLORES_MUSCULARES.keys.mapNotNull { musculo ->
            val freq = musculosFrecuencia[musculo] ?: 0
            if (freq > 0) Triple(musculo, freq, COLORES_MUSCULARES[musculo]!!) else null
        }.sortedByDescending { it.second }

        if (musculosActivos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Grupos musculares:",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            musculosActivos.chunked(2).forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fila.forEach { (musculo, freq, color) ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                // En modo rutina activa muestra el conteo; en selección solo el nombre
                                text = if (freq > 1) "$musculo · $freq ej." else musculo,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = onSurfaceVariant
                            )
                        }
                    }
                    if (fila.size < 2) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
