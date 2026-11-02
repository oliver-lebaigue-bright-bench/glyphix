package com.glyphix.tv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Device checks for timed gates and output shutdown; no microphone or flashing output is started. */
public final class GateInstrumentation extends Instrumentation {
    private TvActivity activity;
    private int passed;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            activity = (TvActivity) startActivitySync(new Intent(Intent.ACTION_MAIN)
                    .setClassName(getTargetContext(), "com.glyphix.tv.TvActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            waitForStage("WARNING", 5000);
            main(() -> {
                Button next = (Button) field(activity, "warningContinue");
                check(!next.isEnabled(), "Startup Continue is locked before narration completion");
                check(field(activity, "narration") != null, "Startup warning has narration");
                check(!(Boolean)field(activity, "speechComplete"), "Narration has not finished");
            });
            passed("Startup narration gate");

            main(() -> {
                Object previous = field(activity, "narration");
                callActivityOnPause(activity);
                check(field(activity, "narration") == null, "Pause releases narration");
                check(!(Boolean)field(activity, "speechComplete"), "Pause resets narration completion");
                callActivityOnResume(activity);
                check(field(activity, "narration") != null && field(activity, "narration") != previous,
                        "Resume restarts narration");
                check(!((Button)field(activity, "warningContinue")).isEnabled(), "Resume keeps Continue locked");
            });
            passed("Interrupted narration restarts");

            main(() -> invoke("showTerms"));
            checkCountdown("I agree and continue");
            passed("TV terms visibly count down for three seconds");

            main(() -> invoke("showPrestart"));
            checkCountdown("Start effects");
            passed("Pre-start gate visibly counts down for three seconds");

            final ReactiveScreenView[] effect = new ReactiveScreenView[1];
            main(() -> {
                effect[0] = seedBlackOutput();
                activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                check("MENU".equals(stage()), "Back returns to menu");
                check(field(activity, "effects") == null, "Back clears active output");
                check(!(Boolean)field(effect[0], "running"), "Back stops the renderer");
                check(field(activity, "microphone") == null, "No microphone remains");
                check(!(Boolean)field(activity, "captureBound"), "No capture service remains bound");
            });
            passed("Back immediately stops output");

            main(() -> {
                effect[0] = seedBlackOutput();
                callActivityOnPause(activity);
                check("MENU".equals(stage()), "Leaving output returns to menu");
                check(field(activity, "effects") == null, "Pause clears active output");
                check(!(Boolean)field(effect[0], "running"), "Pause stops the renderer");
                check((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0,
                        "Pause clears keep-screen-on");
                callActivityOnResume(activity);
            });
            passed("Leaving the app stops output");

            checkBoundServiceStop();
            passed("Notification Stop cleans up while the service remains bound");
            result.putString("stream", "\nPASS: " + passed + " TV gate and lifecycle checks. No flashing output or capture was started.\n");
            result.putInt("numtests", passed);
            result.putInt("numfailures", 0);
            main(() -> activity.finish());
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "\nFAIL after " + passed + " checks: " + error + "\n");
            result.putString("shortMsg", error.toString());
            if (activity != null) main(() -> activity.finish());
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void checkBoundServiceStop() throws InterruptedException {
        CountDownLatch connected = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                try {
                    PlaybackCaptureService service = (PlaybackCaptureService)field(binder, "this$0");
                    boolean[] notified = new boolean[1];
                    ((PlaybackCaptureService.LocalBinder)binder).setListener(new AudioInput.Listener() {
                        @Override public void onLevel(float level, boolean beat) { }
                        @Override public void onError(String message) { notified[0] = true; }
                    });
                    // An unopened controller is enough to verify cleanup runs before unbinding.
                    setField(service, "audio", new AudioInput(activity, new AudioInput.Listener() {
                        @Override public void onLevel(float level, boolean beat) { }
                        @Override public void onError(String message) { }
                    }));
                    service.onStartCommand(new Intent(activity, PlaybackCaptureService.class)
                            .setAction("com.glyphix.tv.STOP_CAPTURE"), 0, 1);
                    check(notified[0], "Bound listener is immediately told capture stopped");
                    check(field(service, "audio") == null, "Audio controller is cleared before unbinding");
                    check((Boolean)field(service, "stopping"), "Capture cleanup ran while bound");
                } catch (Throwable error) { failure[0] = error; }
                finally { connected.countDown(); }
            }
            @Override public void onServiceDisconnected(ComponentName name) { }
        };
        main(() -> check(activity.bindService(new Intent(activity, PlaybackCaptureService.class),
                connection, Context.BIND_AUTO_CREATE), "Can bind capture service"));
        try {
            check(connected.await(5, TimeUnit.SECONDS), "Capture service connected");
            if (failure[0] != null) throw new AssertionError(failure[0]);
        } finally { main(() -> activity.unbindService(connection)); }
    }

    private void checkCountdown(String prefix) {
        final Button[] action = new Button[1];
        main(() -> {
            action[0] = findButton(activity.getWindow().getDecorView(), prefix);
            check(action[0] != null, "Countdown action exists");
            check(!action[0].isEnabled(), "Action starts disabled");
            check(action[0].getText().toString().contains("3s"), "Visible countdown starts at 3s");
        });
        SystemClock.sleep(1200);
        main(() -> check(!action[0].isEnabled(), "Action remains locked before three seconds"));
        SystemClock.sleep(2100);
        main(() -> {
            check(action[0].isEnabled(), "Action unlocks after three seconds");
            check(prefix.equals(action[0].getText().toString()), "Countdown is replaced by action label");
            View reading = (View)field(activity, "gateReading");
            check(reading.focusSearch(View.FOCUS_RIGHT) == action[0], "D-pad right reaches the unlocked action");
        });
    }

    private ReactiveScreenView seedBlackOutput() {
        invoke("showMenu");
        ReactiveScreenView screen = new ReactiveScreenView(activity);
        activity.setContentView(screen);
        screen.start(); // No samples are supplied, so the renderer remains black.
        setField(activity, "effects", screen);
        try {
            Field value = TvActivity.class.getDeclaredField("stage"); value.setAccessible(true);
            for (Object option : value.getType().getEnumConstants())
                if ("RUNNING".equals(option.toString())) value.set(activity, option);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return screen;
    }

    private void waitForStage(String expected, long timeoutMs) {
        long until = SystemClock.elapsedRealtime() + timeoutMs;
        final boolean[] ready = new boolean[1];
        while (SystemClock.elapsedRealtime() < until) {
            main(() -> ready[0] = expected.equals(stage()));
            if (ready[0]) return;
            SystemClock.sleep(50);
        }
        throw new AssertionError("Timed out waiting for " + expected);
    }
    private String stage() { return field(activity, "stage").toString(); }
    private void main(Runnable action) {
        final Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try { action.run(); } catch (Throwable error) { failure[0] = error; }
        });
        if (failure[0] != null) throw new AssertionError(failure[0]);
    }
    private void passed(String message) {
        passed++;
        Bundle update = new Bundle(); update.putString("stream", "PASS: " + message + "\n");
        sendStatus(0, update);
    }
    private static Button findButton(View view, String prefix) {
        if (view instanceof Button && ((Button)view).getText().toString().startsWith(prefix)) return (Button)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static Object field(Object target, String name) {
        try {
            Field value = target.getClass().getDeclaredField(name); value.setAccessible(true); return value.get(target);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void setField(Object target, String name, Object value) {
        try {
            Field targetField = target.getClass().getDeclaredField(name); targetField.setAccessible(true); targetField.set(target, value);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private void invoke(String name) {
        try {
            Method method = TvActivity.class.getDeclaredMethod(name); method.setAccessible(true); method.invoke(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
