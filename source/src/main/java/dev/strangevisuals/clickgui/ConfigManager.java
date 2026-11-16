package dev.strangevisuals.clickgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.strangevisuals.clickgui.setting.BooleanSetting;
import dev.strangevisuals.clickgui.setting.ColorSetting;
import dev.strangevisuals.clickgui.setting.ModeSetting;
import dev.strangevisuals.clickgui.setting.Setting;
import dev.strangevisuals.clickgui.setting.SliderSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages serialization and deserialization of module states, keybindings,
 * and individual settings across multiple configuration profiles in
 * {@code config/strangevisuals/configs/<name>.json}.
 * <p>
 * Supports creating, loading, overwriting, and deleting profiles,
 * as well as opening the configs directory.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_config.json");
    private static final Path CONFIGS_DIR = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals").resolve("configs");

    private static String activeProfile = "default";

    private ConfigManager() {
    }

    public static synchronized String getActiveProfile() {
        return activeProfile;
    }

    public static synchronized List<String> getProfiles() {
        List<String> list = new ArrayList<>();
        try {
            if (Files.isDirectory(CONFIGS_DIR)) {
                try (var stream = Files.list(CONFIGS_DIR)) {
                    stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                        .map(p -> {
                            String fn = p.getFileName().toString();
                            return fn.substring(0, fn.length() - 5);
                        })
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(list::add);
                }
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to list config profiles: {}", e.getMessage());
        }
        if (list.isEmpty()) {
            list.add("default");
        }
        return list;
    }

    public static synchronized void reloadProfiles() {
        ModuleRegistry.syncConfigProfiles();
    }

    public static void openFolder() {
        try {
            if (!Files.exists(CONFIGS_DIR)) {
                Files.createDirectories(CONFIGS_DIR);
            }
            Util.getOperatingSystem().open(CONFIGS_DIR.toFile());
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to open configs folder: {}", e.getMessage());
        }
    }

    public static synchronized boolean createProfile(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            return false;
        }
        return saveProfile(clean);
    }

    public static synchronized boolean deleteProfile(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            return false;
        }
        List<String> profiles = getProfiles();
        if (profiles.size() <= 1) {
            return false;
        }
        Path file = profilePath(clean);
        if (file == null) {
            return false;
        }
        try {
            Files.deleteIfExists(file);
            if (activeProfile.equalsIgnoreCase(clean)) {
                List<String> remaining = getProfiles();
                String fallback = remaining.isEmpty() ? "default" : remaining.get(0);
                loadProfile(fallback);
            } else {
                ModuleRegistry.syncConfigProfiles();
            }
            return true;
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to delete profile {}: {}", clean, e.getMessage());
            return false;
        }
    }

    public static synchronized boolean saveProfile(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            clean = "default";
        }
        try {
            if (!Files.exists(CONFIGS_DIR)) {
                Files.createDirectories(CONFIGS_DIR);
            }
            JsonObject root = new JsonObject();
            root.addProperty("name", clean);
            root.add("modules", buildModulesJson());

            Path file = profilePath(clean);
            if (file == null) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("Rejected unsafe profile name: {}", clean);
                return false;
            }
            try (Writer writer = newAtomicWriter(file)) {
                GSON.toJson(root, writer);
            }
            activeProfile = clean;
            saveGlobal();
            ModuleRegistry.syncConfigProfiles();
            return true;
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save profile {}: {}", clean, e.getMessage());
            return false;
        }
    }

    public static synchronized boolean loadProfile(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            return false;
        }
        Path file = profilePath(clean);
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement elem = JsonParser.parseReader(reader);
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                JsonObject modules = obj.has("modules") && obj.get("modules").isJsonObject()
                    ? obj.getAsJsonObject("modules") : obj;
                applyModulesJson(modules);
                activeProfile = clean;
                saveGlobal();
                ModuleRegistry.syncConfigProfiles();
                return true;
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to load profile {}: {}", clean, e.getMessage());
        }
        return false;
    }

    public static synchronized void load() {
        try {
            if (!Files.exists(CONFIGS_DIR)) {
                Files.createDirectories(CONFIGS_DIR);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to create configs directory: {}", e.getMessage());
        }

        JsonObject root = null;
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (rootElement.isJsonObject()) {
                    root = rootElement.getAsJsonObject();
                }
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to read main config: {}", e.getMessage());
            }
        }

        if (root != null) {
            if (root.has("activeProfile") && root.get("activeProfile").isJsonPrimitive()) {
                activeProfile = sanitizeName(root.get("activeProfile").getAsString());
                if (activeProfile.isEmpty()) {
                    activeProfile = "default";
                }
            }
            if (root.has("menuStyle") && root.get("menuStyle").isJsonPrimitive()) {
                dev.strangevisuals.ui.style.StyleManager.setStyleSilently(
                    dev.strangevisuals.ui.style.MenuStyle.fromName(root.get("menuStyle").getAsString()));
            }
            if (root.has("language") && root.get("language").isJsonPrimitive()) {
                dev.strangevisuals.i18n.I18n.setLanguageOverride(root.get("language").getAsString());
            }
            if (root.has("classicGuiX") && root.get("classicGuiX").isJsonPrimitive()) {
                dev.strangevisuals.clickgui.classic.ClassicGuiState.savedX = root.get("classicGuiX").getAsFloat();
            }
            if (root.has("classicGuiY") && root.get("classicGuiY").isJsonPrimitive()) {
                dev.strangevisuals.clickgui.classic.ClassicGuiState.savedY = root.get("classicGuiY").getAsFloat();
            }
        }

        List<String> existingProfiles = getProfiles();
        Path activeProfilePath = profilePath(activeProfile);
        if (activeProfilePath == null) {
            activeProfile = "default";
            activeProfilePath = profilePath(activeProfile);
        }

        // Migrate legacy config if configs folder was empty
        if (activeProfilePath == null || !Files.exists(activeProfilePath)) {
            if (root != null && root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject defObj = new JsonObject();
                defObj.addProperty("name", "default");
                defObj.add("modules", root.getAsJsonObject("modules"));
                Path defPath = profilePath("default");
                if (defPath != null) {
                    try (Writer writer = newAtomicWriter(defPath)) {
                        GSON.toJson(defObj, writer);
                    } catch (Exception e) {
                        dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to migrate legacy config: {}", e.getMessage());
                    }
                }
                activeProfile = "default";
                activeProfilePath = profilePath("default");
            } else if (!existingProfiles.isEmpty()) {
                Path first = profilePath(existingProfiles.get(0));
                if (first != null && Files.exists(first)) {
                    activeProfile = existingProfiles.get(0);
                    activeProfilePath = first;
                } else {
                    activeProfile = "default";
                    saveProfile("default");
                    activeProfilePath = profilePath("default");
                }
            } else {
                activeProfile = "default";
                saveProfile("default");
                activeProfilePath = profilePath("default");
            }
        }

        if (activeProfilePath != null && Files.isRegularFile(activeProfilePath)) {
            try (Reader reader = Files.newBufferedReader(activeProfilePath, StandardCharsets.UTF_8)) {
                JsonElement profElem = JsonParser.parseReader(reader);
                if (profElem.isJsonObject()) {
                    JsonObject profObj = profElem.getAsJsonObject();
                    JsonObject modulesObj = profObj.has("modules") && profObj.get("modules").isJsonObject()
                        ? profObj.getAsJsonObject("modules") : profObj;
                    applyModulesJson(modulesObj);
                }
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to load profile {}: {}", activeProfile, e.getMessage());
            }
        }

        ModuleRegistry.syncConfigProfiles();
    }

    public static synchronized void save() {
        saveProfile(activeProfile);
    }

    private static void saveGlobal() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("activeProfile", activeProfile);
            root.addProperty("menuStyle", dev.strangevisuals.ui.style.StyleManager.getStyle().name());
            root.addProperty("language", dev.strangevisuals.i18n.I18n.getLanguageOverride());
            if (dev.strangevisuals.clickgui.classic.ClassicGuiState.savedX >= 0.0F) {
                root.addProperty("classicGuiX", dev.strangevisuals.clickgui.classic.ClassicGuiState.savedX);
            }
            if (dev.strangevisuals.clickgui.classic.ClassicGuiState.savedY >= 0.0F) {
                root.addProperty("classicGuiY", dev.strangevisuals.clickgui.classic.ClassicGuiState.savedY);
            }

            if (CONFIG_FILE.getParent() != null) {
                Files.createDirectories(CONFIG_FILE.getParent());
            }
            try (Writer writer = newAtomicWriter(CONFIG_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save global configuration: {}", e.getMessage());
        }
    }

    private static JsonObject buildModulesJson() {
        JsonObject modulesObj = new JsonObject();
        for (ModuleInfo module : ModuleRegistry.all()) {
            JsonObject modJson = new JsonObject();
            modJson.addProperty("enabled", module.enabled());
            modJson.addProperty("keyCode", module.keyCode());
            modJson.addProperty("keybind", module.keybind());

            JsonObject settingsObj = new JsonObject();
            saveSettings(module.leftSettings(), settingsObj);
            saveSettings(module.rightSettings(), settingsObj);
            modJson.add("settings", settingsObj);

            modulesObj.add(module.name(), modJson);
        }
        return modulesObj;
    }

    private static void applyModulesJson(JsonObject modulesObj) {
        if (modulesObj == null) {
            return;
        }
        for (ModuleInfo module : ModuleRegistry.all()) {
            JsonObject modJson = null;
            if (modulesObj.has(module.name())) {
                modJson = modulesObj.getAsJsonObject(module.name());
            } else if ("Filters".equalsIgnoreCase(module.name())) {
                if (modulesObj.has("Фильтры")) {
                    modJson = modulesObj.getAsJsonObject("Фильтры");
                }
            }
            if (modJson == null) {
                continue;
            }
            if (modJson.has("enabled")) {
                module.toggleSilently(modJson.get("enabled").getAsBoolean());
            }
            if (modJson.has("keyCode")) {
                int code = modJson.get("keyCode").getAsInt();
                if (code >= 0) {
                    module.resolveBinding(code);
                } else {
                    module.clearBinding();
                }
            }

            if (modJson.has("settings") && modJson.get("settings").isJsonObject()) {
                JsonObject settingsObj = modJson.getAsJsonObject("settings");
                loadSettings(module.leftSettings(), settingsObj);
                loadSettings(module.rightSettings(), settingsObj);
            }
        }
    }

    private static void loadSettings(Iterable<Setting> settings, JsonObject settingsObj) {
        for (Setting setting : settings) {
            if (!settingsObj.has(setting.name())) {
                continue;
            }
            JsonElement val = settingsObj.get(setting.name());
            try {
                if (setting instanceof BooleanSetting bool) {
                    bool.setValue(val.getAsBoolean());
                } else if (setting instanceof SliderSetting slider) {
                    slider.setValue(val.getAsFloat());
                } else if (setting instanceof ModeSetting mode) {
                    if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) {
                        mode.select(val.getAsInt());
                    } else if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                        String opt = val.getAsString();
                        int idx = mode.options().indexOf(opt);
                        if (idx >= 0) {
                            mode.select(idx);
                        }
                    }
                } else if (setting instanceof ColorSetting color) {
                    color.set(val.getAsInt());
                }
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to apply setting '{}': {}", setting.name(), e.getMessage());
            }
        }
    }

    private static void saveSettings(Iterable<Setting> settings, JsonObject settingsObj) {
        for (Setting setting : settings) {
            if (setting instanceof BooleanSetting bool) {
                settingsObj.addProperty(setting.name(), bool.value());
            } else if (setting instanceof SliderSetting slider) {
                settingsObj.addProperty(setting.name(), slider.value());
            } else if (setting instanceof ModeSetting mode) {
                settingsObj.addProperty(setting.name(), mode.current());
            } else if (setting instanceof ColorSetting color) {
                settingsObj.addProperty(setting.name(), color.rgb());
            }
        }
    }

    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        // CWE-22: strip illegal FS chars AND dot-segments. Names like "..", "../x" or
        // "a..b" tricks must never escape CONFIGS_DIR. We whitelist [letters/digits/space/_/-]
        // plus Cyrillic range, collapse the rest to "_", then reject "." / ".." explicitly.
        String clean = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        clean = clean.replaceAll("[\\p{Cntrl}.]", "_").trim();
        // Remove leading dots/underscores left from ".." sequences ("..foo" -> "foo")
        clean = clean.replaceAll("^[._\\s]+", "");
        if (clean.equals(".") || clean.equals("..")) {
            return "";
        }
        if (clean.length() > 64) {
            clean = clean.substring(0, 64).trim();
        }
        // Final guard: no separators or parent refs survived
        if (clean.isEmpty() || clean.contains("..") || clean.contains("/") || clean.contains("\\")) {
            return "";
        }
        return clean;
    }

    /**
     * Resolves a profile name to a path strictly inside {@link #CONFIGS_DIR}.
     * Returns {@code null} if the resolved path would escape the configs dir (CWE-22).
     */
    private static Path profilePath(String cleanName) {
        if (cleanName == null || cleanName.isEmpty()) {
            return null;
        }
        Path base = CONFIGS_DIR.toAbsolutePath().normalize();
        Path resolved = base.resolve(cleanName + ".json").normalize();
        if (!resolved.startsWith(base)) {
            return null;
        }
        return resolved;
    }

    /**
     * Returns a Writer that commits atomically: JSON is first written to
     * {@code target.json.tmp} in the same directory, then moved over the target with
     * ATOMIC_MOVE (fallback to plain replace when the FS does not support it).
     * This prevents 0-byte corrupt configs on game crash / power loss.
     */
    private static Writer newAtomicWriter(Path target) throws java.io.IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        java.io.BufferedWriter buf =
            Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return new java.io.FilterWriter(buf) {
            private boolean committed;

            @Override
            public void close() throws java.io.IOException {
                if (committed) {
                    return;
                }
                committed = true;
                super.close();
                try {
                    Files.move(tmp, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException fallback) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        };
    }
}
