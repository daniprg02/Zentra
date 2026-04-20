package com.example.zentra.domain.repository

import com.example.zentra.domain.model.MacrosDiarios

/**
 * Contrato del repositorio de macros diarios.
 * Gestiona la persistencia del cuadre nutricional del usuario día a día,
 * proporcionando el estado actual de la calculadora dietética en tiempo real.
 */
interface IMacrosRepositorio {

    /**
     * Obtiene el registro nutricional del usuario para una fecha concreta.
     * @param userId UUID del usuario autenticado.
     * @param fecha Fecha en formato "YYYY-MM-DD".
     * @return [Result] con los [MacrosDiarios] del día o el error producido.
     */
    suspend fun obtenerMacrosDelDia(userId: String, fecha: String): Result<MacrosDiarios>

    /**
     * Guarda o actualiza el registro nutricional de un día.
     * Se llama cada vez que el usuario añade o elimina una ingesta de un slot.
     * @param macrosDiarios El estado actualizado de los macros del día.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun guardarMacrosDelDia(macrosDiarios: MacrosDiarios): Result<Unit>
}
