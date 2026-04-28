package com.example.zentra.ui.screens.recetas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zentra.domain.model.Receta

/**
 * Pantalla de creación de recetas rediseñada.
 * Permite nombrar la receta, listar ingredientes, seleccionar recetas anteriores
 * para auto-rellenar macros, e introducir manualmente los valores nutricionales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaReceta(
    onRecetaGuardada: () -> Unit,
    onNavegacionAtras: () -> Unit,
    viewModel: NuevaRecetaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarDialogoSalida by remember { mutableStateOf(false) }

    val hayDatos by remember(estado.formulario) {
        derivedStateOf {
            estado.formulario.titulo.isNotBlank() ||
            estado.formulario.proteinas.isNotBlank() ||
            estado.formulario.carbos.isNotBlank() ||
            estado.formulario.grasas.isNotBlank() ||
            estado.formulario.ingredientes.any { it.alimento.isNotBlank() }
        }
    }

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onRecetaGuardada()
    }

    BackHandler(enabled = hayDatos) {
        mostrarDialogoSalida = true
    }

    if (mostrarDialogoSalida) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalida = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Si sales ahora perderás los datos del formulario.") },
            confirmButton = {
                TextButton(onClick = onNavegacionAtras) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalida = false }) {
                    Text("Seguir editando")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nueva receta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hayDatos) mostrarDialogoSalida = true else onNavegacionAtras()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        ContenidoNuevaReceta(
            formulario = estado.formulario,
            alimentosGuardados = estado.alimentosGuardados,
            recetasDisponibles = estado.recetasDisponibles,
            recetasSeleccionadas = estado.recetasSeleccionadas,
            onTituloChange = viewModel::actualizarTitulo,
            onAgregarIngrediente = viewModel::agregarIngrediente,
            onEliminarIngrediente = viewModel::eliminarIngrediente,
            onActualizarUnidades = viewModel::actualizarUnidades,
            onActualizarAlimento = viewModel::actualizarAlimento,
            onActualizarPesoG = viewModel::actualizarPesoG,
            onUsarAlimentoGuardado = viewModel::usarAlimentoGuardado,
            onToggleReceta = viewModel::toggleRecetaSeleccionada,
            onProteinasChange = viewModel::actualizarProteinas,
            onCarbosChange = viewModel::actualizarCarbos,
            onGrasasChange = viewModel::actualizarGrasas,
            onGuardar = viewModel::guardarNuevaReceta,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun ContenidoNuevaReceta(
    formulario: FormularioNuevaReceta,
    alimentosGuardados: List<AlimentoGuardado>,
    recetasDisponibles: List<Receta>,
    recetasSeleccionadas: Set<String>,
    onTituloChange: (String) -> Unit,
    onAgregarIngrediente: () -> Unit,
    onEliminarIngrediente: (Int) -> Unit,
    onActualizarUnidades: (Int, String) -> Unit,
    onActualizarAlimento: (Int, String) -> Unit,
    onActualizarPesoG: (Int, String) -> Unit,
    onUsarAlimentoGuardado: (AlimentoGuardado) -> Unit,
    onToggleReceta: (Receta) -> Unit,
    onProteinasChange: (String) -> Unit,
    onCarbosChange: (String) -> Unit,
    onGrasasChange: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kcalCalculadas by remember(formulario.proteinas, formulario.carbos, formulario.grasas) {
        derivedStateOf {
            val p = formulario.proteinas.toFloatOrNull() ?: 0f
            val c = formulario.carbos.toFloatOrNull() ?: 0f
            val g = formulario.grasas.toFloatOrNull() ?: 0f
            ((p * 4f) + (c * 4f) + (g * 9f)).toInt()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ─── Nombre del plato ─────────────────────────────────────────────────

        Spacer(modifier = Modifier.height(20.dp))
        SeccionLabel("NOMBRE DEL PLATO")
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = formulario.titulo,
            onValueChange = onTituloChange,
            placeholder = { Text("Ej. Tortilla de 3 huevos con espinacas") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Ingredientes ─────────────────────────────────────────────────────

        SeccionLabel("INGREDIENTES")
        Spacer(modifier = Modifier.height(12.dp))

        // Encabezado de columnas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cant.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Text(
                "Alimento",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Peso(g)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(68.dp)
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        formulario.ingredientes.forEachIndexed { idx, ing ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ing.unidades,
                    onValueChange = { v -> onActualizarUnidades(idx, v.filter { it.isDigit() || it == '/' }) },
                    placeholder = { Text("1/2", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(64.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = ing.alimento,
                    onValueChange = { onActualizarAlimento(idx, it) },
                    placeholder = { Text("Ej. huevo", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = ing.pesoG,
                    onValueChange = { v -> onActualizarPesoG(idx, v.filter { it.isDigit() || it == '.' }) },
                    placeholder = { Text("g", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(64.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = { onEliminarIngrediente(idx) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Eliminar ingrediente",
                        modifier = Modifier.size(18.dp),
                        tint = if (formulario.ingredientes.size > 1)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (formulario.ingredientes.size >= 10) {
            Text(
                text = "Límite de 10 ingredientes alcanzado.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            TextButton(onClick = onAgregarIngrediente) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Añadir ingrediente", style = MaterialTheme.typography.labelLarge)
            }
        }

        // ─── Recetas anteriores (selector de macros) ──────────────────────────

        if (recetasDisponibles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            SelectorRecetasAnteriores(
                recetas = recetasDisponibles,
                recetasSeleccionadas = recetasSeleccionadas,
                onToggle = onToggleReceta
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Macronutrientes ──────────────────────────────────────────────────

        SeccionLabel("MACRONUTRIENTES")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Introduce los gramos por cada macro para calcular las calorías automáticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampoMacro(
                label = "Proteínas",
                valor = formulario.proteinas,
                onValorChange = onProteinasChange,
                modifier = Modifier.weight(1f)
            )
            CampoMacro(
                label = "Carbos",
                valor = formulario.carbos,
                onValorChange = onCarbosChange,
                modifier = Modifier.weight(1f)
            )
            CampoMacro(
                label = "Grasas",
                valor = formulario.grasas,
                onValorChange = onGrasasChange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Kcal calculadas en tiempo real
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calorías totales",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Calculadas automáticamente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = "$kcalCalculadas kcal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (formulario.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = formulario.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGuardar,
            enabled = !formulario.guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (formulario.guardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    "Guardar receta",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Selector de recetas anteriores con tabla ─────────────────────────────────

@Composable
private fun SelectorRecetasAnteriores(
    recetas: List<Receta>,
    recetasSeleccionadas: Set<String>,
    onToggle: (Receta) -> Unit
) {
    var desplegado by remember { mutableStateOf(true) }

    SeccionLabel("RECETAS ANTERIORES")
    Spacer(modifier = Modifier.height(8.dp))

    // Encabezado colapsable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { desplegado = !desplegado }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (recetasSeleccionadas.isEmpty())
                "Selecciona para añadir sus macros automáticamente"
            else
                "${recetasSeleccionadas.size} receta(s) seleccionada(s)",
            style = MaterialTheme.typography.labelMedium,
            color = if (recetasSeleccionadas.isEmpty())
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.tertiary
        )
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = if (desplegado) "Colapsar" else "Expandir",
            modifier = Modifier
                .size(18.dp)
                .rotate(if (desplegado) 180f else 0f),
            tint = MaterialTheme.colorScheme.primary
        )
    }

    AnimatedVisibility(
        visible = desplegado,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Encabezado de columnas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Receta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "P",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    Text(
                        "C",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    Text(
                        "G",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(44.dp)
                    )
                    Spacer(modifier = Modifier.width(28.dp))
                }

                // Filas de recetas (máx ~5 visibles, con scroll)
                Column(
                    modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())
                ) {
                    recetas.forEach { receta ->
                        val seleccionada = receta.id in recetasSeleccionadas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (seleccionada)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else
                                        androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable { onToggle(receta) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = receta.titulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (seleccionada)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${receta.proteinasG.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = "${receta.carbosG.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = "${receta.grasasG.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = "${receta.kcalTotales}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp)
                            )
                            Icon(
                                imageVector = if (seleccionada)
                                    Icons.Outlined.CheckCircle
                                else
                                    Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (seleccionada) "Deseleccionar" else "Seleccionar",
                                modifier = Modifier.size(20.dp),
                                tint = if (seleccionada)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

// ─── Componentes auxiliares ───────────────────────────────────────────────────

@Composable
private fun SeccionLabel(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CampoMacro(
    label: String,
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = valor,
            onValueChange = { onValorChange(it.filter { c -> c.isDigit() || c == '.' }) },
            suffix = { Text("g", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
