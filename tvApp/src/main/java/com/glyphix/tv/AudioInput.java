package com.glyphix.tv;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicInteger;

/** Foreground-only, in-memory microphone or playback analysis. */
public final class AudioInput {
    public interface Listener {
        void onLevel(float level, boolean beat);
        void onError(String message);
    }

    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger generation = new AtomicInteger();
    private volatile float gain = 1;
    private volatile AudioRecord recorder;
    private Thread worker;

    public AudioInput(Context context, Listener listener) { this.listener = listener; }

    public void setGain(float gain) { this.gain = Math.max(0.1f, Math.min(10, gain)); }

    public void startMicrophone(AudioDeviceInfo preferredDevice, float gain) {
        stop();
        setGain(gain);
        final int session = generation.get();
        try {
            AudioRecord record = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(format())
                    .setBufferSizeInBytes(bufferSize()).build();
            if (preferredDevice != null && !record.setPreferredDevice(preferredDevice)) {
                record.release();
                fail(session, "This microphone cannot be selected. Try System default or reconnect the input device.");
                return;
            }
            begin(record, session);
        } catch (SecurityException e) {
            fail(session, "Microphone permission is required. Allow microphone access in Android settings.");
        } catch (RuntimeException e) {
            fail(session, "Cannot open a microphone. Connect a compatible USB microphone or choose another input.");
        }
    }

    public void startPlayback(AudioPlaybackCaptureConfiguration config, float gain) {
        stop();
        setGain(gain);
        final int session = generation.get();
        if (Build.VERSION.SDK_INT < 29) {
            fail(session, "TV audio capture requires Android 10 or later. Use a microphone instead.");
            return;
        }
        try {
            AudioRecord record = new AudioRecord.Builder().setAudioFormat(format())
                    .setBufferSizeInBytes(bufferSize()).setAudioPlaybackCaptureConfig(config).build();
            begin(record, session);
        } catch (SecurityException e) {
            fail(session, "TV audio capture was not permitted. Grant microphone and capture access, then try again.");
        } catch (RuntimeException e) {
            fail(session, "TV audio capture is unavailable on this device. Try a microphone instead.");
        }
    }

    private AudioFormat format() {
        return new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
    }

    private int bufferSize() {
        return Math.max(8192, AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
    }

    private void begin(AudioRecord record, int session) {
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            record.release();
            fail(session, "The selected audio input could not be initialized. Choose another input.");
            return;
        }
        recorder = record;
        worker = new Thread(() -> read(record, session), "Glyphix-TV-audio");
        worker.start();
    }

    private void read(AudioRecord record, int session) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        SignalAnalyzer analyzer = new SignalAnalyzer();
        short[] samples = new short[1024];
        try {
            record.startRecording();
            if (record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IllegalStateException("Input unavailable");
            }
            while (generation.get() == session) {
                int count = record.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                if (generation.get() != session) break;
                if (count < 0) {
                    fail(session, "Audio input disconnected or became unavailable. Reconnect it and press Start again.");
                    break;
                }
                if (count == 0) continue;
                SignalAnalyzer.Frame frame = analyzer.analyze(samples, count, gain, SystemClock.elapsedRealtime());
                main.post(() -> {
                    if (generation.get() == session) listener.onLevel(frame.level, frame.beat);
                });
            }
        } catch (SecurityException e) {
            fail(session, "Audio access was revoked. Allow microphone access to continue.");
        } catch (RuntimeException e) {
            if (generation.get() == session)
                fail(session, "Audio input is unavailable or already in use. Check the connection and try again.");
        } finally {
            try { record.stop(); } catch (RuntimeException ignored) { }
            record.release();
            if (recorder == record) recorder = null;
        }
    }

    private void fail(int session, String message) {
        main.post(() -> { if (generation.get() == session) listener.onError(message); });
    }

    public void stop() {
        generation.incrementAndGet();
        AudioRecord current = recorder;
        if (current != null) try { current.stop(); } catch (RuntimeException ignored) { }
        Thread previous = worker;
        worker = null;
        if (previous != null && previous != Thread.currentThread()) {
            try { previous.join(350); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
