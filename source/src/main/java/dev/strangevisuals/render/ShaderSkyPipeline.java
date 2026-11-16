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

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Custom procedural skybox rendering pipeline.
 * <p>
 * Renders an internal 36-vertex inverted sky cube with uniforms controlling
 * celestial time, intensity, star frequency, stylistic presets, and gradient color accents.
 */
public final class ShaderSkyPipeline {
    private static final int UNIFORM_SIZE = 64;

    private static final float EXTENT = 100.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/shader_sky"))
            .withVertexShader(StrangeVisuals.id("core/shader_sky"))
            .withFragmentShader(StrangeVisuals.id("core/shader_sky"))
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static GpuBuffer uniformBuffer;
    private static GpuBuffer cubeBuffer;
    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(UNIFORM_SIZE).order(java.nio.ByteOrder.nativeOrder());

    private ShaderSkyPipeline() {
    }

    public static void draw(float time, float intensity, float starAmount, int style, float scale,
                            int accentA, int accentB) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null || main.getColorAttachmentView() == null) {
            return;
        }
        ensureBuffers();

        UNIFORM_DATA.clear();
        UNIFORM_DATA.putFloat(0.0f).putFloat(0.0f).putFloat(time).putFloat(0.0f);
        UNIFORM_DATA.putFloat(intensity).putFloat(starAmount).putFloat(style).putFloat(scale);
        putColor(UNIFORM_DATA, accentA);
        putColor(UNIFORM_DATA, accentB);
        UNIFORM_DATA.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), UNIFORM_DATA);

        try (RenderPass pass = encoder.createRenderPass(
            () -> "strangevisuals:shader_sky",
            main.getColorAttachmentView(), OptionalInt.empty(),
            main.getDepthAttachmentView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);

            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.setVertexBuffer(0, cubeBuffer);
            pass.draw(0, 36);
        }
    }

    private static void putColor(ByteBuffer data, int rgb) {
        data.putFloat((rgb >> 16 & 0xFF) / 255.0f);
        data.putFloat((rgb >> 8 & 0xFF) / 255.0f);
        data.putFloat((rgb & 0xFF) / 255.0f);
        data.putFloat(1.0f);
    }

    private static void ensureBuffers() {
        if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:shader_sky_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_SIZE);
        }
        if (cubeBuffer == null) {
            ByteBuffer cube = buildCube();
            cubeBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:shader_sky_cube", GpuBuffer.USAGE_VERTEX, cube);
            MemoryUtil.memFree(cube);
        }
    }

    private static ByteBuffer buildCube() {
        float[][] faces = {
            {-1, -1, -1,  -1, -1,  1,  -1,  1,  1,  -1,  1, -1},
            { 1, -1, -1,   1,  1, -1,   1,  1,  1,   1, -1,  1},
            {-1, -1, -1,   1, -1, -1,   1, -1,  1,  -1, -1,  1},
            {-1,  1, -1,  -1,  1,  1,   1,  1,  1,   1,  1, -1},
            {-1, -1, -1,  -1,  1, -1,   1,  1, -1,   1, -1, -1},
            {-1, -1,  1,   1, -1,  1,   1,  1,  1,  -1,  1,  1}
        };

        ByteBuffer buffer = MemoryUtil.memAlloc(36 * 3 * Float.BYTES);
        for (float[] face : faces) {
            put(buffer, face, 0);
            put(buffer, face, 1);
            put(buffer, face, 2);
            put(buffer, face, 0);
            put(buffer, face, 2);
            put(buffer, face, 3);
        }
        buffer.flip();
        return buffer;
    }

    private static void put(ByteBuffer buffer, float[] face, int corner) {
        buffer.putFloat(face[corner * 3] * EXTENT);
        buffer.putFloat(face[corner * 3 + 1] * EXTENT);
        buffer.putFloat(face[corner * 3 + 2] * EXTENT);
    }
}
