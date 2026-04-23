package com.example.zentra.ui.screens.calculadora

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.zentra.domain.model.Receta
import com.example.zentra.ui.theme.ZentraTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
 * Muestra el panel de progreso calórico diario, el desglose de macronutrientes,
 * los slots de ingestas del día y el picker de recetas para cada slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCalculadora(
    viewModel: CalculadoraViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    when {
        estado.cargando -> PantallaCargando()
        estado.error != null -> PantallaError(
            mensaje = estado.error!!,
            onReintentar = viewModel::cargarDatosDelDia
        )
        else -> ContenidoCalculadora(
            estado = estado,
            onObjetivoSeleccionado = viewModel::cambiarObjetivo,
            onNivelActividadSeleccionado = viewModel::cambiarNivelActividad,
            onSlotClick = viewModel::abrirSlot
        )
    }

    // El picker de recetas se renderiza fuera del when para cubrir toda la pantalla
    if (estado.slotActivo != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cerrarSlot,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PickerRecetasParaSlot(
                nombreSlot = estado.slotActivo!!,
                recetas = estado.recetasDisponibles,
                cargando = estado.cargandoRecetas,
                onRecetaSeleccionada = viewModel::agregarRecetaASlot
            )
        }
    }
}

// ─── Contenido principal ──────────────────────────────────────────────────────

@Composable
private fun ContenidoCalculadora(
    estado: EstadoCalculadora,
    onObjetivoSeleccionado: (ObjetivoFisico) -> Unit,
    onNivelActividadSeleccionado: (NivelActividad) -> Unit,
    onSlotClick: (String) -> Unit
) {
    val fechaHoy = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES")))
        .replaceFirstChar { it.uppercase() }

    // Colores resueltos antes del Canvas para poder usarlos dentro del DrawScope
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorSecundario = MaterialTheme.colorScheme.secondary
    val colorTerciario = MaterialTheme.colorScheme.tertiary
    val colorError = MaterialTheme.colorScheme.error
    val colorSuperficieVariante = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

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

        item {
            SelectorObjetivo(
                objetivoActual = estado.objetivo,
                onObjetivoSeleccionado = onObjetivoSeleccionado
            )
        }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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

        item {
            Text(
                text = "INGESTAS DEL DÍA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(SLOTS_DEL_DIA) { slot ->
            TarjetaSlotComida(
                nombre = slot.nombre,
                icono = slot.icono,
                ingestas = estado.ingestasDelDia[slot.nombre] ?: emptyList(),
                onClick = { onSlotClick(slot.nombre) }
            )
        }
    }
}

// ─── Picker de recetas (BottomSheet) ─────────────────────────────────────────

/**
 * Contenido del BottomSheet para añadir una receta a un slot de ingesta.
 * Muestra la biblioteca de recetas del usuario. La búsqueda manual mediante API
 * externa (OpenFoodFacts) se implementará en una fase posterior.
 *
 * @param nombreSlot Nombre del slot al que se va a añadir la ingesta.
 * @param recetas Lista de recetas disponibles del usuario.
 * @param cargando Indica si las recetas aún se están cargando.
 * @param onRecetaSeleccionada Callback al confirmar una receta.
 */
@Composable
private fun PickerRecetasParaSlot(
    nombreSlot: String,
    recetas: List<Receta>,
    cargando: Boolean,
    onRecetaSeleccionada: (Receta) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Añadir al $nombreSlot",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "MIS RECETAS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            cargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            recetas.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Aún no tienes recetas guardadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                // Altura acotada para que el BottomSheet no ocupe toda la pantalla
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(recetas, key = { it.id }) { receta ->
                        ItemRecetaPicker(
                            receta = receta,
                            onClick = { onRecetaSeleccionada(receta) }
                        )
                    }
                }
            }
        }

        // Sección de búsqueda manual (próximamente)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Búsqueda manual",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Próximamente · OpenFoodFacts API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Fila del picker que representa una receta disponible para añadir al slot.
 */
@Composable
private fun ItemRecetaPicker(
    receta: Receta,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receta.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "P: ${"%.0f".format(receta.proteinasG)}g  " +
                       "C: ${"%.0f".format(receta.carbosG)}g  " +
                       "G: ${"%.0f".format(receta.grasasG)}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${receta.kcalTotales} kcal",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// ─── Tarjeta de slot ──────────────────────────────────────────────────────────

/**
 * Tarjeta de un slot de comida. Si el slot tiene ingestas añadidas en la sesión actual,
 * muestra el resumen de kcal acumuladas. De lo contrario, invita al usuario a añadir.
 *
 * @param ingestas Lista de recetas añadidas al slot en esta sesión (se pierde al cerrar la app).
 */
@Composable
private fun TarjetaSlotComida(
    nombre: String,
    icono: ImageVector,
    ingestas: List<Receta>,
    onClick: () -> Unit
) {
    val kcalSlot = ingestas.sumOf { it.kcalTotales }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
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
                    if (ingestas.isEmpty()) {
                        Text(
                            text = "Vacío · Toca para añadir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = ingestas.joinToString(", ") { it.titulo },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Resumen de kcal del slot (solo si tiene ingestas) o icono de añadir
            if (ingestas.isNotEmpty()) {
                Text(
                    text = "$kcalSlot kcal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Añadir al slot $nombre",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Componentes de dashboard ─────────────────────────────────────────────────

/**
 * Anillo de progreso calórico dibujado con Canvas.
 * El color cambia dinámicamente: verde → naranja → rojo según la proximidad al objetivo.
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
    val colorAnillo = when {
        progreso >= 1f -> colorError
        progreso >= 0.85f -> colorAdvertencia
        else -> colorPrimario
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosorTrazo = 26.dp.toPx()
            val radio = (size.minDimension - grosorTrazo) / 2f
            val esquinaArco = Offset(size.width / 2f - radio, size.height / 2f - radio)
            val tamanoArco = Size(radio * 2f, radio * 2f)

            drawArc(
                color = colorPista,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = esquinaArco, size = tamanoArco,
                style = Stroke(width = grosorTrazo, cap = StrokeCap.Round)
            )
            if (progresoAnimado > 0f) {
                drawArc(
                    color = colorAnillo,
                    startAngle = -90f, sweepAngle = 360f * progresoAnimado, useCenter = false,
                    topLeft = esquinaArco, size = tamanoArco,
                    style = Stroke(width = grosorTrazo, cap = StrokeCap.Round)
                )
            }
        }

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
            Text(
                text = if (restantes >= 0) "$restantes restantes" else "+${-restantes} excedidas",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (restantes >= 0) colorAnillo else colorError
            )
        }
    }
}

@Composable
private fun TarjetaMacros(
    consumidoProteinasG: Float, objetivoProteinasG: Float,
    consumidoCarbosG: Float, objetivoCarbosG: Float,
    consumidoGrasasG: Float, objetivoGrasasG: Float,
    colorProteinas: Color, colorCarbos: Color, colorGrasas: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BarraMacro("Proteínas", consumidoProteinasG, objetivoProteinasG, colorProteinas)
            BarraMacro("Carbohidratos", consumidoCarbosG, objetivoCarbosG, colorCarbos)
            BarraMacro("Grasas", consumidoGrasasG, objetivoGrasasG, colorGrasas)
        }
    }
}

@Composable
private fun BarraMacro(etiqueta: String, consumidoG: Float, objetivoG: Float, color: Color) {
    val progreso = if (objetivoG > 0f) (consumidoG / objetivoG).coerceIn(0f, 1f) else 0f
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(700),
        label = "animacion_$etiqueta"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${"%.0f".format(consumidoG)} / ${"%.0f".format(objetivoG)}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progresoAnimado },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SelectorObjetivo(objetivoActual: ObjetivoFisico, onObjetivoSeleccionado: (ObjetivoFisico) -> Unit) {
    val opciones = ObjetivoFisico.values()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { index, opcion ->
            SegmentedButton(
                selected = opcion == objetivoActual,
                onClick = { onObjetivoSeleccionado(opcion) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size),
                label = { Text(opcion.etiqueta, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun SelectorNivelActividad(nivelActual: NivelActividad, onNivelSeleccionado: (NivelActividad) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NivelActividad.values().forEach { nivel ->
            FilterChip(
                selected = nivel == nivelActual,
                onClick = { onNivelSeleccionado(nivel) },
                label = { Text(nivel.etiqueta, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

// ─── Estados auxiliares ───────────────────────────────────────────────────────

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
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionPantallaCalculadora() {
    val recetaEjemplo = Receta("1", "u1", "Tortilla de 3 huevos", 280, 22f, 2f, 20f, "")
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
                consumidoGrasasG = 40f,
                ingestasDelDia = mapOf("Desayuno" to listOf(recetaEjemplo))
            ),
            onObjetivoSeleccionado = {},
            onNivelActividadSeleccionado = {},
            onSlotClick = {}
        )
    }
}
