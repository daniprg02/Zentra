package com.example.zentra.ui.screens.recetas

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel del módulo de Gestor de Recetas.
 * Gestionará la carga, creación y eliminación de recetas, tanto las predefinidas
 * del sistema como las personales del usuario almacenadas en Supabase.
 * TODO: Implementar búsqueda de recetas y conexión con el repositorio.
 */
@HiltViewModel
class RecetasViewModel @Inject constructor() : ViewModel() {

    init {
        Log.d("RecetasViewModel", "ViewModel de Recetas inicializado correctamente.")
    }
}
