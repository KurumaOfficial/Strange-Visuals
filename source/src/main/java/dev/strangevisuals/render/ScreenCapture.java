package dev.strangevisuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class ScreenCapture {
    private static final Identifier TEXTURE_ID = StrangeVisuals.id("screen_capture");

    private static SimpleFramebuffer buffer;
    private static LiveTexture liveTexture;

    private ScreenCapture() {
    }

    public static boolean grab() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null || main.getColorAttachmentView() == null) {
            return false;
        }

        int width = Math.max(1, main.textureWidth);
        int height = Math.max(1, main.textureHeight);
        if (buffer == null) {
            buffer = new SimpleFramebuffer("strangevisuals_screen_capture", width, height, false);
        } else if (buffer.textureWidth != width || buffer.textureHeight != height) {
            buffer.resize(width, height);
        }

        BlitPipeline.run(main.getColorAttachmentView(), buffer.getColorAttachmentView());

        if (liveTexture == null) {
            liveTexture = new LiveTexture();
            client.getTextureManager().registerTexture(TEXTURE_ID, liveTexture);
        }
        liveTexture.update(buffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
        return true;
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    /** Frees the capture buffer (world disconnect / menu close). Idempotent. */
    public static void shutdown() {
        if (buffer != null) {
            try {
                buffer.delete();
            } catch (Throwable t) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Screen-capture release failed: {}", t.getMessage());
            }
            buffer = null;
        }
    }

    private static final class LiveTexture extends AbstractTexture {
        void update(GpuTextureView view, GpuSampler sampler) {
            this.glTextureView = view;
            this.sampler = sampler;
        }

        @Override
        public void close() {
        }
    }
}
