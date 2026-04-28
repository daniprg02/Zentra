package com.example.zentra.data.local

import android.content.Context
import android.util.Log
import com.example.zentra.domain.model.DiaRutina
import com.example.zentra.domain.model.Perfil
import com.example.zentra.domain.model.Receta
import com.example.zentra.domain.model.RutinaUsuario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona la caché local de datos esenciales de Zentra usando SharedPreferences.
 * Permite que la aplicación funcione en modo sin conexión mostrando los últimos datos conocidos.
 * Los datos se serializan con Gson ya disponible como dependencia transitiva de Retrofit.
 */
@Singleton
class ZentraCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("zentra_cache_v1", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ─── Perfil del usuario ───────────────────────────────────────────────────

    fun guardarPerfil(perfil: Perfil) {
        try {
            prefs.edit().putString("perfil", gson.toJson(perfil)).apply()
            Log.d("ZentraCacheManager", "Perfil de '${perfil.apodo}' guardado en caché.")
        } catch (e: Exception) {
            Log.e("ZentraCacheManager", "Error al guardar perfil en caché: ${e.message}")
        }
    }

    fun cargarPerfil(): Perfil? = try {
        prefs.getString("perfil", null)?.let { gson.fromJson(it, Perfil::class.java) }
    } catch (e: Exception) {
        Log.e("ZentraCacheManager", "Error al cargar perfil de caché: ${e.message}")
        null
    }

    // ─── Lista de recetas ─────────────────────────────────────────────────────

    fun guardarRecetas(recetas: List<Receta>) {
        try {
            prefs.edit().putString("recetas", gson.toJson(recetas)).apply()
            Log.d("ZentraCacheManager", "${recetas.size} recetas guardadas en caché.")
        } catch (e: Exception) {
            Log.e("ZentraCacheManager", "Error al guardar recetas en caché: ${e.message}")
        }
    }

    fun cargarRecetas(): List<Receta> = try {
        val json = prefs.getString("recetas", null) ?: return emptyList()
        val tipo = object : TypeToken<List<Receta>>() {}.type
        gson.fromJson<List<Receta>>(json, tipo) ?: emptyList()
    } catch (e: Exception) {
        Log.e("ZentraCacheManager", "Error al cargar recetas de caché: ${e.message}")
        emptyList()
    }

    // ─── Rutina activa ─────────────────────────────────────────────────────────

    fun guardarRutinaActiva(cabecera: RutinaUsuario, dias: List<DiaRutina>) {
        try {
            prefs.edit()
                .putString("rutina_cabecera", gson.toJson(cabecera))
                .putString("rutina_dias", gson.toJson(dias))
                .apply()
            Log.d("ZentraCacheManager", "Rutina activa guardada en caché (${dias.size} días).")
        } catch (e: Exception) {
            Log.e("ZentraCacheManager", "Error al guardar rutina en caché: ${e.message}")
        }
    }

    fun cargarRutinaActiva(): Pair<RutinaUsuario, List<DiaRutina>>? = try {
        val cabJson = prefs.getString("rutina_cabecera", null) ?: return null
        val diasJson = prefs.getString("rutina_dias", null) ?: return null
        val cabecera = gson.fromJson(cabJson, RutinaUsuario::class.java) ?: return null
        val tipo = object : TypeToken<List<DiaRutina>>() {}.type
        val dias = gson.fromJson<List<DiaRutina>>(diasJson, tipo) ?: emptyList()
        cabecera to dias
    } catch (e: Exception) {
        Log.e("ZentraCacheManager", "Error al cargar rutina de caché: ${e.message}")
        null
    }

    fun limpiarRutinaActiva() {
        prefs.edit().remove("rutina_cabecera").remove("rutina_dias").apply()
        Log.d("ZentraCacheManager", "Caché de rutina activa limpiada.")
    }
}
