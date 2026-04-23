package com.example.zentra.ui.screens.rutinas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.EjercicioEnRutina
import com.example.zentra.domain.model.RutinaUsuario

private val COLORES_DIA = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
    Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFF00BCD4)
)

private val TODOS_LOS_MUSCULOS = listOf(
    "Pecho", "Espalda", "Hombros",
    "Bíceps", "Tríceps", "Core",
    "Cuádriceps", "Isquiotibiales", "Glúteos", "Gemelos"
)

private val OPCIONES_EXPERIENCIA = listOf(
    "Nunca he entrenado", "Menos de 1 mes", "1 a 3 meses",
    "3 a 6 meses", "6 a 12 meses", "Más de 1 año"
)

private val OPCIONES_LUGAR = listOf(
    "Casa", "Calle", "Gimnasio grande", "Gimnasio mediano", "Gimnasio pequeño", "Mixto"
)

@Composable
fun PantallaRutinas(viewModel: RutinasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    when (val s = estado) {
        is EstadoRutinas.Cargando -> PantallaCargando()
        is EstadoRutinas.SinRutina -> PantallaSinRutina(
            todasLasRutinas = s.todasLasRutinas,
            rutinaParaEliminar = s.rutinaParaEliminar,
            onIniciarCuestionario = viewModel::iniciarCuestionario,
            onActivarRutina = viewModel::activarRutina,
            onPedirEliminar = viewModel::pedirEliminarRutina,
            onCancelarEliminar = viewModel::cancelarEliminarRutina,
            onConfirmarEliminar = viewModel::confirmarEliminarRutina
        )
        is EstadoRutinas.EnCuestionario -> PantallaCuestionario(
            paso = s.paso,
            datos = s.datos,
            onActualizar = viewModel::actualizarDatos,
            onSiguiente = viewModel::siguientePaso,
            onAtras = viewModel::anteriorPaso
        )
        is EstadoRutinas.Generando -> PantallaGenerando()
        is EstadoRutinas.RutinaActiva -> PantallaRutinaActiva(
            cabecera = s.cabecera,
            dias = s.dias,
            todasLasRutinas = s.todasLasRutinas,
            mostrandoDialogoNueva = s.mostrandoDialogoNueva,
            rutinaParaEliminar = s.rutinaParaEliminar,
            onPedirNuevaRutina = viewModel::pedirNuevaRutina,
            onCancelarNuevaRutina = viewModel::cancelarNuevaRutina,
            onConfirmarNuevaRutina = viewModel::confirmarNuevaRutina,
            onActivarRutina = viewModel::activarRutina,
            onPedirEliminar = viewModel::pedirEliminarRutina,
            onCancelarEliminar = viewModel::cancelarEliminarRutina,
            onConfirmarEliminar = viewModel::confirmarEliminarRutina
        )
        is EstadoRutinas.Error -> PantallaError(
            mensaje = s.mensaje,
            onReintentar = viewModel::cargarRutinaActiva
        )
    }
}

// ─────────────────────────────────────────────
// Sin rutina activa
// ─────────────────────────────────────────────

@Composable
private fun PantallaSinRutina(
    todasLasRutinas: List<RutinaUsuario>,
    rutinaParaEliminar: RutinaUsuario?,
    onIniciarCuestionario: () -> Unit,
    onActivarRutina: (RutinaUsuario) -> Unit,
    onPedirEliminar: (RutinaUsuario) -> Unit,
    onCancelarEliminar: () -> Unit,
    onConfirmarEliminar: () -> Unit
) {
    DialogoEliminarRutina(
        rutina = rutinaParaEliminar,
        onCancelar = onCancelarEliminar,
        onConfirmar = onConfirmarEliminar
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Tu plan de entrenamiento",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Responde unas preguntas rápidas y Zentra diseñará una rutina adaptada a ti.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onIniciarCuestionario, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar mi rutina")
                }
            }
        }

        if (todasLasRutinas.isNotEmpty()) {
            item {
                Text(
                    "RUTINAS GUARDADAS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(todasLasRutinas, key = { it.id }) { rutina ->
                TarjetaRutinaHistorial(
                    rutina = rutina,
                    esActiva = rutina.activa,
                    onActivar = { onActivarRutina(rutina) },
                    onEliminar = { onPedirEliminar(rutina) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Cuestionario de generación
// ─────────────────────────────────────────────

@Composable
private fun PantallaCuestionario(
    paso: Int,
    datos: DatosCuestionario,
    onActualizar: (DatosCuestionario) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { paso / 3f }, modifier = Modifier.fillMaxWidth())
        Text(
            "Paso $paso de 3",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            when (paso) {
                1 -> PasoDiasYObjetivo(datos = datos, onActualizar = onActualizar)
                2 -> PasoMusculosPrioritarios(datos = datos, onActualizar = onActualizar)
                3 -> PasoExperienciaYLugar(datos = datos, onActualizar = onActualizar)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onAtras, modifier = Modifier.weight(1f)) {
                Text(if (paso == 1) "Cancelar" else "Atrás")
            }
            Button(onClick = onSiguiente, modifier = Modifier.weight(1f)) {
                Text(if (paso == 3) "Generar rutina" else "Siguiente")
            }
        }
    }
}

@Composable
private fun PasoDiasYObjetivo(datos: DatosCuestionario, onActualizar: (DatosCuestionario) -> Unit) {
    Column {
        Text("¿Cuántos días entrenas a la semana?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (2..6).forEach { n ->
                FilterChip(
                    selected = datos.diasSemana == n,
                    onClick = { onActualizar(datos.copy(diasSemana = n)) },
                    label = { Text("$n") }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text("¿Cuál es tu objetivo principal?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        val opciones = listOf("Déficit", "Mantenimiento", "Superávit")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            opciones.forEachIndexed { index, opcion ->
                SegmentedButton(
                    selected = datos.objetivo == opcion,
                    onClick = { onActualizar(datos.copy(objetivo = opcion)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size)
                ) { Text(opcion, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun PasoMusculosPrioritarios(datos: DatosCuestionario, onActualizar: (DatosCuestionario) -> Unit) {
    Column {
        Text("¿Qué músculos quieres priorizar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Recibirán más ejercicios. Puedes no seleccionar ninguno.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        TODOS_LOS_MUSCULOS.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { musculo ->
                    val sel = musculo in datos.musculosPrioritarios
                    FilterChip(
                        selected = sel,
                        onClick = {
                            val nuevos = datos.musculosPrioritarios.toMutableSet()
                            if (sel) nuevos.remove(musculo) else nuevos.add(musculo)
                            onActualizar(datos.copy(musculosPrioritarios = nuevos))
                        },
                        label = { Text(musculo) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (sel) {
                            { Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null
                    )
                }
                if (fila.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PasoExperienciaYLugar(datos: DatosCuestionario, onActualizar: (DatosCuestionario) -> Unit) {
    Column {
        Text("¿Cuánta experiencia tienes entrenando?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        OPCIONES_EXPERIENCIA.forEach { opcion ->
            val sel = datos.experiencia == opcion
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onActualizar(datos.copy(experiencia = opcion)) }.padding(vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        opcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (sel) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text("¿Dónde vas a entrenar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        OPCIONES_LUGAR.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { lugar ->
                    FilterChip(
                        selected = datos.lugarEntrenamiento == lugar,
                        onClick = { onActualizar(datos.copy(lugarEntrenamiento = lugar)) },
                        label = { Text(lugar) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (fila.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Generando rutina
// ─────────────────────────────────────────────

@Composable
private fun PantallaGenerando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text("Generando tu rutina...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Personalizando ejercicios y volumen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────
// Rutina activa + historial de rutinas
// ─────────────────────────────────────────────

@Composable
private fun PantallaRutinaActiva(
    cabecera: RutinaUsuario,
    dias: List<DiaRutina>,
    todasLasRutinas: List<RutinaUsuario>,
    mostrandoDialogoNueva: Boolean,
    rutinaParaEliminar: RutinaUsuario?,
    onPedirNuevaRutina: () -> Unit,
    onCancelarNuevaRutina: () -> Unit,
    onConfirmarNuevaRutina: () -> Unit,
    onActivarRutina: (RutinaUsuario) -> Unit,
    onPedirEliminar: (RutinaUsuario) -> Unit,
    onCancelarEliminar: () -> Unit,
    onConfirmarEliminar: () -> Unit
) {
    val expandidos = remember { mutableStateMapOf<Int, Boolean>() }

    if (mostrandoDialogoNueva) {
        AlertDialog(
            onDismissRequest = onCancelarNuevaRutina,
            title = { Text("¿Crear una nueva rutina?") },
            text = { Text("Se generará un nuevo plan. Tu rutina actual quedará guardada en el historial.") },
            confirmButton = {
                TextButton(onClick = onConfirmarNuevaRutina) {
                    Text("Sí, crear nueva", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = { TextButton(onClick = onCancelarNuevaRutina) { Text("Cancelar") } }
        )
    }

    DialogoEliminarRutina(
        rutina = rutinaParaEliminar,
        onCancelar = onCancelarEliminar,
        onConfirmar = onConfirmarEliminar
    )

    // Rutinas anteriores (excluye la activa actual)
    val rutinasAnteriores = todasLasRutinas.filter { it.id != cabecera.id }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPedirNuevaRutina,
                icon = { Icon(Icons.Outlined.Refresh, null) },
                text = { Text("Nueva rutina") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CabeceraPlan(cabecera = cabecera) }

            items(dias, key = { it.id }) { dia ->
                val expandido = expandidos[dia.diaNumero] ?: (dia.diaNumero == 1)
                TarjetaDia(
                    dia = dia,
                    colorAccento = COLORES_DIA[(dia.diaNumero - 1) % COLORES_DIA.size],
                    expandido = expandido,
                    onToggle = { expandidos[dia.diaNumero] = !expandido }
                )
            }

            // Sección de rutinas anteriores
            if (rutinasAnteriores.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "RUTINAS GUARDADAS (${rutinasAnteriores.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(rutinasAnteriores, key = { it.id }) { rutina ->
                    TarjetaRutinaHistorial(
                        rutina = rutina,
                        esActiva = false,
                        onActivar = { onActivarRutina(rutina) },
                        onEliminar = { onPedirEliminar(rutina) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────
// Componentes compartidos
// ─────────────────────────────────────────────

@Composable
private fun TarjetaRutinaHistorial(
    rutina: RutinaUsuario,
    esActiva: Boolean,
    onActivar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${rutina.diasSemana}d",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Plan de ${rutina.diasSemana} días",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    rutina.objetivo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!esActiva) {
                TextButton(
                    onClick = onActivar,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Activar", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Eliminar rutina",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DialogoEliminarRutina(
    rutina: RutinaUsuario?,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit
) {
    if (rutina == null) return
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Eliminar esta rutina?") },
        text = { Text("Se borrará permanentemente el plan de ${rutina.diasSemana} días (${rutina.objetivo}). Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Sí, eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun CabeceraPlan(cabecera: RutinaUsuario) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Plan activo · ${cabecera.diasSemana} días",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Objetivo: ${cabecera.objetivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TarjetaDia(
    dia: DiaRutina,
    colorAccento: Color,
    expandido: Boolean,
    onToggle: () -> Unit
) {
    val rotacion by animateFloatAsState(
        targetValue = if (expandido) 180f else 0f,
        animationSpec = tween(200),
        label = "rotacion_flecha_dia"
    )
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colorAccento),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${dia.diaNumero}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Día ${dia.diaNumero}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dia.nombreDia, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text("${dia.ejercicios.size} ejercicios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotacion),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expandido, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    dia.ejercicios.forEach { ej -> ItemEjercicio(ejercicio = ej, colorAccento = colorAccento) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ItemEjercicio(ejercicio: EjercicioEnRutina, colorAccento: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorAccento))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ejercicio.nombre, style = MaterialTheme.typography.bodyMedium)
            Text(ejercicio.grupoMuscular, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${ejercicio.series} × ${ejercicio.repeticiones}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorAccento
        )
    }
}

// ─────────────────────────────────────────────
// Estados auxiliares
// ─────────────────────────────────────────────

@Composable
private fun PantallaCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PantallaError(mensaje: String, onReintentar: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onReintentar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Reintentar")
            }
        }
    }
}
