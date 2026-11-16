package dev.strangevisuals.clickgui.classic.localization;

import dev.strangevisuals.clickgui.Category;
import dev.strangevisuals.clickgui.classic.ClassicCategory;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.i18n.I18n;

/**
 * Localization bridge for Classic GUI, unified with the mod-wide I18n engine.
 */
public final class GuiLocalization {
    private GuiLocalization() {
    }

    public static void initialize() {
    }

    public static GuiLanguage currentLanguage() {
        return I18n.getEffectiveLanguage().startsWith("ru") ? GuiLanguage.RU : GuiLanguage.EN;
    }

    public static void setLanguage(GuiLanguage language) {
        if (language == null) {
            I18n.setLanguageOverride("auto");
        } else {
            I18n.setLanguageOverride(language == GuiLanguage.RU ? "ru_ru" : "en_us");
        }
    }

    public static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static String categoryName(Category category, String fallback) {
        if (category == null) return fallback;
        String key = switch (category) {
            case COMBAT -> "gui.category.combat";
            case MOVEMENT -> "gui.category.movement";
            case VISUAL -> "gui.category.visual";
            case PLAYER -> "gui.category.player";
            case MISC -> "gui.category.misc";
            case CONFIG -> "gui.category.config";
        };
        return tr(key);
    }

    public static String categoryName(ClassicCategory category, String fallback) {
        if (category == null) return fallback;
        String key = switch (category) {
            case PLAYER -> "gui.category.player";
            case WORLD -> "gui.category.world";
            case UTILITIES -> "gui.category.utilities";
            case OTHER -> "gui.category.other";
            case INTERFACE -> "gui.category.interface";
            case THEME -> "gui.category.theme";
        };
        return tr(key);
    }

    public static String themeName(ClassicTheme theme, String fallback) {
        if (theme == null) return fallback;
        return switch (theme) {
            case WHITE -> tr("gui.theme.white");
            case BLACK -> tr("gui.theme.black");
            case TRANSPARENT_WHITE -> tr("gui.theme.transparent_white");
            case TRANSPARENT_BLACK -> tr("gui.theme.transparent_black");
            case PINK -> tr("gui.theme.pink");
            case PURPLE -> tr("gui.theme.purple");
            case NEON -> tr("gui.theme.neon");
        };
    }

    public static String moduleName(String name) {
        if (name == null) return "";
        if ("Фильтры".equalsIgnoreCase(name) || "Filters".equalsIgnoreCase(name)) {
            return I18n.get("module.filters");
        }
        return name;
    }

    public static String moduleDescription(String name, String fallback) {
        if ("Фильтры".equalsIgnoreCase(name) || "Filters".equalsIgnoreCase(name)) {
            return I18n.get("module.filters.desc");
        }
        return fallback;
    }
}
