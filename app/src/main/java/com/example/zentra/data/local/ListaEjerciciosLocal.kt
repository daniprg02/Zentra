package com.example.zentra.data.local

import com.example.zentra.domain.model.Ejercicio

/**
 * Catálogo local de ejercicios disponibles sin conexión.
 * Se usa cuando el usuario genera una rutina básica (sin IA) o cuando
 * no hay internet y se necesita crear un plan de fallback.
 *
 * Cubre los 10 grupos musculares con múltiples tipos de equipamiento.
 */
object ListaEjerciciosLocal {

    val todos: List<Ejercicio> = listOf(

        // ─── PECHO ────────────────────────────────────────────────────────────
        Ejercicio("loc_pec_01", "Push-up",                      "Pecho", "Calistenia",  "Principiante"),
        Ejercicio("loc_pec_02", "Push-up inclinado (pies alto)", "Pecho", "Calistenia",  "Intermedio"),
        Ejercicio("loc_pec_03", "Push-up declinado",             "Pecho", "Calistenia",  "Intermedio"),
        Ejercicio("loc_pec_04", "Fondos entre bancos",           "Pecho", "Calistenia",  "Principiante"),
        Ejercicio("loc_pec_05", "Press de banca",                "Pecho", "Barra",       "Intermedio"),
        Ejercicio("loc_pec_06", "Press de banca inclinado",      "Pecho", "Barra",       "Intermedio"),
        Ejercicio("loc_pec_07", "Press con mancuernas",          "Pecho", "Mancuernas",  "Principiante"),
        Ejercicio("loc_pec_08", "Aperturas con mancuernas",      "Pecho", "Mancuernas",  "Principiante"),
        Ejercicio("loc_pec_09", "Cruce de poleas",               "Pecho", "Cable",       "Intermedio"),
        Ejercicio("loc_pec_10", "Press en máquina pecho",        "Pecho", "Máquina",     "Principiante"),

        // ─── ESPALDA ──────────────────────────────────────────────────────────
        Ejercicio("loc_esp_01", "Dominadas supinas",             "Espalda", "Calistenia", "Intermedio"),
        Ejercicio("loc_esp_02", "Dominadas pronadas",            "Espalda", "Calistenia", "Avanzado"),
        Ejercicio("loc_esp_03", "Remo invertido en barra",       "Espalda", "Calistenia", "Principiante"),
        Ejercicio("loc_esp_04", "Peso muerto",                   "Espalda", "Barra",      "Intermedio"),
        Ejercicio("loc_esp_05", "Remo con barra",                "Espalda", "Barra",      "Intermedio"),
        Ejercicio("loc_esp_06", "Buenos días",                   "Espalda", "Barra",      "Intermedio"),
        Ejercicio("loc_esp_07", "Remo con mancuerna",            "Espalda", "Mancuernas", "Principiante"),
        Ejercicio("loc_esp_08", "Jalón al pecho",                "Espalda", "Cable",      "Principiante"),
        Ejercicio("loc_esp_09", "Remo en polea baja",            "Espalda", "Cable",      "Principiante"),
        Ejercicio("loc_esp_10", "Jalón en máquina",              "Espalda", "Máquina",    "Principiante"),
        Ejercicio("loc_esp_11", "Remo en máquina",               "Espalda", "Máquina",    "Principiante"),

        // ─── HOMBROS ──────────────────────────────────────────────────────────
        Ejercicio("loc_hom_01", "Pike push-up",                      "Hombros", "Calistenia", "Intermedio"),
        Ejercicio("loc_hom_02", "Fondos en paralelas (apoyo)",        "Hombros", "Calistenia", "Intermedio"),
        Ejercicio("loc_hom_03", "Press militar",                      "Hombros", "Barra",      "Intermedio"),
        Ejercicio("loc_hom_04", "Press Arnold",                       "Hombros", "Mancuernas", "Intermedio"),
        Ejercicio("loc_hom_05", "Elevaciones laterales",              "Hombros", "Mancuernas", "Principiante"),
        Ejercicio("loc_hom_06", "Elevaciones frontales",              "Hombros", "Mancuernas", "Principiante"),
        Ejercicio("loc_hom_07", "Pájaro (elevaciones posteriores)",   "Hombros", "Mancuernas", "Principiante"),
        Ejercicio("loc_hom_08", "Face pull",                          "Hombros", "Cable",      "Principiante"),
        Ejercicio("loc_hom_09", "Elevaciones laterales en polea",     "Hombros", "Cable",      "Principiante"),
        Ejercicio("loc_hom_10", "Press en máquina hombros",           "Hombros", "Máquina",    "Principiante"),

        // ─── BÍCEPS ───────────────────────────────────────────────────────────
        Ejercicio("loc_bic_01", "Chin-up",                    "Bíceps", "Calistenia", "Avanzado"),
        Ejercicio("loc_bic_02", "Curl con barra",              "Bíceps", "Barra",      "Principiante"),
        Ejercicio("loc_bic_03", "Curl con barra EZ",           "Bíceps", "Barra",      "Principiante"),
        Ejercicio("loc_bic_04", "Curl con mancuernas",         "Bíceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_bic_05", "Curl martillo",               "Bíceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_bic_06", "Curl concentrado",            "Bíceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_bic_07", "Curl en polea baja",          "Bíceps", "Cable",      "Principiante"),
        Ejercicio("loc_bic_08", "Curl en máquina",             "Bíceps", "Máquina",    "Principiante"),

        // ─── TRÍCEPS ──────────────────────────────────────────────────────────
        Ejercicio("loc_tri_01", "Fondos en banco",              "Tríceps", "Calistenia", "Principiante"),
        Ejercicio("loc_tri_02", "Diamond push-up",              "Tríceps", "Calistenia", "Intermedio"),
        Ejercicio("loc_tri_03", "Press cerrado",                "Tríceps", "Barra",      "Intermedio"),
        Ejercicio("loc_tri_04", "Press francés",                "Tríceps", "Barra",      "Intermedio"),
        Ejercicio("loc_tri_05", "Patada trasera con mancuerna", "Tríceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_tri_06", "Extensión sobre cabeza",       "Tríceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_tri_07", "Extensión en polea alta",      "Tríceps", "Cable",      "Principiante"),
        Ejercicio("loc_tri_08", "Press en polea inversa",       "Tríceps", "Cable",      "Principiante"),
        Ejercicio("loc_tri_09", "Extensión en máquina",         "Tríceps", "Máquina",    "Principiante"),

        // ─── CORE ─────────────────────────────────────────────────────────────
        Ejercicio("loc_cor_01", "Plancha frontal",                    "Core", "Calistenia", "Principiante"),
        Ejercicio("loc_cor_02", "Plancha lateral",                    "Core", "Calistenia", "Principiante"),
        Ejercicio("loc_cor_03", "Crunch",                             "Core", "Calistenia", "Principiante"),
        Ejercicio("loc_cor_04", "Crunch inverso",                     "Core", "Calistenia", "Principiante"),
        Ejercicio("loc_cor_05", "Russian twist",                      "Core", "Calistenia", "Principiante"),
        Ejercicio("loc_cor_06", "Hollow body",                        "Core", "Calistenia", "Intermedio"),
        Ejercicio("loc_cor_07", "Levantamiento de piernas colgado",   "Core", "Calistenia", "Avanzado"),
        Ejercicio("loc_cor_08", "Ab rollout",                         "Core", "Calistenia", "Avanzado"),
        Ejercicio("loc_cor_09", "Crunch en polea",                    "Core", "Cable",      "Principiante"),
        Ejercicio("loc_cor_10", "Crunch en máquina",                  "Core", "Máquina",    "Principiante"),

        // ─── CUÁDRICEPS ───────────────────────────────────────────────────────
        Ejercicio("loc_cua_01", "Sentadilla corporal",        "Cuádriceps", "Calistenia", "Principiante"),
        Ejercicio("loc_cua_02", "Sentadilla búlgara",         "Cuádriceps", "Calistenia", "Intermedio"),
        Ejercicio("loc_cua_03", "Zancada (split squat)",      "Cuádriceps", "Calistenia", "Principiante"),
        Ejercicio("loc_cua_04", "Step-up",                    "Cuádriceps", "Calistenia", "Principiante"),
        Ejercicio("loc_cua_05", "Sentadilla libre",           "Cuádriceps", "Barra",      "Intermedio"),
        Ejercicio("loc_cua_06", "Sentadilla frontal",         "Cuádriceps", "Barra",      "Avanzado"),
        Ejercicio("loc_cua_07", "Sentadilla goblet",          "Cuádriceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_cua_08", "Zancada con mancuernas",     "Cuádriceps", "Mancuernas", "Principiante"),
        Ejercicio("loc_cua_09", "Extensión de cuádriceps",    "Cuádriceps", "Máquina",    "Principiante"),
        Ejercicio("loc_cua_10", "Prensa de piernas",          "Cuádriceps", "Máquina",    "Principiante"),
        Ejercicio("loc_cua_11", "Hack squat",                 "Cuádriceps", "Máquina",    "Intermedio"),

        // ─── ISQUIOTIBIALES ───────────────────────────────────────────────────
        Ejercicio("loc_isq_01", "Puente de isquiotibiales nórdico", "Isquiotibiales", "Calistenia", "Avanzado"),
        Ejercicio("loc_isq_02", "Hip hinge corporal",               "Isquiotibiales", "Calistenia", "Principiante"),
        Ejercicio("loc_isq_03", "Peso muerto rumano",               "Isquiotibiales", "Barra",      "Intermedio"),
        Ejercicio("loc_isq_04", "Peso muerto sumo",                 "Isquiotibiales", "Barra",      "Intermedio"),
        Ejercicio("loc_isq_05", "Buenos días con barra",            "Isquiotibiales", "Barra",      "Intermedio"),
        Ejercicio("loc_isq_06", "Peso muerto rumano mancuernas",    "Isquiotibiales", "Mancuernas", "Principiante"),
        Ejercicio("loc_isq_07", "Curl femoral tumbado",             "Isquiotibiales", "Máquina",    "Principiante"),
        Ejercicio("loc_isq_08", "Curl femoral de pie",              "Isquiotibiales", "Máquina",    "Principiante"),
        Ejercicio("loc_isq_09", "Curl femoral sentado",             "Isquiotibiales", "Máquina",    "Principiante"),

        // ─── GLÚTEOS ──────────────────────────────────────────────────────────
        Ejercicio("loc_glu_01", "Puente de glúteos",              "Glúteos", "Calistenia", "Principiante"),
        Ejercicio("loc_glu_02", "Zancada reversa",                "Glúteos", "Calistenia", "Principiante"),
        Ejercicio("loc_glu_03", "Sentadilla sumo corporal",       "Glúteos", "Calistenia", "Principiante"),
        Ejercicio("loc_glu_04", "Hip thrust con barra",           "Glúteos", "Barra",      "Intermedio"),
        Ejercicio("loc_glu_05", "Sentadilla sumo con barra",      "Glúteos", "Barra",      "Intermedio"),
        Ejercicio("loc_glu_06", "Hip thrust con mancuernas",      "Glúteos", "Mancuernas", "Principiante"),
        Ejercicio("loc_glu_07", "Zancada lateral con mancuerna",  "Glúteos", "Mancuernas", "Intermedio"),
        Ejercicio("loc_glu_08", "Patada trasera en polea",        "Glúteos", "Cable",      "Principiante"),
        Ejercicio("loc_glu_09", "Abductor en máquina",            "Glúteos", "Máquina",    "Principiante"),
        Ejercicio("loc_glu_10", "Hip thrust en máquina",          "Glúteos", "Máquina",    "Principiante"),

        // ─── GEMELOS ──────────────────────────────────────────────────────────
        Ejercicio("loc_gem_01", "Elevación de talones de pie",       "Gemelos", "Calistenia", "Principiante"),
        Ejercicio("loc_gem_02", "Elevación de talones a una pierna", "Gemelos", "Calistenia", "Intermedio"),
        Ejercicio("loc_gem_03", "Salto a la comba (jumping rope)",   "Gemelos", "Calistenia", "Principiante"),
        Ejercicio("loc_gem_04", "Elevación de talones con barra",    "Gemelos", "Barra",      "Intermedio"),
        Ejercicio("loc_gem_05", "Elevación de talones con mancuerna","Gemelos", "Mancuernas", "Principiante"),
        Ejercicio("loc_gem_06", "Elevación de talones en prensa",    "Gemelos", "Máquina",    "Principiante"),
        Ejercicio("loc_gem_07", "Elevación de talones sentado",      "Gemelos", "Máquina",    "Principiante")
    )

    /**
     * Filtra los ejercicios según el equipamiento y el nivel de experiencia.
     * Siempre incluye ejercicios de Calistenia como base universal.
     */
    fun obtenerEjercicios(equipos: List<String>, niveles: List<String>): List<Ejercicio> {
        val equiposConCalistenia = (equipos + "Calistenia").distinct()
        return todos.filter { it.equipo in equiposConCalistenia && it.nivel in niveles }
    }
}
