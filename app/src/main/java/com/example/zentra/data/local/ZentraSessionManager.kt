package com.example.zentra.data.local

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persiste la sesión de Supabase en SharedPreferences para que sobreviva
 * al cierre y reapertura de la app. Sin esto, Auth usa MemorySessionManager
 * y el usuario tiene que volver a iniciar sesión cada vez que mata la app.
 */
class ZentraSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("zentra_auth_v1", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun loadSession(): UserSession? = try {
        val raw = prefs.getString(KEY, null) ?: return null
        json.decodeFromString<UserSession>(raw).also {
            Log.d("ZentraSessionManager", "Sesión cargada del almacenamiento local.")
        }
    } catch (e: Exception) {
        Log.e("ZentraSessionManager", "Error al cargar sesión: ${e.message}")
        null
    }

    override suspend fun saveSession(userSession: UserSession) {
        try {
            prefs.edit().putString(KEY, json.encodeToString(userSession)).apply()
            Log.d("ZentraSessionManager", "Sesión guardada en almacenamiento local.")
        } catch (e: Exception) {
            Log.e("ZentraSessionManager", "Error al guardar sesión: ${e.message}")
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY).apply()
        Log.d("ZentraSessionManager", "Sesión eliminada del almacenamiento local.")
    }

    companion object {
        private const val KEY = "supabase_session_v1"
    }
}
