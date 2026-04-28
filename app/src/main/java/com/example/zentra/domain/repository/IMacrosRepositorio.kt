package com.example.zentra.domain.repository

import com.example.zentra.domain.model.IngestaSlot
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
     * Se llama cada vez que el usuario añade, edita o elimina una ingesta.
     * @param macrosDiarios El estado actualizado de los macros del día.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun guardarMacrosDelDia(macrosDiarios: MacrosDiarios): Result<Unit>

    /**
     * Obtiene todas las ingestas del usuario para una fecha concreta, agrupadas por slot.
     * @param userId UUID del usuario autenticado.
     * @param fecha Fecha en formato "YYYY-MM-DD".
     * @return [Result] con un mapa slot → lista de [IngestaSlot] ordenada por campo `orden`.
     */
    suspend fun obtenerIngestasDelDia(userId: String, fecha: String): Result<Map<String, List<IngestaSlot>>>

    /**
     * Persiste una ingesta en `daily_intakes` usando upsert.
     * Si el [IngestaSlot.dbId] es vacío, genera un UUID nuevo y devuelve el slot actualizado.
     * Si ya tiene ID, actualiza la fila existente (p.ej. cuando se edita la cantidad).
     * @return [Result] con el [IngestaSlot] incluyendo el dbId definitivo.
     */
    suspend fun guardarIngesta(
        userId: String,
        fecha: String,
        slotNombre: String,
        slot: IngestaSlot
    ): Result<IngestaSlot>

    /**
     * Elimina una ingesta concreta de `daily_intakes` por su UUID.
     * @param dbId UUID de la fila en `daily_intakes`.
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun eliminarIngesta(dbId: String): Result<Unit>

    /**
     * Elimina todas las ingestas de un día (llamado al reiniciar el contador diario).
     * @param userId UUID del usuario autenticado.
     * @param fecha Fecha en formato "YYYY-MM-DD".
     * @return [Result] vacío si la operación es exitosa.
     */
    suspend fun eliminarIngestasDelDia(userId: String, fecha: String): Result<Unit>
}
