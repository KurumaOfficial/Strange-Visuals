package dev.strangevisuals.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class HudLayoutStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_hud.json");
    private static final Type MAP_TYPE = new TypeToken<Map<String, float[]>>() {}.getType();

    private HudLayoutStorage() {
    }

    public static Map<String, float[]> load() {
        if (!Files.isRegularFile(FILE)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Map<String, float[]> map = GSON.fromJson(reader, MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException | RuntimeException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to load HUD layout, using defaults: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public static void save(Map<String, float[]> positions) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(positions, MAP_TYPE, writer);
            }
        } catch (IOException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save HUD layout: {}", e.getMessage());
        }
    }
}
