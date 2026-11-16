package dev.strangevisuals.hud;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PotionLayoutMode {
    public enum Mode {
        SEPARATE,
        BLOCK
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_potion_layout.txt");

    private static Mode current;

    private PotionLayoutMode() {
    }

    public static Mode get() {
        if (current == null) {
            current = load();
        }
        return current;
    }

    public static void set(Mode mode) {
        current = mode;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, mode.name(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save potion layout: {}", e.getMessage());
        }
    }

    private static Mode load() {
        try {
            if (Files.isRegularFile(FILE)) {
                return Mode.valueOf(Files.readString(FILE, StandardCharsets.UTF_8).trim());
            }
        } catch (IOException | IllegalArgumentException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to load potion layout mode, defaulting to SEPARATE: {}", e.getMessage());
        }
        return Mode.SEPARATE;
    }
}
