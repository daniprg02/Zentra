package com.example.zentra.data.remote.gemini

import android.util.Log
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.EjercicioEnRutina
import com.example.zentra.domain.model.Ejercicio
import com.example.zentra.ui.screens.rutinas.DatosCuestionario
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@Serializable
private data class RespuestaGemini(val dias: List<DiaGemini> = emptyList())

@Serializable
private data class DiaGemini(
    val nombre: String = "",
    val ejercicios: List<EjercicioGemini> = emptyList()
)

@Serializable
private data class EjercicioGemini(
    val id: String = "",
    val nombre: String = "",
    @SerialName("grupo_muscular") val grupoMuscular: String = "",
    val series: Int = 3,
    val repeticiones: String = "8-12"
)

/**
 * Llama a Gemini para generar un plan de entrenamiento personalizado.
 * Recibe el catálogo de ejercicios disponibles y devuelve los días del plan,
 * o null si falla (el ViewModel usará el algoritmo local como fallback).
 */
class GeminiGeneradorRutinas @Inject constructor(
    private val model: GenerativeModel
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generarRutina(
        datos: DatosCuestionario,
        ejercicios: List<Ejercicio>,
        rutinaId: String
    ): List<DiaRutina>? {
        return try {
            if (ejercicios.isEmpty()) return null

            val prompt = construirPrompt(datos, ejercicios)
            Log.d("GeminiGeneradorRutinas", "Prompt enviado. Ejercicios en catálogo: ${ejercicios.size}")

            val respuesta = model.generateContent(prompt)
            val texto = respuesta.text ?: run {
                Log.w("GeminiGeneradorRutinas", "Respuesta vacía de Gemini.")
                return null
            }

            val jsonLimpio = extraerJson(texto)
            Log.d("GeminiGeneradorRutinas", "JSON recibido (primeros 200 chars): ${jsonLimpio.take(200)}")

            val respuestaGemini = json.decodeFromString<RespuestaGemini>(jsonLimpio)

            if (respuestaGemini.dias.size != datos.diasSemana) {
                Log.w("GeminiGeneradorRutinas", "Días incorrectos: ${respuestaGemini.dias.size} vs ${datos.diasSemana} esperados. Fallback.")
                return null
            }

            val poolPorId = ejercicios.associateBy { it.id }

            val dias = respuestaGemini.dias.mapIndexed { index, diaGemini ->
                val ejerciciosValidos = diaGemini.ejercicios.mapNotNull { ejGemini ->
                    val ejReal = poolPorId[ejGemini.id] ?: return@mapNotNull null
                    EjercicioEnRutina(
                        ejercicioId = ejReal.id,
                        nombre = ejReal.nombre,
                        grupoMuscular = ejReal.grupoMuscular,
                        series = ejGemini.series.coerceIn(2, 6),
                        repeticiones = ejGemini.repeticiones.ifBlank { "8-12" }
                    )
                }
                if (ejerciciosValidos.isEmpty()) {
                    Log.w("GeminiGeneradorRutinas", "Día '${diaGemini.nombre}' sin ejercicios válidos. Fallback.")
                    return null
                }
                DiaRutina(
                    id = UUID.randomUUID().toString(),
                    rutinaId = rutinaId,
                    diaNumero = index + 1,
                    nombreDia = diaGemini.nombre,
                    ejercicios = ejerciciosValidos
                )
            }

            Log.d("GeminiGeneradorRutinas", "Rutina generada por Gemini: ${dias.size} días, ${dias.sumOf { it.ejercicios.size }} ejercicios totales.")
            dias
        } catch (e: Exception) {
            Log.e("GeminiGeneradorRutinas", "Error al generar rutina con Gemini: ${e.message}")
            null
        }
    }

    private fun construirPrompt(datos: DatosCuestionario, ejercicios: List<Ejercicio>): String {
        val musculosPrioritariosStr = datos.musculosPrioritarios
            .takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Ninguno"
        val (series, reps) = when (datos.objetivo) {
            "Déficit" -> "3" to "12-15"
            "Superávit" -> "4" to "6-10"
            else -> "4" to "8-12"
        }
        val catalogoStr = ejercicios.joinToString("\n") {
            "- id=\"${it.id}\" | nombre=\"${it.nombre}\" | grupo=\"${it.grupoMuscular}\""
        }

        return """
Eres un entrenador personal experto. Genera una rutina de entrenamiento personalizada.

PERFIL:
- Objetivo: ${datos.objetivo} (Déficit=perder grasa, Mantenimiento=mantener, Superávit=ganar músculo)
- Días/semana: ${datos.diasSemana}
- Experiencia: ${datos.experiencia}
- Lugar de entrenamiento: ${datos.lugarEntrenamiento}
- Músculos prioritarios: $musculosPrioritariosStr

REGLAS DE GENERACIÓN:
1. Crea EXACTAMENTE ${datos.diasSemana} días.
2. Cada día: entre 4 y 7 ejercicios.
3. Músculos prioritarios: incluye 3 ejercicios. Resto de músculos: 2 ejercicios.
4. Series para todos los ejercicios: $series | Repeticiones: $reps.
5. Usa ÚNICAMENTE ejercicios del catálogo (copia el id exactamente, sin modificarlo).
6. Aplica el split más adecuado para ${datos.diasSemana} días (Full Body, Push/Pull/Legs, Upper/Lower, etc.).

CATÁLOGO DE EJERCICIOS DISPONIBLES:
$catalogoStr

RESPONDE ÚNICAMENTE con JSON válido, sin texto extra ni bloques markdown:
{"dias":[{"nombre":"Push","ejercicios":[{"id":"id-exacto-del-catalogo","nombre":"Nombre exacto","grupo_muscular":"Grupo","series":$series,"repeticiones":"$reps"}]}]}
""".trimIndent()
    }

    private fun extraerJson(texto: String): String {
        val sinMarkdown = texto
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        val inicio = sinMarkdown.indexOf('{')
        val fin = sinMarkdown.lastIndexOf('}')
        return if (inicio != -1 && fin > inicio) sinMarkdown.substring(inicio, fin + 1) else sinMarkdown
    }
}
