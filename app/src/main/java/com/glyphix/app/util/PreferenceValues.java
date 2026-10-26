package com.glyphix.app.util;

/** Tolerate renamed/removed enum values in preferences retained across beta upgrades. */
public final class PreferenceValues {
    private PreferenceValues() {}

    public static <T extends Enum<T>> T enumOrDefault(Class<T> type, String value, T fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException invalidSavedValue) {
            return fallback;
        }
    }
}
