package com.glyphix.tv;

/** Local PCM amplitude and transient detection. Never retains or stores audio samples. */
public final class SignalAnalyzer {
    private double average = 0.015;
    private float envelope;
    private long lastBeat = -1000;

    public static final class Frame {
        public final float level;
        public final boolean beat;
        Frame(float level, boolean beat) { this.level = level; this.beat = beat; }
    }

    public Frame analyze(short[] samples, int count, float gain, long nowMs) {
        if (count <= 0 || count > samples.length) return new Frame(0, false);
        double sum = 0;
        for (int i = 0; i < count; i++) {
            double value = samples[i] / 32768.0;
            sum += value * value;
        }
        double rms = Math.sqrt(sum / count) * Math.max(0.1f, Math.min(10, gain));
        boolean beat = rms > 0.006 && rms > Math.max(0.01, average * 1.45)
                && nowMs - lastBeat >= 500;
        if (beat) lastBeat = nowMs;
        average = average * 0.97 + rms * 0.03;
        float target = rms < 0.003 ? 0 : (float) Math.min(1, rms * 5);
        envelope += (target - envelope) * (target > envelope ? 0.6f : 0.16f);
        if (envelope < 0.004f) envelope = 0;
        return new Frame(envelope, beat);
    }
}
