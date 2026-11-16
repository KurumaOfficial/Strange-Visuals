package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class SdfPipeline {
    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/sdf_glyph"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(StrangeVisuals.id("core/icon"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private SdfPipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, GlyphAtlas atlas, GlyphAtlas.Glyph glyph,
                            float x, float y, float width, float height, int argb) {
        if (!glyph.visible || glyph.regionWidth <= 0 || glyph.regionHeight <= 0) {
            return;
        }
        org.joml.Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(width / glyph.regionWidth, height / glyph.regionHeight);
        context.drawTexture(PIPELINE, atlas.texture(), 0, 0, glyph.u, glyph.v,
            glyph.regionWidth, glyph.regionHeight, glyph.regionWidth, glyph.regionHeight,
            atlas.atlasWidth(), atlas.atlasHeight(), argb);
        matrices.popMatrix();
    }
}
