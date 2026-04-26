package com.example.zentra.data.remote.api

import com.example.zentra.data.remote.dto.RespuestaBusquedaAlimentos
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaz Retrofit para la API pública de OpenFoodFacts (subdominio España).
 * No requiere autenticación. Base URL: https://es.openfoodfacts.org/
 * Documentación: https://wiki.openfoodfacts.org/API
 */
interface OpenFoodFactsService {

    /**
     * Busca productos alimentarios por nombre, restringido al catálogo español.
     * @param terminos Texto libre del alimento a buscar (ej: "tortilla", "pechuga pollo").
     * @param pageSize Número máximo de resultados a devolver.
     * @param campos Proyección de campos para reducir el tamaño de la respuesta.
     * @param idioma Código de idioma para los nombres de producto (por defecto "es").
     */
    @GET("cgi/search.pl")
    suspend fun buscarProductos(
        @Query("search_terms") terminos: String,
        @Query("json") json: Int = 1,
        @Query("action") accion: String = "process",
        @Query("search_simple") simple: Int = 1,
        @Query("page_size") pageSize: Int = 12,
        @Query("fields") campos: String = "product_name,nutriments,serving_size",
        @Query("lc") idioma: String = "es"
    ): RespuestaBusquedaAlimentos
}
