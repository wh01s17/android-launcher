package com.wh01s17.launcher.core.system.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Lo que suena en una app, para pintarlo y controlarlo. */
sealed interface MediaState {
    /** Falta el acceso a notificaciones: Android no deja ver la sesión. */
    data object NoAccess : MediaState

    /** La app no tiene una sesión activa (no está reproduciendo ni en pausa). */
    data object Idle : MediaState

    data class Session(
        val title: String?,
        val artist: String?,
        val album: String?,
        val art: Bitmap?,
        val durationMillis: Long,
        val playing: Boolean,
        /** Posición cuando se leyó, en [positionAtElapsed] (reloj monotónico), y la velocidad. */
        val positionMillis: Long,
        val positionAtElapsed: Long,
        val speed: Float,
        internal val controller: MediaController,
    ) : MediaState {
        /** La posición ahora mismo, avanzando si suena. */
        fun positionAt(elapsedNow: Long): Long {
            val moved = if (playing) ((elapsedNow - positionAtElapsed) * speed).toLong() else 0
            return (positionMillis + moved).coerceIn(0, durationMillis.takeIf { it > 0 } ?: Long.MAX_VALUE)
        }
    }
}

/** Botones de transporte, como `z` `x` `c` `v` de tidalamp. */
enum class MediaAction { Previous, PlayPause, Stop, Next }

/**
 * Sesiones multimedia de otras apps (plan.md §5.8, `MediaSessionManager`). Necesita el acceso a
 * notificaciones de [WhNotificationListener]; sin él emite [MediaState.NoAccess].
 */
class MediaSource(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(appContext, WhNotificationListener::class.java)

    /** El estado de la sesión de [packageName], mientras se recoja. */
    fun session(packageName: String): Flow<MediaState> = watch { controllers -> controllers.firstOrNull { it.packageName == packageName } }

    /**
     * La sesión que suena ahora (o, si ninguna suena, la más reciente), sea de la app que sea: la
     * que controlan el módulo `media` de la barra y los menús. Android ordena las sesiones por prioridad.
     */
    fun active(): Flow<MediaState> =
        watch { controllers ->
            controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
        }

    private fun watch(choose: (List<MediaController>) -> MediaController?): Flow<MediaState> =
        callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            var controllers = emptyList<MediaController>()

            fun publish() {
                trySend(choose(controllers)?.let(::stateOf) ?: MediaState.Idle)
            }
            val callback =
                object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publish()

                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                }

            fun attach(current: List<MediaController>?) {
                controllers.forEach { it.unregisterCallback(callback) }
                controllers = current.orEmpty()
                controllers.forEach { it.registerCallback(callback, handler) }
                publish()
            }
            val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }
            try {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
                attach(manager.getActiveSessions(listener))
            } catch (ignored: SecurityException) {
                trySend(MediaState.NoAccess)
            }
            awaitClose {
                controllers.forEach { it.unregisterCallback(callback) }
                manager.removeOnActiveSessionsChangedListener(sessionsListener)
            }
        }.distinctUntilChanged()

    fun perform(
        state: MediaState.Session,
        action: MediaAction,
    ) {
        val controls = state.controller.transportControls
        when (action) {
            MediaAction.Previous -> controls.skipToPrevious()
            MediaAction.PlayPause -> if (state.playing) controls.pause() else controls.play()
            MediaAction.Stop -> controls.stop()
            MediaAction.Next -> controls.skipToNext()
        }
    }

    fun seek(
        state: MediaState.Session,
        positionMillis: Long,
    ) = state.controller.transportControls.seekTo(positionMillis)

    /** La pantalla del sistema donde se da el acceso a notificaciones. */
    fun accessSettings(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun stateOf(controller: MediaController): MediaState {
        val playback = controller.playbackState
        val metadata = controller.metadata ?: return MediaState.Idle
        return MediaState.Session(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
            art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART),
            durationMillis = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            playing = playback?.state == PlaybackState.STATE_PLAYING,
            positionMillis = playback?.position ?: 0,
            positionAtElapsed = playback?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = playback?.playbackSpeed?.takeIf { it > 0 } ?: 1f,
            controller = controller,
        )
    }
}
