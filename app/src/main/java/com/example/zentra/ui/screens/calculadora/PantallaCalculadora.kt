package com.example.zentra.ui.screens.calculadora

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zentra.domain.model.NivelActividad
import com.example.zentra.domain.model.ObjetivoFisico
import com.example.zentra.ui.theme.ZentraTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Slots de comida predefinidos que estructuran el día del usuario.
private data class SlotComida(val nombre: String, val icono: ImageVector)

private val SLOTS_DEL_DIA = listOf(
    SlotComida("Desayuno", Icons.Outlined.WbSunny),
    SlotComida("Almuerzo", Icons.Outlined.LocalCafe),
    SlotComida("Comida", Icons.Outlined.Restaurant),
    SlotComida("Merienda", Icons.Outlined.LocalCafe),
    SlotComida("Cena", Icons.Outlined.Bedtime),
    SlotComida("Pre-entreno", Icons.Outlined.Bolt),
    SlotComida("Post-entreno", Icons.AutoMirrored.Outlined.DirectionsRun)
)

/**
 * Pantalla principal del módulo de Calculadora Dietética.
 * Muestra el panel de progreso calórico diario, el desglose de macronutrientes
 * y los slots de ingestas del día estructurados por momento de la jornada.
 */
@Composable
fun PantallaCalculadora(
    viewModel: CalculadoraViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    when {
        estado.cargando -> PantallaCargando()
        estado.error != null -> PantallaError(mensaje = estado.error!!, onReintentar = viewModel::cargarDatosDelDia)
        else -> ContenidoCalculadora(
            estado = estado,
            onObjetivoSeleccionado = viewModel::cambiarObjetivo,
            onNivelActividadSeleccionado = viewModel::cambiarNivelActividad
        )
    }
}

@Composable
private fun ContenidoCalculadora(
    estado: EstadoCalculadora,
    onObjetivoSeleccionado: (ObjetivoFisico) -> Unit,
    onNivelActividadSeleccionado: (NivelActividad) -> Unit
) {
    val fechaHoy = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES")))
        .replaceFirstChar { it.uppercase() }

    // Colores resueltos antes del Canvas para poder acceder a MaterialTheme dentro del DrawScope
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorSecundario = MaterialTheme.colorScheme.secondary
    val colorTerciario = MaterialTheme.colorScheme.tertiary
    val colorError = MaterialTheme.colorScheme.error
    val colorSuperficieVariante = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Cabecera: saludo y fecha
        item {
            Column {
                Text(
                    text = "Hola, ${estado.apodo}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = fechaHoy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selector de objetivo físico
        item {
            SelectorObjetivo(
                objetivoActual = estado.objetivo,
                onObjetivoSeleccionado = onObjetivoSeleccionado
            )
        }

        // Anillo de progreso calórico central
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnilloCaloricoProgress(
                    consumidoKcal = estado.consumidoKcal,
                    objetivoKcal = estado.objetivoKcal,
                    colorPrimario = colorSecundario,
                    colorAdvertencia = colorTerciario,
                    colorError = colorError,
                    colorPista = colorSuperficieVariante
                )
            }
        }

        // Tarjeta con desglose de macronutrientes
        item {
            TarjetaMacros(
                consumidoProteinasG = estado.consumidoProteinasG,
                objetivoProteinasG = estado.objetivoProteinasG,
                consumidoCarbosG = estado.consumidoCarbosG,
                objetivoCarbosG = estado.objetivoCarbosG,
                consumidoGrasasG = estado.consumidoGrasasG,
                objetivoGrasasG = estado.objetivoGrasasG,
                colorProteinas = colorPrimario,
                colorCarbos = colorSecundario,
                colorGrasas = colorTerciario
            )
        }

        // Selector de nivel de actividad física
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nivel de actividad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectorNivelActividad(
                    nivelActual = estado.nivelActividad,
                    onNivelSeleccionado = onNivelActividadSeleccionado
                )
            }
        }

        // Título de la sección de ingestas
        item {
            Text(
                text = "INGESTAS DEL DÍA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Slots de comida del día
        items(SLOTS_DEL_DIA) { slot ->
            TarjetaSlotComida(nombre = slot.nombre, icono = slot.icono)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Componentes privados ──────────────────────────────────────────────────────

/**
 * Anillo de progreso calórico dibujado con Canvas.
 * Cambia de color dinámicamente según la proximidad al objetivo: verde → naranja → rojo.
 *
 * @param consumidoKcal Calorías consumidas hasta el momento.
 * @param objetivoKcal Calorías objetivo del día.
 */
@Composable
private fun AnilloCaloricoProgress(
    consumidoKcal: Int,
    objetivoKcal: Int,
    colorPrimario: Color,
    colorAdvertencia: Color,
    colorError: Color,
    colorPista: Color
) {
    val progreso = if (objetivoKcal > 0) (consumidoKcal.toFloat() / objetivoKcal).coerceIn(0f, 1f) else 0f
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 900),
        label = "animacion_anillo_calorico"
    )

    // El color del anillo se evalúa en el scope composable donde MaterialTheme es accesible
    val colorAnillo = when {
        progreso >= 1f -> colorError
        progreso >= 0.85f -> colorAdvertencia
        else -> colorPrimario
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosorTrazo = 26.dp.toPx()
            val radio = (size.minDimension - grosorTrazo) / 2f
            val esquinaArco = Offset(
                x = size.width / 2f - radio,
                y = size.height / 2f - radio
            )
            val tamanoArco = Size(radio * 2f, radio * 2f)

            // Pista de fondo del anillo
            drawArc(
                color = colorPista,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = esquinaArco,
                size = tamanoArco,
                style = Stroke(width = grosorTrazo, cap = StrokeCap.Round)
            )

            // Arco de progreso animado
            if (progresoAnimado > 0f) {
                drawArc(
                    color = colorAnillo,
                    startAngle = -90f,
                    sweepAngle = 360f * progresoAnimado,
                    useCenter = false,
                    topLeft = esquinaArco,
                    size = tamanoArco,
                    style = Stroke(width = grosorTrazo, cap = StrokeCap.Round)
                )
            }
        }

        // Texto central con el resumen calórico
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = consumidoKcal.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "de $objetivoKcal kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val restantes = objetivoKcal - consumidoKcal
            if (restantes >= 0) {
                Text(
                    text = "$restantes restantes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorAnillo
                )
            } else {
                Text(
                    text = "+${-restantes} excedidas",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorError
                )
            }
        }
    }
}

/**
 * Tarjeta con las tres barras de progreso de macronutrientes (proteínas, carbos y grasas).
 */
@Composable
private fun TarjetaMacros(
    consumidoProteinasG: Float,
    objetivoProteinasG: Float,
    consumidoCarbosG: Float,
    objetivoCarbosG: Float,
    consumidoGrasasG: Float,
    objetivoGrasasG: Float,
    colorProteinas: Color,
    colorCarbos: Color,
    colorGrasas: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BarraMacro(
                etiqueta = "Proteínas",
                consumidoG = consumidoProteinasG,
                objetivoG = objetivoProteinasG,
                color = colorProteinas
            )
            BarraMacro(
                etiqueta = "Carbohidratos",
                consumidoG = consumidoCarbosG,
                objetivoG = objetivoCarbosG,
                color = colorCarbos
            )
            BarraMacro(
                etiqueta = "Grasas",
                consumidoG = consumidoGrasasG,
                objetivoG = objetivoGrasasG,
                color = colorGrasas
            )
        }
    }
}

/**
 * Fila de progreso para un macronutriente individual.
 *
 * @param etiqueta Nombre del macronutriente.
 * @param consumidoG Gramos consumidos hasta el momento.
 * @param objetivoG Gramos objetivo del día.
 * @param color Color identificativo del macronutriente.
 */
@Composable
private fun BarraMacro(
    etiqueta: String,
    consumidoG: Float,
    objetivoG: Float,
    color: Color
) {
    val progreso = if (objetivoG > 0f) (consumidoG / objetivoG).coerceIn(0f, 1f) else 0f
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 700),
        label = "animacion_$etiqueta"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${"%.0f".format(consumidoG)} / ${"%.0f".format(objetivoG)}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progresoAnimado },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Selector de objetivo físico con tres botones segmentados (Déficit, Mantenimiento, Superávit).
 */
@Composable
private fun SelectorObjetivo(
    objetivoActual: ObjetivoFisico,
    onObjetivoSeleccionado: (ObjetivoFisico) -> Unit
) {
    val opciones = ObjetivoFisico.values()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { index, opcion ->
            SegmentedButton(
                selected = opcion == objetivoActual,
                onClick = { onObjetivoSeleccionado(opcion) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size),
                label = {
                    Text(
                        text = opcion.etiqueta,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

/**
 * Carrusel horizontal de chips para seleccionar el nivel de actividad física.
 * Se presenta como un scroll horizontal para acomodar los cinco niveles sin recortes.
 */
@Composable
private fun SelectorNivelActividad(
    nivelActual: NivelActividad,
    onNivelSeleccionado: (NivelActividad) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NivelActividad.values().forEach { nivel ->
            FilterChip(
                selected = nivel == nivelActual,
                onClick = { onNivelSeleccionado(nivel) },
                label = {
                    Text(
                        text = nivel.etiqueta,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

/**
 * Tarjeta de un slot de comida del día. Muestra el nombre del momento y un icono representativo.
 * La interacción (añadir alimentos) se implementará en la siguiente fase con un BottomSheet.
 */
@Composable
private fun TarjetaSlotComida(
    nombre: String,
    icono: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vacío · Toca para añadir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Añadir al slot $nombre",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Estados de carga y error ─────────────────────────────────────────────────

@Composable
private fun PantallaCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PantallaError(mensaje: String, onReintentar: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            androidx.compose.material3.TextButton(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaCalculadora() {
    ZentraTheme(temaOscuro = true) {
        ContenidoCalculadora(
            estado = EstadoCalculadora(
                cargando = false,
                apodo = "Dani",
                objetivoKcal = 2450,
                consumidoKcal = 1200,
                objetivoProteinasG = 160f,
                consumidoProteinasG = 85f,
                objetivoCarbosG = 255f,
                consumidoCarbosG = 110f,
                objetivoGrasasG = 82f,
                consumidoGrasasG = 40f
            ),
            onObjetivoSeleccionado = {},
            onNivelActividadSeleccionado = {}
        )
    }
}
