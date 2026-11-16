package dev.strangevisuals.render;

import dev.strangevisuals.clickgui.ModuleRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.AbstractWorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class ScreenFilterRenderer {
    private static ScreenFilterRenderer instance;

    private final MinecraftClient client;
    private final ScreenFilterPipeline pipeline = new ScreenFilterPipeline();
    private SimpleFramebuffer sceneCopy;

    private float brightness = 0.0f;
    private float contrast = 1.0f;
    private float saturation = 1.2f;
    private float exposure = 1.0f;
    private float gamma = 1.0f;
    private float vignette = 0.0f;
    private float grain = 0.0f;
    private int tintColor = 0xFFFFFFFF;
    private float tintIntensity = 0.0f;
    private final long startTime = System.nanoTime();

    private ScreenFilterRenderer() {
        this.client = MinecraftClient.getInstance();
    }

    public static ScreenFilterRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenFilterRenderer();
        }
        return instance;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public void setVignette(float vignette) {
        this.vignette = vignette;
    }

    public void setGrain(float grain) {
        this.grain = grain;
    }

    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
    }

    public void setTintIntensity(float tintIntensity) {
        this.tintIntensity = tintIntensity;
    }

    public static void renderWorld(AbstractWorldRenderContext context) {
        if (ModuleRegistry.filters() == null || !ModuleRegistry.filters().enabled()) {
            // Module off: drop the fullscreen copy instead of holding it forever.
            if (instance != null) {
                instance.close();
            }
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }

        ScreenFilterRenderer renderer = getInstance();
        renderer.setSaturation(ModuleRegistry.filterSaturation().value());
        renderer.setBrightness(ModuleRegistry.filterBrightness().value());
        renderer.setContrast(ModuleRegistry.filterContrast().value());
        renderer.setVignette(ModuleRegistry.filterVignette().value());
        renderer.setExposure(ModuleRegistry.filterExposure().value());
        renderer.setGamma(ModuleRegistry.filterGamma().value());
        renderer.setGrain(ModuleRegistry.filterGrain().value());
        renderer.setTintColor(ModuleRegistry.filterTintColor().rgb());
        renderer.setTintIntensity(ModuleRegistry.filterUseTint().value() ? ModuleRegistry.filterTintStrength().value() : 0.0f);
        renderer.render();
    }

    public void render() {
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }

        int width = Math.max(1, framebuffer.textureWidth);
        int height = Math.max(1, framebuffer.textureHeight);
        sceneCopy = fit(sceneCopy, "strangevisuals_screen_filter_scene", width, height);

        BlitPipeline.run(framebuffer.getColorAttachmentView(), sceneCopy.getColorAttachmentView());

        float time = (System.nanoTime() - startTime) / 1_000_000_000.0f;
        pipeline.composite(
                framebuffer.getColorAttachmentView(),
                sceneCopy.getColorAttachmentView(),
                width,
                height,
                brightness,
                contrast,
                saturation,
                exposure,
                gamma,
                vignette,
                grain,
                tintColor,
                tintIntensity,
                time
        );
    }

    private static SimpleFramebuffer fit(SimpleFramebuffer buffer, String name, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (buffer == null) {
            return new SimpleFramebuffer(name, w, h, false);
        }
        if (buffer.textureWidth != w || buffer.textureHeight != h) {
            buffer.resize(w, h);
        }
        return buffer;
    }

    public void close() {
        if (sceneCopy != null) {
            try {
                sceneCopy.delete();
            } catch (Throwable t) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Screen-filter buffer release failed: {}", t.getMessage());
            }
            sceneCopy = null;
        }
        pipeline.close();
    }

    /** Releases all instances (world disconnect). Safe on any thread — GL work is guarded. */
    public static void shutdown() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Throwable t) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Screen-filter shutdown failed: {}", t.getMessage());
            }
        }
    }
}
