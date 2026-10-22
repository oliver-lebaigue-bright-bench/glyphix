package com.glyphix.app.logic.smartcapture

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.glyphix.app.logic.AudioProcessor
import kotlinx.coroutines.*

/** Owned by the capture service. Only numeric range magnitudes leave the device. */
class SmartCaptureOrchestrator(private val context: Context, private val playbackEngine: SmartCapturePlaybackEngine) {
    private val repository = LightshowRepository(context.cacheDir)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cache = object : LinkedHashMap<String, SongVisualSequence>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongVisualSequence>?) = size > 4
    }
    private var lookup: Job? = null
    private var key = ""
    private var duration = 0L
    private var offset = 0L
    private var anchor = 0L
    private var speed = 1f
    private var playing = false
    private var recording = false
    private var loaded: SongVisualSequence? = null
    private val frames = mutableListOf<GlyphFrame>()
    private val prefs get() = context.getSharedPreferences("viz_prefs", Context.MODE_PRIVATE)

    private fun networkAllowed(): Boolean {
        if (!prefs.getBoolean("smart_capture_wifi_only", true)) return true
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    @Synchronized
    fun onSongChanged(artist: String, title: String, durationMs: Long, startPositionMs: Long,
                      config: AudioProcessor.VisualizerConfig, deviceType: Int, audioSessionId: Int) {
        val layout = "$deviceType:${config.presetKey}:" + config.uniqueRanges.joinToString(";") { "${it.lowHz},${it.highHz}" }
        val next = SmartSourceRules.key(artist, title, durationMs, layout)
        if (next == key) return
        finishRecording()
        lookup?.cancel()
        playbackEngine.stop()
        loaded = null
        key = next
        duration = durationMs
        frames.clear()
        recording = false
        if (next.isEmpty()) return
        offset = startPositionMs
        anchor = SystemClock.elapsedRealtime()
        val cached = cache[next]
        if (cached != null) { loaded = cached; return }
        recording = prefs.getBoolean("smart_capture_contribute", false)
        lookup = scope.launch {
            val sequence = repository.fetchLightshow(next, networkAllowed())
            ensureActive()
            synchronized(this@SmartCaptureOrchestrator) {
                if (key != next) return@synchronized
                if (sequence != null && sequence.durationMs == durationMs && sequence.frames.first().intensities.size == config.uniqueRanges.size) {
                    cache[next] = sequence
                    loaded = sequence
                    recording = false
                    frames.clear()
                }
            }
        }
    }

    @Synchronized
    fun updatePlayback(positionMs: Long, isPlaying: Boolean, playbackSpeed: Float) {
        offset = positionMs.coerceAtLeast(0)
        anchor = SystemClock.elapsedRealtime()
        playing = isPlaying
        speed = playbackSpeed
        val seq = loaded
        if (seq != null && playing && speed == 1f) {
            playbackEngine.start(seq, offset)
        } else playbackEngine.pause()
    }

    @Synchronized
    fun isDatabasePlayback(): Boolean = loaded != null && playing && speed == 1f && position() < duration

    private fun position(): Long = offset + ((SystemClock.elapsedRealtime() - anchor) * speed).toLong()

    @Synchronized
    fun recordLiveFrame(intensities: FloatArray) {
        if (!recording || !playing || speed != 1f || !prefs.getBoolean("smart_capture_contribute", false)) return
        val time = position()
        if (time !in 0..duration || frames.size >= 60000 || intensities.isEmpty() || intensities.any { !it.isFinite() || it < 0 }) return
        val previous = frames.lastOrNull()?.timestampMs
        // A seek invalidates this attempt; never upload a stitched or reordered song.
        if (previous != null && (time < previous || time - previous > 500)) { recording = false; frames.clear(); return }
        if (previous != null && time - previous < 33) return
        frames.add(GlyphFrame(time, intensities.toList()))
        if (time >= duration - 100) finishRecording()
    }

    private fun finishRecording() {
        if (recording && prefs.getBoolean("smart_capture_contribute", false)) {
            val seq = SongVisualSequence(key, duration, frames.toList())
            if (seq.isValid()) {
                cache[key] = seq
                scope.launch {
                    repository.saveLocal(seq)
                    if (networkAllowed()) repository.uploadLightshow(seq)
                }
            }
        }
        recording = false
        frames.clear()
    }

    @Synchronized
    fun onPlaybackStopped() {
        finishRecording()
        key = ""
        loaded = null
        playing = false
        lookup?.cancel()
        playbackEngine.stop()
    }

    fun close() { onPlaybackStopped(); scope.cancel() }
}
