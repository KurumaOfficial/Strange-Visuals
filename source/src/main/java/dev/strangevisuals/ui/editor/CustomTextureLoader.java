package dev.strangevisuals.ui.editor;

import dev.strangevisuals.StrangeVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronously and safely loads custom background photos (PNG/JPG)
 * from user files or the mod folder into Minecraft's texture pipeline.
 */
public final class CustomTextureLoader {
    private static final Identifier CUSTOM_BG_ID = StrangeVisuals.id("textures/gui/custom_menu_bg.png");
    private static NativeImageBackedTexture currentTexture = null;
    private static String lastLoadedPath = null;
    private static long lastModifiedTime = 0;
    private static boolean loaded = false;
    private static long lastCheckTime = 0;
    private static Path cachedPath = null;
    private static String cachedConfigPath = null;

    // Render-thread stall fix: disk stat + PNG decode moved off the render thread.
    // getCustomTexture() is called every frame from menu render — the old code ran
    // Files.getLastModifiedTime() + NativeImage.read() inline, causing micro-freezes.
    // Now the render thread only touches volatiles; a daemon loader decodes in
    // background and the render thread performs just the GPU upload (registerTexture).
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "StrangeVisuals-CustomBG");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean LOAD_IN_FLIGHT = new AtomicBoolean(false);
    private static volatile NativeImage pendingImage;
    private static volatile String pendingPathStr;
    private static volatile long pendingModified;

    private CustomTextureLoader() {
    }

    public static List<String> getAvailableImages() {
        List<String> list = new ArrayList<>();
        Path dir = getDefaultCustomImageDirectory();
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp")) {
                        list.add(p.getFileName().toString());
                    }
                });
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to list custom images: {}", e.getMessage());
            }
        }
        return list;
    }

    public static void forceReload() {
        loaded = false;
        lastLoadedPath = null;
        lastCheckTime = 0;
        cachedPath = null;
        cachedConfigPath = null;
        NativeImage stale = pendingImage;
        pendingImage = null;
        if (stale != null) {
            try {
                stale.close();
            } catch (Exception ignored) {
            }
        }
        if (currentTexture != null) {
            try {
                currentTexture.close();
            } catch (Exception ignored) {
            }
            currentTexture = null;
        }
    }

    /**
     * Render-thread entry point. Performs <b>zero</b> disk I/O or image decoding:
     * only volatile reads and (at most once per image) a GPU texture upload.
     * Heavy work happens in {@link #loadInBackground(String)}.
     */
    public static Identifier getCustomTexture() {
        MainMenuConfig config = MainMenuConfig.get();
        if (config.backgroundMode == MainMenuConfig.BackgroundMode.SHADER) {
            return null;
        }

        // 1. Pick up a background-decoded image (GPU upload stays on render thread).
        NativeImage ready = pendingImage;
        if (ready != null) {
            pendingImage = null;
            String pathStr = pendingPathStr;
            long modified = pendingModified;
            try {
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "custom_background", ready);
                ready = null; // ownership moved to texture
                if (currentTexture != null) {
                    try {
                        currentTexture.close();
                    } catch (Exception ignored) {
                    }
                }
                currentTexture = texture;
                MinecraftClient.getInstance().getTextureManager().registerTexture(CUSTOM_BG_ID, texture);
                lastLoadedPath = pathStr;
                lastModifiedTime = modified;
                loaded = true;
                cachedConfigPath = config.customImagePath == null ? "" : config.customImagePath;
            } catch (Exception e) {
                try {
                    ready.close();
                } catch (Exception ignored) {
                }
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Custom background upload failed: {}", e.getMessage());
                return loaded && currentTexture != null ? CUSTOM_BG_ID : null;
            }
        }

        long now = System.currentTimeMillis();
        String currentConfig = config.customImagePath == null ? "" : config.customImagePath;

        // Fast path: texture ready, config unchanged, throttle window not expired.
        if (loaded && currentTexture != null && currentConfig.equals(cachedConfigPath)
            && (now - lastCheckTime < 5000L)) {
            return CUSTOM_BG_ID;
        }
        lastCheckTime = now;

        // Config changed or never loaded or file may have changed on disk:
        // schedule a background stat+decode, return the stale texture meanwhile (no hitch).
        if (LOAD_IN_FLIGHT.compareAndSet(false, true)) {
            final String snapshot = currentConfig;
            LOADER.execute(() -> loadInBackground(snapshot));
        }
        return loaded && currentTexture != null ? CUSTOM_BG_ID : null;
    }

    /**
     * Background worker (NOT render thread): resolves the path, stats mtime and
     * decodes the PNG/JPG off-thread. Result is handed to the render thread
     * via {@link #pendingImage} for GPU upload.
     */
    private static void loadInBackground(String configPath) {
        try {
            Path imagePath = resolveImagePath(configPath);
            cachedConfigPath = configPath;
            cachedPath = imagePath;
            if (imagePath == null || !Files.isRegularFile(imagePath)) {
                return;
            }
            long currentModified;
            try {
                currentModified = Files.getLastModifiedTime(imagePath).toMillis();
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to get modified time for '{}': {}", imagePath, e.getMessage());
                return;
            }
            String currentPathStr = imagePath.toAbsolutePath().toString();
            if (loaded && currentPathStr.equals(lastLoadedPath) && currentModified == lastModifiedTime) {
                return; // unchanged since last upload
            }
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(imagePath);
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Custom background read failed: {}", e.getMessage());
                return;
            }
            NativeImage image;
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                image = NativeImage.read(in);
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Custom background decode failed: {}", e.getMessage());
                return;
            }
            long pixels = (long) image.getWidth() * image.getHeight();
            if (pixels > 4096L * 2048L) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Custom background is {}x{} ({} MP), "
                    + "consider downscaling to <=2048px to save VRAM.",
                    image.getWidth(), image.getHeight(), String.format(Locale.ROOT, "%.1f", pixels / 1_000_000.0));
            }
            NativeImage prev = pendingImage;
            pendingPathStr = currentPathStr;
            pendingModified = currentModified;
            pendingImage = image;
            if (prev != null) {
                try {
                    prev.close();
                } catch (Exception ignored) {
                }
            }
        } finally {
            LOAD_IN_FLIGHT.set(false);
        }
    }

    public static Path resolveImagePath(String customPath) {
        if (customPath != null && !customPath.isBlank()) {
            Path p = Path.of(customPath);
            if (Files.isRegularFile(p)) {
                return p;
            }
            // Check inside .minecraft/strangevisuals/
            Path inDir = getDefaultCustomImageDirectory().resolve(customPath);
            if (Files.isRegularFile(inDir)) {
                return inDir;
            }
        }
        // First check any available file in strangevisuals/
        Path dir = getDefaultCustomImageDirectory();
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                var first = stream.filter(Files::isRegularFile).filter(p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
                }).findFirst();
                if (first.isPresent()) {
                    return first.get();
                }
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to scan custom image directory: {}", e.getMessage());
            }
        }
        // Fallback: strangevisuals/background.png
        Path fallback = dir.resolve("background.png");
        if (Files.isRegularFile(fallback)) {
            return fallback;
        }
        // Also check config/strangevisuals_background.png
        Path configFallback = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_background.png");
        if (Files.isRegularFile(configFallback)) {
            return configFallback;
        }
        return null;
    }

    public static Path getDefaultCustomImageDirectory() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("strangevisuals");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            StrangeVisuals.LOGGER.warn("[StrangeVisuals] Failed to create custom image directory: {}", e.getMessage());
        }
        return dir;
    }
}
