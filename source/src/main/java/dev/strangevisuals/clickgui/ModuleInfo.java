package dev.strangevisuals.clickgui;

import dev.strangevisuals.clickgui.setting.Setting;
import dev.strangevisuals.hud.notify.NotificationManager;
import dev.strangevisuals.ui.Spring;

import java.util.List;
import java.util.function.Consumer;

public final class ModuleInfo {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> leftSettings;
    private final List<Setting> rightSettings;
    private final Consumer<Boolean> onToggle;
    private dev.strangevisuals.clickgui.classic.ClassicCategory classicCategory;

    private boolean enabled;
    private boolean expanded;
    private String keybind = "NONE";
    private int keyCode = -1;
    private boolean binding;
    private boolean keyDown;

    private long lastToggle = Long.MIN_VALUE;

    public final Spring enableGlow = new Spring(0.0, 900.0, 46.0);
    public final Spring expandProgress = new Spring(0.0, 780.0, 44.0);
    public final Spring hover = new Spring(0.0, 900.0, 50.0);

    public ModuleInfo(String name, String description, Category category, List<Setting> leftSettings, List<Setting> rightSettings) {
        this(name, description, category, leftSettings, rightSettings, null);
    }

    public ModuleInfo(String name, String description, Category category, List<Setting> leftSettings, List<Setting> rightSettings, Consumer<Boolean> onToggle) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.leftSettings = leftSettings;
        this.rightSettings = rightSettings;
        this.onToggle = onToggle;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public dev.strangevisuals.clickgui.classic.ClassicCategory classicCategory() {
        if (classicCategory != null) {
            return classicCategory;
        }
        return switch (category) {
            case COMBAT, PLAYER -> dev.strangevisuals.clickgui.classic.ClassicCategory.PLAYER;
            case MOVEMENT -> dev.strangevisuals.clickgui.classic.ClassicCategory.UTILITIES;
            case VISUAL -> dev.strangevisuals.clickgui.classic.ClassicCategory.WORLD;
            case MISC, CONFIG -> dev.strangevisuals.clickgui.classic.ClassicCategory.OTHER;
        };
    }

    public ModuleInfo withClassicCategory(dev.strangevisuals.clickgui.classic.ClassicCategory classicCategory) {
        this.classicCategory = classicCategory;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public void toggleSilently(boolean value) {
        enabled = value;
    }

    public void toggle() {
        enabled = !enabled;
        lastToggle = System.currentTimeMillis();
        NotificationManager.push(name, enabled);
        if (onToggle != null) {
            onToggle.accept(enabled);
        }
    }

    public long lastToggle() {
        return lastToggle;
    }

    public boolean expanded() {
        return expanded;
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }

    public String keybind() {
        return keybind;
    }

    public int keyCode() {
        return keyCode;
    }

    public boolean hasKeybind() {
        return keyCode >= 0;
    }

    public boolean binding() {
        return binding;
    }

    public void armBinding() {
        binding = true;
    }

    public void startBinding() {
        armBinding();
    }

    public void cancelBinding() {
        binding = false;
    }

    public void resolveBinding(int code) {
        this.keyCode = code;
        this.keybind = KeyNames.of(code);
        this.binding = false;
    }

    public void clearBinding() {
        this.keyCode = -1;
        this.keybind = "NONE";
        this.binding = false;
    }

    public boolean keyDown() {
        return keyDown;
    }

    public void setKeyDown(boolean keyDown) {
        this.keyDown = keyDown;
    }

    public boolean pollKey(boolean isDown) {
        if (isDown) {
            if (!this.keyDown) {
                this.keyDown = true;
                return true;
            }
        } else {
            this.keyDown = false;
        }
        return false;
    }

    public List<Setting> leftSettings() {
        return leftSettings;
    }

    public List<Setting> rightSettings() {
        return rightSettings;
    }

    public boolean hasSettings() {
        return !leftSettings.isEmpty() || !rightSettings.isEmpty();
    }
}
