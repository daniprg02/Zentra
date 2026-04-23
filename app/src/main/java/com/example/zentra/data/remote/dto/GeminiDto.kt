package com.example.zentra.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Request ──────────────────────────────────────────────────────────────────

data class PeticionGemini(
    val contents: List<ContenidoGeminiReq>,
    @SerializedName("generationConfig") val configuracion: ConfiguracionGemini = ConfiguracionGemini()
)

data class ContenidoGeminiReq(
    val parts: List<ParteGeminiReq>
)

data class ParteGeminiReq(
    val text: String
)

data class ConfiguracionGemini(
    val temperature: Float = 1.0f,
    val maxOutputTokens: Int = 8192
)

// ─── Response ─────────────────────────────────────────────────────────────────

data class RespuestaGeminiApi(
    val candidates: List<CandidatoGemini>?
)

data class CandidatoGemini(
    val content: ContenidoGeminiResp?
)

data class ContenidoGeminiResp(
    val parts: List<ParteGeminiReq>?
)
