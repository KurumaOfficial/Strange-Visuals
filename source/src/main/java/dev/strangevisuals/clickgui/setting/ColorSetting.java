package dev.strangevisuals.clickgui.setting;

import dev.strangevisuals.ui.Theme;

public final class ColorSetting extends Setting {
    public static final int[] PRESETS = {
        0xF2F4F8,
        0xFF6B6B,
        0x9B8CFF,
        0x4ADE80,
        0x5CC8E8
    };

    private static final float ROW_HEIGHT = 20.0f;

    private float hue;
    private float saturation;
    private float value;
    private boolean open;

    public ColorSetting(String name, int rgb) {
        super(name);
        set(rgb);
    }

    public int rgb() {
        return Theme.hsvToRgb(hue, saturation, value);
    }

    public void set(int rgb) {
        float[] hsv = Theme.rgbToHsv(rgb);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    public float hue() {
        return hue;
    }

    public float saturation() {
        return saturation;
    }

    public float value() {
        return value;
    }

    public void setHue(float newHue) {
        hue = Math.max(0.0f, Math.min(1.0f, newHue));
    }

    public void setSaturationValue(float newSaturation, float newValue) {
        saturation = Math.max(0.0f, Math.min(1.0f, newSaturation));
        value = Math.max(0.0f, Math.min(1.0f, newValue));
    }

    public int hueColor() {
        return Theme.hsvToRgb(hue, 1.0f, 1.0f);
    }

    public boolean open() {
        return open;
    }

    public void toggleOpen() {
        open = !open;
    }

    @Override
    public float height() {
        return ROW_HEIGHT;
    }
}
