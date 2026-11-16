package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class GlassWindowPipeline {
    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/glass_window"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/glass_window"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private GlassWindowPipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, float x, float y, float width, float height, int argb) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(width, height);
        float u = (float) Theme.radiusLevel();
        context.drawTexture(PIPELINE, HudGlassCapture.textureId(), 0, 0, u, 0.0f, 1, 1, 1, 1, 1, 1, argb);
        matrices.popMatrix();
    }
}
