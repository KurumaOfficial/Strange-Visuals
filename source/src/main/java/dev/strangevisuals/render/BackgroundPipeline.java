package dev.strangevisuals.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import dev.strangevisuals.ui.editor.MainMenuConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;

public final class BackgroundPipeline {
    private static final int UNIFORM_SIZE = 48;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/menu_background"))
            .withVertexShader(StrangeVisuals.id("core/menu_background"))
            .withFragmentShader(StrangeVisuals.id("core/menu_background"))
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withUniform("MenuBackgroundData", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(UNIFORM_SIZE).order(ByteOrder.nativeOrder());
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static final long startedAt = System.nanoTime();

    private BackgroundPipeline() {
    }

    public static void draw(float opened) {
        MainMenuConfig config = MainMenuConfig.get();
        int shaderMode = 0;
        float intensity = 1.0f;
        float darkening = 0.0f;
        float opacity = 1.0f;
        if (config != null) {
            if (config.shaderPreset != null) {
                shaderMode = config.shaderPreset.id;
            }
            intensity = config.shaderIntensity;
            darkening = config.imageDarkening;
            if (config.backgroundMode == MainMenuConfig.BackgroundMode.IMAGE_AND_SHADER) {
                opacity = config.shaderOpacity;
            }
        }
        draw(opened, shaderMode, intensity, darkening, opacity);
    }

    public static void draw(float opened, int shaderMode, float intensity, float darkening, float opacity) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer target = client.getFramebuffer();
        if (target == null || target.getColorAttachmentView() == null) {
            return;
        }

        ensureBuffers();

        float seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0f;
        UNIFORM_DATA.clear();
        UNIFORM_DATA.putFloat((float) target.textureWidth).putFloat((float) target.textureHeight).putFloat(0.0f).putFloat(darkening);
        UNIFORM_DATA.putFloat(seconds).putFloat(opened).putFloat((float) shaderMode).putFloat(intensity);
        int accent = dev.strangevisuals.ui.Theme.accent();
        UNIFORM_DATA.putFloat((accent >> 16 & 0xFF) / 255.0f).putFloat((accent >> 8 & 0xFF) / 255.0f)
            .putFloat((accent & 0xFF) / 255.0f).putFloat(opacity);
        UNIFORM_DATA.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), UNIFORM_DATA);

        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:menu_background",
            target.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.setUniform("MenuBackgroundData", uniformBuffer);
            pass.draw(0, 3);
        }
    }

    private static void ensureBuffers() {
        if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:menu_background_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_SIZE);
        }
        if (dummyVertexBuffer == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:menu_background_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummy);
            MemoryUtil.memFree(dummy);
        }
    }
}
