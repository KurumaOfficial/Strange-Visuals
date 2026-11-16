package dev.strangevisuals.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;

/**
 * Post-processing Ambience pipeline.
 * <p>
 * Implements bright-pass extraction, downsampled bloom pyramids (1/2, 1/4, 1/8 resolution),
 * and HDR composite tone mapping with exposure, contrast, saturation, and vignette parameters.
 */
public final class AmbiencePipeline {
    private static final int UNIFORM_SIZE = 64;

    private static final RenderPipeline BRIGHT = build("bright_pass", false);
    private static final RenderPipeline COMPOSITE = build("ambience_composite", true);

    private static SimpleFramebuffer sceneCopy;
    private static SimpleFramebuffer bloomHalf;
    private static SimpleFramebuffer bloomQuarter;
    private static SimpleFramebuffer bloomEighth;

    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;

    private AmbiencePipeline() {
    }

    private static RenderPipeline build(String shader, boolean twoSamplers) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/" + shader))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/" + shader))
            .withSampler("Sampler0")
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false);
        if (twoSamplers) {
            builder.withSampler("Sampler1");
        }
        return RenderPipelines.register(builder.build());
    }

    public static void run(float exposure, float contrast, float saturation, float vignette,
                           int tintRgb, float tintStrength, float bloomThreshold, float bloomStrength) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null || main.getColorAttachmentView() == null) {
            return;
        }

        int width = Math.max(1, main.textureWidth);
        int height = Math.max(1, main.textureHeight);
        sceneCopy = fit(sceneCopy, "strangevisuals_ambience_scene", width, height);
        bloomHalf = fit(bloomHalf, "strangevisuals_ambience_bloom_half", width / 2, height / 2);
        bloomQuarter = fit(bloomQuarter, "strangevisuals_ambience_bloom_quarter", width / 4, height / 4);
        bloomEighth = fit(bloomEighth, "strangevisuals_ambience_bloom_eighth", width / 8, height / 8);

        ensureBuffers();
        writeUniforms(exposure, contrast, saturation, vignette, tintRgb, tintStrength,
            bloomThreshold, bloomStrength, width, height);

        BlitPipeline.run(main.getColorAttachmentView(), sceneCopy.getColorAttachmentView());

        if (bloomStrength > 0.001f) {
            pass(BRIGHT, sceneCopy.getColorAttachmentView(), null, bloomHalf.getColorAttachmentView());
            DownsamplePipeline.run(bloomHalf.getColorAttachmentView(), bloomQuarter.getColorAttachmentView());
            DownsamplePipeline.run(bloomQuarter.getColorAttachmentView(), bloomEighth.getColorAttachmentView());
        } else {
            pass(BRIGHT, sceneCopy.getColorAttachmentView(), null, bloomEighth.getColorAttachmentView());
        }

        pass(COMPOSITE, sceneCopy.getColorAttachmentView(), bloomEighth.getColorAttachmentView(),
            main.getColorAttachmentView());
    }

    private static void pass(RenderPipeline pipeline, GpuTextureView source, GpuTextureView second, GpuTextureView target) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
            () -> "strangevisuals:ambience", target, OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            renderPass.bindTexture("Sampler0", source, sampler);
            if (second != null) {
                renderPass.bindTexture("Sampler1", second, sampler);
            }
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.setUniform("Uniforms", uniformBuffer);
            renderPass.draw(0, 3);
        }
    }

    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(UNIFORM_SIZE).order(ByteOrder.nativeOrder());

    private static void writeUniforms(float exposure, float contrast, float saturation, float vignette,
                                      int tintRgb, float tintStrength, float bloomThreshold,
                                      float bloomStrength, int width, int height) {
        UNIFORM_DATA.clear();
        UNIFORM_DATA.putFloat(exposure).putFloat(contrast).putFloat(saturation).putFloat(vignette);
        UNIFORM_DATA.putFloat((tintRgb >> 16 & 0xFF) / 255.0f)
            .putFloat((tintRgb >> 8 & 0xFF) / 255.0f)
            .putFloat((tintRgb & 0xFF) / 255.0f)
            .putFloat(tintStrength);
        UNIFORM_DATA.putFloat(bloomThreshold).putFloat(bloomStrength).putFloat(0.0f).putFloat(0.0f);
        UNIFORM_DATA.putFloat(width).putFloat(height).putFloat(0.0f).putFloat(0.0f);
        UNIFORM_DATA.flip();

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(), UNIFORM_DATA);
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

    private static void ensureBuffers() {
        if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:ambience_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE);
        }
        if (dummyVertexBuffer == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:ambience_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
            MemoryUtil.memFree(dummy);
        }
    }

    /**
     * Frees fullscreen VRAM (scene + 3 bloom mips). Must run on the render thread.
     * Called on world disconnect and when the Ambience module is disabled, so
     * 1440p/4K buffers don't hang around forever.
     */
    public static void shutdown() {
        deleteQuietly(sceneCopy);
        sceneCopy = null;
        deleteQuietly(bloomHalf);
        bloomHalf = null;
        deleteQuietly(bloomQuarter);
        bloomQuarter = null;
        deleteQuietly(bloomEighth);
        bloomEighth = null;
        // Uniform/dummy buffers are tiny (bytes); keep them to avoid re-register cost.
    }

    private static void deleteQuietly(SimpleFramebuffer fb) {
        if (fb == null) {
            return;
        }
        try {
            fb.delete();
        } catch (Throwable t) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Framebuffer release failed: {}", t.getMessage());
        }
    }
}
