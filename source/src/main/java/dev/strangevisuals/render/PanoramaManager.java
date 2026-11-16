package dev.strangevisuals.render;

import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.CubemapTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public class PanoramaManager {
    public enum PanoramaType {
        MAIN_MENU("title/background/panorama"),
        SINGLEPLAYER("panoramas/singleplayer/panorama"),
        MULTIPLAYER("panoramas/multiplayer/panorama"),
        SETTINGS("panoramas/settings/panorama"),
        LOADING("panoramas/loading/panorama"),
        AMPLIFIED("panoramas/amplified/panorama"),
        SINKHOLE("panoramas/sinkhole/panorama");

        private final String relativePath;

        PanoramaType(String relativePath) {
            this.relativePath = relativePath;
        }

        public Identifier getId() {
            if (this == MAIN_MENU) {
                return Identifier.ofVanilla("textures/gui/title/background/panorama");
            }
            return StrangeVisuals.id("textures/gui/" + relativePath);
        }
    }

    private static final PanoramaType[] PANORAMA_POOL = {
        PanoramaType.MAIN_MENU,
        PanoramaType.AMPLIFIED,
        PanoramaType.SINKHOLE,
        PanoramaType.SINGLEPLAYER,
        PanoramaType.MULTIPLAYER,
        PanoramaType.SETTINGS,
        PanoramaType.LOADING
    };

    private static final Map<PanoramaType, RotatingCubeMapRenderer> RENDERERS = new EnumMap<>(PanoramaType.class);
    private static final Map<PanoramaType, Boolean> INITIALIZED = new EnumMap<>(PanoramaType.class);
    private static final java.util.Random RNG = new java.util.Random();

    public static final long CYCLE_INTERVAL_MS = 300_000L; // 5 minutes (300 seconds)
    public static final long TRANSITION_DURATION_MS = 6_000L; // 6 seconds total
    public static final long FADE_OUT_DURATION_MS = 3_000L; // 3 seconds fade out to black

    // Start with Sakura panorama (PanoramaType.MAIN_MENU at index 0) upon launch
    private static int currentIndex = 0;
    private static int previousIndex = 0;
    private static long lastCycleTime = System.currentTimeMillis();
    private static long transitionStartTime = -1L;

    /**
     * Background continuous tick called on every client tick.
     * Operates regardless of whether the player is in-game, in main menu, or between tabs.
     */
    public static synchronized void tick() {
        long now = System.currentTimeMillis();
        if (now - lastCycleTime >= CYCLE_INTERVAL_MS) {
            triggerTransition();
        }
    }

    /**
     * Immediately triggers a smooth 3s fade-out + 3s fade-in transition to the next panorama.
     */
    public static synchronized void triggerTransition() {
        long now = System.currentTimeMillis();
        previousIndex = getCurrentDisplayTypeIndex(now);
        currentIndex = (currentIndex + 1) % PANORAMA_POOL.length;
        transitionStartTime = now;
        lastCycleTime = now;
    }

    public static synchronized int getCurrentDisplayTypeIndex(long now) {
        if (transitionStartTime > 0 && now - transitionStartTime < TRANSITION_DURATION_MS) {
            long elapsed = now - transitionStartTime;
            if (elapsed < FADE_OUT_DURATION_MS) {
                return previousIndex;
            }
        }
        return currentIndex;
    }

    public static synchronized PanoramaType getCurrentDisplayType() {
        return PANORAMA_POOL[getCurrentDisplayTypeIndex(System.currentTimeMillis())];
    }

    public static synchronized RotatingCubeMapRenderer getRenderer(PanoramaType type) {
        if (RENDERERS.containsKey(type)) {
            return RENDERERS.get(type);
        }

        if (INITIALIZED.getOrDefault(type, false)) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null || client.getResourceManager() == null) {
            return null;
        }

        try {
            Identifier id = type.getId();
            TextureManager tm = client.getTextureManager();
            CubemapTexture cubemapTexture = new CubemapTexture(id);
            try (TextureContents contents = cubemapTexture.loadContents(client.getResourceManager())) {
                cubemapTexture.reload(contents);
            }
            tm.registerTexture(id, cubemapTexture);

            CubeMapRenderer cubeMap = new CubeMapRenderer(id);
            RotatingCubeMapRenderer rotating = new RotatingCubeMapRenderer(cubeMap);
            RENDERERS.put(type, rotating);
            INITIALIZED.put(type, true);
            return rotating;
        } catch (Throwable t) {
            StrangeVisuals.LOGGER.warn("Failed to initialize panorama for {}: {}", type, t.getMessage());
            return null;
        }
    }

    public static RotatingCubeMapRenderer getRendererFor(Screen screen) {
        return getRenderer(getCurrentDisplayType());
    }

    public static PanoramaType getProceduralMainMenuType() {
        return getCurrentDisplayType();
    }

    public static PanoramaType getProceduralLoadingType() {
        return getCurrentDisplayType();
    }

    public static PanoramaType getProceduralSingleplayerType() {
        return getCurrentDisplayType();
    }

    public static PanoramaType getTypeFor(Screen screen) {
        return getCurrentDisplayType();
    }

    public static void render(DrawContext context, int width, int height, float delta) {
        long now = System.currentTimeMillis();
        PanoramaType displayType = PANORAMA_POOL[getCurrentDisplayTypeIndex(now)];
        RotatingCubeMapRenderer renderer = getRenderer(displayType);
        if (renderer != null) {
            try {
                renderer.render(context, width, height, true);
            } catch (Throwable t) {
                renderFallback(context, width, height);
            }
        } else {
            renderFallback(context, width, height);
        }
        renderTransitionOverlay(context, width, height, now);
    }

    public static void render(DrawContext context, PanoramaType type, int width, int height, float delta) {
        render(context, width, height, delta);
    }

    public static void render(DrawContext context, Screen screen, int width, int height, float delta) {
        render(context, width, height, delta);
    }

    private static void renderTransitionOverlay(DrawContext context, int width, int height, long now) {
        if (transitionStartTime <= 0) {
            return;
        }
        long elapsed = now - transitionStartTime;
        if (elapsed < 0 || elapsed >= TRANSITION_DURATION_MS) {
            return;
        }

        float fadeAlpha;
        if (elapsed < FADE_OUT_DURATION_MS) {
            // 0.0s -> 3.0s: Smooth fade-out to pure black
            float progress = (float) elapsed / (float) FADE_OUT_DURATION_MS;
            fadeAlpha = smoothStep(progress);
        } else {
            // 3.0s -> 6.0s: Smooth fade-in from pure black
            float progress = (float) (elapsed - FADE_OUT_DURATION_MS) / (float) (TRANSITION_DURATION_MS - FADE_OUT_DURATION_MS);
            fadeAlpha = 1.0f - smoothStep(progress);
        }

        int alphaInt = Math.max(0, Math.min(255, Math.round(fadeAlpha * 255.0f)));
        if (alphaInt > 0) {
            context.fill(0, 0, width, height, (alphaInt << 24));
        }
    }

    private static float smoothStep(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }

    public static void renderFallback(DrawContext context, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.gameRenderer != null) {
            RotatingCubeMapRenderer nativeR = client.gameRenderer.getRotatingPanoramaRenderer();
            if (nativeR != null) {
                try {
                    nativeR.render(context, width, height, true);
                    return;
                } catch (Throwable t) {
                    StrangeVisuals.LOGGER.debug("Native panorama fallback render failed: {}", t.getMessage());
                }
            }
        }
        context.fillGradient(0, 0, width, height, 0xFF141720, 0xFF0E1017);
    }

    public static synchronized void reset() {
        RENDERERS.clear();
        INITIALIZED.clear();
        currentIndex = 0;
        previousIndex = 0;
        transitionStartTime = -1L;
        lastCycleTime = System.currentTimeMillis();
    }
}
