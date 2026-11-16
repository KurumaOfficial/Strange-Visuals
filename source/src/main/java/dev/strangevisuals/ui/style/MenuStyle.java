package dev.strangevisuals.ui.style;

/**
 * Available UI layout and visual styles across StrangeVisuals custom screens:
 * Main Menu, Server List, Settings, and Loading/Connecting screens.
 */
public enum MenuStyle {
    BETA("Beta", "Flagship aesthetic: asymmetric side navigation, breathing brand mark, and cyber glow."),
    VANILLA("Vanilla", "Authentic enhanced Minecraft aesthetic with iconic pixel-art icons and classic textures.");

    private final String displayName;
    private final String description;

    MenuStyle(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static MenuStyle fromName(String name) {
        if (name == null || name.isBlank()) {
            return BETA;
        }
        for (MenuStyle style : values()) {
            if (style.name().equalsIgnoreCase(name) || style.displayName.equalsIgnoreCase(name)) {
                return style;
            }
        }
        return BETA;
    }
}
