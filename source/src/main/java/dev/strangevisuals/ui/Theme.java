package dev.strangevisuals.ui;

import dev.strangevisuals.clickgui.ModuleRegistry;

public final class Theme {
    public static final int DEFAULT_ACCENT = 0xAFC6FF;
    public static final int DEFAULT_SECONDARY = 0xC7B4FF;

    public static int hudTint(String element) {
        return ModuleRegistry.tint(element) ? glassTint() : 0xFFFFFF;
    }

    public static int glassTint() {
        float[] hsv = rgbToHsv(accent());
        return hsvToRgb(hsv[0], hsv[1] * 0.62f, 1.0f);
    }

    public static final int RADIUS_LEVELS = 11;

    private Theme() {
    }

    public static int accent() {
        if (ModuleRegistry.uiAccent() == null) {
            return DEFAULT_ACCENT;
        }
        return usable(ModuleRegistry.uiAccent().rgb());
    }

    private static int usable(int rgb) {
        float[] hsv = rgbToHsv(rgb);
        return hsvToRgb(hsv[0], hsv[1], Math.max(0.45f, hsv[2]));
    }

    public static int secondary() {
        float[] hsv = rgbToHsv(accent());
        return hsvToRgb(hsv[0] + 0.075f, Math.min(1.0f, hsv[1] * 0.92f), Math.min(1.0f, hsv[2] * 1.02f));
    }

    public static int radiusLevel() {
        if (ModuleRegistry.uiRadius() == null) {
            return 5;
        }
        return Math.max(0, Math.min(RADIUS_LEVELS - 1, Math.round(ModuleRegistry.uiRadius().value())));
    }

    public static int skyAccent() {
        if (ModuleRegistry.skyColor() == null) {
            return DEFAULT_ACCENT;
        }
        return usable(ModuleRegistry.skyColor().rgb());
    }

    public static int skySecondary() {
        float[] hsv = rgbToHsv(skyAccent());
        return hsvToRgb(hsv[0] + 0.085f, Math.min(1.0f, hsv[1] * 0.9f), Math.min(1.0f, hsv[2] * 1.02f));
    }

    public static float[] rgbToHsv(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255.0f;
        float g = (rgb >> 8 & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0.0f;
        if (delta > 0.00001f) {
            if (max == r) {
                hue = (g - b) / delta / 6.0f;
            } else if (max == g) {
                hue = ((b - r) / delta + 2.0f) / 6.0f;
            } else {
                hue = ((r - g) / delta + 4.0f) / 6.0f;
            }
        }
        return new float[] {(hue % 1.0f + 1.0f) % 1.0f, max <= 0.0f ? 0.0f : delta / max, max};
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue % 1.0f + 1.0f) % 1.0f * 6.0f;
        float s = Math.max(0.0f, Math.min(1.0f, saturation));
        float v = Math.max(0.0f, Math.min(1.0f, value));

        int sector = (int) h;
        float f = h - sector;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));

        return switch (sector % 6) {
            case 0 -> pack(v, t, p);
            case 1 -> pack(q, v, p);
            case 2 -> pack(p, v, t);
            case 3 -> pack(p, q, v);
            case 4 -> pack(t, p, v);
            default -> pack(v, p, q);
        };
    }

    private static int pack(float r, float g, float b) {
        return Math.round(r * 255.0f) << 16 | Math.round(g * 255.0f) << 8 | Math.round(b * 255.0f);
    }

    public static int lighten(int rgb, float amount) {
        float[] hsv = rgbToHsv(rgb);
        return hsvToRgb(hsv[0], hsv[1] * (1.0f - amount), Math.min(1.0f, hsv[2] + (1.0f - hsv[2]) * amount));
    }
}
