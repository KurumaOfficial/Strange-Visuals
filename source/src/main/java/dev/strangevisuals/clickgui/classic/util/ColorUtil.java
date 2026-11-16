package dev.strangevisuals.clickgui.classic.util;

import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int replAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }

    public static int multAlpha(int color, float factor) {
        int currentAlpha = getAlpha(color);
        int newAlpha = Math.max(0, Math.min(255, Math.round(currentAlpha * Math.max(0.0f, Math.min(1.0f, factor)))));
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    public static int interpolate(int color1, int color2, float progress) {
        float p = Math.max(0.0f, Math.min(1.0f, progress));
        int a = Math.round(getAlpha(color1) + (getAlpha(color2) - getAlpha(color1)) * p);
        int r = Math.round(red(color1) + (red(color2) - red(color1)) * p);
        int g = Math.round(green(color1) + (green(color2) - green(color1)) * p);
        int b = Math.round(blue(color1) + (blue(color2) - blue(color1)) * p);
        return argb(a, r, g, b);
    }

    public static int getMainColor(int speed, int index) {
        ClassicTheme current = ClassicThemeManager.getTheme();
        ClassicTheme pre = ClassicThemeManager.getPreTheme();
        float t = ClassicThemeManager.getProgress();
        return interpolate(pre.getMain(), current.getMain(), t);
    }

    public static int getBackGroundColor(int speed, int index) {
        ClassicTheme current = ClassicThemeManager.getTheme();
        ClassicTheme pre = ClassicThemeManager.getPreTheme();
        float t = ClassicThemeManager.getProgress();
        return interpolate(pre.getBg(), current.getBg(), t);
    }

    public static int getTextColor(int speed, int index) {
        ClassicTheme current = ClassicThemeManager.getTheme();
        ClassicTheme pre = ClassicThemeManager.getPreTheme();
        float t = ClassicThemeManager.getProgress();
        return interpolate(pre.getText(), current.getText(), t);
    }
}
