package com.glyphix.shared.logic

import com.glyphix.shared.util.elapsedRealtime

class BeatDetector(
    var sensitivity: Float = 1.0f,
    var cooldownMs: Long = 60L
) {
    private val deltaHistory = FloatArray(61)
    private val sortedHistory = FloatArray(61)
    private var deltaIndex = 0
    private var deltaCount = 0
    private var prevEnergy = 0f
    private var lastTriggerMs = 0L
    private var thresholdMask = 0f

    fun detect(magnitude: FloatArray, binLo: Int, binHi: Int): Boolean {
        if (magnitude.isEmpty()) return false

        val start = maxOf(0, minOf(binLo, magnitude.lastIndex))
        val end = maxOf(start, minOf(binHi, magnitude.lastIndex))

        var sum = 0f
        for (i in start..end) {
            sum += magnitude[i]
        }

        val energy = kotlin.math.ln(1f + sum)
        val delta = energy - prevEnergy
        prevEnergy = energy

        pushDelta(delta)

        val threshold = maxOf(medianDelta() * (2.2f * sensitivity), thresholdMask)
        val now = elapsedRealtime()

        val triggered = delta > threshold && delta > 0.025f && (now - lastTriggerMs) >= cooldownMs
        if (triggered) {
            lastTriggerMs = now
            thresholdMask = delta * 0.8f
        }

        thresholdMask *= 0.85f
        return triggered
    }

    private fun pushDelta(delta: Float) {
        deltaHistory[deltaIndex] = delta.coerceAtLeast(0.0001f)
        deltaIndex = (deltaIndex + 1) % deltaHistory.size
        if (deltaCount < deltaHistory.size) deltaCount++
    }

    private fun medianDelta(): Float {
        if (deltaCount == 0) return 0.01f
        // Replace System.arraycopy with simple loop or shared util
        for (i in 0 until deltaCount) {
            sortedHistory[i] = deltaHistory[i]
        }
        
        // Use a simple sort or shared util
        val list = sortedHistory.sliceArray(0 until deltaCount).sorted()
        
        return if (deltaCount % 2 == 1) {
            list[deltaCount / 2]
        } else {
            val mid = deltaCount / 2
            (list[mid - 1] + list[mid]) * 0.5f
        }
    }

    fun reset() {
        deltaIndex = 0
        deltaCount = 0
        prevEnergy = 0f
        lastTriggerMs = 0L
        thresholdMask = 0f
        deltaHistory.fill(0f)
    }
}
