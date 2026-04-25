package com.example.zentra.ui.screens.recetas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zentra.domain.model.Receta
import com.example.zentra.ui.theme.ZentraTheme
import kotlin.math.abs

// Paleta de colores para las tarjetas de recetas (se asigna por hash del ID)
private val COLORES_TARJETA = listOf(
    Color(0xFF1565C0),
    Color(0xFF00695C),
    Color(0xFF6A1B9A),
    Color(0xFFAD1457),
    Color(0xFF2E7D32),
    Color(0xFFE65100)
)

private fun colorParaReceta(id: String): Color =
    COLORES_TARJETA[abs(id.hashCode()) % COLORES_TARJETA.size]

/**
 * Pantalla principal del módulo de Gestor de Recetas.
 * Muestra la barra de búsqueda, el carrusel de favoritas, la lista completa de recetas
 * y el FAB para añadir nuevas recetas manualmente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecetas(
    viewModel: RecetasViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val recetasFiltradas by remember(estado.recetas, estado.filtro) {
        derivedStateOf {
            if (estado.filtro.isBlank()) estado.recetas
            else estado.recetas.filter { it.titulo.contains(estado.filtro, ignoreCase = true) }
        }
    }
    val recetasFijadas by remember(estado.recetas) {
        derivedStateOf { estado.recetas.filter { it.fijada } }
    }
    // Alimentos usados en recetas previas (con unidades y peso) para el desplegable del formulario
    val alimentosGuardados by remember(estado.recetas) {
        derivedStateOf {
            estado.recetas
                .flatMap { r ->
                    r.ingredientes.split("\n", ",").mapNotNull { linea ->
                        val raw = linea.trim()
                        if (raw.length < 2) null
                        else {
                            val pesoG = if (raw.contains("("))
                                raw.substringAfter("(").substringBefore(")").removeSuffix("g").trim()
                            else ""
                            val sinPeso = if (raw.contains("(")) raw.substringBefore("(").trim() else raw
                            val unidades = sinPeso.takeWhile { it.isDigit() || it == '/' || it == '.' }.trim()
                            val nombre = sinPeso.removePrefix(unidades).trim()
                            if (nombre.length < 2) null
                            else AlimentoGuardado(nombre = nombre, unidades = unidades, pesoG = pesoG)
                        }
                    }
                }
                .distinctBy { it.nombre }
                .sortedBy { it.nombre }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            estado.cargando -> PantallaCargando()
            estado.error != null -> PantallaError(
                mensaje = estado.error!!,
                onReintentar = viewModel::cargarRecetas
            )
            else -> ContenidoRecetas(
                recetasFiltradas = recetasFiltradas,
                recetasFijadas = recetasFijadas,
                filtro = estado.filtro,
                onFiltroChange = viewModel::actualizarFiltro,
                onTogglePin = viewModel::toggleFijada,
                onEliminar = viewModel::eliminarReceta
            )
        }

        // FAB visible siempre que no estemos en estado de carga
        if (!estado.cargando) {
            ExtendedFloatingActionButton(
                onClick = viewModel::mostrarFormulario,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Añadir receta") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    BackHandler(enabled = estado.mostrandoFormulario) {
        viewModel.ocultarFormulario()
    }

    // El BottomSheet se renderiza fuera del Box para que cubra toda la pantalla correctamente
    if (estado.mostrandoFormulario) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { it != SheetValue.Hidden }
            ),
            dragHandle = {}
        ) {
            ContenidoFormulario(
                formulario = estado.formulario,
                alimentosGuardados = alimentosGuardados,
                onTituloChange = viewModel::actualizarTitulo,
                onAgregarIngrediente = viewModel::agregarIngrediente,
                onEliminarIngrediente = viewModel::eliminarIngrediente,
                onActualizarUnidades = viewModel::actualizarUnidades,
                onActualizarAlimento = viewModel::actualizarAlimento,
                onActualizarPesoG = viewModel::actualizarPesoG,
                onUsarAlimentoGuardado = viewModel::usarAlimentoGuardado,
                onProteinasChange = viewModel::actualizarProteinas,
                onCarbosChange = viewModel::actualizarCarbos,
                onGrasasChange = viewModel::actualizarGrasas,
                onGuardar = viewModel::guardarNuevaReceta,
                onCancelar = viewModel::ocultarFormulario
            )
        }
    }
}

// ─── Contenido principal ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoRecetas(
    recetasFiltradas: List<Receta>,
    recetasFijadas: List<Receta>,
    filtro: String,
    onFiltroChange: (String) -> Unit,
    onTogglePin: (Receta) -> Unit,
    onEliminar: (Receta) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Barra de búsqueda
        item {
            BarraBusqueda(
                valor = filtro,
                onValorChange = onFiltroChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Carrusel de favoritas (solo si hay recetas fijadas y no hay filtro activo)
        if (recetasFijadas.isNotEmpty() && filtro.isBlank()) {
            item {
                Text(
                    text = "MIS FAVORITAS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(recetasFijadas, key = { it.id }) { receta ->
                        TarjetaFavorita(receta = receta)
                    }
                }
            }
        }

        // Encabezado de la lista completa
        item {
            Text(
                text = if (filtro.isBlank()) "TODAS MIS RECETAS" else "RESULTADOS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Lista vacía
        if (recetasFiltradas.isEmpty()) {
            item {
                EstadoVacio(
                    hayFiltro = filtro.isNotBlank(),
                    modifier = Modifier.padding(top = 64.dp)
                )
            }
        } else {
            items(recetasFiltradas, key = { it.id }) { receta ->
                TarjetaRecetaSwipeable(
                    receta = receta,
                    onTogglePin = { onTogglePin(receta) },
                    onEliminar = { onEliminar(receta) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem()
                )
            }
        }
    }
}

// ─── Componentes de lista ─────────────────────────────────────────────────────

/**
 * Tarjeta cuadrada para el carrusel horizontal de recetas fijadas.
 * Usa un gradiente sobre el color de la receta para superponer el texto de forma legible.
 */
@Composable
private fun TarjetaFavorita(receta: Receta) {
    val colorBase = colorParaReceta(receta.id)

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorBase)
    ) {
        // Gradiente inferior para legibilidad del texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        startY = 30f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = receta.titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
            Text(
                text = "${receta.kcalTotales} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Tarjeta de receta con soporte para deslizar hacia la izquierda y eliminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaRecetaSwipeable(
    receta: Receta,
    onTogglePin: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart) {
                onEliminar()
                true
            } else false
        },
        positionalThreshold = { it * 0.45f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            // Fondo rojo que aparece al deslizar
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300),
                label = "color_fondo_swipe"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar receta",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        TarjetaReceta(
            receta = receta,
            onTogglePin = onTogglePin
        )
    }
}

/**
 * Tarjeta apaisada de receta con bloque de color a la izquierda, datos nutricionales
 * a la derecha y botón de pin en la esquina superior derecha.
 */
@Composable
private fun TarjetaReceta(
    receta: Receta,
    onTogglePin: () -> Unit
) {
    val colorBase = colorParaReceta(receta.id)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bloque de color izquierdo con la inicial del título
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .background(colorBase),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = receta.titulo.first().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Datos nutricionales
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = receta.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${receta.kcalTotales} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "P: ${"%.0f".format(receta.proteinasG)}g  " +
                           "C: ${"%.0f".format(receta.carbosG)}g  " +
                           "G: ${"%.0f".format(receta.grasasG)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón de pin
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = if (receta.fijada) "Desfijar receta" else "Fijar receta",
                    tint = if (receta.fijada)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Barra de búsqueda ────────────────────────────────────────────────────────

@Composable
private fun BarraBusqueda(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = valor,
        onValueChange = onValorChange,
        placeholder = {
            Text(
                text = "Buscar en mis recetas...",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { onValorChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ─── Formulario de creación ───────────────────────────────────────────────────

/**
 * Formulario de creación manual de recetas, presentado dentro de un ModalBottomSheet.
 * Las kcal se calculan automáticamente a partir de los macros introducidos (P×4 + C×4 + G×9).
 */
@Composable
private fun ContenidoFormulario(
    formulario: FormularioNuevaReceta,
    alimentosGuardados: List<AlimentoGuardado>,
    onTituloChange: (String) -> Unit,
    onAgregarIngrediente: () -> Unit,
    onEliminarIngrediente: (Int) -> Unit,
    onActualizarUnidades: (Int, String) -> Unit,
    onActualizarAlimento: (Int, String) -> Unit,
    onActualizarPesoG: (Int, String) -> Unit,
    onUsarAlimentoGuardado: (AlimentoGuardado) -> Unit,
    onProteinasChange: (String) -> Unit,
    onCarbosChange: (String) -> Unit,
    onGrasasChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val kcalCalculadas by remember(formulario.proteinas, formulario.carbos, formulario.grasas) {
        derivedStateOf {
            val p = formulario.proteinas.toFloatOrNull() ?: 0f
            val c = formulario.carbos.toFloatOrNull() ?: 0f
            val g = formulario.grasas.toFloatOrNull() ?: 0f
            ((p * 4f) + (c * 4f) + (g * 9f)).toInt()
        }
    }
    var alimentosDesplegados by remember { mutableStateOf(true) }
    val alLimite = formulario.ingredientes.size >= 10

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nueva receta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }

        // Título
        OutlinedTextField(
            value = formulario.titulo,
            onValueChange = onTituloChange,
            label = { Text("Título *") },
            placeholder = { Text("Ej. Tortilla de 3 huevos") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Sección ingredientes ───────────────────────────────────────────────

        // Alimentos guardados en recetas anteriores (desplegable)
        if (alimentosGuardados.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { alimentosDesplegados = !alimentosDesplegados }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ALIMENTOS USADOS ANTERIORMENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (alimentosDesplegados) 180f else 0f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (alimentosDesplegados) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Column scrollable con altura máxima (~7 items) — evita LazyColumn anidada dentro del scroll externo
                    Column(modifier = Modifier
                        .heightIn(max = 252.dp)
                        .verticalScroll(rememberScrollState())
                    ) {
                        alimentosGuardados.take(20).forEach { alimento ->
                            Text(
                                text = alimento.nombre,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUsarAlimentoGuardado(alimento) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
        }

        // Encabezado de columnas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cant.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(62.dp))
            Text("Alimento", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Peso(g)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(68.dp))
            Spacer(modifier = Modifier.size(32.dp))
        }

        // Una fila por cada ingrediente
        formulario.ingredientes.forEachIndexed { idx, ing ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cantidad: solo dígitos y /
                OutlinedTextField(
                    value = ing.unidades,
                    onValueChange = { v -> onActualizarUnidades(idx, v.filter { it.isDigit() || it == '/' }) },
                    placeholder = { Text("Ej. 1/2", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(68.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                // Nombre del alimento
                OutlinedTextField(
                    value = ing.alimento,
                    onValueChange = { onActualizarAlimento(idx, it) },
                    placeholder = { Text("Ej. huevo", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                // Peso en gramos
                OutlinedTextField(
                    value = ing.pesoG,
                    onValueChange = { v -> onActualizarPesoG(idx, v.filter { it.isDigit() || it == '.' }) },
                    placeholder = { Text("g", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(62.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                // Eliminar fila (solo si hay más de 1)
                IconButton(
                    onClick = { onEliminarIngrediente(idx) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Eliminar ingrediente",
                        modifier = Modifier.size(16.dp),
                        tint = if (formulario.ingredientes.size > 1)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }

        if (alLimite) {
            Text(
                text = "Límite de 10 ingredientes. Borra uno para añadir más.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TextButton(
                onClick = onAgregarIngrediente,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir ingrediente", style = MaterialTheme.typography.labelMedium)
            }
        }

        HorizontalDivider()

        // Macronutrientes en fila de tres campos
        Text(
            text = "Macronutrientes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = formulario.proteinas,
                onValueChange = onProteinasChange,
                label = { Text("Proteínas (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = formulario.carbos,
                onValueChange = onCarbosChange,
                label = { Text("Carbos (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = formulario.grasas,
                onValueChange = onGrasasChange,
                label = { Text("Grasas (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // Kcal calculadas (solo lectura, se actualiza en tiempo real)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calorías totales calculadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$kcalCalculadas kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Error de validación
        if (formulario.error != null) {
            Text(
                text = formulario.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Botón guardar
        androidx.compose.material3.Button(
            onClick = onGuardar,
            enabled = !formulario.guardando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (formulario.guardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Guardar receta", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Estados auxiliares ───────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(hayFiltro: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hayFiltro) "Sin resultados para esa búsqueda"
                   else "Todavía no tienes recetas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hayFiltro) {
            Text(
                text = "Pulsa \"Añadir receta\" para empezar tu biblioteca",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
            TextButton(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PrevisualizacionRecetas() {
    ZentraTheme(temaOscuro = true) {
        val recetasEjemplo = listOf(
            Receta("1", "u1", "Tortilla de 3 huevos", 280, 22f, 2f, 20f, "", fijada = true),
            Receta("2", "u1", "Pollo al horno con boniato", 520, 45f, 42f, 12f, "", fijada = true),
            Receta("3", "u1", "Avena con plátano y whey", 450, 38f, 55f, 8f, ""),
            Receta("4", "u1", "Ensalada de atún", 310, 30f, 10f, 14f, "")
        )
        ContenidoRecetas(
            recetasFiltradas = recetasEjemplo,
            recetasFijadas = recetasEjemplo.filter { it.fijada },
            filtro = "",
            onFiltroChange = {},
            onTogglePin = {},
            onEliminar = {}
        )
    }
}
