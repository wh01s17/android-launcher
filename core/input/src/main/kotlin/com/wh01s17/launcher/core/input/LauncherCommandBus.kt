package com.wh01s17.launcher.core.input

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Punto único por el que pasan los [LauncherCommand]. */
interface LauncherCommandBus {
    val commands: SharedFlow<LauncherCommand>

    /** No suspende: se puede llamar desde un callback de UI. */
    fun send(command: LauncherCommand)
}

/**
 * Bus en memoria. Sin receptores los comandos se pierden (una pulsación en la barra mientras la
 * app no atiende no debe ejecutarse más tarde); con receptores lentos, se descartan los más viejos.
 */
class DefaultLauncherCommandBus : LauncherCommandBus {
    private val flow = MutableSharedFlow<LauncherCommand>(extraBufferCapacity = BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val commands: SharedFlow<LauncherCommand> = flow.asSharedFlow()

    override fun send(command: LauncherCommand) {
        flow.tryEmit(command)
    }

    private companion object {
        const val BUFFER = 16
    }
}
