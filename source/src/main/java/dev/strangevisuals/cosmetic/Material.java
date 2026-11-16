package dev.strangevisuals.cosmetic;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Material {
    private static final RenderPipeline BODY_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/cosmetic_body"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static final RenderPipeline LIGHT_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/cosmetic_light"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());

    private static final RenderPipeline SOLID_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(StrangeVisuals.id("pipeline/cosmetic_solid"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withCull(false)
            .build());

    public static final RenderLayer SOLID = RenderLayer.of("strangevisuals_cosmetic_solid",
        RenderSetup.builder(SOLID_PIPELINE).translucent().expectedBufferSize(8192).build());

    public static final RenderLayer BODY = RenderLayer.of("strangevisuals_cosmetic_body",
        RenderSetup.builder(BODY_PIPELINE).translucent().expectedBufferSize(4096).build());

    public static final RenderLayer LIGHT = RenderLayer.of("strangevisuals_cosmetic_light",
        RenderSetup.builder(LIGHT_PIPELINE).expectedBufferSize(2048).build());

    private Material() {
    }

    private static final Vector3f LIGHT_DIR = new Vector3f(-0.42f, 0.78f, -0.46f).normalize();

    private static final Vector3f normal = new Vector3f();
    private static final Vector3f edge1 = new Vector3f();
    private static final Vector3f edge2 = new Vector3f();
    private static final Vector3f toEye = new Vector3f();

    public static void body(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                            int near, int far) {
        if (ctx.pass != CosmeticContext.SHEET) {
            return;
        }
        float gain = surface(ctx, a, b, c, d);
        band(ctx.target, ctx.matrix, a, b, c, d, gain(near, gain), gain(far, gain));
    }

    public static void bodyQuad(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                int colorA, int colorB, int colorC, int colorD) {
        if (ctx.pass != CosmeticContext.SHEET) {
            return;
        }
        float gain = surface(ctx, a, b, c, d);
        quad(ctx.target, ctx.matrix, a, b, c, d,
            gain(colorA, gain), gain(colorB, gain), gain(colorC, gain), gain(colorD, gain));
    }

    private static float surface(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        edge1.set(b).sub(a);
        edge2.set(d).sub(a);
        edge1.cross(edge2, normal);
        float len = normal.length();
        if (len < 1.0e-6f) {
            return 1.0f;
        }
        normal.div(len);

        toEye.set(a).add(b).add(c).add(d).mul(0.25f).sub(ctx.eye);
        float distance = toEye.length();
        if (distance < 1.0e-6f) {
            return 1.0f;
        }
        float facing = Math.abs(normal.dot(toEye) / distance);
        float thickness = Math.min(2.7f, 1.0f / Math.max(0.17f, facing));
        float lambert = 0.6f + 0.4f * Math.abs(normal.dot(LIGHT_DIR));
        return thickness * lambert;
    }

    public static void bodySmooth(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c,
                                  Vector3f d, Vector3f na, Vector3f nb, Vector3f nc, Vector3f nd,
                                  int colorA, int colorB, int colorC, int colorD) {
        if (ctx.pass != CosmeticContext.SOLID) {
            return;
        }
        quad(ctx.target, ctx.matrix, a, b, c, d,
            gain(colorA, corner(ctx, a, na)), gain(colorB, corner(ctx, b, nb)),
            gain(colorC, corner(ctx, c, nc)), gain(colorD, corner(ctx, d, nd)));
    }

    public static void rimSmooth(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c,
                                 Vector3f d, Vector3f na, Vector3f nb, Vector3f nc, Vector3f nd,
                                 int rgb, float strength) {
        if (ctx.pass != CosmeticContext.LIGHT) {
            return;
        }
        quad(ctx.target, ctx.matrix, a, b, c, d,
            argb(rgb, rim(ctx, a, na) * strength), argb(rgb, rim(ctx, b, nb) * strength),
            argb(rgb, rim(ctx, c, nc) * strength), argb(rgb, rim(ctx, d, nd) * strength));
    }

    private static float facing(CosmeticContext ctx, Vector3f at, Vector3f normal) {
        toEye.set(ctx.eye).sub(at);
        float distance = toEye.length();
        if (distance < 1.0e-6f) {
            return 1.0f;
        }
        return Math.abs(normal.dot(toEye) / distance);
    }

    private static float corner(CosmeticContext ctx, Vector3f at, Vector3f normal) {
        float facing = facing(ctx, at, normal);
        float thickness = Math.min(2.7f, 1.0f / Math.max(0.17f, facing));
        float lambert = 0.6f + 0.4f * Math.abs(normal.dot(LIGHT_DIR));
        return thickness * lambert;
    }

    private static float rim(CosmeticContext ctx, Vector3f at, Vector3f normal) {
        return (float) Math.pow(1.0f - facing(ctx, at, normal), 2.2);
    }

    private static int gain(int argb, float amount) {
        int alpha = Math.min(255, Math.round((argb >>> 24) * amount));
        return alpha << 24 | argb & 0xFFFFFF;
    }

    public static void light(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                             int near, int far) {
        if (ctx.lit) {
            band(ctx.target, ctx.matrix, a, b, c, d, near, far);
        }
    }

    public static void lightQuad(CosmeticContext ctx, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                 int colorA, int colorB, int colorC, int colorD) {
        if (ctx.lit) {
            quad(ctx.target, ctx.matrix, a, b, c, d, colorA, colorB, colorC, colorD);
        }
    }

    public static void quad(VertexConsumer buffer, Matrix4f matrix,
                            Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                            int colorA, int colorB, int colorC, int colorD) {
        vertex(buffer, matrix, a, colorA);
        vertex(buffer, matrix, b, colorB);
        vertex(buffer, matrix, c, colorC);
        vertex(buffer, matrix, d, colorD);
    }

    public static void band(VertexConsumer buffer, Matrix4f matrix,
                            Vector3f a, Vector3f b, Vector3f c, Vector3f d, int near, int far) {
        quad(buffer, matrix, a, b, c, d, near, near, far, far);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vector3f at, int argb) {
        buffer.vertex(matrix, at.x, at.y, at.z)
            .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
    }

    public static int argb(int rgb, float alpha) {
        int clamped = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return clamped << 24 | rgb & 0xFFFFFF;
    }

    public static int lift(int rgb, float amount) {
        return mix(rgb, 0xFFFFFF, amount);
    }

    public static int deepen(int rgb, float amount) {
        return mix(rgb, 0x000000, amount);
    }

    public static int shift(int rgb, float degrees) {
        float[] hsv = dev.strangevisuals.ui.Theme.rgbToHsv(rgb);
        float hue = (hsv[0] + degrees / 360.0f) % 1.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        return dev.strangevisuals.ui.Theme.hsvToRgb(hue, hsv[1], hsv[2]);
    }

    public static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    public static float noise(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h >>> 8 & 0xFFFF) / 65535.0f;
    }
}
