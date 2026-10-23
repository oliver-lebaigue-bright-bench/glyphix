package com.glyphix.app.service;

import com.glyphix.app.R;
import com.glyphix.app.util.PermissionTrampolineActivity;
import android.app.PendingIntent;
import android.annotation.SuppressLint;
import android.os.Build;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class HapticsTileService extends TileService {
    @Override public void onStartListening() { super.onStartListening(); refresh(); }
    @Override public void onClick() {
        super.onClick();
        boolean running = AudioCaptureService.isRunning();
        boolean currentHaptics = AudioCaptureService.isHapticEnabledGlobal(this);
        boolean newState = !currentHaptics;
        
        // Always update the preference immediately for consistency
        getSharedPreferences("viz_prefs", MODE_PRIVATE)
                .edit().putBoolean("haptic_motor_enabled", newState).apply();
        
        if (!running) {
            refresh(newState);
            if (newState) {
                unlockAndRun(() -> {
                    Intent i = new Intent(this, PermissionTrampolineActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this,
                            4,
                            i,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startActivityAndCollapse(pendingIntent);
                    } else {
                        startLegacyActivityAndCollapse(i);
                    }
                });
            }
        } else {
            // Service is running, toggle haptics via intent
            Intent intent = new Intent(this, AudioCaptureService.class);
            intent.setAction(AudioCaptureService.ACTION_TOGGLE_HAPTICS);
            startService(intent);
            refresh(newState); // Immediate UI feedback
        }
    }
    
    // The PendingIntent overload was added in API 34. Older supported phones
    // must use the Intent overload, and every caller is guarded by SDK_INT < 34.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private void startLegacyActivityAndCollapse(Intent intent) {
        startActivityAndCollapse(intent);
    }

    public static void requestRefresh(Context context) {
        TileService.requestListeningState(context, new ComponentName(context, HapticsTileService.class));
    }
    
    private void refresh() {
        refresh(AudioCaptureService.isHapticEnabledGlobal(this));
    }

    private void refresh(boolean hapticsEnabled) {
        boolean vizRunning = AudioCaptureService.isRunning();
        
        Tile t = getQsTile(); if (t == null) return;
        
        // Active if haptics are enabled in preferences
        t.setState(hapticsEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        
        t.setLabel("Glyphix Haptics");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            t.setSubtitle(!vizRunning
                    ? (hapticsEnabled ? "Enabled" : "Disabled")
                    : (hapticsEnabled ? "Active" : "Ready"));
        }
        // Use the vibration icon
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_vibration));
        t.updateTile();
    }
}
