package com.example.zentra

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal de Zentra.
 * La anotación @HiltAndroidApp genera el componente raíz de Hilt e inicializa
 * el contenedor de inyección de dependencias para toda la aplicación.
 */
@HiltAndroidApp
class ZentraApp : Application()
