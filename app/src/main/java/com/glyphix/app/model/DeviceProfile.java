package com.glyphix.app.model;

import com.nothing.ketchum.Common;

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

    private DeviceProfile() {
    }

    public static int detectDevice() {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return DEVICE_UNKNOWN;
        }
        String model = android.os.Build.MODEL != null ? android.os.Build.MODEL.toUpperCase(java.util.Locale.US) : "";
        if (Common.is20111() || model.contains("A063") || model.contains("20111") || model.contains("PHONE 1") || model.contains("PHONE (1)")) {
            return DEVICE_NP1;
        } else if (Common.is22111() || model.contains("A065") || model.contains("AIN065") || model.contains("22111") || model.contains("PHONE 2") || model.contains("PHONE (2)")) {
            return DEVICE_NP2;
        } else if (Common.is23111() || Common.is23113() || model.contains("A142") || model.contains("23111") || model.contains("23113") || model.contains("PHONE 2A") || model.contains("PHONE (2A)")) {
            return DEVICE_NP2A;
        } else if (Common.is25111p() || model.contains("A059P") || model.contains("A069P") || model.contains("24111P") || model.contains("25111P") || model.contains("3A PRO") || model.contains("3A_PRO") || model.contains("3A PLUS") || model.contains("3A+") || model.contains("4A PRO") || model.contains("4A_PRO")) {
            return DEVICE_NP4APRO;
        } else if (Common.is24111() || model.contains("A059") || model.contains("24111") || model.contains("PHONE 3A") || model.contains("PHONE (3A)")) {
            return DEVICE_NP3A;
        } else if (Common.is25111() || model.contains("A069") || model.contains("25111") || model.contains("PHONE 4A") || model.contains("PHONE (4A)")) {
            return DEVICE_NP4A;
        } else if (Common.is23112() || model.contains("A024") || model.contains("23112") || model.contains("PHONE 3") || model.contains("PHONE (3)")) {
            return DEVICE_NP3;
        } else if (model.contains("26111") || model.contains("PHONE 4B") || model.contains("PHONE (4B)")) {
            return DEVICE_NP4B;
        } else {
            return DEVICE_UNKNOWN;
        }
    }

    public static String deviceName(int device) {
        return switch (device) {
            case DEVICE_NP1 -> "Phone (1)";
            case DEVICE_NP2 -> "Phone (2)";
            case DEVICE_NP2A -> "Phone (2a) / 2a+";
            case DEVICE_NP3A -> "Phone (3a)";
            case DEVICE_NP4APRO -> "Phone (3a) Pro";
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
            case DEVICE_NP3A -> 36;
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
