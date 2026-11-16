package dev.strangevisuals.player;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.cosmetic.CosmeticContext;
import dev.strangevisuals.cosmetic.Material;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public final class HitEffects {
    private static final int POOL = 8;
    private static final int RING_SEGMENTS = 40;

    private static final float BUBBLE_RADIUS = 0.45f;

    private static final double[] atX = new double[POOL];
    private static final double[] atY = new double[POOL];
    private static final double[] atZ = new double[POOL];
    private static final float[] dirX = new float[POOL];
    private static final float[] dirZ = new float[POOL];
    private static final float[] born = new float[POOL];
    private static final float[] force = new float[POOL];
    private static int next;

    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private static float clock;
    private static long lastFrame = System.nanoTime();

    private HitEffects() {
    }

    public static void add(Entity target, float charge, Vec3d impact) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !ModuleRegistry.hitEffects().enabled()) {
            return;
        }
        Vec3d eye = client.player.getEyePos();

        Vec3d point = impact != null ? impact
            : target.getEntityPos().add(0.0, target.getHeight() * 0.6, 0.0);

        Vec3d back = point.subtract(eye);
        float fx = 0.0f;
        float fy = 0.0f;
        float fz = 1.0f;
        if (back.lengthSquared() > 0.0001) {
            Vec3d along = back.normalize();
            fx = (float) along.x;
            fy = (float) along.y;
            fz = (float) along.z;
        }

        float stand = 0.3f;

        atX[next] = point.x - fx * stand;
        atY[next] = point.y - fy * stand;
        atZ[next] = point.z - fz * stand;
        dirX[next] = fx;
        dirZ[next] = fz;
        born[next] = clock;
        force[next] = Math.max(0.35f, charge);
        next = (next + 1) % POOL;
    }

    public static int live() {
        float life = "Bubble".equals(ModuleRegistry.hitStyle().current())
            ? bubbleLife() : ModuleRegistry.hitLife().value();
        int count = 0;
        for (int i = 0; i < POOL; i++) {
            float age = (clock - born[i]) / life;
            if (age >= 0.0f && age < 1.0f) {
                count++;
            }
        }
        return count;
    }

    public static int fillBubbles(ByteBuffer data, Vec3d eye, int max) {
        float life = bubbleLife();
        float scale = ModuleRegistry.hitSize().value();
        int count = 0;

        data.position(32);
        for (int i = 0; i < POOL && count < max; i++) {
            float age = (clock - born[i]) / life;
            if (age < 0.0f || age >= 1.0f) {
                continue;
            }

            float grown = Math.min(1.0f, age * 6.0f);
            grown = grown * grown * (3.0f - 2.0f * grown);
            data.putFloat((float) (atX[i] - eye.x));
            data.putFloat((float) (atY[i] - eye.y + age * 0.22f));
            data.putFloat((float) (atZ[i] - eye.z));
            data.putFloat(BUBBLE_RADIUS * scale * force[i] * (0.5f + grown * 0.5f));
            count++;
        }
        while (data.position() < 32 + max * 16) {
            data.putFloat(0.0f);
        }

        data.position(32 + max * 16);
        int written = 0;
        for (int i = 0; i < POOL && written < count; i++) {
            float age = (clock - born[i]) / life;
            if (age < 0.0f || age >= 1.0f) {
                continue;
            }

            float fade = 1.0f - smoothstep(0.72f, 1.0f, age);
            data.putFloat(fade).putFloat(0.05f + fade * 0.03f).putFloat(age).putFloat(0.0f);
            written++;
        }
        while (data.position() < 32 + max * 32) {
            data.putFloat(0.0f);
        }
        return count;
    }

    private static float bubbleLife() {
        return 1.4f + ModuleRegistry.hitLife().value() * 3.2f;
    }

    private static float smoothstep(float from, float to, float x) {
        float t = Math.max(0.0f, Math.min(1.0f, (x - from) / (to - from)));
        return t * t * (3.0f - 2.0f * t);
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        clock += (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        if (!ModuleRegistry.hitEffects().enabled() || client.world == null) {
            return;
        }
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        if (consumers == null || matrices == null || camera == null || client.player == null) {
            return;
        }

        float life = ModuleRegistry.hitLife().value();
        Vec3d eye = camera.getCameraPos();
        int colour = ModuleRegistry.hitColor().rgb();
        String style = ModuleRegistry.hitStyle().current();

        for (int pass = 0; pass < 2; pass++) {
            boolean lit = pass == 1;
            VertexConsumer buffer = consumers.getBuffer(lit ? Material.LIGHT : Material.BODY);
            CosmeticContext ctx = new CosmeticContext(buffer, lit ? CosmeticContext.LIGHT : CosmeticContext.SHEET,
                matrices.peek().getPositionMatrix(), client.player,
                clock, 1.0f, colour, 0.0f, 0.0f, 1.0f);

            for (int i = 0; i < POOL; i++) {
                float age = (clock - born[i]) / life;
                if (age < 0.0f || age >= 1.0f) {
                    continue;
                }
                float x = (float) (atX[i] - eye.x);
                float y = (float) (atY[i] - eye.y);
                float z = (float) (atZ[i] - eye.z);
                float size = ModuleRegistry.hitSize().value() * force[i];
                switch (style) {
                    case "Slash" -> slash(ctx, x, y, z, dirX[i], dirZ[i], age, size, i);
                    case "Shock" -> shock(ctx, x, y, z, age, size);

                    case "Bubble" -> { }
                    default -> impact(ctx, x, y, z, dirX[i], dirZ[i], age, size);
                }
            }
        }
    }

    private static void impact(CosmeticContext ctx, float x, float y, float z, float fx, float fz,
                               float age, float size) {
        float open = size * (0.25f + age * 1.1f);
        float band = size * 0.16f * (1.0f - age);
        float fade = (1.0f - age) * (1.0f - age);
        int deep = Material.deepen(ctx.color, 0.2f);
        int bright = Material.lift(ctx.color, 0.55f);

        float sideX = -fz;
        float sideZ = fx;
        float step = (float) (Math.PI * 2.0 / RING_SEGMENTS);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float from = i * step;
            float to = from + step;
            face(a, x, y, z, sideX, sideZ, from, open - band);
            face(b, x, y, z, sideX, sideZ, to, open - band);
            face(c, x, y, z, sideX, sideZ, to, open + band);
            face(d, x, y, z, sideX, sideZ, from, open + band);
            Material.body(ctx, a, b, c, d, Material.argb(deep, fade * 0.45f),
                Material.argb(deep, 0.0f));
            Material.light(ctx, a, b, c, d, Material.argb(bright, fade * 0.5f),
                Material.argb(bright, 0.0f));
        }

        for (int i = 0; i < 5; i++) {
            float seed = Material.noise(i * 5 + 1);
            float spread = (seed - 0.5f) * 1.2f;
            float reach = size * (0.5f + seed) * age * 1.6f;
            float thin = size * 0.05f * (1.0f - age);
            float ox = fx + sideX * spread;
            float oz = fz + sideZ * spread;
            float len = (float) Math.sqrt(ox * ox + oz * oz);
            ox /= len;
            oz /= len;
            float rise = (seed - 0.35f) * size * 0.6f * age;

            a.set(x + ox * reach, y + rise + thin, z + oz * reach);
            b.set(x + ox * reach, y + rise - thin, z + oz * reach);
            c.set(x + ox * reach * 0.55f, y + rise * 0.55f - thin, z + oz * reach * 0.55f);
            d.set(x + ox * reach * 0.55f, y + rise * 0.55f + thin, z + oz * reach * 0.55f);
            Material.lightQuad(ctx, a, b, c, d,
                Material.argb(bright, fade * 0.55f), Material.argb(bright, fade * 0.55f),
                Material.argb(bright, 0.0f), Material.argb(bright, 0.0f));
        }
    }

    private static void slash(CosmeticContext ctx, float x, float y, float z, float fx, float fz,
                              float age, float size, int index) {
        int steps = 22;
        float open = size * (0.5f + age * 0.75f);
        float fade = (1.0f - age) * (1.0f - age);
        int deep = Material.deepen(ctx.color, 0.15f);
        int bright = Material.lift(ctx.color, 0.55f);

        float lean = (index & 1) == 0 ? 0.85f : -0.85f;
        float span = 2.1f;
        float sideX = -fz;
        float sideZ = fx;

        for (int i = 0; i < steps; i++) {
            float from = (i / (float) steps - 0.5f) * span;
            float to = ((i + 1) / (float) steps - 0.5f) * span;

            float thickFrom = size * 0.2f * (float) Math.cos(from * 1.4f) * fade;
            float thickTo = size * 0.2f * (float) Math.cos(to * 1.4f) * fade;
            if (thickFrom <= 0.0f || thickTo <= 0.0f) {
                continue;
            }
            arc(a, x, y, z, sideX, sideZ, from, lean, open - thickFrom);
            arc(b, x, y, z, sideX, sideZ, to, lean, open - thickTo);
            arc(c, x, y, z, sideX, sideZ, to, lean, open + thickTo);
            arc(d, x, y, z, sideX, sideZ, from, lean, open + thickFrom);
            Material.body(ctx, a, b, c, d, Material.argb(deep, fade * 0.5f),
                Material.argb(deep, fade * 0.12f));
            Material.light(ctx, a, b, c, d, Material.argb(bright, fade * 0.45f),
                Material.argb(bright, 0.0f));
        }
    }

    private static void shock(CosmeticContext ctx, float x, float y, float z, float age,
                              float size) {
        float open = size * (0.3f + age * 2.2f);
        float band = size * 0.2f * (1.0f - age);
        float fade = (1.0f - age) * (1.0f - age);
        float floor = y - 1.1f;
        int deep = Material.deepen(ctx.color, 0.2f);
        int bright = Material.lift(ctx.color, 0.5f);
        float step = (float) (Math.PI * 2.0 / RING_SEGMENTS);

        for (int i = 0; i < RING_SEGMENTS; i++) {
            float from = i * step;
            float to = from + step;
            flat(a, x, floor, z, from, open - band);
            flat(b, x, floor, z, to, open - band);
            flat(c, x, floor, z, to, open + band);
            flat(d, x, floor, z, from, open + band);
            Material.body(ctx, a, b, c, d, Material.argb(deep, fade * 0.4f),
                Material.argb(deep, 0.0f));
            Material.light(ctx, a, b, c, d, Material.argb(bright, fade * 0.35f),
                Material.argb(bright, 0.0f));
        }
    }

    private static void face(Vector3f out, float x, float y, float z, float sideX, float sideZ,
                             float angle, float radius) {
        float across = (float) Math.cos(angle) * radius;
        float up = (float) Math.sin(angle) * radius;
        out.set(x + sideX * across, y + up, z + sideZ * across);
    }

    private static void arc(Vector3f out, float x, float y, float z, float sideX, float sideZ,
                            float angle, float lean, float radius) {
        float across = (float) Math.sin(angle) * radius;
        float up = (float) Math.cos(angle) * radius * lean;
        out.set(x + sideX * across, y + up, z + sideZ * across);
    }

    private static void flat(Vector3f out, float x, float y, float z, float angle, float radius) {
        out.set(x + (float) Math.cos(angle) * radius, y, z + (float) Math.sin(angle) * radius);
    }
}
