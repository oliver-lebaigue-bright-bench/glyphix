package com.glyphix.shared.util

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
