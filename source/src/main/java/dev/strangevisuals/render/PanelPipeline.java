package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class PanelPipeline {
    private static final Identifier WHITE = StrangeVisuals.id("textures/gui/white.png");

    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/panel"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/panel"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private static final RenderPipeline OUTLINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/panel_outline"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/panel_outline"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private static final RenderPipeline GLOW = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/panel_glow"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/panel_glow"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private PanelPipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, float x, float y, float width, float height, int argb) {
        quad(context, PIPELINE, x, y, width, height, argb);
    }

    public static void drawOutline(DrawContext context, float x, float y, float width, float height, int argb) {
        quad(context, OUTLINE, x, y, width, height, argb);
    }

    public static void drawGlow(DrawContext context, float x, float y, float width, float height, int argb) {
        float spread = height * 0.5f;
        quad(context, GLOW, x - spread, y - spread, width + spread * 2.0f, height + spread * 2.0f, argb);
    }

    private static void quad(DrawContext context, RenderPipeline pipeline, float x, float y, float width, float height, int argb) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(width, height);
        context.drawTexture(pipeline, WHITE, 0, 0, 0.0f, 0.0f, 1, 1, 1, 1, 1, 1, argb);
        matrices.popMatrix();
    }
}
