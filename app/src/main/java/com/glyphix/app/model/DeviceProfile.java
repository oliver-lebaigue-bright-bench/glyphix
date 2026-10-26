package com.glyphix.app.model;

import java.util.Locale;

/**
 * Device detection and display helpers used by the visualizer runtime.
 * Legacy hardcoded frequency maps live in zones.config now and no longer belong here.
 */
public final class DeviceProfile {

    public static final int DEVICE_UNKNOWN = 0;
    public static final int DEVICE_NP1 = 1;
    public static final int DEVICE_NP2 = 2;
    public static final int DEVICE_NP2A = 3;
    public static final int DEVICE_NP3A = 4;
    public static final int DEVICE_NP4A = 5;
    public static final int DEVICE_NP4APRO = 6;
    public static final int DEVICE_NP3 = 7;
    public static final int DEVICE_NP4B = 8;
    // Preserve existing stored IDs. The 3a Pro is a strip device, not ID 6 (4a Pro matrix).
    public static final int DEVICE_NP3APRO = 9;

    private DeviceProfile() {
    }

    public static int detectDevice() {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return DEVICE_UNKNOWN;
        }
        return detectModel(android.os.Build.MODEL);
    }

    /** Exact model aliases prevent a Pro suffix or unrelated model matching another phone. */
    public static int detectModel(String model) {
        String key = model == null ? "" : model.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (key.startsWith("NOTHING")) key = key.substring("NOTHING".length());
        return switch (key) {
            case "A063", "20111", "PHONE1" -> DEVICE_NP1;
            case "A065", "AIN065", "22111", "PHONE2" -> DEVICE_NP2;
            case "A142", "A142P", "23111", "23113", "PHONE2A", "PHONE2APLUS" -> DEVICE_NP2A;
            case "A059P", "24111P", "PHONE3APRO", "PHONE3APLUS" -> DEVICE_NP3APRO;
            case "A059", "24111", "PHONE3A" -> DEVICE_NP3A;
            case "A069P", "25111P", "PHONE4APRO" -> DEVICE_NP4APRO;
            case "A069", "25111", "PHONE4A" -> DEVICE_NP4A;
            case "A024", "23112", "PHONE3" -> DEVICE_NP3;
            case "25131", "26111", "PHONE4B" -> DEVICE_NP4B;
            default -> DEVICE_UNKNOWN;
        };
    }

    public static String deviceName(int device) {
        return switch (device) {
            case DEVICE_NP1 -> "Phone (1)";
            case DEVICE_NP2 -> "Phone (2)";
            case DEVICE_NP2A -> "Phone (2a) / 2a+";
            case DEVICE_NP3A -> "Phone (3a)";
            case DEVICE_NP3APRO -> "Phone (3a) Pro";
            case DEVICE_NP4APRO -> "Phone (4a) Pro";
            case DEVICE_NP4A -> "Phone (4a)";
            case DEVICE_NP3 -> "Phone (3)";
            case DEVICE_NP4B -> "Phone (4b)";
            default -> "Unknown";
        };
    }

    public static int getLedCount(int device) {
        return switch (device) {
            case DEVICE_NP1 -> 15;
            case DEVICE_NP2 -> 33;
            case DEVICE_NP2A -> 26;
            case DEVICE_NP3A, DEVICE_NP3APRO -> 36;
            case DEVICE_NP4A -> 7;
            case DEVICE_NP4APRO -> 169;
            case DEVICE_NP3 -> 625;
            case DEVICE_NP4B -> 5;
            default -> 0;
        };
    }

    public static int getMatrixWidth(int device) {
        return switch (device) {
            case DEVICE_NP3 -> 25;
            case DEVICE_NP4APRO -> 13;
            default -> 0;
        };
    }

    public static int getMatrixHeight(int device) {
        return switch (device) {
            case DEVICE_NP3 -> 25;
            case DEVICE_NP4APRO -> 13;
            default -> 0;
        };
    }
}
