package com.glyphix.tv;

/** A precautionary rate cap, not a guarantee against photosensitive reactions. */
public final class FlashLimiter {
    private long last = -100000;
    private long intervalMs = 500;
    public void setRate(float perSecond) {
        intervalMs = (long) Math.ceil(1000.0 / Math.max(0.5f, Math.min(2f, perSecond)));
    }
    public boolean allow(long nowMs) {
        if (nowMs - last < intervalMs) return false;
        last = nowMs;
        return true;
    }
    public void reset() { last = -100000; }
}
