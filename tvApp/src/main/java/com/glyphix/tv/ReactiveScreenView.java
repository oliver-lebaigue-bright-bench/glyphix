package com.glyphix.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/** Full-screen screen effects. No camera, torch, network, or persistent audio access. */
public final class ReactiveScreenView extends View {
    public enum Mode { WHITE, COLOUR, BARS, RAVE }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final FlashLimiter limiter = new FlashLimiter();
    private Mode mode = Mode.BARS;
    private float brightness = 0.3f;
    private float level;
    private float drawnLevel;
    private float hue = 190;
    private long lastInput;
    private long lastPulse = -10000;
    private long lastDraw;
    private boolean running;

    public ReactiveScreenView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setFocusable(false);
        setContentDescription("Audio reactive screen. Press Back to stop.");
    }

    public void setMode(Mode value) { mode = value == null ? Mode.BARS : value; invalidate(); }
    public void setBrightness(float value) { brightness = Math.max(0.02f, Math.min(1, value)); }
    public void setMaxFlashRate(float value) { limiter.setRate(value); }

    public void setLevel(float value, boolean beat) {
        if (!running) return;
        lastInput = SystemClock.elapsedRealtime();
        level = Math.max(0, Math.min(1, value));
        if (beat && limiter.allow(lastInput)) {
            lastPulse = lastInput;
            hue = (hue + 47) % 360;
        }
    }

    public void start() {
        level = drawnLevel = 0;
        lastPulse = -10000;
        lastInput = lastDraw = SystemClock.elapsedRealtime();
        limiter.reset();
        running = true;
        postInvalidateOnAnimation();
    }

    public void stop() {
        running = false;
        level = drawnLevel = 0;
        lastPulse = -10000;
        invalidate();
    }

    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }

    private int colour(float hue, float saturation, float intensity) {
        return Color.HSVToColor(new float[]{hue % 360, saturation,
                Math.max(0, Math.min(1, intensity * brightness))});
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (!running) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastInput > 350) level = 0;
        float dt = Math.min(0.1f, Math.max(0.001f, (now - lastDraw) / 1000f));
        lastDraw = now;
        drawnLevel += (level - drawnLevel) * Math.min(1, dt * (level > drawnLevel ? 10 : 5));
        float pulse = Math.max(0, 1 - (now - lastPulse) / 180f);
        float w = getWidth(), h = getHeight();
        switch (mode) {
            case WHITE:
                canvas.drawColor(colour(0, 0, pulse * Math.max(0.25f, drawnLevel)));
                break;
            case COLOUR:
                canvas.drawColor(colour(hue, 0.75f, drawnLevel * 0.7f + pulse * 0.3f));
                break;
            case BARS:
                for (int i = 0; i < 12; i++) {
                    float shape = 0.45f + 0.55f * (float)Math.abs(Math.sin((i + 1) * 0.9));
                    float height = h * drawnLevel * shape;
                    paint.setColor(colour(hue + i * 5, 0.7f, 0.9f));
                    canvas.drawRoundRect(i * w / 12 + w * 0.006f, h - height,
                            (i + 1) * w / 12 - w * 0.006f, h, 8, 8, paint);
                }
                break;
            case RAVE:
                if (drawnLevel > 0.005f) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(4, h * 0.025f * drawnLevel));
                    float advance = Math.min(1, (now - lastPulse) / 800f);
                    for (int i = 0; i < 5; i++) {
                        float radius = (i + advance) * Math.min(w, h) / 7;
                        paint.setColor(colour(hue + i * 16, 0.75f, drawnLevel));
                        canvas.drawCircle(w / 2, h / 2, radius, paint);
                    }
                    paint.setStyle(Paint.Style.FILL);
                }
                break;
        }
        postInvalidateOnAnimation();
    }
}
