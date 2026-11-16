package dev.strangevisuals.clickgui.classic.theme;

import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;

public enum ClassicTheme {
    WHITE("Белая", 0xFFF7F8FA, 0xFFFFFFFF, 0xFF000000),
    BLACK("Чёрная", 0xFF141414, 0xFF171717, 0xFFFFFFFF),
    TRANSPARENT_WHITE("Прозрачная Белая", 0x80F7F8FA, 0xFFFFFFFF, 0xFF000000),
    TRANSPARENT_BLACK("Прозрачная Чёрная", 0x80141414, 0xFF171717, 0xFFFFFFFF),
    PINK("Розовая", 0xFFFFCCE2, 0x80FFD9E9, 0xFF000000),
    PURPLE("Фиолетовая", 0xFFB2A4FF, 0x80C6BCFF, 0xFF000000),
    NEON("Неоновая", 0xFF2A0040, 0xFF0D0018, 0xFFD090FF);

    private final String fallbackName;
    private final int main;
    private final int bg;
    private final int text;

    ClassicTheme(String fallbackName, int main, int bg, int text) {
        this.fallbackName = fallbackName;
        this.main = main;
        this.bg = bg;
        this.text = text;
    }

    public String getName() {
        return GuiLocalization.themeName(this, fallbackName);
    }

    public int getMain() {
        return main;
    }

    public int getBg() {
        return bg;
    }

    public int getText() {
        return text;
    }
}
