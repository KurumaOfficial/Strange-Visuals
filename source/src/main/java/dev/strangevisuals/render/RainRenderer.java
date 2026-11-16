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

public final class RainRenderer {
    private static final int UNIFORM_SIZE = 64 + 64 + 64;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/rain"))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/rain"))
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
    private static final Matrix4f forward = new Matrix4f();
    private static final Matrix4f inverse = new Matrix4f();
    private static final ByteBuffer UNIFORM_DATA = ByteBuffer.allocateDirect(UNIFORM_SIZE).order(java.nio.ByteOrder.nativeOrder());
    private static float clock;
    private static long lastFrame = System.nanoTime();
    private static float presence;
    private static double stepX;
    private static double stepZ;
    private static float stepAt = -10.0f;

    private static final float STEP_LENGTH = 0.9f;
    private static final float STEP_LIFE = 0.8f;

    private RainRenderer() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float delta = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        float wanted = wanted(client);
        presence += (wanted - presence) * Math.min(1.0f, delta * 1.4f);
        if (presence < 0.01f) {
            // Rain faded out (module off / clear weather): drop the fullscreen copy.
            if (wanted <= 0.001f) {
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
        clock += delta;

        BlitPipeline.run(main.getColorAttachmentView(), scene.getColorAttachmentView());

        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(
            client.options.getFov().getValue());

        forward.set(projection).mul(RenderSystem.getModelViewMatrix());
        inverse.set(forward).invert();

        Vec3d eye = camera.getCameraPos();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        UNIFORM_DATA.clear();
        UNIFORM_DATA.putFloat(main.textureWidth).putFloat(main.textureHeight)
            .putFloat(clock).putFloat(presence * ModuleRegistry.rainStrength().value());
        UNIFORM_DATA.putFloat((float) eye.x).putFloat((float) eye.y).putFloat((float) eye.z).putFloat(0.0f);
        int tint = ModuleRegistry.rainColor().rgb();
        UNIFORM_DATA.putFloat((tint >> 16 & 0xFF) / 255.0f).putFloat((tint >> 8 & 0xFF) / 255.0f)
            .putFloat((tint & 0xFF) / 255.0f).putFloat(1.0f);
        putStep(UNIFORM_DATA, client);
        putMatrix(UNIFORM_DATA, inverse);
        putMatrix(UNIFORM_DATA, forward);
        UNIFORM_DATA.flip();

        encoder.writeToBuffer(uniforms.slice(), UNIFORM_DATA);

        GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler nearest = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:rain",
            main.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.bindTexture("Sampler0", scene.getColorAttachmentView(), linear);
            pass.bindTexture("Sampler1", main.getDepthAttachmentView(), nearest);
            pass.setUniform("Uniforms", uniforms);
            pass.setVertexBuffer(0, dummyVertex);
            pass.draw(0, 3);
        }
    }

    private static float wanted(MinecraftClient client) {
        if (!ModuleRegistry.shaders().enabled() || client.world == null) {
            return 0.0f;
        }
        if ("Off".equals(ModuleRegistry.rainMode().current())) {
            return 0.0f;
        }
        return client.world.getRainGradient(1.0f);
    }

    private static void putStep(ByteBuffer data, MinecraftClient client) {
        boolean quality = "Quality".equals(ModuleRegistry.rainMode().current());
        if (client.player == null) {
            data.putFloat(0.0f).putFloat(0.0f).putFloat(2.0f).putFloat(quality ? 1.0f : 0.0f);
            return;
        }
        Vec3d at = client.player.getEntityPos();
        boolean grounded = client.player.isOnGround();
        double moved = Math.hypot(at.x - stepX, at.z - stepZ);
        if (grounded && moved > STEP_LENGTH) {
            stepX = at.x;
            stepZ = at.z;
            stepAt = clock;
        }
        float age = (clock - stepAt) / STEP_LIFE;
        data.putFloat((float) stepX).putFloat((float) stepZ)
            .putFloat(age).putFloat(quality ? 1.0f : 0.0f);
    }

    private static void putMatrix(ByteBuffer data, Matrix4f matrix) {
        float[] values = new float[16];
        matrix.get(values);
        for (float value : values) {
            data.putFloat(value);
        }
    }

    private static boolean ensureBuffers(Framebuffer main) {
        int width = Math.max(1, main.textureWidth);
        int height = Math.max(1, main.textureHeight);
        if (scene == null) {
            scene = new SimpleFramebuffer("strangevisuals_rain_scene", width, height, false);
        } else if (scene.textureWidth != width || scene.textureHeight != height) {
            scene.resize(width, height);
        }
        if (uniforms == null) {
            uniforms = RenderSystem.getDevice().createBuffer(() -> "strangevisuals:rain_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE);
        }
        if (dummyVertex == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertex = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:rain_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
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
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Rain buffer release failed: {}", t.getMessage());
            }
            scene = null;
        }
    }
}
