package dev.strangevisuals.clickgui.classic.util;

public class ScrollUtil {
    private float target;
    private float scroll;
    private float max;
    private float speed = 12.0f;
    private boolean enabled = true;

    public ScrollUtil() {
    }

    public void update() {
        scroll = lerp(scroll, target, speed / 100.0f);
    }

    public void handleScroll(double scrollY) {
        if (!enabled) {
            return;
        }
        float wheel = (float) scrollY * (speed * 10.0f);
        target = Math.min(Math.max(target + (wheel / 2.0f), max), 0.0f);
    }

    private static float lerp(float input, float target, float step) {
        return input + step * (target - input);
    }

    public void reset() {
        this.scroll = 0.0f;
        this.target = 0.0f;
    }

    public void setMax(float contentHeight, float listHeight) {
        float newMax = Math.min(0.0f, -contentHeight + listHeight);
        this.max = newMax;
        this.target = Math.min(Math.max(target, newMax), 0.0f);
        this.scroll = Math.min(Math.max(scroll, newMax), 0.0f);

        if (newMax == 0.0f) {
            reset();
        }
    }

    public float getScroll() {
        return scroll;
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
    }

    public float getTarget() {
        return target;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
