package com.glyphix.shared.model

data class GlobalStats(
    var totalVisualizedTimeMs: Long = 0L,
    var totalActiveTimeMs: Long = 0L,
    var totalIdleTimeMs: Long = 0L,
    var totalGlyphTimeMs: Long = 0L,
    var totalHapticTimeMs: Long = 0L,
    var totalFlashlightTimeMs: Long = 0L,
    var totalSessions: Long = 0L,
    var totalBeatsDetected: Long = 0L,
    var userCount: Long = 0L
)
