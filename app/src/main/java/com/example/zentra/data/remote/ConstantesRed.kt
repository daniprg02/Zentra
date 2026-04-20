package com.example.zentra.data.remote

/**
 * Constantes de conexión con los servicios externos de Zentra.
 *
 * IMPORTANTE: Antes de compilar en un entorno real, sustituye los valores de
 * Supabase con los de tu proyecto. Los encontrarás en:
 * Panel de Supabase → Settings → API → Project URL y Project API Keys (anon/public).
 *
 * Para mayor seguridad en producción, estos valores deberían cargarse desde
 * BuildConfig o un fichero local.properties, nunca commiteados directamente.
 */
object ConstantesRed {

    // TODO: Introduce la URL de tu proyecto de Supabase (ej. https://abcdefghij.supabase.co)
    const val SUPABASE_URL = "https://pjqrzbpdjlhexhwarcrh.supabase.co"

    // TODO: Introduce tu clave pública (anon) de Supabase
    const val SUPABASE_ANON_KEY = "sb_publishable_A7ltf_8aBVpPfzGO_M5Mtg_sLiYn8LA"

    // OpenFoodFacts es una API pública y gratuita, no requiere autenticación
    const val API_NUTRICIONAL_BASE_URL = "https://world.openfoodfacts.org/"
}
