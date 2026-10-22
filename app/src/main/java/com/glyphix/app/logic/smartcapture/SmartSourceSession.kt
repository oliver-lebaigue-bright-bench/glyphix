package com.glyphix.app.logic.smartcapture

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import com.glyphix.app.logic.AudioProcessor
import com.glyphix.app.service.GlyphNotificationListener

/** Polled by the service, so playback continues when the activity is closed. */
class SmartSourceSession(context: Context, pushFrame: (FloatArray) -> Unit) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, GlyphNotificationListener::class.java)
    private val orchestrator = SmartCaptureOrchestrator(context, SmartCapturePlaybackEngine(pushFrame))
    private var lastPoll = 0L

    fun tick(config: AudioProcessor.VisualizerConfig, device: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPoll < 250) return
        lastPoll = now
        try {
            val controllers = manager?.getActiveSessions(component).orEmpty()
            val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()
            val metadata = controller?.metadata
            val state = controller?.playbackState
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            if (title.isBlank() || artist.isBlank() || duration <= 0 || state == null || state.position < 0) {
                orchestrator.onPlaybackStopped()
                return
            }
            val playing = state.state == PlaybackState.STATE_PLAYING
            val elapsed = if (playing && state.lastPositionUpdateTime > 0) (now - state.lastPositionUpdateTime).coerceAtLeast(0) else 0L
            val position = (state.position + (elapsed * state.playbackSpeed).toLong()).coerceIn(0, duration)
            orchestrator.onSongChanged(artist, title, duration, position, config, device, 0)
            orchestrator.updatePlayback(position, playing, state.playbackSpeed)
        } catch (_: SecurityException) {
            // Notification access is optional; no root or private player APIs are used.
            orchestrator.onPlaybackStopped()
        }
    }

    fun isDatabasePlayback() = orchestrator.isDatabasePlayback()
    fun recordLiveFrame(magnitudes: FloatArray) = orchestrator.recordLiveFrame(magnitudes)
    fun stop() = orchestrator.onPlaybackStopped()
    fun close() = orchestrator.close()
}
