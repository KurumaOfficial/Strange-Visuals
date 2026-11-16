package dev.strangevisuals.clickgui.classic.theme;

public final class ClassicThemeManager {
    private static ClassicTheme currentTheme = ClassicTheme.WHITE;
    private static ClassicTheme previousTheme = ClassicTheme.WHITE;

    private static float animation = 1.0f;
    private static final float ANIMATION_SPEED = 0.08f;

    private ClassicThemeManager() {
    }

    public static void setTheme(ClassicTheme theme) {
        if (theme == null || theme == currentTheme) {
            return;
        }

        previousTheme = currentTheme;
        currentTheme = theme;
        animation = 0.0f;
    }

    public static void update() {
        if (animation < 1.0f) {
            animation += ANIMATION_SPEED;
            if (animation > 1.0f) {
                animation = 1.0f;
            }
        }
    }

    public static ClassicTheme getTheme() {
        return currentTheme;
    }

    public static ClassicTheme getPreTheme() {
        return previousTheme;
    }

    public static float getProgress() {
        return animation;
    }
}
