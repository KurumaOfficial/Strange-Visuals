package dev.strangevisuals.clickgui.classic;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;
import net.minecraft.util.Identifier;

public enum ClassicCategory {
    PLAYER("На игроке", "player"),
    WORLD("В мире", "world"),
    UTILITIES("Утилиты", "utilities"),
    OTHER("Остальное", "other"),
    INTERFACE("Интерфейс", "interface"),
    THEME("Темы", "theme");

    private final String name;
    private final Identifier icon;

    ClassicCategory(String name, String iconName) {
        this.name = name;
        this.icon = StrangeVisuals.id("textures/gui/" + iconName + ".png");
    }

    public String getName() {
        return GuiLocalization.categoryName(this, name);
    }

    public String rawName() {
        return name;
    }

    public Identifier icon() {
        return icon;
    }
}
