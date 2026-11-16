package dev.strangevisuals.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.player.KillEffects;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class KillShaderRenderer {
    private static final int MAX = 4;

    private static final int UNIFORM_SIZE = 32 + MAX * 16 * 2 + 64;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/kill_effect"))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/kill_effect"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static SimpleFramebuffer scene;
    private static GpuBuffer uniforms;
    private static GpuBuffer dummyVertex;
    private static final Matrix4f inverse = new Matrix4f();
    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(UNIFORM_SIZE).order(java.nio.ByteOrder.nativeOrder());

    private KillShaderRenderer() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModuleRegistry.killEffects().enabled() || client.world == null
            || !KillEffects.shaderStyle() || KillEffects.live() == 0) {
            if (!ModuleRegistry.killEffects().enabled() || client.world == null) {
                shutdown();
            }
            return;
        }
        Framebuffer main = client.getFramebuffer();
        Camera camera = client.gameRenderer.getCamera();
        if (main == null || main.getColorAttachmentView() == null
            || main.getDepthAttachmentView() == null || camera == null) {
            return;
        }
        if (!ensureBuffers(main)) {
            return;
        }

        BlitPipeline.run(main.getColorAttachmentView(), scene.getColorAttachmentView());

        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(
            client.options.getFov().getValue());
        inverse.set(projection).mul(RenderSystem.getModelViewMatrix()).invert();
        Vec3d eye = camera.getCameraPos();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        UNIFORM_DATA.clear();
        int count = KillEffects.fillEvents(UNIFORM_DATA, eye, MAX);
        UNIFORM_DATA.rewind();
        UNIFORM_DATA.putFloat(main.textureWidth).putFloat(main.textureHeight)
            .putFloat(KillEffects.time()).putFloat(count);
        int tint = ModuleRegistry.killColor().rgb();
        UNIFORM_DATA.putFloat((tint >> 16 & 0xFF) / 255.0f).putFloat((tint >> 8 & 0xFF) / 255.0f)
            .putFloat((tint & 0xFF) / 255.0f).putFloat(1.0f);
        UNIFORM_DATA.position(32 + MAX * 16 * 2);
        inverse.get(MATRIX_VALUES);
        for (float value : MATRIX_VALUES) {
            UNIFORM_DATA.putFloat(value);
        }
        UNIFORM_DATA.position(UNIFORM_SIZE);
        UNIFORM_DATA.flip();

        encoder.writeToBuffer(uniforms.slice(), UNIFORM_DATA);

        GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler nearest = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:kill_effect",
            main.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.bindTexture("Sampler0", scene.getColorAttachmentView(), linear);
            pass.bindTexture("Sampler1", main.getDepthAttachmentView(), nearest);
            pass.setUniform("Uniforms", uniforms);
            pass.setVertexBuffer(0, dummyVertex);
            pass.draw(0, 3);
        }
    }

    private static final float[] MATRIX_VALUES = new float[16];

    private static boolean ensureBuffers(Framebuffer main) {
        int width = Math.max(1, main.textureWidth);
        int height = Math.max(1, main.textureHeight);
        if (scene == null) {
            scene = new SimpleFramebuffer("strangevisuals_kill_scene", width, height, false);
        } else if (scene.textureWidth != width || scene.textureHeight != height) {
            scene.resize(width, height);
        }
        if (uniforms == null) {
            uniforms = RenderSystem.getDevice().createBuffer(() -> "strangevisuals:kill_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE);
        }
        if (dummyVertex == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertex = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:kill_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
            MemoryUtil.memFree(dummy);
        }
        return scene.getColorAttachmentView() != null;
    }

    /** Frees the fullscreen scene copy. Safe to call when already released. */
    public static void shutdown() {
        if (scene != null) {
            try {
                scene.delete();
            } catch (Throwable t) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Kill-shader buffer release failed: {}", t.getMessage());
            }
            scene = null;
        }
    }
}
