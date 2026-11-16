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

public final class CirclePipeline {
    private static final Identifier WHITE = StrangeVisuals.id("textures/gui/white.png");

    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/circle"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/circle"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private CirclePipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, float centerX, float centerY, float radius, int argb) {
        if (radius <= 0.0f) {
            return;
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX - radius, centerY - radius);
        matrices.scale(radius * 2.0f, radius * 2.0f);
        context.drawTexture(PIPELINE, WHITE, 0, 0, 0.0f, 0.0f, 1, 1, 1, 1, 1, 1, argb);
        matrices.popMatrix();
    }
}
