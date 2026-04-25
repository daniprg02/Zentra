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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zentra.domain.model.NivelActividad
import com.example.zentra.domain.model.ObjetivoFisico
import com.example.zentra.domain.model.Receta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
 * Muestra el panel de progreso calórico diario, los macronutrientes,
 * la navegación de fechas con modo historial (solo lectura) y
 * el picker de recetas + búsqueda en OpenFoodFacts para cada slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCalculadora(
    onLogout: () -> Unit = {},
    viewModel: CalculadoraViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.sesionCerrada) {
        if (estado.sesionCerrada) onLogout()
    }

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
            onSlotClick = viewModel::abrirSlot,
            onEliminarReceta = viewModel::eliminarRecetaDeSlot,
            onCambiarFecha = viewModel::cambiarFecha,
            onVolverAHoy = viewModel::volverAHoy,
            onLogout = viewModel::cerrarSesion,
            onEditarIngesta = viewModel::abrirEdicionIngesta
        )
    }

    // El botón físico de retroceso cierra el sheet cuando está abierto
    BackHandler(enabled = estado.slotActivo != null) {
        viewModel.cerrarSlot()
    }

    if (estado.slotActivo != null) {
        ModalBottomSheet(
            // No cerrar al tocar fuera ni al hacer swipe — solo con la X o al añadir un alimento
            onDismissRequest = {},
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { it != SheetValue.Hidden }
            ),
            dragHandle = {}
        ) {
            PickerRecetasParaSlot(
                nombreSlot = estado.slotActivo!!,
                recetas = estado.recetasDisponibles,
                ingestasActuales = estado.ingestasDelDia[estado.slotActivo!!] ?: emptyList(),
                cargando = estado.cargandoRecetas,
                busquedaTexto = estado.busquedaTexto,
                resultadosLocales = estado.resultadosLocales,
                resultadosBusqueda = estado.resultadosBusqueda,
                buscandoAlimento = estado.buscandoAlimento,
                onRecetaSeleccionada = viewModel::agregarRecetaASlot,
                onEliminarIngesta = { receta ->
                    viewModel.eliminarRecetaDeSlot(estado.slotActivo!!, receta)
                },
                onActualizarBusqueda = viewModel::actualizarBusqueda,
                onCerrar = viewModel::cerrarSlot
            )
        }
    }

    // Diálogo de edición de cantidad (gramos) de una ingesta ya añadida
    if (estado.ingestaEditando != null) {
        val edicion = estado.ingestaEditando!!
        val cantidadFloat = edicion.cantidadTexto.toFloatOrNull()
        val kcalPreview = cantidadFloat?.let { ((edicion.recetaOriginal.kcalTotales * it) / 100f).toInt() }

        AlertDialog(
            onDismissRequest = viewModel::cerrarEdicionIngesta,
            title = {
                Text(
                    text = edicion.recetaOriginal.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Base: ${edicion.recetaOriginal.kcalTotales} kcal / 100 g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = edicion.cantidadTexto,
                        onValueChange = viewModel::actualizarCantidadEdicion,
                        label = { Text("Cantidad (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (kcalPreview != null) {
                        Text(
                            text = "→ $kcalPreview kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmarEdicionIngesta,
                    enabled = cantidadFloat != null && cantidadFloat > 0f
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarEdicionIngesta) { Text("Cancelar") }
            }
        )
    }
}

// ─── Contenido principal ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoCalculadora(
    estado: EstadoCalculadora,
    onObjetivoSeleccionado: (ObjetivoFisico) -> Unit,
    onNivelActividadSeleccionado: (NivelActividad) -> Unit,
    onSlotClick: (String) -> Unit,
    onEliminarReceta: (String, Receta) -> Unit,
    onCambiarFecha: (LocalDate) -> Unit,
    onVolverAHoy: () -> Unit,
    onLogout: () -> Unit,
    onEditarIngesta: (String, Receta) -> Unit
) {
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val fechaObj = LocalDate.parse(estado.fechaVisualizando)
    val esHoy = fechaObj == LocalDate.now()
    val fechaFormateada = if (esHoy) {
        "Hoy · " + fechaObj.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES")))
    } else {
        fechaObj.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES")))
            .replaceFirstChar { it.uppercase() }
    }

    // Colores resueltos fuera del Canvas
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorSecundario = MaterialTheme.colorScheme.secondary
    val colorTerciario = MaterialTheme.colorScheme.tertiary
    val colorError = MaterialTheme.colorScheme.error
    val colorSuperficieVariante = MaterialTheme.colorScheme.surfaceVariant

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaObj
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                // Solo se pueden seleccionar el día actual y días anteriores
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis() + 86_400_000L
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onCambiarFecha(fecha)
                    }
                    mostrarDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner de modo historial (solo lectura)
        if (estado.esModoHistorial) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial · Solo lectura",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    TextButton(onClick = onVolverAHoy) {
                        Text("Volver a hoy", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                var mostrarMenuUsuario by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hola, ${estado.apodo}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        // Fecha tappable que abre el DatePicker
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { mostrarDatePicker = true }
                        ) {
                            Text(
                                text = fechaFormateada,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (estado.esModoHistorial) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Abrir calendario",
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(16.dp),
                                tint = if (estado.esModoHistorial) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { mostrarMenuUsuario = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Perfil",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = mostrarMenuUsuario,
                            onDismissRequest = { mostrarMenuUsuario = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ExitToApp, contentDescription = null)
                                },
                                onClick = {
                                    mostrarMenuUsuario = false
                                    onLogout()
                                }
                            )
                        }
                    }
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

            // La sección de slots solo se muestra si no estamos en modo historial
            if (!estado.esModoHistorial) {
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
                        onClick = { onSlotClick(slot.nombre) },
                        onEliminarReceta = { receta -> onEliminarReceta(slot.nombre, receta) },
                        onEditarIngesta = { receta -> onEditarIngesta(slot.nombre, receta) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "Las ingestas detalladas por slot no se guardan históricamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ─── Picker de recetas (BottomSheet) ─────────────────────────────────────────

@Composable
private fun PickerRecetasParaSlot(
    nombreSlot: String,
    recetas: List<Receta>,
    ingestasActuales: List<Receta>,
    cargando: Boolean,
    busquedaTexto: String,
    resultadosLocales: List<Receta>,
    resultadosBusqueda: List<Receta>,
    buscandoAlimento: Boolean,
    onRecetaSeleccionada: (Receta) -> Unit,
    onEliminarIngesta: (Receta) -> Unit,
    onActualizarBusqueda: (String) -> Unit,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Añadir al $nombreSlot",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onCerrar) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Consejo informativo sobre la base de datos
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Los alimentos se proporcionan en cantidades de 100 gramos. Si no encuentras lo que buscas, crea tu propia receta en la pestaña Recetas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de búsqueda en OpenFoodFacts
        OutlinedTextField(
            value = busquedaTexto,
            onValueChange = onActualizarBusqueda,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar alimento...") },
            leadingIcon = {
                if (buscandoAlimento) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (busquedaTexto.isNotBlank()) {
                    IconButton(onClick = { onActualizarBusqueda("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (busquedaTexto.isNotBlank()) {
            // Modo búsqueda: locales (inmediatos) + API (con debounce)
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (resultadosLocales.isNotEmpty()) {
                    item {
                        Text(
                            text = "LISTA LOCAL · ZENTRA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    items(resultadosLocales, key = { "local_${it.titulo}" }) { receta ->
                        ItemRecetaPicker(receta = receta, onClick = { onRecetaSeleccionada(receta) })
                    }
                }

                if (resultadosBusqueda.isNotEmpty()) {
                    item {
                        Text(
                            text = "OPENFOODFACTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = if (resultadosLocales.isNotEmpty()) 10.dp else 0.dp, bottom = 6.dp)
                        )
                    }
                    items(resultadosBusqueda, key = { it.id }) { receta ->
                        ItemRecetaPicker(receta = receta, onClick = { onRecetaSeleccionada(receta) })
                    }
                }

                if (resultadosLocales.isEmpty() && resultadosBusqueda.isEmpty() && !buscandoAlimento) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin resultados para \"$busquedaTexto\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Modo normal: ingestas actuales + mis recetas
            if (ingestasActuales.isNotEmpty()) {
                Text(
                    text = "EN ESTE SLOT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Chips eliminables con las ingestas actuales del slot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ingestasActuales.forEach { receta ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${receta.titulo} · ${receta.kcalTotales} kcal",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Eliminar ${receta.titulo}",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onEliminarIngesta(receta) }
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

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
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 340.dp),
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
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ItemRecetaPicker(receta: Receta, onClick: () -> Unit) {
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

@Composable
private fun TarjetaSlotComida(
    nombre: String,
    icono: ImageVector,
    ingestas: List<Receta>,
    onClick: () -> Unit,
    onEliminarReceta: (Receta) -> Unit,
    onEditarIngesta: (Receta) -> Unit
) {
    val kcalSlot = ingestas.sumOf { it.kcalTotales }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        }
                    }
                }

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

            // Chips de ingestas con botón de eliminar (visibles si hay ingestas)
            if (ingestas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ingestas.forEach { receta ->
                        SuggestionChip(
                            onClick = { onEditarIngesta(receta) },
                            label = {
                                Text(
                                    text = receta.titulo,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Eliminar ${receta.titulo}",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onEliminarReceta(receta) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Componentes de dashboard ─────────────────────────────────────────────────

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
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
