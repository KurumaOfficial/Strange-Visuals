package dev.strangevisuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class HudGlassCapture {
    public static final int DOWNSCALE = 8;

    private static final Identifier TEXTURE_ID = StrangeVisuals.id("hud_glass_capture");

    private static final Identifier SHARP_ID = StrangeVisuals.id("hud_glass_sharp");

    private static SimpleFramebuffer half;
    private static SimpleFramebuffer quarter;
    private static SimpleFramebuffer eighth;
    private static LiveTexture liveTexture;
    private static LiveTexture sharpTexture;

    private HudGlassCapture() {
    }

    public static void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null || main.getColorAttachmentView() == null) {
            return;
        }

        half = fit(half, "strangevisuals_hud_glass_half", main.textureWidth / 2, main.textureHeight / 2);
        quarter = fit(quarter, "strangevisuals_hud_glass_quarter", main.textureWidth / 4, main.textureHeight / 4);
        eighth = fit(eighth, "strangevisuals_hud_glass", main.textureWidth / DOWNSCALE, main.textureHeight / DOWNSCALE);

        DownsamplePipeline.run(main.getColorAttachmentView(), half.getColorAttachmentView());
        DownsamplePipeline.run(half.getColorAttachmentView(), quarter.getColorAttachmentView());
        DownsamplePipeline.run(quarter.getColorAttachmentView(), eighth.getColorAttachmentView());

        if (liveTexture == null) {
            liveTexture = new LiveTexture();
            client.getTextureManager().registerTexture(TEXTURE_ID, liveTexture);
        }
        liveTexture.update(eighth.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));

        if (sharpTexture == null) {
            sharpTexture = new LiveTexture();
            client.getTextureManager().registerTexture(SHARP_ID, sharpTexture);
        }
        sharpTexture.update(quarter.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
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

    public static Identifier sharpTextureId() {
        return SHARP_ID;
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    /** Frees the 1/2 + 1/4 + 1/8 downscale chain. Idempotent, render-thread only. */
    public static void shutdown() {
        boolean released = false;
        for (SimpleFramebuffer fb : new SimpleFramebuffer[]{half, quarter, eighth}) {
            if (fb != null) {
                try {
                    fb.delete();
                    released = true;
                } catch (Throwable t) {
                    dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] HUD-glass release failed: {}", t.getMessage());
                }
            }
        }
        half = null;
        quarter = null;
        eighth = null;
        if (released) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("[StrangeVisuals] HUD-glass capture buffers released");
        }
    }

    private static final class LiveTexture extends AbstractTexture {
        void update(com.mojang.blaze3d.textures.GpuTextureView view, net.minecraft.client.gl.GpuSampler sampler) {
            this.glTextureView = view;
            this.sampler = sampler;
        }

        @Override
        public void close() {
        }
    }
}
