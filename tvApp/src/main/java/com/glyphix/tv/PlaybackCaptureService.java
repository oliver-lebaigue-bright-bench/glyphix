package com.glyphix.tv;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Owns the one-use MediaProjection grant for one foreground visualizer session. */
public final class PlaybackCaptureService extends Service {
    private static final String CHANNEL = "glyphix-tv-audio";
    private static final String ACTION_STOP = "com.glyphix.tv.STOP_CAPTURE";
    private final LocalBinder binder = new LocalBinder();
    private AudioInput audio;
    private AudioInput.Listener listener;
    private MediaProjection projection;
    private String lastError;
    private boolean stopping;
    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override public void onStop() {
            if (!stopping) reportError("TV audio sharing has ended. Press Start to grant a new capture session.");
        }
    };

    public static Intent createStartIntent(Context context, int resultCode, Intent data, float gain) {
        return new Intent(context, PlaybackCaptureService.class)
                .putExtra("resultCode", resultCode).putExtra("projectionData", data).putExtra("gain", gain);
    }

    public final class LocalBinder extends Binder {
        public void setListener(AudioInput.Listener value) {
            listener = value;
            if (value != null && lastError != null) value.onError(lastError);
        }
        public void setGain(float gain) { if (audio != null) audio.setGain(gain); }
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            reportError("TV audio sharing has stopped. Press Start to begin a new session.");
            return START_NOT_STICKY;
        }
        if (projection != null || audio != null) return START_NOT_STICKY;
        stopping = false;
        lastError = null;
        if (Build.VERSION.SDK_INT < 29) {
            reportError("TV audio capture requires Android 10 or later. Use a microphone instead.");
            return START_NOT_STICKY;
        }
        try {
            NotificationManager notifications = getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "TV audio visualization", NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop = PendingIntent.getService(this, 2,
                    new Intent(this, PlaybackCaptureService.class).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent open = PendingIntent.getActivity(this, 1,
                    new Intent(this, TvActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification notification = new Notification.Builder(this, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("Glyphix TV is reacting to TV audio")
                    .setContentText("Audio is processed on this device. Stop to end sharing.")
                    .setContentIntent(open).setOngoing(true)
                    .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).build();
            startForeground(31, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            Intent data = intent.getParcelableExtra("projectionData");
            int code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            if (data == null || code != Activity.RESULT_OK) {
                reportError("TV audio sharing was cancelled. Press Start to try again.");
                return START_NOT_STICKY;
            }
            MediaProjectionManager manager = getSystemService(MediaProjectionManager.class);
            projection = manager.getMediaProjection(code, data);
            if (projection == null) {
                reportError("Android did not grant TV audio sharing. Try a microphone instead.");
                return START_NOT_STICKY;
            }
            projection.registerCallback(projectionCallback, new Handler(Looper.getMainLooper()));
            AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .excludeUid(android.os.Process.myUid()).build();
            audio = new AudioInput(this, new AudioInput.Listener() {
                @Override public void onLevel(float level, boolean beat) {
                    if (listener != null) listener.onLevel(level, beat);
                }
                @Override public void onError(String message) { reportError(message); }
            });
            audio.startPlayback(config, intent.getFloatExtra("gain", 1));
        } catch (RuntimeException e) {
            reportError("TV audio sharing could not start. Grant a fresh capture request or use a microphone.");
        }
        return START_NOT_STICKY;
    }

    private void reportError(String message) {
        lastError = message;
        // A bound service stays alive after stopSelf(): release capture immediately.
        endCapture();
        if (listener != null) listener.onError(message);
        stopSelf();
    }

    private void endCapture() {
        stopping = true;
        if (audio != null) { audio.stop(); audio = null; }
        if (projection != null) {
            MediaProjection current = projection;
            projection = null;
            try { current.unregisterCallback(projectionCallback); } catch (RuntimeException ignored) { }
            try { current.stop(); } catch (RuntimeException ignored) { }
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override public void onDestroy() {
        listener = null;
        endCapture();
        super.onDestroy();
    }
}
