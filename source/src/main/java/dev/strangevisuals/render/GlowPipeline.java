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

public final class GlowPipeline {
    private static final Identifier WHITE = StrangeVisuals.id("textures/gui/white.png");

    private static final RenderPipeline RADIAL = build("glow", "core/glow");
    private static final RenderPipeline LINE = build("neon_line", "core/neon_line");
    private static final RenderPipeline LINE_V = build("neon_line_v", "core/neon_line_v");

    private GlowPipeline() {
    }

    public static void init() {
    }

    private static RenderPipeline build(String name, String shader) {
        return Pipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(StrangeVisuals.id("pipeline/" + name))
                .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
                .withFragmentShader(StrangeVisuals.id(shader))
                .withSampler("Sampler0")
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .build());
    }

    public static void radial(DrawContext context, float centerX, float centerY, float radius, int argb) {
        quad(context, RADIAL, centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, argb);
    }

    public static void line(DrawContext context, float centerX, float centerY, float width, float height, int argb) {
        quad(context, LINE, centerX - width * 0.5f, centerY - height * 0.5f, width, height, argb);
    }

    public static void lineVertical(DrawContext context, float centerX, float centerY, float width, float height, int argb) {
        quad(context, LINE_V, centerX - width * 0.5f, centerY - height * 0.5f, width, height, argb);
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
