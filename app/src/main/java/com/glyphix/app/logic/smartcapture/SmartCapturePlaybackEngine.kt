package com.glyphix.app.logic.smartcapture

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class SmartCapturePlaybackEngine(
    private val pushFrameCallback: (FloatArray) -> Unit
) {
    @Volatile private var isPlaying = false
    @Volatile private var currentSequence: SongVisualSequence? = null
    
    private var timestamps = LongArray(0)

    // Time tracking
    private var playbackStartTimeMs: Long = 0
    private var startOffsetMs: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            
            tick()
            // ~60 FPS
            handler.postDelayed(this, 16)
        }
    }

    fun start(sequence: SongVisualSequence, startPositionMs: Long) {
        if (currentSequence !== sequence) timestamps = sequence.frames.map { it.timestampMs }.toLongArray()
        currentSequence = sequence
        startOffsetMs = startPositionMs
        playbackStartTimeMs = SystemClock.elapsedRealtime()
        
        if (!isPlaying) {
            isPlaying = true
            handler.post(tickRunnable)
        }
    }
    
    fun updatePosition(positionMs: Long) {
        startOffsetMs = positionMs
        playbackStartTimeMs = SystemClock.elapsedRealtime()
    }

    fun pause() {
        isPlaying = false
        handler.removeCallbacks(tickRunnable)
    }

    fun stop() {
        isPlaying = false
        currentSequence = null
        handler.removeCallbacks(tickRunnable)
    }

    private fun tick() {
        val seq = currentSequence ?: return
        
        val elapsedSinceStart = SystemClock.elapsedRealtime() - playbackStartTimeMs
        val currentPositionMs = startOffsetMs + elapsedSinceStart
        
        if (currentPositionMs > seq.durationMs) {
            stop()
            return
        }
        
        val frames = seq.frames
        if (frames.isEmpty()) return
        val found = SmartSourceRules.frameIndex(timestamps, currentPositionMs)
        if (found >= 0) pushFrameCallback(frames[found].getIntensitiesArray())
    }
}
