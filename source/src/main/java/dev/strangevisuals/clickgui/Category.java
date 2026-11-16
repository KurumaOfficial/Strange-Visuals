package dev.strangevisuals.clickgui;

import dev.strangevisuals.StrangeVisuals;
import net.minecraft.util.Identifier;

public enum Category {
    COMBAT("Combat", "ico_spark"),
    MOVEMENT("Movement", "ico_bolt"),
    VISUAL("Visual", "ico_orbit"),
    PLAYER("Player", "ico_paint"),
    MISC("Misc", "ico_trash"),
    CONFIG("Config", "ico_save");

    private final String label;
    private final Identifier icon;

    Category(String label, String iconName) {
        this.label = label;
        this.icon = StrangeVisuals.id("textures/gui/" + iconName + ".png");
    }

    public String label() {
        return label;
    }

    public Identifier icon() {
        return icon;
    }
}
