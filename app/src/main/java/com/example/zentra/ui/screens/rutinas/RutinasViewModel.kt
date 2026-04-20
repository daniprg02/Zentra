package com.example.zentra.ui.screens.rutinas

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel del módulo de Rutinas.
 * Gestionará la carga, generación y persistencia de los planes de entrenamiento del usuario.
 * TODO: Implementar lógica de generación de rutinas con el algoritmo de la Masterlist.
 */
@HiltViewModel
class RutinasViewModel @Inject constructor() : ViewModel() {

    init {
        Log.d("RutinasViewModel", "ViewModel de Rutinas inicializado correctamente.")
    }
}
