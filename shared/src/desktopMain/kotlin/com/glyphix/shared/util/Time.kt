package com.glyphix.shared.util

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000L // Approximation for desktop
