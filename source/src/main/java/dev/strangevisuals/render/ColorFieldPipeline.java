package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ColorFieldPipeline {
    private static final Identifier WHITE = StrangeVisuals.id("textures/gui/white.png");

    private static final RenderPipeline FIELD = build("color_field");
    private static final RenderPipeline HUE = build("hue_strip");
    private static final RenderPipeline HUE_V = build("hue_strip_v");

    private ColorFieldPipeline() {
    }

    public static void init() {
    }

    private static RenderPipeline build(String shader) {
        return Pipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(StrangeVisuals.id("pipeline/" + shader))
                .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
                .withFragmentShader(StrangeVisuals.id("core/" + shader))
                .withSampler("Sampler0")
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR,
                    com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .build());
    }

    public static void field(DrawContext context, float x, float y, float w, float h, int argb) {
        quad(context, FIELD, x, y, w, h, argb);
    }

    public static void hue(DrawContext context, float x, float y, float w, float h, int alpha) {
        quad(context, HUE, x, y, w, h, alpha << 24 | 0xFFFFFF);
    }

    public static void hueVertical(DrawContext context, float x, float y, float w, float h, int alpha) {
        quad(context, HUE_V, x, y, w, h, alpha << 24 | 0xFFFFFF);
    }

    private static void quad(DrawContext context, RenderPipeline pipeline, float x, float y, float w, float h, int argb) {
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        // Was push/translate(x,y)/scale(w,h)/draw(0,0,1,1)/pop per quad — arithmetically
        // identical to drawing at (x,y) size (w,h) directly, minus 4 matrix ops.
        context.drawTexture(pipeline, WHITE, Math.round(x), Math.round(y), 0.0f, 0.0f,
            Math.max(1, Math.round(w)), Math.max(1, Math.round(h)), 1, 1, 1, 1, argb);
    }
}
