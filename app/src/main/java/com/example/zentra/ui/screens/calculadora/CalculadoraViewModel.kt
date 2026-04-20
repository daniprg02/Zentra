package com.example.zentra.ui.screens.calculadora

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel del módulo de Calculadora Dietética.
 * Gestionará el objetivo calórico diario, el registro de ingestas por slot y la
 * interconexión con el módulo de Rutinas para el ajuste dinámico del TDEE.
 * TODO: Implementar lógica de cálculo de TMB y distribución de macronutrientes (Mifflin-St Jeor).
 */
@HiltViewModel
class CalculadoraViewModel @Inject constructor() : ViewModel() {

    init {
        Log.d("CalculadoraViewModel", "ViewModel de Calculadora inicializado correctamente.")
    }
}
