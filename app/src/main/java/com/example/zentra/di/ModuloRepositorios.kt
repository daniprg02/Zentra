package com.example.zentra.di

import com.example.zentra.data.repository.MacrosRepositorioImpl
import com.example.zentra.data.repository.PerfilRepositorioImpl
import com.example.zentra.data.repository.RecetasRepositorioImpl
import com.example.zentra.data.repository.RutinasRepositorioImpl
import com.example.zentra.domain.repository.IMacrosRepositorio
import com.example.zentra.domain.repository.IPerfilRepositorio
import com.example.zentra.domain.repository.IRecetasRepositorio
import com.example.zentra.domain.repository.IRutinasRepositorio
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula las interfaces del dominio con sus implementaciones concretas en la capa de datos.
 * Usar @Binds en lugar de @Provides es más eficiente porque evita crear una función wrapper.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloRepositorios {

    /**
     * Vincula [IPerfilRepositorio] con su implementación [PerfilRepositorioImpl].
     * Hilt inyectará [PerfilRepositorioImpl] en cualquier clase que declare [IPerfilRepositorio] como dependencia.
     */
    @Binds
    @Singleton
    abstract fun bindPerfilRepositorio(
        impl: PerfilRepositorioImpl
    ): IPerfilRepositorio

    /**
     * Vincula [IRecetasRepositorio] con su implementación [RecetasRepositorioImpl].
     */
    @Binds
    @Singleton
    abstract fun bindRecetasRepositorio(
        impl: RecetasRepositorioImpl
    ): IRecetasRepositorio

    /**
     * Vincula [IMacrosRepositorio] con su implementación [MacrosRepositorioImpl].
     */
    @Binds
    @Singleton
    abstract fun bindMacrosRepositorio(
        impl: MacrosRepositorioImpl
    ): IMacrosRepositorio

    /**
     * Vincula [IRutinasRepositorio] con su implementación [RutinasRepositorioImpl].
     */
    @Binds
    @Singleton
    abstract fun bindRutinasRepositorio(
        impl: RutinasRepositorioImpl
    ): IRutinasRepositorio
}
