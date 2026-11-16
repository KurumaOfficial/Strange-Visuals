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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class TargetMarks {
    private static final int RING_SEGMENTS = 48;
    private static final int TAIL = 9;

    private static final float REACTION = 0.4f;

    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private static Entity held;
    private static Entity hitEntity;
    private static long hitAt = Long.MIN_VALUE / 2;
    private static float presence;
    private static float lostAt = -100.0f;
    private static float clock;
    private static long lastFrame = System.nanoTime();

    private TargetMarks() {
    }

    public static void clear() {
        held = null;
        hitEntity = null;
        presence = 0.0f;
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float delta = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;
        clock += delta * ModuleRegistry.targetSpeed().value();

        if (client.world == null || client.player == null) {
            // World gone (disconnect): drop strong Entity refs or they pin the
            // entire ClientWorld (entity.getWorld() -> chunks) in GC. clear() is also
            // called on DISCONNECT, this is the safety net for dimension switches.
            clear();
            return;
        }
        // Dimension switch / entity from a dead world must never be kept.
        if (held != null && (held.isRemoved() || client.world.getEntityById(held.getId()) != held)) {
            held = null;
        }
        if (hitEntity != null && (hitEntity.isRemoved() || client.world.getEntityById(hitEntity.getId()) != hitEntity)) {
            hitEntity = null;
        }
        Entity wanted = ModuleRegistry.targetMarks().enabled() ? looking(client) : null;
        if (wanted != null) {
            if (wanted != held) {
                presence = 0.0f;
            }
            held = wanted;
            lostAt = clock;
        }

        float hold = ModuleRegistry.targetHold().value();
        boolean keeping = wanted != null || (held != null && clock - lostAt < hold);
        presence += ((keeping ? 1.0f : 0.0f) - presence) * Math.min(1.0f, delta * 8.0f);
        if (presence < 0.01f || held == null || held.isRemoved()) {
            held = wanted;
            return;
        }
        if (held.squaredDistanceTo(client.player) > 64.0 * 64.0) {
            held = null;
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        if (consumers == null || matrices == null || camera == null) {
            return;
        }

        Vec3d eye = camera.getCameraPos();

        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        Box box = held.getBoundingBox()
            .offset(held.getLerpedPos(tickDelta).subtract(held.getEntityPos()));
        int colour = ModuleRegistry.targetColor().rgb();
        for (int pass = 0; pass < 2; pass++) {
            boolean lit = pass == 1;
            VertexConsumer buffer = consumers.getBuffer(lit ? Material.LIGHT : Material.BODY);
            CosmeticContext ctx = new CosmeticContext(buffer, lit ? CosmeticContext.LIGHT : CosmeticContext.SHEET,
                matrices.peek().getPositionMatrix(), client.player,
                clock, 1.0f, colour, 0.0f, 0.0f, 1.0f);
            draw(ctx, box, eye, presence, health(held));
        }
    }

    private static void draw(CosmeticContext ctx, Box box, Vec3d eye, float show, float health) {
        float midX = (float) (box.getCenter().x - eye.x);
        float midZ = (float) (box.getCenter().z - eye.z);
        float low = (float) (box.minY - eye.y);
        float high = (float) (box.maxY - eye.y);
        float size = ModuleRegistry.targetSize().value();
        float radius = ((float) Math.max(box.getLengthX(), box.getLengthZ()) * 0.72f + 0.14f) * size;

        ring(ctx, midX, midZ, low + 0.02f, radius, show, clock * 0.8f, 1.0f, health);

        switch (ModuleRegistry.targetStyle().current()) {
            case "Orbit" -> orbit(ctx, midX, midZ, low, high, radius, show);
            case "Cubes" -> cubes(ctx, midX, midZ, low, high, radius, show);
            case "Sweep" -> sweep(ctx, midX, midZ, low, high, radius, show);
            default -> { }
        }
    }

    private static void orbit(CosmeticContext ctx, float midX, float midZ, float low, float high,
                              float radius, float show) {
        int shards = 3;
        float centre = low + (high - low) * 0.55f;
        float lift = (high - low) * 0.22f;
        float out = radius * (1.15f + (1.0f - show) * 1.1f);
        float flare = 1.0f + struck() * 0.5f;
        int deep = Material.deepen(ctx.color, 0.25f);
        int bright = Material.lift(ctx.color, 0.5f);

        for (int i = 0; i < shards; i++) {
            float lead = (float) (Math.PI * 2.0 * i / shards);
            for (int step = 0; step < TAIL; step++) {
                float behind = step * 0.05f;
                float angle = clock * 1.9f + lead - behind;
                float fade = (1.0f - step / (float) TAIL);
                fade *= fade;

                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float y = centre + (float) Math.sin(angle * 1.5f) * lift;
                float span = radius * 0.16f * fade * flare;

                float cx = midX + cos * out;
                float cz = midZ + sin * out;

                a.set(cx, y + span, cz);
                b.set(cx - sin * span, y, cz + cos * span);
                c.set(cx, y - span, cz);
                d.set(cx + sin * span, y, cz - cos * span);

                float alpha = fade * 0.55f * show;
                Material.bodyQuad(ctx, a, b, c, d,
                    Material.argb(bright, alpha), Material.argb(deep, alpha * 0.6f),
                    Material.argb(deep, alpha * 0.6f), Material.argb(deep, alpha * 0.6f));
                if (step == 0) {
                    Material.lightQuad(ctx, a, b, c, d,
                        Material.argb(bright, 0.5f * show), Material.argb(bright, 0.15f * show),
                        Material.argb(bright, 0.15f * show), Material.argb(bright, 0.15f * show));
                }
            }
        }
    }

    private static void cubes(CosmeticContext ctx, float midX, float midZ, float low, float high,
                              float radius, float show) {
        int count = 3;
        int trail = 5;
        float centre = low + (high - low) * 0.55f;
        float lift = (high - low) * 0.2f;
        float out = radius * (1.2f + (1.0f - show) * 1.1f);
        float flare = 1.0f + struck() * 0.6f;

        for (int i = 0; i < count; i++) {
            float lead = (float) (Math.PI * 2.0 * i / count);
            for (int step = 0; step < trail; step++) {
                float angle = clock * 1.6f + lead - step * 0.07f;
                float fade = 1.0f - step / (float) trail;
                fade *= fade;
                float span = radius * 0.13f * fade * flare;
                float y = centre + (float) Math.sin(angle * 1.4f) * lift;
                cube(ctx, midX + (float) Math.cos(angle) * out, y,
                    midZ + (float) Math.sin(angle) * out, span,
                    clock * 1.3f + i, fade * show, step == 0);
            }
        }
    }

    private static void cube(CosmeticContext ctx, float x, float y, float z, float half,
                             float spin, float alpha, boolean head) {
        int deep = Material.deepen(ctx.color, 0.25f);
        int bright = Material.lift(ctx.color, 0.5f);
        float cos = (float) Math.cos(spin) * half;
        float sin = (float) Math.sin(spin) * half;

        for (int face = 0; face < 6; face++) {
            int axis = face >> 1;
            float sign = (face & 1) == 0 ? 1.0f : -1.0f;
            for (int corner = 0; corner < 4; corner++) {
                float u = (corner == 0 || corner == 3) ? -1.0f : 1.0f;
                float v = corner < 2 ? -1.0f : 1.0f;
                float lx;
                float ly;
                float lz;
                if (axis == 0) {
                    lx = sign;
                    ly = v;
                    lz = u;
                } else if (axis == 1) {
                    lx = u;
                    ly = sign;
                    lz = v;
                } else {
                    lx = u;
                    ly = v;
                    lz = sign;
                }

                float wx = lx * cos - lz * sin;
                float wz = lx * sin + lz * cos;
                Vector3f target = corner == 0 ? a : corner == 1 ? b : corner == 2 ? c : d;
                target.set(x + wx, y + ly * half, z + wz);
            }
            Material.body(ctx, a, b, c, d,
                Material.argb(deep, alpha * 0.5f), Material.argb(deep, alpha * 0.32f));
            if (head) {
                Material.light(ctx, a, b, c, d,
                    Material.argb(bright, alpha * 0.22f), Material.argb(bright, 0.0f));
            }
        }
    }

    private static void sweep(CosmeticContext ctx, float midX, float midZ, float low, float high,
                              float radius, float show) {
        float tall = Math.max(0.4f, high - low);

        float cycle = (clock * 0.42f) % 2.0f;
        boolean rising = cycle < 1.0f;
        float along = rising ? cycle : 2.0f - cycle;
        float eased = along * along * (3.0f - 2.0f * along);
        float y = low + tall * eased;

        float out = radius * (1.0f + (1.0f - show) * 0.9f);
        float skirt = tall * 0.3f * (rising ? -1.0f : 1.0f);
        float flare = struck();

        int hot = Material.mix(ctx.color, 0xFF2A20, flare);
        int deep = Material.deepen(hot, 0.2f);
        int bright = Material.lift(hot, 0.55f);
        float step = (float) (Math.PI * 2.0 / RING_SEGMENTS);

        for (int i = 0; i < RING_SEGMENTS; i++) {
            float from = i * step;
            float to = from + step;
            plot(a, midX, midZ, from, out, y);
            plot(b, midX, midZ, to, out, y);
            plot(c, midX, midZ, to, out, y + skirt);
            plot(d, midX, midZ, from, out, y + skirt);

            Material.body(ctx, a, b, c, d,
                Material.argb(deep, (0.45f + flare * 0.3f) * show), Material.argb(deep, 0.02f));
        }

        float band = out * 0.045f;
        float head = clock * 1.4f;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float from = i * step;
            float to = from + step * 0.8f;
            float lit = 0.35f + 0.65f * runner(from, head);
            plot(a, midX, midZ, from, out - band, y);
            plot(b, midX, midZ, to, out - band, y);
            plot(c, midX, midZ, to, out + band, y);
            plot(d, midX, midZ, from, out + band, y);
            Material.light(ctx, a, b, c, d,
                Material.argb(bright, (0.3f + flare * 0.4f) * lit * show),
                Material.argb(bright, 0.05f * show));
        }
    }

    private static void ring(CosmeticContext ctx, float midX, float midZ, float y, float radius,
                             float show, float turn, float weight, float health) {
        int deep = Material.deepen(ctx.color, 0.25f);
        int bright = Material.lift(ctx.color, 0.5f);
        float band = radius * 0.07f;
        float step = (float) (Math.PI * 2.0 / RING_SEGMENTS);
        float open = radius * (1.0f + (1.0f - show) * 1.2f);

        for (int i = 0; i < RING_SEGMENTS; i++) {
            float from = turn + i * step;
            float to = from + step * 0.7f;
            float share = i / (float) RING_SEGMENTS;

            boolean filled = health < 0.0f || share <= health;
            float alpha = (filled ? 0.5f : 0.1f) * show * weight;
            plot(a, midX, midZ, from, open - band, y);
            plot(b, midX, midZ, to, open - band, y);
            plot(c, midX, midZ, to, open + band, y);
            plot(d, midX, midZ, from, open + band, y);
            Material.body(ctx, a, b, c, d, Material.argb(filled ? bright : deep, alpha),
                Material.argb(deep, alpha * 0.25f));
            if (filled) {
                Material.light(ctx, a, b, c, d, Material.argb(bright, alpha * 0.45f),
                    Material.argb(bright, 0.0f));
            }
        }
    }

    private static float struck() {
        if (!ModuleRegistry.targetReact().value() || hitEntity == null || hitEntity != held) {
            return 0.0f;
        }
        float age = (System.nanoTime() - hitAt) / 1_000_000_000.0f / REACTION;
        if (age >= 1.0f) {
            // Reaction window over: release the strong ref so the corpse can GC.
            hitEntity = null;
            return 0.0f;
        }
        return (1.0f - age) * (1.0f - age);
    }

    public static void noteHit(Entity target) {
        hitEntity = target;
        hitAt = System.nanoTime();
    }

    private static float runner(float angle, float head) {
        float full = (float) (Math.PI * 2.0);
        float behind = (head - angle) % full;
        if (behind < 0.0f) {
            behind += full;
        }
        if (behind > 1.3f) {
            return 0.0f;
        }
        float fade = 1.0f - behind / 1.3f;
        return fade * fade;
    }

    private static void plot(Vector3f out, float midX, float midZ, float angle, float radius,
                             float y) {
        out.set(midX + (float) Math.cos(angle) * radius, y, midZ + (float) Math.sin(angle) * radius);
    }

    private static void tipped(Vector3f out, float midX, float midY, float midZ, float angle,
                               float radius, float tip) {
        float cos = (float) Math.cos(angle) * radius;
        float sin = (float) Math.sin(angle) * radius;
        out.set(midX + cos, midY + sin * tip, midZ + sin);
    }

    private static float health(Entity entity) {
        if (!(entity instanceof LivingEntity living) || living.getMaxHealth() <= 0.0f) {
            return -1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, living.getHealth() / living.getMaxHealth()));
    }

    private static Entity looking(MinecraftClient client) {
        if (FakePlayer.aimedAt(client)) {
            return FakePlayer.entity();
        }
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() != client.player) {
            return entityHit.getEntity();
        }
        return null;
    }
}
