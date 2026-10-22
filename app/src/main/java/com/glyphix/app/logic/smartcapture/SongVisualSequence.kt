package com.glyphix.app.logic.smartcapture

data class GlyphFrame(
    var timestampMs: Long = 0L,
    var intensities: List<Float> = emptyList()
) {
    fun getIntensitiesArray(): FloatArray {
        val arr = FloatArray(intensities.size)
        for (i in intensities.indices) {
            arr[i] = intensities[i]
        }
        return arr
    }
}

data class SongVisualSequence(
    var songKey: String = "",
    var durationMs: Long = 0L,
    var frames: List<GlyphFrame> = emptyList()
)

/** Reject partial, unsorted, unbounded and malformed public database records. */
fun SongVisualSequence.isValid(): Boolean {
    if (!songKey.matches(Regex("v2_[a-f0-9]{64}")) || frames.size !in 2..60000) return false
    val width = frames.first().intensities.size
    if (width !in 1..512) return false
    var last = -1L
    var gap = 0L
    for (frame in frames) {
        if (frame.timestampMs <= last || frame.timestampMs > durationMs || frame.intensities.size != width ||
            frame.intensities.any { !it.isFinite() || it < 0f || it > 1000000f }) return false
        if (last >= 0) gap = maxOf(gap, frame.timestampMs - last)
        last = frame.timestampMs
    }
    return SmartSourceRules.complete(durationMs, frames.first().timestampMs, last, gap, frames.size)
}
