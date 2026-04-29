package com.example.zentra.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitoriza el estado real de la conectividad usando ConnectivityManager.
 * Permite distinguir entre un error de red genuino y un fallo temporal de autenticación,
 * evitando mostrar el banner de "sin conexión" cuando el dispositivo sí tiene internet.
 */
@Singleton
class MonitorConectividad @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Devuelve true si el dispositivo tiene acceso a internet verificado en este momento. */
    fun hayConexion(): Boolean {
        val red = connectivityManager.activeNetwork ?: return false
        val capacidades = connectivityManager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Flow reactivo que emite true al recuperarse la conexión y false al perderse.
     * Los ViewModels que observen este flujo pueden recargar datos automáticamente
     * sin que el usuario tenga que pulsar reintentar.
     */
    fun observarConectividad(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val solicitud = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(solicitud, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
