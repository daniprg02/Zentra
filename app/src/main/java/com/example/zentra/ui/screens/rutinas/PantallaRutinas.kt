package com.example.zentra.ui.screens.rutinas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val OPCIONES_LUGAR_MIXTO = listOf(
    "Casa", "Calle", "Gimnasio grande", "Gimnasio mediano", "Gimnasio pequeño"
)

@Composable
fun PantallaRutinas(viewModel: RutinasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    when (val s = estado) {
        is EstadoRutinas.Cargando -> PantallaCargando()

        is EstadoRutinas.SinRutina -> PantallaSinRutina(
            todasLasRutinas = s.todasLasRutinas,
            rutinaParaEliminar = s.rutinaParaEliminar,
            sinConexion = s.sinConexion,
            onIniciarCuestionarioIA = viewModel::iniciarCuestionario,
            onIniciarCuestionarioLocal = viewModel::iniciarCuestionarioLocal,
            onActivarRutina = viewModel::activarRutina,
            onPedirEliminar = viewModel::pedirEliminarRutina,
            onCancelarEliminar = viewModel::cancelarEliminarRutina,
            onConfirmarEliminar = viewModel::confirmarEliminarRutina
        )

        is EstadoRutinas.EnCuestionario -> PantallaCuestionario(
            paso = s.paso,
            datos = s.datos,
            sexo = s.sexo,
            onActualizar = viewModel::actualizarDatos,
            onSiguiente = viewModel::siguientePaso,
            onAtras = viewModel::anteriorPaso
        )

        is EstadoRutinas.Generando -> PantallaGenerando(mensaje = s.mensaje, esIA = s.esIA)

        is EstadoRutinas.RutinaActiva -> PantallaRutinaActiva(
            cabecera = s.cabecera,
            dias = s.dias,
            todasLasRutinas = s.todasLasRutinas,
            sexo = s.sexo,
            mostrandoDialogoNueva = s.mostrandoDialogoNueva,
            rutinaParaEliminar = s.rutinaParaEliminar,
            ejercicioEditando = s.ejercicioEditando,
            sustitucionEnCurso = s.sustitucionEnCurso,
            sinConexion = s.sinConexion,
            esRutinaBasica = s.esRutinaBasica,
            onPedirNuevaRutinaIA = viewModel::pedirNuevaRutinaIA,
            onPedirNuevaRutinaLocal = viewModel::pedirNuevaRutinaLocal,
            onCancelarNuevaRutina = viewModel::cancelarNuevaRutina,
            onConfirmarNuevaRutina = viewModel::confirmarNuevaRutina,
            onActivarRutina = viewModel::activarRutina,
            onPedirEliminar = viewModel::pedirEliminarRutina,
            onCancelarEliminar = viewModel::cancelarEliminarRutina,
            onConfirmarEliminar = viewModel::confirmarEliminarRutina,
            onIniciarEdicion = viewModel::iniciarEdicionEjercicio,
            onCancelarEdicion = viewModel::cancelarEdicionEjercicio,
            onGuardarEdicion = viewModel::guardarEdicionEjercicio,
            onSustituirConIA = viewModel::sustituirEjercicioConIA,
            onCambiarGrupoMuscular = viewModel::cambiarGrupoMuscularEjercicio
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
    sinConexion: Boolean,
    onIniciarCuestionarioIA: () -> Unit,
    onIniciarCuestionarioLocal: () -> Unit,
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

    Column(modifier = Modifier.fillMaxSize()) {
        if (sinConexion) BannerSinConexion()

        LazyColumn(
            modifier = Modifier.weight(1f),
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
                            Icons.Outlined.FitnessCenter, contentDescription = null,
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
                    // Botón principal: rutina con IA
                    Button(
                        onClick = onIniciarCuestionarioIA,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar con IA")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Botón secundario: rutina básica sin red
                    OutlinedButton(
                        onClick = onIniciarCuestionarioLocal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear rutina básica (sin internet)")
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
}

// ─────────────────────────────────────────────
// Cuestionario de generación (4 pasos)
// ─────────────────────────────────────────────

@Composable
private fun PantallaCuestionario(
    paso: Int,
    datos: DatosCuestionario,
    sexo: String,
    onActualizar: (DatosCuestionario) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit
) {
    val maxPasos = if (datos.generarConIA) 4 else 3
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { paso / maxPasos.toFloat() }, modifier = Modifier.fillMaxWidth())
        Text(
            "Paso $paso de $maxPasos${if (!datos.generarConIA) " · Rutina básica" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            when (paso) {
                1 -> PasoDiasYObjetivo(datos = datos, onActualizar = onActualizar)
                2 -> PasoMusculosPrioritarios(datos = datos, sexo = sexo, onActualizar = onActualizar)
                3 -> PasoExperienciaYLugar(datos = datos, onActualizar = onActualizar)
                4 -> PasoLesiones(datos = datos, onActualizar = onActualizar)
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
                Text(if (paso == maxPasos) "Generar rutina" else "Siguiente")
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
private fun PasoMusculosPrioritarios(
    datos: DatosCuestionario,
    sexo: String,
    onActualizar: (DatosCuestionario) -> Unit
) {
    val frecuenciaParaDiagrama = remember(datos.musculosPrioritarios) {
        datos.musculosPrioritarios.associateWith { 5 }
    }

    Column {
        Text("¿Qué músculos quieres priorizar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Recibirán más ejercicios. Puedes no seleccionar ninguno.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            DiagramaCuerpoHumano(
                musculosFrecuencia = frecuenciaParaDiagrama,
                sexo = sexo,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            )
        }

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
                modifier = Modifier.fillMaxWidth()
                    .clickable { onActualizar(datos.copy(experiencia = opcion)) }
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        onClick = {
                            onActualizar(
                                datos.copy(
                                    lugarEntrenamiento = lugar,
                                    materialDisponible = "",
                                    lugaresSeleccionados = emptyList()
                                )
                            )
                        },
                        label = { Text(lugar) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (fila.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (datos.lugarEntrenamiento == "Casa" || datos.lugarEntrenamiento == "Calle") {
            Spacer(modifier = Modifier.height(8.dp))
            val placeholder = if (datos.lugarEntrenamiento == "Casa")
                "Ej: mancuernas, goma elástica, esterilla..."
            else
                "Ej: barras de dominadas, anillas... o deja vacío si no tienes"
            val label = if (datos.lugarEntrenamiento == "Casa")
                "¿Qué material tienes disponible?"
            else
                "¿Tienes algún equipamiento?"
            OutlinedTextField(
                value = datos.materialDisponible,
                onValueChange = { onActualizar(datos.copy(materialDisponible = it)) },
                label = { Text(label) },
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (datos.lugarEntrenamiento == "Mixto") {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Selecciona hasta 2 lugares:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OPCIONES_LUGAR_MIXTO.chunked(2).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fila.forEach { lugar ->
                        val idx = datos.lugaresSeleccionados.indexOf(lugar)
                        val seleccionado = idx >= 0
                        val badge = when (idx) { 0 -> " ①"; 1 -> " ②"; else -> "" }
                        FilterChip(
                            selected = seleccionado,
                            onClick = {
                                val nuevos = datos.lugaresSeleccionados.toMutableList()
                                if (seleccionado) {
                                    nuevos.remove(lugar)
                                } else if (nuevos.size < 2) {
                                    nuevos.add(lugar)
                                }
                                onActualizar(datos.copy(lugaresSeleccionados = nuevos))
                            },
                            label = { Text(lugar + badge) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (fila.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PasoLesiones(datos: DatosCuestionario, onActualizar: (DatosCuestionario) -> Unit) {
    Column {
        Text(
            "¿Tienes alguna lesión o limitación física?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Gemini la tendrá en cuenta para evitar sobrecargar la zona afectada. Si no tienes ninguna, puedes dejarlo vacío.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = datos.lesiones,
            onValueChange = { onActualizar(datos.copy(lesiones = it)) },
            label = { Text("Lesiones o limitaciones (opcional)") },
            placeholder = { Text("Ej: tendinitis en el hombro derecho, dolor lumbar crónico, rodilla operada...") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ─────────────────────────────────────────────
// Generando rutina
// ─────────────────────────────────────────────

@Composable
private fun PantallaGenerando(mensaje: String, esIA: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "ia_pulse")
    val escala by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala_ia"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_ia"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            if (esIA) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(escala)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF9B4DCA).copy(alpha = alpha),
                                    Color(0xFF2563EB).copy(alpha = alpha)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "INTELIGENCIA ARTIFICIAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7B2FBE),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                mensaje,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Puede tardar unos segundos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────
// Rutina activa + historial
// ─────────────────────────────────────────────

@Composable
private fun PantallaRutinaActiva(
    cabecera: RutinaUsuario,
    dias: List<DiaRutina>,
    todasLasRutinas: List<RutinaUsuario>,
    sexo: String,
    mostrandoDialogoNueva: Boolean,
    rutinaParaEliminar: RutinaUsuario?,
    ejercicioEditando: EjercicioEditando?,
    sustitucionEnCurso: Pair<Int, Int>?,
    sinConexion: Boolean,
    esRutinaBasica: Boolean,
    onPedirNuevaRutinaIA: () -> Unit,
    onPedirNuevaRutinaLocal: () -> Unit,
    onCancelarNuevaRutina: () -> Unit,
    onConfirmarNuevaRutina: () -> Unit,
    onActivarRutina: (RutinaUsuario) -> Unit,
    onPedirEliminar: (RutinaUsuario) -> Unit,
    onCancelarEliminar: () -> Unit,
    onConfirmarEliminar: () -> Unit,
    onIniciarEdicion: (diaNumero: Int, ejercicioIdx: Int) -> Unit,
    onCancelarEdicion: () -> Unit,
    onGuardarEdicion: (series: Int, reps: String) -> Unit,
    onSustituirConIA: (diaNumero: Int, ejercicioIdx: Int) -> Unit,
    onCambiarGrupoMuscular: (String) -> Unit
) {
    val expandidos = remember { mutableStateMapOf<Int, Boolean>() }
    var fabExpandido by remember { mutableStateOf(false) }
    val rotacionFab by animateFloatAsState(
        targetValue = if (fabExpandido) 180f else 0f,
        animationSpec = tween(200),
        label = "rotacion_fab_nueva_rutina"
    )

    val musculosFrecuencia = remember(dias) {
        dias.flatMap { dia -> dia.ejercicios.map { it.grupoMuscular } }
            .groupingBy { it }.eachCount()
    }

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

    DialogoEditarEjercicio(
        edicion = ejercicioEditando,
        onCancelar = onCancelarEdicion,
        onGuardar = onGuardarEdicion,
        onCambiarGrupoMuscular = onCambiarGrupoMuscular
    )

    val rutinasAnteriores = todasLasRutinas.filter { it.id != cabecera.id }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sub-FABs: visibles cuando el FAB principal está expandido
                AnimatedVisibility(
                    visible = fabExpandido,
                    enter = expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(200)) + fadeIn(tween(150)),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(200)) + fadeOut(tween(150))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sub-FAB plantilla local (colores de la app)
                        Box(
                            modifier = Modifier
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    fabExpandido = false
                                    onPedirNuevaRutinaLocal()
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Plantilla local",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Sub-FAB IA (diseño original conservado)
                        Box(
                            modifier = Modifier
                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF7B2FBE), Color(0xFF2563EB)))
                                )
                                .clickable {
                                    fabExpandido = false
                                    onPedirNuevaRutinaIA()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        "IA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Rutina con IA",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // FAB principal: "Nueva Rutina" — expande/colapsa los sub-FABs
                Box(
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { fabExpandido = !fabExpandido }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Nueva Rutina",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp).rotate(rotacionFab)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sinConexion) {
                item { BannerSinConexion() }
            }

            if (esRutinaBasica) {
                item { BannerRutinaBasica() }
            }

            item { CabeceraPlan(cabecera = cabecera) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    DiagramaCuerpoHumano(
                        musculosFrecuencia = musculosFrecuencia,
                        sexo = sexo,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            }

            items(dias, key = { it.id }) { dia ->
                val expandido = expandidos[dia.diaNumero] ?: (dia.diaNumero == 1)
                TarjetaDia(
                    dia = dia,
                    colorAccento = COLORES_DIA[(dia.diaNumero - 1) % COLORES_DIA.size],
                    expandido = expandido,
                    sustitucionEnCurso = sustitucionEnCurso,
                    onToggle = { expandidos[dia.diaNumero] = !expandido },
                    onIniciarEdicion = { ejercicioIdx -> onIniciarEdicion(dia.diaNumero, ejercicioIdx) },
                    onSustituirConIA = { ejercicioIdx -> onSustituirConIA(dia.diaNumero, ejercicioIdx) }
                )
            }

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
private fun BannerSinConexion() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "Sin conexión · Mostrando datos guardados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun BannerRutinaBasica() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    "Rutina básica generada localmente",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Puedes usarla como plantilla y personalizarla editando series, repeticiones o cambiando grupos musculares con el botón de intercambio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ChipTipoRutina(esIA: Boolean) {
    val color = if (esIA) Color(0xFF7B2FBE) else MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = if (esIA) "IA" else "Local",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TarjetaRutinaHistorial(
    rutina: RutinaUsuario,
    esActiva: Boolean,
    onActivar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Plan de ${rutina.diasSemana} días",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ChipTipoRutina(esIA = rutina.generadaConIA)
                }
                Text(rutina.objetivo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!esActiva) {
                TextButton(onClick = onActivar, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Activar", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar rutina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DialogoEliminarRutina(rutina: RutinaUsuario?, onCancelar: () -> Unit, onConfirmar: () -> Unit) {
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

/**
 * Diálogo para editar series, repeticiones y grupo muscular de un ejercicio.
 * El botón de intercambio despliega burbujas coloreadas de grupos disponibles
 * (excluye el grupo actual y los ya presentes en ese día).
 */
@Composable
private fun DialogoEditarEjercicio(
    edicion: EjercicioEditando?,
    onCancelar: () -> Unit,
    onGuardar: (series: Int, reps: String) -> Unit,
    onCambiarGrupoMuscular: (String) -> Unit
) {
    if (edicion == null) return

    var seriesLocal by remember(edicion) { mutableStateOf(edicion.series) }
    var repsLocal by remember(edicion) { mutableStateOf(edicion.repeticiones) }
    var mostraBurbujas by remember(edicion) { mutableStateOf(false) }

    val colorGrupoActual = COLORES_MUSCULARES[edicion.grupoMuscular] ?: MaterialTheme.colorScheme.primary
    // Permitimos cualquier grupo distinto al actual; la unicidad se controla a nivel de ejercicio en el ViewModel.
    val gruposDisponibles = TODOS_LOS_MUSCULOS.filter { it != edicion.grupoMuscular }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar ejercicio") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    edicion.nombreEjercicio,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                // Burbuja del grupo muscular actual + icono de cambio
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorGrupoActual.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, colorGrupoActual)
                    ) {
                        Text(
                            edicion.grupoMuscular,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorGrupoActual,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (gruposDisponibles.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { mostraBurbujas = !mostraBurbujas },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.SwapHoriz,
                                contentDescription = "Cambiar grupo muscular",
                                modifier = Modifier.size(18.dp),
                                tint = if (mostraBurbujas) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grid colapsable de grupos disponibles para cambiar
                AnimatedVisibility(visible = mostraBurbujas) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            "Cambiar a:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        gruposDisponibles.chunked(2).forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                fila.forEach { grupo ->
                                    val colorGrupo = COLORES_MUSCULARES[grupo] ?: MaterialTheme.colorScheme.primary
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onCambiarGrupoMuscular(grupo) },
                                        shape = RoundedCornerShape(50),
                                        color = colorGrupo.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, colorGrupo)
                                    ) {
                                        Text(
                                            grupo,
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .fillMaxWidth(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorGrupo,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (fila.size < 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Series:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..6).forEach { n ->
                        FilterChip(
                            selected = seriesLocal == n,
                            onClick = { seriesLocal = n },
                            label = { Text("$n") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = repsLocal,
                    onValueChange = { repsLocal = it },
                    label = { Text("Repeticiones") },
                    placeholder = { Text("Ej: 8-12, 15, 10-15") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(seriesLocal, repsLocal) }) { Text("Guardar") }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Plan activo · ${cabecera.diasSemana} días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ChipTipoRutina(esIA = cabecera.generadaConIA)
                }
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
    sustitucionEnCurso: Pair<Int, Int>?,
    onToggle: () -> Unit,
    onIniciarEdicion: (ejercicioIdx: Int) -> Unit,
    onSustituirConIA: (ejercicioIdx: Int) -> Unit
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
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotacion), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expandido, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    dia.ejercicios.forEachIndexed { idx, ej ->
                        val esSustituyendo = sustitucionEnCurso != null &&
                            sustitucionEnCurso.first == dia.diaNumero &&
                            sustitucionEnCurso.second == idx
                        ItemEjercicio(
                            ejercicio = ej,
                            ejercicioIdx = idx,
                            esSustituyendo = esSustituyendo,
                            onEditar = { onIniciarEdicion(idx) },
                            onSustituir = { onSustituirConIA(idx) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ItemEjercicio(
    ejercicio: EjercicioEnRutina,
    ejercicioIdx: Int,
    esSustituyendo: Boolean,
    onEditar: () -> Unit,
    onSustituir: () -> Unit
) {
    val colorMusculo = COLORES_MUSCULARES[ejercicio.grupoMuscular]
        ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorMusculo))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ejercicio.nombre, style = MaterialTheme.typography.bodyMedium)
            Text(ejercicio.grupoMuscular, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (esSustituyendo) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colorMusculo)
        } else {
            Text(
                "${ejercicio.series} × ${ejercicio.repeticiones}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorMusculo
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Editar series y repeticiones",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSustituir, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Sync,
                    contentDescription = "Sustituir ejercicio con IA",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
