package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class CardPipeline {
    private static final Identifier WHITE = StrangeVisuals.id("textures/gui/white.png");

    private static final RenderPipeline PIPELINE = Pipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/card"))
            .withVertexShader(StrangeVisuals.id("core/rounded_rect"))
            .withFragmentShader(StrangeVisuals.id("core/card"))
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());

    private CardPipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, float x, float y, float width, float height, int argb) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        float guiScale = mc != null && mc.getWindow() != null ? (float) mc.getWindow().getScaleFactor() : 1.0f;
        float radiusPx = (float) Theme.radiusLevel() * 4.0f * guiScale;

        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
        float z = radiusPx;

        ScreenRect bounds = new ScreenRect(
            (int) Math.floor(x),
            (int) Math.floor(y),
            (int) Math.ceil(width),
            (int) Math.ceil(height)
        ).transformEachVertex(pose);

        ScreenRect scissor = context.scissorStack != null ? context.scissorStack.peekLast() : null;

        context.state.addSimpleElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer consumer) {
                org.joml.Vector2f p0 = pose.transformPosition(x, y, new org.joml.Vector2f());
                org.joml.Vector2f p1 = pose.transformPosition(x, y + height, new org.joml.Vector2f());
                org.joml.Vector2f p2 = pose.transformPosition(x + width, y + height, new org.joml.Vector2f());
                org.joml.Vector2f p3 = pose.transformPosition(x + width, y, new org.joml.Vector2f());

                consumer.vertex(p0.x, p0.y, z).texture(0.0f, 0.0f).color(argb);
                consumer.vertex(p1.x, p1.y, z).texture(0.0f, 1.0f).color(argb);
                consumer.vertex(p2.x, p2.y, z).texture(1.0f, 1.0f).color(argb);
                consumer.vertex(p3.x, p3.y, z).texture(1.0f, 0.0f).color(argb);
            }

            @Override
            public RenderPipeline pipeline() {
                return PIPELINE;
            }

            @Override
            public TextureSetup textureSetup() {
                if (mc == null || mc.getTextureManager() == null) {
                    return TextureSetup.empty();
                }
                AbstractTexture tex = mc.getTextureManager().getTexture(HudGlassCapture.sharpTextureId());
                if (tex == null || tex.getGlTextureView() == null) {
                    tex = mc.getTextureManager().getTexture(WHITE);
                }
                if (tex == null || tex.getGlTextureView() == null) {
                    return TextureSetup.empty();
                }
                return TextureSetup.of(tex.getGlTextureView(), tex.getSampler());
            }

            @Override
            public ScreenRect scissorArea() {
                return scissor;
            }

            @Override
            public ScreenRect bounds() {
                return bounds;
            }
        });
    }
}
