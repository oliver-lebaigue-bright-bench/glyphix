package com.glyphix.app.logic.smartcapture;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;

/** Versioned identity: Unicode-safe, edition-duration and analysis-layout specific. */
public final class SmartSourceRules {
    private SmartSourceRules() {}
    public static String key(String artist, String title, long duration, String layout) {
        if (artist == null || title == null || artist.trim().isEmpty() || title.trim().isEmpty()
                || duration <= 0 || duration > 1800000) return "";
        try {
            String identity = normalize(artist) + "\n" + normalize(title) + "\n" + duration + "\n" + layout;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder("v2_");
            for (byte b : digest) key.append(String.format(Locale.ROOT, "%02x", b & 255));
            return key.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
    public static boolean complete(long duration, long first, long last, long maximumGap, int count) {
        return duration > 0 && duration <= 1800000 && count >= 2 && first >= 0 && first <= 500
                && last >= duration - 500 && last <= duration && maximumGap <= 500;
    }
    /** Last sample at or before the player position, including backwards seeks. */
    public static int frameIndex(long[] timestamps, long position) {
        int low = 0, high = timestamps.length - 1, found = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (timestamps[mid] <= position) { found = mid; low = mid + 1; }
            else high = mid - 1;
        }
        return found;
    }
    /** Unlike readByteArray(count), accepts a short HTTP body without throwing EOF. */
    public static byte[] readBounded(java.io.InputStream input, int limit) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer, 0, Math.min(buffer.length, limit + 1 - output.size()))) != -1) {
            output.write(buffer, 0, count);
            if (output.size() > limit) throw new java.io.IOException("Sequence exceeds size limit");
        }
        return output.toByteArray();
    }

}
