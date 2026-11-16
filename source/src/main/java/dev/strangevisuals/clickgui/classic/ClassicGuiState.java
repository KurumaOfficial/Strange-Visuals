package dev.strangevisuals.clickgui.classic;

import dev.strangevisuals.clickgui.ModuleInfo;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.clickgui.classic.util.ScrollUtil;
import dev.strangevisuals.clickgui.setting.*;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClassicGuiState {
    public static final float MODULE_PANEL_X_OFFSET = 7.0F;
    public static final float MODULE_PANEL_Y_OFFSET = 48.0F;
    public static final float MODULE_PANEL_WIDTH = 195.0F;
    public static final float MODULE_HEADER_HEIGHT = 22.0F;
    public static final float MODULE_VERTICAL_SPACING = 26.0F;

    public static float x;
    public static float y;
    public static float width = 225.0F;
    public static float height = 255.0F;
    public static float savedX = -1.0F;
    public static float savedY = -1.0F;

    public static ClassicCategory selectedCategory = ClassicCategory.WORLD;
    public static String searchQuery = "";
    public static boolean searchActive = false;
    public static final ScrollUtil scroll = new ScrollUtil();

    private ClassicGuiState() {
    }

    public static void updatePositions(int screenWidth, int screenHeight) {
        width = 225.0F;
        height = 255.0F;
        if (savedX >= 0.0F && savedY >= 0.0F) {
            x = Math.max(0.0F, Math.min(screenWidth - width, savedX));
            y = Math.max(0.0F, Math.min(screenHeight - height, savedY));
        } else {
            x = (screenWidth - width) / 2.0F;
            y = (screenHeight - height) / 2.0F;
        }
    }

    public static boolean isHovered(double mouseX, double mouseY, float targetX, float targetY, float targetWidth, float targetHeight) {
        return mouseX >= targetX && mouseY >= targetY && mouseX < targetX + targetWidth && mouseY < targetY + targetHeight;
    }

    public static ModulePanel modulePanelBounds() {
        return new ModulePanel(
            x + MODULE_PANEL_X_OFFSET,
            y + MODULE_PANEL_Y_OFFSET,
            MODULE_PANEL_WIDTH,
            height - MODULE_PANEL_Y_OFFSET - 8.0F
        );
    }

    public static float listStartY() {
        return modulePanelBounds().y() + 6.0F;
    }

    public static float listHeight() {
        return modulePanelBounds().height() - 14.0F;
    }

    public static List<ModuleInfo> visibleModules() {
        List<ModuleInfo> visible = new ArrayList<>();
        boolean hasQuery = !searchQuery.isBlank();

        for (ModuleInfo module : ModuleRegistry.all()) {
            if (hasQuery) {
                if (matchesSearch(module)) {
                    visible.add(module);
                }
            } else {
                if (module.classicCategory() == selectedCategory) {
                    visible.add(module);
                }
            }
        }
        return visible;
    }

    public static boolean matchesSearch(ModuleInfo module) {
        String query = normalizeSearch(searchQuery);
        if (query.isEmpty()) {
            return true;
        }
        return containsSearch(module.name(), query)
            || containsSearch(module.description(), query)
            || containsSearch(module.classicCategory().getName(), query);
    }

    private static boolean containsSearch(String value, String query) {
        return normalizeSearch(value).contains(query);
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static float calcUP(ModuleInfo module) {
        if (module == null || !module.hasSettings() || !module.expanded()) {
            return 0.0F;
        }
        float leftHeight = measureSettingsHeight(module.leftSettings());
        float rightHeight = measureSettingsHeight(module.rightSettings());
        return Math.max(leftHeight, rightHeight);
    }

    public static float measureSettingsHeight(List<Setting> settings) {
        if (settings == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (Setting setting : settings) {
            total += measureSetting(setting);
        }
        return total;
    }

    public static float measureSetting(Setting setting) {
        if (setting instanceof BooleanSetting) {
            return 20.0F;
        }
        if (setting instanceof SliderSetting) {
            return 20.0F;
        }
        if (setting instanceof ModeSetting mode) {
            float itemH = 11.0F;
            return 20.0F + (mode.open() ? (mode.options().size() * itemH + 4.0F) : 0.0F);
        }
        if (setting instanceof ColorSetting color) {
            return 20.0F + (color.open() ? 80.0F : 0.0F);
        }
        if (setting instanceof KeybindSetting) {
            return 20.0F;
        }
        return 20.0F;
    }

    public static void resetTransientState() {
        searchQuery = "";
        searchActive = false;
        for (ModuleInfo m : ModuleRegistry.all()) {
            m.cancelBinding();
            if (m.hasSettings()) {
                for (Setting s : m.leftSettings()) {
                    closeSetting(s);
                }
                for (Setting s : m.rightSettings()) {
                    closeSetting(s);
                }
            }
        }
    }

    private static void closeSetting(Setting s) {
        if (s instanceof ModeSetting mode) {
            mode.close();
        }
        if (s instanceof ColorSetting color) {
            if (color.open()) {
                color.toggleOpen();
            }
        }
    }

    public record ModulePanel(float x, float y, float width, float height) {
    }
}
