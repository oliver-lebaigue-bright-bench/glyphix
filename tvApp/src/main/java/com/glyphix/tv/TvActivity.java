package com.glyphix.tv;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** TV-only foreground experience. No mobile code or torch permissions are used. */
public final class TvActivity extends Activity {
    static final String STARTUP_WARNING = "Warning. Glyphix TV displays full-screen flashing lights, rapidly changing colours and moving patterns. These can trigger seizures in people with photosensitive epilepsy and may cause dizziness, nausea, headaches or eye discomfort. Do not use if you or anyone nearby is sensitive to flashing light. If you have a history of seizures or are unsure, seek medical advice before use. Keep the room well lit, sit well back, start at low brightness and take regular breaks. Warn everyone nearby and make sure they agree before starting. Stop immediately if anyone feels unwell. Keep the remote within reach. Press Back at any time to stop. Continuing does not make these effects safe.";
    private static final String TERMS = "Glyphix TV is an entertainment light-effects app. You decide when and where to use it and are responsible for warning everyone who can see the screen and obtaining their agreement.\n\nDo not use around anyone sensitive to flashing light. Keep the room well lit, sit well back, use low brightness, take regular breaks and keep the remote within reach. Do not use where effects could distract drivers, obstruct safe movement or expose unsuspecting people. Stop immediately if anyone feels unwell.\n\nUse at your own risk. To the extent permitted by applicable law, the developers and contributors disclaim warranties and liability arising from use or misuse. Nothing in these terms excludes liability that cannot legally be excluded or limits your statutory rights. No setting or acknowledgement guarantees safety.\n\nThese terms apply only to Glyphix TV. By selecting I agree and continue, you acknowledge these risks and agree to use the app responsibly.";
    private static final String PRESTART = "Flashing lights and moving patterns are about to begin. They can trigger seizures and cause discomfort. Make sure everyone nearby has been warned and agrees. Keep the room well lit and the remote within reach. Stop immediately if anyone feels unwell. Press Back to stop.";
    private static final int INK = Color.rgb(13, 15, 22), WHITE = Color.rgb(244, 245, 250);
    private static final int MUTED = Color.rgb(175, 183, 203), ACCENT = Color.rgb(180, 246, 105);
    private static final int MIC_PERMISSION = 10, PLAYBACK_REQUEST = 11;
    private enum Stage { SPLASH, WARNING, TERMS, MENU, PRESTART, RUNNING }
    private Stage stage = Stage.SPLASH;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer narration;
    private AudioInput microphone;
    private ReactiveScreenView effects;
    private PlaybackCaptureService.LocalBinder captureBinder;
    private boolean captureBound, resumed, speechComplete;
    private int screenGeneration, modeIndex, sourceIndex, deviceIndex;
    private float brightness = .30f, gain = 2f, flashRate = 1f;
    private Intent projectionData;
    private int projectionResult;
    private String notice = "";
    private TextView narrationStatus, levelStatus;
    private ScrollView gateReading;
    private Button warningContinue;
    private final List<AudioDeviceInfo> inputs = new ArrayList<>();
    private final String[] modes = {"White pulses", "Colour pulses", "Reactive bars", "Rave patterns"};
    private final ReactiveScreenView.Mode[] effectModes = {ReactiveScreenView.Mode.WHITE,
            ReactiveScreenView.Mode.COLOUR, ReactiveScreenView.Mode.BARS, ReactiveScreenView.Mode.RAVE};

    private final AudioInput.Listener audioListener = new AudioInput.Listener() {
        @Override public void onLevel(float level, boolean beat) {
            if (stage != Stage.RUNNING || effects == null) return;
            effects.setLevel(level, beat);
            if (levelStatus != null) levelStatus.setText(level > .015f
                    ? "INPUT ACTIVE  •  BACK OR OK TO STOP" : "LISTENING FOR AUDIO  •  BACK OR OK TO STOP");
        }
        @Override public void onError(String message) {
            if (stage != Stage.RUNNING) return;
            stopOutput();
            notice = message;
            showMenu();
        }
    };
    private final ServiceConnection captureConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            if (stage != Stage.RUNNING) { stopOutput(); return; }
            captureBinder = (PlaybackCaptureService.LocalBinder) service;
            captureBinder.setListener(audioListener);
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            captureBinder = null;
            if (stage == Stage.RUNNING) audioListener.onError("TV audio sharing ended. Start again or choose a microphone.");
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        loadSettings();
        showSplash();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void changeStage(Stage next) {
        screenGeneration++;
        handler.removeCallbacksAndMessages(null);
        releaseNarration();
        stage = next;
    }
    private TextView label(String text, float size, int colour) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(colour);
        view.setLineSpacing(dp(3), 1.08f);
        return view;
    }
    private GradientDrawable rounded(int colour, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colour); drawable.setCornerRadius(dp(12));
        if (stroke != 0) drawable.setStroke(dp(2), stroke);
        return drawable;
    }
    private Button button(String text, Runnable action) {
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText(text); button.setAllCaps(false); button.setTextSize(18);
        button.setTextColor(new android.content.res.ColorStateList(new int[][] {
                {-android.R.attr.state_enabled}, {android.R.attr.state_focused}, {}},
                new int[] {Color.rgb(105, 112, 130), INK, WHITE}));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {-android.R.attr.state_enabled}, rounded(Color.rgb(26, 29, 40), 0));
        background.addState(new int[] {android.R.attr.state_focused}, rounded(ACCENT, WHITE));
        background.addState(new int[] {}, rounded(Color.rgb(34, 39, 54), Color.rgb(55, 64, 83)));
        button.setBackground(background); button.setMinHeight(dp(52));
        button.setPadding(dp(20), dp(10), dp(20), dp(10));
        button.setFocusable(true); button.setOnClickListener(v -> action.run());
        return button;
    }
    private void addButton(LinearLayout parent, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10); parent.addView(button, params);
    }
    private LinearLayout page(String kicker, String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(INK); scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(48), dp(26), dp(48), dp(30));
        TextView brand = label("GLYPHIX  /  TV", 16, ACCENT);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); body.addView(brand);
        TextView tag = label(kicker, 12, MUTED); tag.setPadding(0, dp(14), 0, dp(5)); body.addView(tag);
        TextView heading = label(title, 30, WHITE); heading.setTypeface(null, Typeface.BOLD); body.addView(heading);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView description = label(subtitle, 17, MUTED);
            description.setPadding(0, dp(8), 0, dp(12)); body.addView(description);
        }
        scroll.addView(body); setContentView(scroll); return body;
    }
    private void showSplash() {
        changeStage(Stage.SPLASH);
        LinearLayout frame = new LinearLayout(this); frame.setGravity(Gravity.CENTER);
        frame.setOrientation(LinearLayout.VERTICAL); frame.setBackgroundColor(INK);
        TextView logo = label("GLYPHIX", 48, WHITE); logo.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        frame.addView(logo); frame.addView(label("SCREEN REACTIVE  /  TV EDITION", 14, ACCENT));
        setContentView(frame);
        handler.postDelayed(() -> { if (resumed) showStartupWarning(); }, 900);
    }

    /** Keep actions visible while remote users scroll the complete warning. */
    private LinearLayout gatePage(String kicker, String title, String copy) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(INK); root.setPadding(dp(48), dp(22), dp(48), dp(22));
        TextView brand = label("GLYPHIX / TV    ·    " + kicker, 13, ACCENT);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); root.addView(brand);
        TextView heading = label(title, 28, WHITE); heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(8), 0, dp(14)); root.addView(heading);
        LinearLayout columns = new LinearLayout(this);
        root.addView(columns, new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView reading = new ScrollView(this); reading.setFillViewport(false);
        gateReading = reading;
        reading.setFocusable(true); reading.setContentDescription("Warning text. Use up and down to read, right for actions.");
        TextView text = label(copy, 17, WHITE); text.setPadding(0, 0, dp(24), dp(12)); reading.addView(text);
        columns.addView(reading, new LinearLayout.LayoutParams(0, -1, 1.65f));
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(dp(18), 0, 0, 0);
        columns.addView(actions, new LinearLayout.LayoutParams(0, -1, 1));
        TextView hint = label("D-pad ↑ ↓ to read  ·  → for actions  ·  Back to leave", 13, MUTED);
        hint.setPadding(0, dp(12), 0, 0); root.addView(hint);
        setContentView(root); reading.requestFocus(); return actions;
    }

    private void showStartupWarning() {
        changeStage(Stage.WARNING); speechComplete = false;
        LinearLayout body = gatePage("01 / HEALTH & SAFETY", "Before the lights", STARTUP_WARNING);
        narrationStatus = label("Listen to the complete warning. Turn your TV volume on. Voice: Daniel, English (UK).", 14, ACCENT);
        body.addView(narrationStatus);
        warningContinue = button("Listening to safety warning…", () -> {
            if (speechComplete) showTerms();
        });
        warningContinue.setEnabled(false); addButton(body, warningContinue);
        Button replay = button("Replay spoken warning", this::playNarration);
        addButton(body, replay); gateReading.setNextFocusRightId(replay.getId());
        Button exit = button("Exit Glyphix TV", this::finish); addButton(body, exit);
        playNarration();
    }
    private void playNarration() {
        if (stage != Stage.WARNING || !resumed) return;
        releaseNarration(); speechComplete = false;
        warningContinue.setEnabled(false); warningContinue.setText("Listening to safety warning…");
        final int generation = screenGeneration;
        try {
            narration = MediaPlayer.create(this, R.raw.safety_warning);
            if (narration == null) throw new IllegalStateException("Narration unavailable");
            narration.setOnCompletionListener(player -> {
                if (stage != Stage.WARNING || generation != screenGeneration || !resumed) return;
                speechComplete = true;
                narrationStatus.setText("Warning finished. Continuing does not make the effects safe.");
                warningContinue.setText("I understand — continue"); warningContinue.setEnabled(true);
                gateReading.setNextFocusRightId(warningContinue.getId());
                releaseNarration();
            });
            narration.setOnErrorListener((player, what, extra) -> {
                narrationStatus.setText("The spoken warning could not play. Choose Replay to retry, or Exit.");
                releaseNarration(); return true;
            });
            narrationStatus.setText("Playing the safety warning • Daniel, English (UK). Continue unlocks when it finishes.");
            narration.start();
        } catch (RuntimeException error) {
            narrationStatus.setText("The spoken warning could not play. Choose Replay to retry, or Exit.");
            releaseNarration();
        }
    }
    private void releaseNarration() {
        if (narration != null) {
            narration.setOnCompletionListener(null); narration.setOnErrorListener(null);
            narration.release(); narration = null;
        }
    }
    private void showTerms() {
        changeStage(Stage.TERMS);
        LinearLayout body = gatePage("02 / TV EDITION TERMS", "Your choice. Your responsibility.", TERMS);
        Button agree = button("I agree and continue", this::showMenu); addButton(body, agree);
        Button exit = button("Decline and exit", this::finish); addButton(body, exit);
        countdown(agree, "I agree and continue");
    }
    private void countdown(Button button, String readyText) {
        final int generation = screenGeneration;
        final long readyAt = SystemClock.elapsedRealtime() + 3000;
        button.setEnabled(false);
        Runnable update = new Runnable() {
            @Override public void run() {
                if (generation != screenGeneration || !resumed) return;
                long remaining = readyAt - SystemClock.elapsedRealtime();
                if (remaining <= 0) {
                    button.setText(readyText); button.setEnabled(true);
                    gateReading.setNextFocusRightId(button.getId());
                } else {
                    button.setText(readyText + "  ·  " + ((remaining + 999) / 1000) + "s");
                    handler.postDelayed(this, Math.min(remaining, 200));
                }
            }
        };
        update.run();
    }

    private void loadSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("tv-settings", MODE_PRIVATE);
        modeIndex = Math.max(0, Math.min(3, prefs.getInt("mode", 2)));
        sourceIndex = Math.max(0, Math.min(1, prefs.getInt("source", 0)));
        brightness = Math.max(.1f, Math.min(.8f, prefs.getFloat("brightness", .3f)));
        gain = Math.max(1f, Math.min(8f, prefs.getFloat("gain", 2f)));
        flashRate = Math.max(.5f, Math.min(2f, prefs.getFloat("rate", 1f)));
    }
    private void saveSettings() {
        getSharedPreferences("tv-settings", MODE_PRIVATE).edit().putInt("mode", modeIndex)
                .putInt("source", sourceIndex).putFloat("brightness", brightness)
                .putFloat("gain", gain).putFloat("rate", flashRate).apply();
    }
    private void refreshInputs() {
        inputs.clear(); inputs.add(null);
        AudioManager manager = getSystemService(AudioManager.class);
        for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (device.getType() != AudioDeviceInfo.TYPE_TELEPHONY) inputs.add(device);
        }
        if (deviceIndex >= inputs.size()) deviceIndex = 0;
    }
    private String deviceName() {
        if (deviceIndex == 0) return "System default";
        AudioDeviceInfo input = inputs.get(deviceIndex);
        return input.getProductName() + " (input " + input.getId() + ")";
    }
    private void showMenu() {
        changeStage(Stage.MENU); projectionData = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        refreshInputs();
        LinearLayout body = page("AUDIO → LIGHT", "Make the room move.",
                "Choose an effect and an audio source. Use the D-pad and OK to change settings. No lights run until you confirm Start.");
        if (!notice.isEmpty()) { body.addView(label(notice, 16, ACCENT)); notice = ""; }
        LinearLayout columns = new LinearLayout(this); columns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL);
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(28), dp(16), 0, 0);
        columns.addView(controls, new LinearLayout.LayoutParams(0, -2, 1.2f));
        columns.addView(info, new LinearLayout.LayoutParams(0, -2, 1f)); body.addView(columns);
        Button mode = button("Effect  ›  " + modes[modeIndex], () -> {});
        mode.setOnClickListener(v -> { modeIndex = (modeIndex + 1) % modes.length;
            mode.setText("Effect  ›  " + modes[modeIndex]); saveSettings(); }); addButton(controls, mode);
        Button source = button("Audio  ›  " + (sourceIndex == 0 ? "Microphone" : "TV playback (Android 10+)"), () -> {});
        source.setOnClickListener(v -> { sourceIndex = 1 - sourceIndex; saveSettings(); showMenu(); }); addButton(controls, source);
        if (sourceIndex == 0) {
            Button device = button("Mic  ›  " + deviceName(), () -> {});
            device.setOnClickListener(v -> { deviceIndex = (deviceIndex + 1) % inputs.size(); device.setText("Mic  ›  " + deviceName()); });
            addButton(controls, device);
        }
        Button output = button("Brightness  ›  " + Math.round(brightness * 100) + "%", () -> {});
        output.setOnClickListener(v -> { brightness = brightness >= .79f ? .1f : brightness + .1f;
            output.setText("Brightness  ›  " + Math.round(brightness * 100) + "%"); saveSettings(); }); addButton(controls, output);
        Button sensitivity = button("Sensitivity  ›  " + gain + "×", () -> {});
        sensitivity.setOnClickListener(v -> { gain = gain >= 8 ? 1 : gain * 2;
            sensitivity.setText("Sensitivity  ›  " + gain + "×"); saveSettings(); }); addButton(controls, sensitivity);
        Button rate = button("Pulse limit  ›  " + flashRate + " / second", () -> {});
        rate.setOnClickListener(v -> { flashRate = flashRate >= 2 ? .5f : flashRate + .5f;
            rate.setText("Pulse limit  ›  " + flashRate + " / second"); saveSettings(); }); addButton(controls, rate);
        Button start = button("Start  →", this::prepareStart); addButton(controls, start);
        addButton(controls, button("Safety warning & TV terms", this::showStartupWarning));
        addButton(controls, button("Exit", this::finish));
        info.addView(label("LISTEN LOCALLY", 15, ACCENT));
        info.addView(label("Microphone: react to your voice, claps, instruments or music. Connect a compatible USB mic if your TV has no accessible microphone. Most remote voice buttons are reserved for the TV assistant.\n\nTV playback: Android can share eligible media audio after you grant permission. Protected or opted-out apps may remain silent. Some TVs do not support capture.\n\nAudio is processed on this TV. It is never recorded to a file or uploaded. Silence leaves the output dark.", 16, MUTED));
        TextView caution = label("LOWER BRIGHTNESS AND SLOWER PULSES DO NOT GUARANTEE SAFETY.\n\nBack or OK stops immediately. Leaving the app stops capture. Each new session requires confirmation.", 14, ACCENT);
        caution.setPadding(0, dp(20), 0, 0); info.addView(caution); mode.requestFocus();
    }

    private void prepareStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION); return;
        }
        if (sourceIndex == 1) {
            if (Build.VERSION.SDK_INT < 29) { notice = "TV playback requires Android 10+. Choose Microphone."; showMenu(); return; }
            try {
                startActivityForResult(getSystemService(MediaProjectionManager.class).createScreenCaptureIntent(), PLAYBACK_REQUEST);
            } catch (RuntimeException error) { notice = "This TV cannot grant playback capture. Choose Microphone."; showMenu(); }
        } else showPrestart();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == MIC_PERMISSION) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) prepareStart();
            else { notice = "Audio permission was denied. Enable Microphone permission in TV Settings, then try again."; showMenu(); }
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PLAYBACK_REQUEST) {
            if (result == RESULT_OK && data != null) {
                projectionData = data; projectionResult = result; showPrestart();
            } else { notice = "TV audio sharing was cancelled. You can use a microphone instead."; showMenu(); }
        }
    }
    private void showPrestart() {
        changeStage(Stage.PRESTART);
        LinearLayout body = gatePage("FINAL CHECK / EVERY SESSION", "Lights are about to begin", PRESTART);
        body.addView(label(modes[modeIndex] + "  •  " + Math.round(brightness * 100) + "% brightness  •  "
                + flashRate + " pulses/second maximum", 16, ACCENT));
        Button start = button("Start effects", this::beginOutput); addButton(body, start);
        Button cancel = button("Cancel", this::showMenu); addButton(body, cancel);
        countdown(start, "Start effects");
    }
    private void beginOutput() {
        if (!resumed || stage != Stage.PRESTART) return;
        if (sourceIndex == 1 && projectionData == null) {
            notice = "Grant a new TV playback capture session before starting."; showMenu(); return;
        }
        Intent consent = projectionData; int result = projectionResult;
        projectionData = null; changeStage(Stage.RUNNING);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout frame = new FrameLayout(this); frame.setBackgroundColor(Color.BLACK);
        effects = new ReactiveScreenView(this); effects.setMode(effectModes[modeIndex]);
        effects.setBrightness(brightness); effects.setMaxFlashRate(flashRate);
        frame.addView(effects, new FrameLayout.LayoutParams(-1, -1));
        levelStatus = label("LISTENING FOR AUDIO  •  BACK OR OK TO STOP", 14, WHITE);
        levelStatus.setGravity(Gravity.CENTER); levelStatus.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-1, dp(42), Gravity.BOTTOM);
        frame.addView(levelStatus, statusParams); frame.setOnClickListener(v -> stopAndShowMenu());
        frame.setFocusableInTouchMode(true); setContentView(frame); frame.requestFocus(); effects.start();
        try {
            if (sourceIndex == 0) {
                microphone = new AudioInput(this, audioListener);
                microphone.startMicrophone(inputs.get(deviceIndex), gain);
            } else {
                Intent intent = PlaybackCaptureService.createStartIntent(this, result, consent, gain);
                startForegroundService(intent);
                captureBound = bindService(intent, captureConnection, Context.BIND_AUTO_CREATE);
                if (!captureBound) audioListener.onError("Could not connect to TV audio capture. Try again or choose Microphone.");
            }
        } catch (RuntimeException error) { audioListener.onError("Audio capture could not start. Check permissions and the selected input."); }
    }
    private void stopOutput() {
        if (effects != null) { effects.stop(); effects = null; }
        if (microphone != null) { microphone.stop(); microphone = null; }
        if (captureBinder != null) { captureBinder.setListener(null); captureBinder = null; }
        if (captureBound) { unbindService(captureConnection); captureBound = false; }
        stopService(new Intent(this, PlaybackCaptureService.class));
        projectionData = null; levelStatus = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void stopAndShowMenu() { stopOutput(); showMenu(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (stage == Stage.RUNNING && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_ESCAPE:
                case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_MEDIA_STOP: case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    stopAndShowMenu(); return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if (stage == Stage.RUNNING) stopAndShowMenu();
        else if (stage == Stage.PRESTART) showMenu();
        else finish();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        if (stage == Stage.WARNING && narration == null && !speechComplete) playNarration();
        else if (stage == Stage.TERMS) showTerms();
        else if (stage == Stage.PRESTART) showPrestart();
        else if (stage == Stage.SPLASH) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::showStartupWarning, 900);
        }
    }
    @Override protected void onPause() {
        resumed = false; handler.removeCallbacksAndMessages(null); releaseNarration();
        if (stage == Stage.WARNING) speechComplete = false;
        if (stage == Stage.RUNNING) { stopOutput(); showMenu(); }
        super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && stage == Stage.RUNNING) stopAndShowMenu();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null); releaseNarration(); stopOutput(); super.onDestroy();
    }
}
