package com.example.zentra.ui.screens.rutinas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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

// Ratios intrínsecos (ancho/alto) de cada PNG, medidos del archivo real
private const val RATIO_HOMBRE_DELANTE = 1249f / 1536f  // 0.8132
private const val RATIO_HOMBRE_DETRAS  = 986f  / 1536f  // 0.6419
private const val RATIO_MUJER_DELANTE  = 1086f / 1536f  // 0.7070
private const val RATIO_MUJER_DETRAS   = 1046f / 1536f  // 0.6810

// Coordenadas (xFrac, yFrac) de cada músculo calibradas individualmente por imagen.
// xFrac y yFrac son fracción respecto al tamaño REAL de la imagen (no del contenedor),
// por eso el Canvas aplica primero el cálculo letterbox antes de usarlas.

private val POSICIONES_HOMBRE_DELANTE: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"    to listOf(0.34f to 0.20f, 0.62f to 0.20f),
    "Pecho"      to listOf(0.48f to 0.26f),
    "Bíceps"     to listOf(0.32f to 0.30f, 0.64f to 0.30f),
    "Core"       to listOf(0.48f to 0.40f),
    "Cuádriceps" to listOf(0.38f to 0.60f, 0.58f to 0.60f),
    "Gemelos"    to listOf(0.43f to 0.78f, 0.54f to 0.78f)
)

private val POSICIONES_HOMBRE_DETRAS: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"        to listOf(0.32f to 0.21f, 0.66f to 0.21f),
    "Espalda"        to listOf(0.50f to 0.29f),
    "Tríceps"        to listOf(0.29f to 0.32f, 0.71f to 0.32f),
    "Glúteos"        to listOf(0.40f to 0.52f, 0.57f to 0.52f),
    "Isquiotibiales" to listOf(0.39f to 0.64f, 0.58f to 0.64f),
    "Gemelos"        to listOf(0.39f to 0.80f, 0.59f to 0.80f)
)

private val POSICIONES_MUJER_DELANTE: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"    to listOf(0.34f to 0.20f, 0.62f to 0.20f),
    "Pecho"      to listOf(0.48f to 0.26f),
    "Bíceps"     to listOf(0.32f to 0.30f, 0.64f to 0.30f),
    "Core"       to listOf(0.48f to 0.40f),
    "Cuádriceps" to listOf(0.38f to 0.60f, 0.58f to 0.60f),
    "Gemelos"    to listOf(0.41f to 0.78f, 0.54f to 0.78f)
)

private val POSICIONES_MUJER_DETRAS: Map<String, List<Pair<Float, Float>>> = mapOf(
    "Hombros"        to listOf(0.32f to 0.21f, 0.65f to 0.21f),
    "Espalda"        to listOf(0.48f to 0.29f),
    "Tríceps"        to listOf(0.30f to 0.30f, 0.66f to 0.30f),
    "Glúteos"        to listOf(0.40f to 0.50f, 0.57f to 0.50f),
    "Isquiotibiales" to listOf(0.39f to 0.64f, 0.58f to 0.64f),
    "Gemelos"        to listOf(0.41f to 0.80f, 0.57f to 0.80f)
)

/**
 * Diagrama anatómico del cuerpo humano con imágenes PNG reales.
 * Superpone círculos de colores sobre cada grupo muscular activo.
 * Permite deslizar (o pulsar los botones) para cambiar entre vista frontal y trasera.
 *
 * @param musculosFrecuencia Mapa de grupo muscular → intensidad (0 = inactivo, ≥1 = activo).
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) { pagina ->
            val esFrente = pagina == 0

            val imageRes = when {
                esFrente && esMasculino  -> R.drawable.hombre_delante
                esFrente && !esMasculino -> R.drawable.mujer_delante
                !esFrente && esMasculino -> R.drawable.hombre_detras
                else                     -> R.drawable.mujer_detras
            }
            val imgRatio = when {
                esFrente && esMasculino  -> RATIO_HOMBRE_DELANTE
                esFrente && !esMasculino -> RATIO_MUJER_DELANTE
                !esFrente && esMasculino -> RATIO_HOMBRE_DETRAS
                else                     -> RATIO_MUJER_DETRAS
            }
            val posiciones = when {
                esFrente && esMasculino  -> POSICIONES_HOMBRE_DELANTE
                esFrente && !esMasculino -> POSICIONES_MUJER_DELANTE
                !esFrente && esMasculino -> POSICIONES_HOMBRE_DETRAS
                else                     -> POSICIONES_MUJER_DETRAS
            }

            Box {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = if (esFrente) "Vista frontal" else "Vista trasera",
                    modifier = Modifier.fillMaxSize(),
                    // Fit mantiene las proporciones originales sin distorsión.
                    // El Canvas calcula el área real de la imagen para colocar los círculos correctamente.
                    contentScale = ContentScale.Fit
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val W = size.width
                    val H = size.height
                    val radio = minOf(W, H) * 0.030f

                    // Calcula los límites reales de la imagen dentro del Canvas (Fit → pillarbox o letterbox)
                    val containerRatio = W / H
                    val imgW: Float
                    val imgH: Float
                    val imgLeft: Float
                    val imgTop: Float
                    if (containerRatio > imgRatio) {
                        // Más ancho que la imagen → pillarbox: imagen a altura completa, centrada horizontalmente
                        imgH = H
                        imgW = H * imgRatio
                        imgLeft = (W - imgW) / 2f
                        imgTop = 0f
                    } else {
                        // Más alto que la imagen → letterbox: imagen a ancho completo, centrada verticalmente
                        imgW = W
                        imgH = W / imgRatio
                        imgLeft = 0f
                        imgTop = (H - imgH) / 2f
                    }

                    posiciones.forEach { (musculo, puntos) ->
                        val frecuencia = musculosFrecuencia[musculo] ?: 0
                        if (frecuencia > 0) {
                            val color = COLORES_MUSCULARES[musculo] ?: Color.Gray
                            val alpha = (0.72f + frecuencia.coerceAtMost(4) * 0.07f).coerceAtMost(1f)
                            puntos.forEach { (xFrac, yFrac) ->
                                // Las fracciones se aplican sobre las dimensiones REALES de la imagen, no del contenedor
                                val cx = imgLeft + imgW * xFrac
                                val cy = imgTop + imgH * yFrac
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
