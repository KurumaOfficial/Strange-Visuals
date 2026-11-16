package dev.strangevisuals.hands;

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
import dev.strangevisuals.cosmetic.Material;
import dev.strangevisuals.render.BlitPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * First-person hand outline and motion trail pipeline.
 * <p>
 * Extracts hand geometry via depth-buffer delta comparison before and after first-person hand rendering,
 * then executes full-screen passes for outline generation (Line, Glow, Aura, Energy) and smoke/motion trails.
 */
public final class HandOutline {
    private static final int TRAIL_UNIFORM = 32;
    private static final int OUTLINE_UNIFORM = 96;

    private static final RenderPipeline TRAIL_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/hand_trail"))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/hand_trail"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static final RenderPipeline OUTLINE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(StrangeVisuals.id("pipeline/hand_outline"))
            .withVertexShader(StrangeVisuals.id("core/downsample"))
            .withFragmentShader(StrangeVisuals.id("core/hand_outline"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)

            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static final ByteBuffer TRAIL_DATA = ByteBuffer.allocateDirect(TRAIL_UNIFORM).order(java.nio.ByteOrder.nativeOrder());
    private static final ByteBuffer OUTLINE_DATA = ByteBuffer.allocateDirect(OUTLINE_UNIFORM).order(java.nio.ByteOrder.nativeOrder());

    private static SimpleFramebuffer before;
    private static SimpleFramebuffer after;
    private static SimpleFramebuffer trailFront;
    private static SimpleFramebuffer trailBack;
    private static GpuBuffer trailUniforms;
    private static GpuBuffer outlineUniforms;
    private static GpuBuffer dummyVertex;

    private static int width;
    private static int height;
    private static boolean armed;
    private static float clock;
    private static long lastFrame = System.nanoTime();

    private HandOutline() {
    }

    public static boolean enabled() {
        if (!ModuleRegistry.betterHands().enabled()) {
            return false;
        }
        return !"None".equals(ModuleRegistry.handOutline().current())
            || !"None".equals(ModuleRegistry.handMotion().current());
    }

    public static void captureBefore() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (!enabled() || main == null || main.getDepthAttachment() == null) {
            // Hands FX off: don't hold 4 fullscreen buffers (2x depth + 2x trail).
            if (!enabled()) {
                shutdown();
            }
            armed = false;
            return;
        }
        if (!ensureBuffers(main)) {
            armed = false;
            return;
        }
        copyDepth(main, before);
        armed = true;
    }

    public static void composite() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (!armed || main == null || main.getColorAttachmentView() == null) {
            return;
        }
        armed = false;
        if (main.textureWidth != width || main.textureHeight != height) {
            return;
        }

        long now = System.nanoTime();
        clock += (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        copyDepth(main, after);

        BlitPipeline.run(main.getColorAttachmentView(), after.getColorAttachmentView());

        runTrail();
        runOutline(main);

        SimpleFramebuffer swap = trailFront;
        trailFront = trailBack;
        trailBack = swap;
    }

    private static void runTrail() {
        String motion = ModuleRegistry.handMotion().current();

        float decay = switch (motion) {
            case "Soft" -> 0.93f;
            case "Long" -> 0.966f;
            case "Smoke" -> 0.975f;
            default -> 0.0f;
        };
        boolean smoke = "Smoke".equals(motion);
        float spread = smoke ? 3.1f : 2.4f;

        float rise = smoke ? 1.4f : 0.0f;
        float wander = smoke ? 3.2f : 0.0f;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        TRAIL_DATA.clear();
        TRAIL_DATA.putFloat(width).putFloat(height).putFloat(decay).putFloat(spread);
        TRAIL_DATA.putFloat(rise).putFloat(wander).putFloat(clock).putFloat(0.0f);
        TRAIL_DATA.flip();

        encoder.writeToBuffer(trailUniforms.slice(), TRAIL_DATA);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:hand_trail",
            trailFront.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(TRAIL_PIPELINE);
            pass.bindTexture("Sampler0", before.getDepthAttachmentView(), sampler);
            pass.bindTexture("Sampler1", after.getDepthAttachmentView(), sampler);
            pass.bindTexture("Sampler2", trailBack.getColorAttachmentView(), sampler);
            pass.setUniform("Uniforms", trailUniforms);
            pass.setVertexBuffer(0, dummyVertex);
            pass.draw(0, 3);
        }
    }

    private static void runOutline(Framebuffer main) {
        String style = ModuleRegistry.handOutline().current();
        int styleIndex = switch (style) {
            case "Line" -> 1;
            case "Glow" -> 2;
            case "Aura" -> 3;
            case "Energy" -> 4;
            default -> 0;
        };
        float strength = ModuleRegistry.handStrength().value();
        float trailStrength = "None".equals(ModuleRegistry.handMotion().current())
            ? 0.0f : ModuleRegistry.handTrailStrength().value();
        int tint = ModuleRegistry.handOutlineColor().rgb();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        OUTLINE_DATA.clear();
        OUTLINE_DATA.putFloat(width).putFloat(height).putFloat(clock).putFloat(styleIndex);

        OUTLINE_DATA.putFloat(1.4f + strength * 1.1f).putFloat(strength)
            .putFloat(trailStrength).putFloat(10.0f + strength * 14.0f);
        putColor(OUTLINE_DATA, tint);
        putColor(OUTLINE_DATA, Material.shift(tint, 30.0f));
        HandEditor.putSelection(OUTLINE_DATA);
        OUTLINE_DATA.flip();

        encoder.writeToBuffer(outlineUniforms.slice(), OUTLINE_DATA);

        GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        try (RenderPass pass = encoder.createRenderPass(() -> "strangevisuals:hand_outline",
            main.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(OUTLINE_PIPELINE);
            pass.bindTexture("Sampler0", after.getColorAttachmentView(), linear);

            pass.bindTexture("Sampler1", trailFront.getColorAttachmentView(), linear);
            pass.setUniform("Uniforms", outlineUniforms);
            pass.setVertexBuffer(0, dummyVertex);
            pass.draw(0, 3);
        }
    }

    private static void putColor(ByteBuffer data, int rgb) {
        data.putFloat((rgb >> 16 & 0xFF) / 255.0f);
        data.putFloat((rgb >> 8 & 0xFF) / 255.0f);
        data.putFloat((rgb & 0xFF) / 255.0f);
        data.putFloat(1.0f);
    }

    private static void copyDepth(Framebuffer main, SimpleFramebuffer into) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            main.getDepthAttachment(), into.getDepthAttachment(), 0, 0, 0, 0, 0, width, height);
    }

    private static boolean ensureBuffers(Framebuffer main) {
        int w = Math.max(1, main.textureWidth);
        int h = Math.max(1, main.textureHeight);
        if (before != null && (w != width || h != height)) {
            before.resize(w, h);
            after.resize(w, h);
            trailFront.resize(w, h);
            trailBack.resize(w, h);
        }
        width = w;
        height = h;

        if (before == null) {
            before = new SimpleFramebuffer("strangevisuals_hand_before", w, h, true);
            after = new SimpleFramebuffer("strangevisuals_hand_after", w, h, true);
            trailFront = new SimpleFramebuffer("strangevisuals_hand_trail_a", w, h, false);
            trailBack = new SimpleFramebuffer("strangevisuals_hand_trail_b", w, h, false);

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorTexture(trailFront.getColorAttachment(), 0);
            encoder.clearColorTexture(trailBack.getColorAttachment(), 0);
        }
        if (trailUniforms == null) {
            trailUniforms = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:hand_trail_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, TRAIL_UNIFORM);
            outlineUniforms = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:hand_outline_uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, OUTLINE_UNIFORM);
        }
        if (dummyVertex == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertex = RenderSystem.getDevice().createBuffer(
                () -> "strangevisuals:hand_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
            MemoryUtil.memFree(dummy);
        }
        return before.getDepthAttachment() != null;
    }

    /** Frees 4 fullscreen buffers (before/after depth + 2x trail). Idempotent. */
    public static void shutdown() {
        armed = false;
        for (SimpleFramebuffer fb : new SimpleFramebuffer[]{before, after, trailFront, trailBack}) {
            if (fb != null) {
                try {
                    fb.delete();
                } catch (Throwable t) {
                    dev.strangevisuals.StrangeVisuals.LOGGER.warn("[StrangeVisuals] Hand-outline release failed: {}", t.getMessage());
                }
            }
        }
        before = null;
        after = null;
        trailFront = null;
        trailBack = null;
        width = 0;
        height = 0;
    }
}
