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
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class BlitPipeline {
    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/blit"))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/blit"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static GpuBuffer dummyVertexBuffer;

    private BlitPipeline() {
    }

    public static void init() {
    }

    public static void run(GpuTextureView source, GpuTextureView target) {
        if (source == null || target == null) {
            return;
        }
        ensureBuffer();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:blit", target, OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.bindTexture("Sampler0", source, sampler);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.draw(0, 3);
        }
    }

    private static void ensureBuffer() {
        if (dummyVertexBuffer != null) {
            return;
        }
        ByteBuffer dummy = MemoryUtil.memAlloc(4);
        dummy.putInt(0);
        dummy.flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
            () -> "strangevisuals:blit_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
        MemoryUtil.memFree(dummy);
    }
}
