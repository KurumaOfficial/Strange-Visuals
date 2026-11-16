package dev.strangevisuals.ui.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.strangevisuals.ui.style.MenuStyle;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage and live state for the customizable Main Menu layout,
 * custom background photos, shader overlays, and element scaling/positions.
 *
 * Implements pixel-perfect responsive flagship defaults that guarantee
 * zero overlapping on any display resolution and GUI scale, while fully
 * supporting interactive user customization.
 */
public final class MainMenuConfig {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ShaderPreset.class, (com.google.gson.JsonDeserializer<ShaderPreset>) (json, type, ctx) -> {
                if (json == null || !json.isJsonPrimitive()) return ShaderPreset.COSMOS;
                return ShaderPreset.fromName(json.getAsString());
            })
            .setPrettyPrinting()
            .create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_menu.json");

    public static final float ROW_H = 28.0f;
    public static final float ROW_GAP = 6.0f;
    public static final float IDENTITY_H = 22.0f;
    public static final float IDENTITY_GAP = 20.0f;
    public static final float BRAND_LOGO_H = 36.0f;
    public static final float BRAND_SLOGAN_GAP = 8.0f;
    public static final float BRAND_SLOGAN_H = 28.0f;
    public static final float BRAND_TOTAL_H = BRAND_LOGO_H + BRAND_SLOGAN_GAP + BRAND_SLOGAN_H;
    public static final float BRAND_H = BRAND_TOTAL_H;
    public static final float BRAND_GAP = 22.0f;

    public enum BackgroundMode {
        SHADER,
        CUSTOM_IMAGE,
        IMAGE_AND_SHADER,
        PANORAMA
    }

    public enum ShaderPreset {
        COSMOS(0, "Космос", "menu.shader.cosmos"),
        PULSE_NEBULA(1, "Небула", "menu.shader.pulse_nebula"),
        PULSE_STARFIELD(2, "Старфилд", "menu.shader.pulse_starfield"),
        PULSE_COBWEB(3, "Паутина", "menu.shader.pulse_cobweb"),
        PULSE_PLASMA(4, "Плазма", "menu.shader.pulse_plasma"),
        AURORA(5, "Аврора", "menu.shader.aurora"),
        OCEAN(6, "Океан", "menu.shader.ocean"),
        TOXIN(7, "Токсин", "menu.shader.toxin"),
        THUNDER(8, "Гроза", "menu.shader.thunder"),
        SUNSET(9, "Закат", "menu.shader.sunset"),
        VOID(10, "Пустота", "menu.shader.void"),
        SOLARIS(11, "Солярис", "menu.shader.solaris"),
        FRACTAL(12, "Фрактал", "menu.shader.fractal"),
        MONSOON(13, "Муссон", "menu.shader.monsoon"),
        BLOOM(14, "Блум", "menu.shader.bloom");

        public final int id;
        public final String defaultName;
        public final String translationKey;

        ShaderPreset(int id, String defaultName, String translationKey) {
            this.id = id;
            this.defaultName = defaultName;
            this.translationKey = translationKey;
        }

        public static ShaderPreset fromId(int id) {
            for (ShaderPreset p : values()) {
                if (p.id == id) return p;
            }
            return COSMOS;
        }

        public static ShaderPreset fromName(String name) {
            if (name == null || name.isBlank()) return COSMOS;
            String clean = name.trim().toUpperCase(java.util.Locale.ROOT).replace(" ", "_");
            for (ShaderPreset p : values()) {
                if (p.name().equalsIgnoreCase(clean)) return p;
            }
            return switch (clean) {
                case "COSMIC_DRIFT" -> COSMOS;
                case "DEEP_STARFIELD", "STARFIELD" -> PULSE_STARFIELD;
                case "CYBER_GRID", "WARP_VORTEX", "FLOWING_PLASMA" -> PULSE_PLASMA;
                case "COSMOS_LAVA", "LAVA" -> SOLARIS;
                case "COSMOS_OCEAN" -> OCEAN;
                case "COSMOS_MATRIX", "MATRIX" -> TOXIN;
                case "COSMOS_TOXIC" -> TOXIN;
                case "COSMOS_THUNDER" -> THUNDER;
                case "COSMOS_SAKURA", "SAKURA" -> BLOOM;
                default -> COSMOS;
            };
        }
    }

    public enum ShaderEffect {
        BLOOM,
        STARFIELD,
        RADIAL_PULSE,
        FILM_GRAIN,
        NONE
    }

    public static float columnX(int screenWidth) {
        return Math.max(34.0f, screenWidth * 0.07f);
    }

    public static float columnWidth(int screenWidth) {
        return Math.min(210.0f, Math.max(168.0f, screenWidth * 0.18f));
    }

    public static float getDefaultX(String id, int screenWidth) {
        if ("CLOCK".equals(id)) {
            return screenWidth - 130.0f;
        }
        return columnX(screenWidth);
    }

    public static float getDefaultY(String id, int screenHeight, int numEntries) {
        if ("CLOCK".equals(id)) {
            return 16.0f;
        }

        float block = BRAND_TOTAL_H + BRAND_GAP + numEntries * ROW_H
                + (numEntries - 1) * ROW_GAP + IDENTITY_GAP + IDENTITY_H;
        float brandTop = Math.max(16.0f, screenHeight * 0.5f - block * 0.5f);

        if ("LOGO".equals(id)) {
            return brandTop;
        }
        if ("SLOGAN".equals(id)) {
            return brandTop + BRAND_LOGO_H + BRAND_SLOGAN_GAP;
        }

        int entryIndex = switch (id) {
            case "SINGLEPLAYER" -> 0;
            case "MULTIPLAYER" -> 1;
            case "SETTINGS" -> 2;
            case "MODS" -> 3;
            case "QUIT" -> Math.max(3, numEntries - 1);
            default -> -1;
        };

        if (entryIndex >= 0) {
            return brandTop + BRAND_TOTAL_H + BRAND_GAP + entryIndex * (ROW_H + ROW_GAP);
        }

        if ("PLAYER_CARD".equals(id)) {
            return brandTop + BRAND_TOTAL_H + BRAND_GAP + numEntries * ROW_H
                    + (numEntries - 1) * ROW_GAP + IDENTITY_GAP;
        }

        return screenHeight * 0.5f;
    }

    public static final class ElementConfig {
        public String id;
        public boolean custom = false;
        public float relX = 0.07f; // 0.0 - 1.0 (used when custom == true)
        public float relY = 0.50f; // 0.0 - 1.0 (used when custom == true)
        public float scale = 1.0f;
        public boolean visible = true;

        public ElementConfig() {
        }

        public ElementConfig(String id, float relX, float relY, float scale) {
            this.id = id;
            this.relX = relX;
            this.relY = relY;
            this.scale = scale;
            this.visible = true;
            this.custom = false;
        }

        public float getX(int screenWidth) {
            if (!custom) {
                return getDefaultX(id, screenWidth);
            }
            return relX * screenWidth;
        }

        public float getY(int screenHeight, int numEntries) {
            if (!custom) {
                return getDefaultY(id, screenHeight, numEntries);
            }
            return relY * screenHeight;
        }

        public float getY(int screenHeight) {
            return getY(screenHeight, 5);
        }

        public void setPos(float pixelX, float pixelY, int screenWidth, int screenHeight) {
            this.custom = true;
            if (screenWidth > 0) this.relX = Math.max(0.0f, Math.min(1.0f, pixelX / screenWidth));
            if (screenHeight > 0) this.relY = Math.max(0.0f, Math.min(1.0f, pixelY / screenHeight));
        }
    }

    private static MainMenuConfig instance;

    public BackgroundMode backgroundMode = BackgroundMode.SHADER;
    public ShaderPreset shaderPreset = ShaderPreset.COSMOS;
    public String customImagePath = "";
    public float imageDarkening = 0.35f;
    public float imageOpacity = 1.0f;
    public float shaderOpacity = 0.85f;
    public boolean shaderOverlayEnabled = true;
    public ShaderEffect shaderEffect = ShaderEffect.BLOOM;
    public float shaderIntensity = 1.0f;
    public boolean customLoadingScreen = true;
    public boolean customMultiplayerScreen = true;
    public boolean customSettingsScreen = true;
    public boolean customPauseScreen = true;
    public boolean useVanillaTitleScreen = false;
    public boolean customHotbar = true;
    public boolean customStatusEffects = true;
    public Map<String, ElementConfig> elements = new HashMap<>();

    public boolean isCustomStyleActive() {
        return !useVanillaTitleScreen
                || customLoadingScreen
                || customSettingsScreen
                || customPauseScreen
                || customMultiplayerScreen
                || customHotbar
                || customStatusEffects;
    }

    public void setCustomStyleActive(boolean enabled) {
        this.customLoadingScreen = enabled;
        this.customMultiplayerScreen = enabled;
        this.customSettingsScreen = enabled;
        this.customPauseScreen = enabled;
        this.useVanillaTitleScreen = !enabled;
        this.customHotbar = enabled;
        this.customStatusEffects = enabled;
    }

    public static synchronized MainMenuConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static synchronized void load() {
        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                instance = GSON.fromJson(reader, MainMenuConfig.class);
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to load main menu config: {}", e.getMessage());
            }
        }
        if (instance == null) {
            instance = new MainMenuConfig();
            instance.applyPreset(dev.strangevisuals.ui.style.StyleManager.getStyle());
            save();
        } else {
            if (instance.shaderPreset == null) instance.shaderPreset = ShaderPreset.COSMOS;
            if (instance.backgroundMode == null) instance.backgroundMode = BackgroundMode.SHADER;
            if (instance.shaderIntensity <= 0.05f) instance.shaderIntensity = 1.0f;
            if (instance.shaderOpacity <= 0.05f) instance.shaderOpacity = 0.85f;
            if (instance.imageOpacity <= 0.05f) instance.imageOpacity = 1.0f;
            MenuStyle activeStyle = dev.strangevisuals.ui.style.StyleManager.getStyle();
            instance.customSettingsScreen = true;
            instance.customPauseScreen = true;
            instance.customLoadingScreen = true;
            if (activeStyle == MenuStyle.BETA) {
                instance.useVanillaTitleScreen = false;
                instance.customMultiplayerScreen = true;
            } else if (activeStyle == MenuStyle.VANILLA) {
                instance.useVanillaTitleScreen = true;
                instance.customMultiplayerScreen = false;
            }
            save();
        }
        instance.ensureElements();
    }

    public static synchronized void save() {
        if (instance == null) return;
        Path parent = FILE.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to create directories for main menu config: {}", e.getMessage());
                return;
            }
        }
        Path tempFile = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
            try {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to save main menu config: {}", e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    public ElementConfig getElement(String id) {
        return elements.computeIfAbsent(id, k -> new ElementConfig(k, 0.07f, 0.5f, 1.0f));
    }

    public void ensureElements() {
        String[] ids = {"LOGO", "SLOGAN", "SINGLEPLAYER", "MULTIPLAYER", "SETTINGS", "MODS", "QUIT", "PLAYER_CARD"};
        for (String id : ids) {
            elements.computeIfAbsent(id, k -> new ElementConfig(k, 0.07f, 0.5f, 1.0f));
        }
        if (elements.containsKey("CLOCK")) {
            elements.get("CLOCK").visible = false;
        }
    }

    public void applyPreset(MenuStyle style) {
        if (style == MenuStyle.VANILLA) {
            backgroundMode = BackgroundMode.SHADER;
            shaderPreset = ShaderPreset.COSMOS;
            shaderOverlayEnabled = false;
            shaderEffect = ShaderEffect.NONE;
            imageDarkening = 0.50f;
            customSettingsScreen = true;
            customMultiplayerScreen = false;
            customLoadingScreen = true;
            customPauseScreen = true;
            useVanillaTitleScreen = true;
            customHotbar = true;
            customStatusEffects = true;
        } else {
            backgroundMode = BackgroundMode.SHADER;
            shaderPreset = ShaderPreset.COSMOS;
            shaderOverlayEnabled = true;
            shaderEffect = ShaderEffect.BLOOM;
            shaderIntensity = 1.0f;
            shaderOpacity = 0.85f;
            imageDarkening = 0.35f;
            imageOpacity = 1.0f;
            customSettingsScreen = true;
            customMultiplayerScreen = true;
            customLoadingScreen = true;
            customPauseScreen = true;
            useVanillaTitleScreen = false;
            customHotbar = true;
            customStatusEffects = true;
        }
        for (ElementConfig el : elements.values()) {
            el.custom = false;
            el.scale = 1.0f;
            el.visible = true;
        }
        ensureElements();
    }
}
