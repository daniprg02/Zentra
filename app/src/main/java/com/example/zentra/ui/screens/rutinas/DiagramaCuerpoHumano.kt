package com.example.zentra.ui.screens.rutinas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zentra.domain.model.DiaRutina

/**
 * Diagrama anatómico simplificado del cuerpo humano en Canvas.
 * Resalta los grupos musculares según su frecuencia en la rutina activa.
 * Aplica proporciones distintas para perfil masculino y femenino.
 */
@Composable
fun DiagramaCuerpoHumano(
    dias: List<DiaRutina>,
    sexo: String,
    modifier: Modifier = Modifier
) {
    val musculosFrecuencia = remember(dias) {
        dias.flatMap { dia -> dia.ejercicios.map { it.grupoMuscular } }
            .groupingBy { it }
            .eachCount()
    }

    var vistaFrente by remember { mutableStateOf(true) }

    val bodyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val inactiveMuscleColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Músculos trabajados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.65f)) {
            SegmentedButton(
                selected = vistaFrente,
                onClick = { vistaFrente = true },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Frente") }
            SegmentedButton(
                selected = !vistaFrente,
                onClick = { vistaFrente = false },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Espalda") }
        }

        Spacer(Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .aspectRatio(0.48f)
        ) {
            val W = size.width
            val H = size.height

            // ── Coordenadas Y (como fracción de H) ─────────────────────────────
            val headCY = H * 0.065f
            val headR = H * 0.062f
            val neckTop = H * 0.13f
            val neckBot = H * 0.17f
            val chestTop = H * 0.17f
            val chestBot = H * 0.30f
            val coreTop = H * 0.30f
            val coreBot = H * 0.43f
            val hipTop = H * 0.43f
            val hipBot = H * 0.50f
            val thighTop = H * 0.50f
            val thighBot = H * 0.73f
            val calfTop = H * 0.74f
            val calfBot = H * 0.93f

            // ── Coordenadas X según sexo ────────────────────────────────────────
            val esMasculino = sexo != "Femenino"
            val torsoLeft = if (esMasculino) W * 0.22f else W * 0.27f
            val torsoW = if (esMasculino) W * 0.56f else W * 0.46f
            val armLeft = if (esMasculino) W * 0.03f else W * 0.08f
            val armW = if (esMasculino) W * 0.17f else W * 0.15f
            val hipLeft = if (esMasculino) W * 0.24f else W * 0.18f
            val hipW = if (esMasculino) W * 0.52f else W * 0.64f
            val thighGap = W * 0.04f
            val leftThighW = (hipW - thighGap) / 2f
            val rightThighX = hipLeft + leftThighW + thighGap
            val calfInset = W * 0.02f
            val leftCalfW = leftThighW - 2f * calfInset

            val shoulderR = W * if (esMasculino) 0.11f else 0.10f
            val shoulderCY = chestTop + (chestBot - chestTop) * 0.38f
            val leftShoulderCX = torsoLeft - shoulderR * 0.15f
            val rightShoulderCX = torsoLeft + torsoW + shoulderR * 0.15f

            // Función de color: inactivo → gris; activo → primary con alpha por frecuencia
            fun muscColor(musculo: String): Color {
                val freq = musculosFrecuencia[musculo] ?: 0
                return if (freq == 0) inactiveMuscleColor
                else primaryColor.copy(alpha = (0.45f + freq.coerceAtMost(5) * 0.11f).coerceAtMost(1f))
            }

            val cr5 = CornerRadius(5.dp.toPx())
            val cr6 = CornerRadius(6.dp.toPx())
            val cr8 = CornerRadius(8.dp.toPx())

            // ── Silueta base ───────────────────────────────────────────────────
            drawCircle(bodyColor, radius = headR, center = Offset(W * 0.5f, headCY))
            drawRoundRect(bodyColor, Offset(W * 0.43f, neckTop), Size(W * 0.14f, neckBot - neckTop), cr5)
            drawRoundRect(bodyColor, Offset(torsoLeft, chestTop), Size(torsoW, coreBot - chestTop), cr8)
            drawRoundRect(bodyColor, Offset(armLeft, chestTop), Size(armW, coreBot - chestTop), cr6)
            drawRoundRect(bodyColor, Offset(W - armLeft - armW, chestTop), Size(armW, coreBot - chestTop), cr6)
            drawRoundRect(bodyColor, Offset(hipLeft, hipTop), Size(hipW, hipBot - hipTop), cr6)
            drawRoundRect(bodyColor, Offset(hipLeft, thighTop), Size(leftThighW, thighBot - thighTop), cr8)
            drawRoundRect(bodyColor, Offset(rightThighX, thighTop), Size(leftThighW, thighBot - thighTop), cr8)
            drawRoundRect(bodyColor, Offset(hipLeft + calfInset, calfTop), Size(leftCalfW, calfBot - calfTop), cr6)
            drawRoundRect(bodyColor, Offset(rightThighX + calfInset, calfTop), Size(leftCalfW, calfBot - calfTop), cr6)

            if (vistaFrente) {
                // ── Vista frontal ───────────────────────────────────────────────
                drawCircle(muscColor("Hombros"), shoulderR, Offset(leftShoulderCX, shoulderCY))
                drawCircle(muscColor("Hombros"), shoulderR, Offset(rightShoulderCX, shoulderCY))
                drawRoundRect(muscColor("Pecho"),
                    Offset(torsoLeft + W * 0.01f, chestTop + H * 0.01f),
                    Size(torsoW - W * 0.02f, chestBot - chestTop - H * 0.015f), cr6)
                drawRoundRect(muscColor("Bíceps"),
                    Offset(armLeft, chestTop + H * 0.02f),
                    Size(armW, (coreBot - chestTop) * 0.55f), cr5)
                drawRoundRect(muscColor("Bíceps"),
                    Offset(W - armLeft - armW, chestTop + H * 0.02f),
                    Size(armW, (coreBot - chestTop) * 0.55f), cr5)
                drawRoundRect(muscColor("Core"),
                    Offset(torsoLeft + W * 0.02f, coreTop + H * 0.005f),
                    Size(torsoW - W * 0.04f, coreBot - coreTop - H * 0.01f), cr6)
                drawRoundRect(muscColor("Cuádriceps"),
                    Offset(hipLeft + W * 0.01f, thighTop + H * 0.01f),
                    Size(leftThighW - W * 0.01f, thighBot - thighTop - H * 0.02f), cr8)
                drawRoundRect(muscColor("Cuádriceps"),
                    Offset(rightThighX, thighTop + H * 0.01f),
                    Size(leftThighW - W * 0.01f, thighBot - thighTop - H * 0.02f), cr8)
                drawRoundRect(muscColor("Gemelos"),
                    Offset(hipLeft + calfInset, calfTop + H * 0.01f),
                    Size(leftCalfW, calfBot - calfTop - H * 0.015f), cr6)
                drawRoundRect(muscColor("Gemelos"),
                    Offset(rightThighX + calfInset, calfTop + H * 0.01f),
                    Size(leftCalfW, calfBot - calfTop - H * 0.015f), cr6)
            } else {
                // ── Vista trasera ────────────────────────────────────────────────
                drawCircle(muscColor("Hombros"), shoulderR, Offset(leftShoulderCX, shoulderCY))
                drawCircle(muscColor("Hombros"), shoulderR, Offset(rightShoulderCX, shoulderCY))
                drawRoundRect(muscColor("Espalda"),
                    Offset(torsoLeft + W * 0.01f, chestTop + H * 0.01f),
                    Size(torsoW - W * 0.02f, coreBot - chestTop - H * 0.02f), cr6)
                drawRoundRect(muscColor("Tríceps"),
                    Offset(armLeft, chestTop + H * 0.02f),
                    Size(armW, (coreBot - chestTop) * 0.55f), cr5)
                drawRoundRect(muscColor("Tríceps"),
                    Offset(W - armLeft - armW, chestTop + H * 0.02f),
                    Size(armW, (coreBot - chestTop) * 0.55f), cr5)
                val gluteH = (thighBot - thighTop) * 0.38f
                drawRoundRect(muscColor("Glúteos"),
                    Offset(hipLeft + W * 0.01f, thighTop + H * 0.005f),
                    Size(hipW - W * 0.02f, gluteH), cr8)
                drawRoundRect(muscColor("Isquiotibiales"),
                    Offset(hipLeft + W * 0.01f, thighTop + gluteH + H * 0.01f),
                    Size(leftThighW - W * 0.01f, thighBot - thighTop - gluteH - H * 0.02f), cr8)
                drawRoundRect(muscColor("Isquiotibiales"),
                    Offset(rightThighX, thighTop + gluteH + H * 0.01f),
                    Size(leftThighW - W * 0.01f, thighBot - thighTop - gluteH - H * 0.02f), cr8)
                drawRoundRect(muscColor("Gemelos"),
                    Offset(hipLeft + calfInset, calfTop + H * 0.01f),
                    Size(leftCalfW, calfBot - calfTop - H * 0.015f), cr6)
                drawRoundRect(muscColor("Gemelos"),
                    Offset(rightThighX + calfInset, calfTop + H * 0.01f),
                    Size(leftCalfW, calfBot - calfTop - H * 0.015f), cr6)
            }
        }

        // ── Leyenda ────────────────────────────────────────────────────────────
        val musculosActivos = musculosFrecuencia.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }

        if (musculosActivos.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                "Grupos musculares:",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            musculosActivos.forEach { (musculo, freq) ->
                val alpha = (0.45f + freq.coerceAtMost(5) * 0.11f).coerceAtMost(1f)
                Row(
                    modifier = Modifier.fillMaxWidth(0.75f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(primaryColor.copy(alpha = alpha), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$musculo · $freq ej.",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
