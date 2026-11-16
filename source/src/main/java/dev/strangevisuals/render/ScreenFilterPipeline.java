package dev.strangevisuals.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class ScreenFilterPipeline {

    private static final Identifier PIPELINE_ID = StrangeVisuals.id("pipeline/screen_filter");
    private static final Identifier SHADER = StrangeVisuals.id("core/screen_filter");
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("FilterData", UniformType.UNIFORM_BUFFER)
                    .withSampler("SceneSampler")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final int BUFFER_SIZE = 64;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized;

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:screen_filter_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);
        initialized = true;
    }

    public void composite(GpuTextureView targetView, GpuTextureView sceneView,
                          int width, int height,
                          float brightness, float contrast, float saturation,
                          float exposure, float gamma, float vignette,
                          float grain, int tintColor, float tintIntensity, float time) {
        ensureInitialized();
        prepareUniformData(width, height, brightness, contrast, saturation, exposure, gamma, vignette, grain, tintColor, tintIntensity, time);

        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "strangevisuals:screen_filter_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "strangevisuals:screen_filter_pass",
                targetView,
                OptionalInt.empty()
        )) {
            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", sceneView, sampler);
            renderPass.setUniform("FilterData", uniformBuffer);

            renderPass.draw(0, 6);
        }
    }

    private void prepareUniformData(int width, int height,
                                    float brightness, float contrast, float saturation,
                                    float exposure, float gamma, float vignette,
                                    float grain, int tintColor, float tintIntensity, float time) {
        dataBuffer.clear();

        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);
        dataBuffer.putFloat(brightness);
        dataBuffer.putFloat(contrast);

        dataBuffer.putFloat(saturation);
        dataBuffer.putFloat(exposure);
        dataBuffer.putFloat(gamma);
        dataBuffer.putFloat(vignette);

        dataBuffer.putFloat(grain);
        dataBuffer.putFloat(tintIntensity);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(0.0f);

        dataBuffer.putFloat(((tintColor >> 16) & 0xFF) / 255.0f);
        dataBuffer.putFloat(((tintColor >> 8) & 0xFF) / 255.0f);
        dataBuffer.putFloat((tintColor & 0xFF) / 255.0f);
        dataBuffer.putFloat(((tintColor >> 24) & 0xFF) / 255.0f);
        dataBuffer.flip();
    }

    public void close() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}
