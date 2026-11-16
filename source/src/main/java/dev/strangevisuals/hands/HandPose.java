package dev.strangevisuals.hands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Arm;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HandPose {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
        FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_hands.json");

    private static final float MIN_SCALE = 0.2f;
    private static final float MAX_SCALE = 3.0f;

    private static final float REACH = 0.3f;
    private static final float NEAR = -0.3f;
    private static final float FAR = 0.5f;

    private static Stored stored = new Stored();

    private HandPose() {
    }

    public static float x(Arm arm) {
        return arm == Arm.RIGHT ? stored.rightX : stored.leftX;
    }

    public static float y(Arm arm) {
        return arm == Arm.RIGHT ? stored.rightY : stored.leftY;
    }

    public static float z(Arm arm) {
        return arm == Arm.RIGHT ? stored.rightZ : stored.leftZ;
    }

    public static float scale(Arm arm) {
        return arm == Arm.RIGHT ? stored.rightScale : stored.leftScale;
    }

    public static boolean moved(Arm arm) {
        return Math.abs(x(arm)) > 0.0001f || Math.abs(y(arm)) > 0.0001f
            || Math.abs(z(arm)) > 0.0001f || Math.abs(scale(arm) - 1.0f) > 0.0001f;
    }

    public static void move(Arm arm, float dx, float dy, float dz) {
        if (arm == Arm.RIGHT) {
            stored.rightX = clampReach(stored.rightX + dx);
            stored.rightY = clampReach(stored.rightY + dy);
            stored.rightZ = clampDepth(stored.rightZ + dz);
        } else {
            stored.leftX = clampReach(stored.leftX + dx);
            stored.leftY = clampReach(stored.leftY + dy);
            stored.leftZ = clampDepth(stored.leftZ + dz);
        }
    }

    public static void resize(Arm arm, float amount) {
        if (arm == Arm.RIGHT) {
            stored.rightScale = clampScale(stored.rightScale * amount);
        } else {
            stored.leftScale = clampScale(stored.leftScale * amount);
        }
    }

    public static void reset(Arm arm) {
        if (arm == Arm.RIGHT) {
            stored.rightX = 0.0f;
            stored.rightY = 0.0f;
            stored.rightZ = 0.0f;
            stored.rightScale = 1.0f;
        } else {
            stored.leftX = 0.0f;
            stored.leftY = 0.0f;
            stored.leftZ = 0.0f;
            stored.leftScale = 1.0f;
        }
    }

    private static float clampReach(float value) {
        return Math.max(-REACH, Math.min(REACH, value));
    }

    private static float clampDepth(float value) {
        return Math.max(NEAR, Math.min(FAR, value));
    }

    private static float clampScale(float value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    public static void load() {
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Stored read = GSON.fromJson(reader, Stored.class);
            if (read != null) {
                stored = read;

                stored.rightX = clampReach(stored.rightX);
                stored.rightY = clampReach(stored.rightY);
                stored.rightZ = clampDepth(stored.rightZ);
                stored.rightScale = clampScale(stored.rightScale);
                stored.leftX = clampReach(stored.leftX);
                stored.leftY = clampReach(stored.leftY);
                stored.leftZ = clampDepth(stored.leftZ);
                stored.leftScale = clampScale(stored.leftScale);
            }
        } catch (IOException | RuntimeException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to load hand pose, using defaults: {}", e.getMessage());
        }
    }

    public static void mirror() {
        stored.leftX = -stored.rightX;
        stored.leftY = stored.rightY;
        stored.leftZ = stored.rightZ;
        stored.leftScale = stored.rightScale;
    }

    public static void save() {
        Path parent = FILE.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to create directories for hand pose: {}", e.getMessage());
                return;
            }
        }
        Path tempFile = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(stored, writer);
            }
            try {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save hand pose: {}", e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    private static final class Stored {
        float rightX;
        float rightY;
        float rightZ;
        float rightScale = 1.0f;
        float leftX;
        float leftY;
        float leftZ;
        float leftScale = 1.0f;
    }
}
