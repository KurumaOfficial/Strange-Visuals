package dev.strangevisuals.player;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.cosmetic.CosmeticContext;
import dev.strangevisuals.cosmetic.Material;
import dev.strangevisuals.cosmetic.Plume;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;

public final class KillEffects {
    private static final int POOL = 4;
    private static final int WATCHED = 8;

    private static final float WATCH = 8.0f;
    private static final int RING_STEPS = 48;

    private static final Entity[] watching = new Entity[WATCHED];
    private static final float[] watchedAt = new float[WATCHED];
    private static int nextWatch;

    private static final double[] atX = new double[POOL];
    private static final double[] atY = new double[POOL];
    private static final double[] atZ = new double[POOL];
    private static final float[] facing = new float[POOL];
    private static final float[] tall = new float[POOL];
    private static final float[] wide = new float[POOL];
    private static final float[] born = new float[POOL];
    private static final float[] echoAt = new float[POOL];
    private static final EntityRenderState[] pose = new EntityRenderState[POOL];
    private static int next;

    private static final Matrix4f frame = new Matrix4f();
    private static final Vector3f axis = new Vector3f();
    private static final Vector3f side = new Vector3f();
    private static final Vector3f lift = new Vector3f();
    private static final Vector3f qa = new Vector3f();
    private static final Vector3f qb = new Vector3f();
    private static final Vector3f qc = new Vector3f();
    private static final Vector3f qd = new Vector3f();

    private static float clock;
    private static long lastFrame = System.nanoTime();

    private KillEffects() {
    }

    public static void clear() {
        for (int i = 0; i < WATCHED; i++) {
            watching[i] = null;
        }
        for (int i = 0; i < POOL; i++) {
            pose[i] = null;
        }
    }

    public static void watch(Entity target) {
        for (int i = 0; i < WATCHED; i++) {
            if (watching[i] == target) {
                watchedAt[i] = clock;
                return;
            }
        }
        watching[nextWatch] = target;
        watchedAt[nextWatch] = clock;
        nextWatch = (nextWatch + 1) % WATCHED;
    }

    private static void sweepWatchList(MinecraftClient client) {
        for (int i = 0; i < WATCHED; i++) {
            Entity entity = watching[i];
            if (entity == null) {
                continue;
            }
            // Never pin entities (and via entity.getWorld(), whole worlds) across
            // disconnects / dimension switches. clear() on DISCONNECT is the primary
            // path; these guards cover dimension reuse and stale slots.
            if (client.world == null || entity.isRemoved() || client.world.getEntityById(entity.getId()) != entity) {
                watching[i] = null;
                continue;
            }
            if (clock - watchedAt[i] > WATCH) {
                watching[i] = null;
                continue;
            }
            if (!(entity instanceof LivingEntity living) || living.isAlive()) {
                continue;
            }
            watching[i] = null;
            Vec3d at = living.getEntityPos();
            atX[next] = at.x;
            atY[next] = at.y;
            atZ[next] = at.z;
            facing[next] = living.bodyYaw;
            tall[next] = Math.max(0.7f, living.getHeight());
            wide[next] = Math.max(0.3f, living.getWidth());
            born[next] = clock;
            pose[next] = snapshot(client, living);
            opening(client, at, next);
            next = (next + 1) % POOL;
        }
    }

    private static EntityRenderState snapshot(MinecraftClient client, LivingEntity living) {
        EntityRenderState state;
        try {
            state = client.getEntityRenderDispatcher().getAndUpdateRenderState(living,
                client.getRenderTickCounter().getTickProgress(false));
        } catch (RuntimeException failed) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("EntityRenderState snapshot failed: {}", failed.getMessage());
            return null;
        }
        state.onFire = false;
        state.invisible = false;
        state.displayName = null;
        state.nameLabelPos = null;
        state.shadowRadius = 0.0f;
        state.shadowPieces.clear();
        state.leashDatas = List.of();
        state.outlineColor = EntityRenderState.NO_OUTLINE;
        if (state instanceof LivingEntityRenderState frozen) {
            frozen.deathTime = 0.0f;
            frozen.hurt = false;
            frozen.invisibleToPlayer = false;
        }
        return state;
    }

    private static void opening(MinecraftClient client, Vec3d at, int slot) {
        echoAt[slot] = -1.0f;
        if (client.world == null) {
            return;
        }
        switch (ModuleRegistry.killStyle().current()) {
            case "Portal" -> {
                play(client, at, SoundEvents.BLOCK_PORTAL_TRIGGER, 0.55f, 1.45f);
                play(client, at, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.65f);
                echoAt[slot] = clock + 0.32f;
            }
            case "Tornado" -> {
                play(client, at, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.55f);
                play(client, at, SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), 0.9f, 0.7f);
                echoAt[slot] = clock + 0.45f;
            }
            case "Shatter" -> {
                play(client, at, SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
                play(client, at, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 0.9f, 0.65f);
                echoAt[slot] = clock + 0.14f;
            }
            default -> {
                play(client, at, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 0.8f, 0.55f);
                play(client, at, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.35f, 1.6f);
                echoAt[slot] = clock + 0.6f;
            }
        }
    }

    private static void echo(MinecraftClient client, int slot) {
        Vec3d at = new Vec3d(atX[slot], atY[slot], atZ[slot]);
        switch (ModuleRegistry.killStyle().current()) {
            case "Portal" -> play(client, at, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                0.35f, 1.9f);
            case "Tornado" -> play(client, at, SoundEvents.ENTITY_BREEZE_WIND_BURST.value(),
                0.6f, 0.5f);
            case "Shatter" -> play(client, at, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                0.45f, 1.7f);
            default -> play(client, at, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.75f);
        }
    }

    private static void play(MinecraftClient client, Vec3d at, SoundEvent sound, float volume,
                             float pitch) {
        if (client.world == null) {
            return;
        }
        client.world.playSoundClient(at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume,
            pitch, false);
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        clock += (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        if (!ModuleRegistry.killEffects().enabled() || client.world == null
            || client.player == null) {
            if (client.world == null) {
                clear();
            }
            return;
        }
        sweepWatchList(client);

        float life = ModuleRegistry.killLife().value();
        boolean any = false;
        for (int i = 0; i < POOL; i++) {
            float age = (clock - born[i]) / life;
            boolean running = age >= 0.0f && age < 1.0f;
            if (running) {
                any = true;
                if (echoAt[i] > 0.0f && clock >= echoAt[i]) {
                    echoAt[i] = -1.0f;
                    echo(client, i);
                }
            } else if (pose[i] != null) {
                pose[i] = null;
            }
        }
        if (!any) {
            return;
        }

        String style = ModuleRegistry.killStyle().current();
        if (shaderStyle()) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        if (consumers == null || matrices == null || camera == null) {
            return;
        }

        float scale = ModuleRegistry.killSize().value();
        int colour = ModuleRegistry.killColor().rgb();
        Vec3d eye = camera.getCameraPos();
        boolean breaking = "Shatter".equals(style);

        drawBodies(context, client, matrices, eye, life, scale, breaking);

        for (int pass = 0; pass < 3; pass++) {
            VertexConsumer buffer = consumers.getBuffer(pass == CosmeticContext.SOLID
                ? Material.SOLID : pass == CosmeticContext.SHEET ? Material.BODY : Material.LIGHT);
            CosmeticContext ctx = new CosmeticContext(buffer, pass,
                matrices.peek().getPositionMatrix(), client.player,
                clock, scale, colour, 0.0f, 0.0f, 1.0f);

            for (int i = 0; i < POOL; i++) {
                float age = (clock - born[i]) / life;
                if (age < 0.0f || age >= 1.0f) {
                    continue;
                }
                float x = (float) (atX[i] - eye.x);
                float y = (float) (atY[i] - eye.y);
                float z = (float) (atZ[i] - eye.z);
                float height = tall[i] * scale;
                float girth = Math.max(wide[i], tall[i] * 0.4f) * scale;
                if (breaking) {
                    shatter(ctx, x, y, z, height, girth, age, i);
                } else {
                    soul(ctx, x, y, z, height, girth, age, i);
                }
            }
        }
    }

    private static float climb(float height, float age) {
        float pull = age * age * (3.0f - 2.0f * age) * 0.4f + age * 0.6f;
        return height * 0.85f + (height * 2.0f + 4.5f) * pull;
    }

    private static float held(float age) {
        return 1.0f - Material.smoothstep(0.16f, 0.32f, age);
    }

    private static void drawBodies(WorldRenderContext context, MinecraftClient client,
                                   MatrixStack matrices, Vec3d eye, float life, float scale,
                                   boolean breaking) {
        OrderedRenderCommandQueue queue = context.commandQueue();
        CameraRenderState cameraState = context.worldState() == null
            ? null : context.worldState().cameraRenderState;
        EntityRenderManager manager = client.getEntityRenderDispatcher();
        if (queue == null || cameraState == null) {
            return;
        }

        for (int i = 0; i < POOL; i++) {
            float age = (clock - born[i]) / life;
            if (age < 0.0f || age >= 1.0f || pose[i] == null) {
                continue;
            }
            float height = tall[i] * scale;
            float rise;
            float size;
            float shakeX = 0.0f;
            float shakeZ = 0.0f;

            if (breaking) {
                float hold = held(age);
                if (hold <= 0.02f) {
                    continue;
                }
                float shake = (1.0f - hold) * height * 0.06f + 0.01f;
                rise = 0.0f;
                size = scale * (0.86f + 0.14f * hold);
                shakeX = (Material.noise((int) (clock * 90.0f) + i * 31) - 0.5f) * shake;
                shakeZ = (Material.noise((int) (clock * 90.0f) + 17 + i * 31) - 0.5f) * shake;
            } else {
                rise = climb(height, age);

                size = scale * (1.0f + 0.14f * Material.smoothstep(0.0f, 0.4f, age)
                    - 0.95f * Material.smoothstep(0.78f, 1.0f, age));
                shakeX = (float) Math.sin(clock * 0.8 + i) * 0.06f;
            }
            if (size <= 0.02f) {
                continue;
            }

            matrices.push();
            matrices.translate(atX[i] - eye.x + shakeX, atY[i] - eye.y + rise,
                atZ[i] - eye.z + shakeZ);
            matrices.scale(size, size, size);
            try {
                manager.render(pose[i], cameraState, 0.0, 0.0, 0.0, matrices, queue);
            } catch (RuntimeException failed) {
                dev.strangevisuals.StrangeVisuals.LOGGER.debug("KillEffect entity model render failed: {}", failed.getMessage());
                pose[i] = null;
            }
            matrices.pop();
        }
    }

    private static void soul(CosmeticContext ctx, float x, float y, float z, float height,
                             float girth, float age, int slot) {
        float presence = age < 0.25f
            ? Material.smoothstep(0.0f, 0.15f, age)
            : 1.0f - Material.smoothstep(0.72f, 1.0f, age);
        if (presence < 0.01f) {
            return;
        }
        float rise = climb(height, age);
        float open = Material.smoothstep(0.05f, 0.42f, age);
        float sway = (float) Math.sin(clock * 0.8 + slot) * 0.06f;
        int bright = Material.lift(ctx.color, 0.6f);

        float ground = Material.smoothstep(0.0f, 0.35f, age);
        if (ground < 1.0f) {
            frame.identity().translate(x, y + 0.02f, z);
            ring(ctx, height * (0.4f + ground * 1.6f), (1.0f - ground) * 0.4f);
        }

        frame.identity()
            .translate(x + sway, y + rise, z)
            .rotateY((float) Math.toRadians(-facing[slot]));

        wings(ctx, height, girth, open, presence, slot);
        halo(ctx, height, girth, presence * open);

        for (int i = 0; i < 22; i++) {
            float a = Material.noise(i * 11 + slot * 5 + 1);
            float b = Material.noise(i * 11 + 2 + slot * 5);
            float step = (age * (0.7f + a * 0.9f) + b) % 1.0f;
            float angle = b * (float) (Math.PI * 2.0) + step * 2.4f;
            float out = girth * (0.5f + a * 1.4f) * (0.4f + step);
            spark(ctx, x + (float) Math.cos(angle) * out,
                y + rise + height * (0.1f + step * 1.1f) - height * 0.3f,
                z + (float) Math.sin(angle) * out,
                height * 0.05f * (1.0f - step * 0.6f),
                Material.argb(bright, presence * (1.0f - step) * 0.6f),
                Material.argb(ctx.color, 0.0f));
        }
    }

    private static void halo(CosmeticContext ctx, float height, float girth, float alpha) {
        if (alpha < 0.01f) {
            return;
        }
        int bright = Material.lift(ctx.color, 0.5f);
        int rings = 7;
        for (int i = 0; i < rings; i++) {
            float t = (i + 0.5f) / rings;
            float radius = girth * (0.7f + 1.1f * (float) Math.sin(t * Math.PI));
            float band = height * 0.02f;
            float atY = height * t;
            float fade = alpha * 0.1f * (float) Math.sin(t * Math.PI);
            double step = Math.PI * 2.0 / 24.0;
            for (int k = 0; k < 24; k++) {
                double from = k * step + clock * 0.4;
                double to = from + step;
                place((float) Math.cos(from) * radius, atY - band,
                    (float) Math.sin(from) * radius, qa);
                place((float) Math.cos(to) * radius, atY - band,
                    (float) Math.sin(to) * radius, qb);
                place((float) Math.cos(to) * radius, atY + band,
                    (float) Math.sin(to) * radius, qc);
                place((float) Math.cos(from) * radius, atY + band,
                    (float) Math.sin(from) * radius, qd);
                Material.lightQuad(ctx, qa, qb, qc, qd, Material.argb(bright, fade),
                    Material.argb(bright, fade), Material.argb(bright, fade),
                    Material.argb(bright, fade));
            }
        }
    }

    private static void wings(CosmeticContext ctx, float height, float girth, float open,
                              float presence, int slot) {
        float span = Math.max(height, 1.4f) * (0.22f + open * 1.0f);
        float beat = (float) Math.sin(clock * 1.5 + slot * 1.3) * 0.13f * open;
        Plume.render(ctx, frame, height * 0.92f, girth * 0.24f, -girth * 0.32f, span, open,
            presence * open, beat, slot * 7, Material.deepen(ctx.color, 0.42f),
            Material.lift(ctx.color, 0.6f));
    }

    private static void shatter(CosmeticContext ctx, float x, float y, float z, float height,
                                float girth, float age, int slot) {
        float hold = held(age);
        float centre = y + height * 0.55f;
        int deep = Material.deepen(ctx.color, 0.32f);
        int bright = Material.lift(ctx.color, 0.62f);

        if (hold > 0.02f) {
            float wind = 1.0f - hold;
            for (int i = 0; i < 14; i++) {
                float a = Material.noise(i * 5 + slot * 13);
                float b = Material.noise(i * 5 + 1 + slot * 13);
                float up = height * (0.15f + a * 0.75f);
                float angle = b * (float) (Math.PI * 2.0);
                float out = girth * 0.42f;
                thread(ctx, x + (float) Math.cos(angle) * out, y + up,
                    z + (float) Math.sin(angle) * out,
                    x + (float) Math.cos(angle + 0.9f) * out, y + up + height * 0.12f,
                    z + (float) Math.sin(angle + 0.9f) * out,
                    height * 0.012f, Material.argb(bright, wind * 0.7f));
            }
            frame.identity().translate(x, y + 0.02f, z);
            ring(ctx, girth * (1.0f + wind * 0.8f), wind * 0.5f);
        }

        float burst = Math.max(0.0f, (age - 0.28f) / 0.72f);
        if (burst <= 0.0f) {
            return;
        }
        float fade = 1.0f - Material.smoothstep(0.72f, 1.0f, burst);

        float flash = 1.0f - Material.smoothstep(0.0f, 0.22f, burst);
        if (flash > 0.0f) {
            frame.identity().translate(x, centre, z);
            for (int i = 0; i < 3; i++) {
                float grown = Material.smoothstep(0.0f, 0.16f + i * 0.14f, burst);
                ring(ctx, girth * (0.9f + grown * (4.5f + i * 2.6f)),
                    (1.0f - grown) * (0.75f - i * 0.2f));
            }
        }

        int shards = 46;
        for (int i = 0; i < shards; i++) {
            float a = Material.noise(i * 7 + slot * 23 + 1);
            float b = Material.noise(i * 7 + 2 + slot * 23);
            float c = Material.noise(i * 7 + 3 + slot * 23);
            float up = 1.0f - 2.0f * (i + 0.5f) / shards;
            float rim = (float) Math.sqrt(Math.max(0.0, 1.0 - up * up));
            float angle = i * 2.39996323f + b * 0.7f;

            float speed = (1.5f + a * 2.2f) * Math.max(1.0f, girth * 1.5f);
            float travel = speed * (1.0f - (float) Math.exp(-burst * 1.3f)) / 1.3f;
            float drop = 2.2f * burst * burst;

            float dx = (float) Math.cos(angle) * rim;
            float dz = (float) Math.sin(angle) * rim;
            float px = x + dx * travel;
            float pz = z + dz * travel;
            float py = centre + up * travel * 0.75f - drop;

            float trail = travel * 0.3f;
            thread(ctx, px, py, pz, px - dx * trail, py - up * trail * 0.75f + drop * 0.3f,
                pz - dz * trail, height * 0.02f * (0.6f + c),
                Material.argb(bright, fade * 0.3f));

            float span = height * 0.08f * (0.45f + c) * (1.0f - burst * 0.3f);
            float spin = clock * (2.2f + a * 3.5f) + i;
            frame.identity().translate(px, py, pz).rotateY(spin * 0.55f).rotateX(spin);
            place(0.0f, span * 1.7f, 0.0f, qa);
            place(span, 0.0f, span * 0.3f, qb);
            place(0.0f, -span * 1.1f, 0.0f, qc);
            place(-span, 0.0f, -span * 0.3f, qd);
            Material.bodyQuad(ctx, qa, qb, qc, qd, Material.argb(bright, fade * 0.75f),
                Material.argb(deep, fade * 0.5f), Material.argb(deep, fade * 0.5f),
                Material.argb(deep, fade * 0.5f));
            Material.lightQuad(ctx, qa, qb, qc, qd, Material.argb(bright, fade * 0.3f),
                Material.argb(bright, 0.0f), Material.argb(bright, 0.0f),
                Material.argb(bright, 0.0f));
        }

        for (int i = 0; i < 26; i++) {
            float a = Material.noise(i * 9 + slot * 3 + 5);
            float b = Material.noise(i * 9 + 6 + slot * 3);
            float step = Math.min(1.0f, burst * (0.5f + a * 0.7f));
            float angle = b * (float) (Math.PI * 2.0) + step * 1.6f;
            float out = girth * (0.4f + a * 1.9f) * (0.3f + step);
            float alpha = fade * (1.0f - step) * 0.7f;
            if (alpha < 0.02f) {
                continue;
            }
            spark(ctx, x + (float) Math.cos(angle) * out,
                centre + step * height * (0.6f + b * 1.4f) - height * 0.2f,
                z + (float) Math.sin(angle) * out,
                height * 0.035f * (1.0f - step * 0.5f),
                Material.argb(bright, alpha), Material.argb(ctx.color, 0.0f));
        }

        float dust = Material.smoothstep(0.0f, 0.75f, burst);
        if (dust < 1.0f) {
            frame.identity().translate(x, y + 0.02f, z);
            ring(ctx, girth * (1.2f + dust * 7.0f), (1.0f - dust) * fade * 0.35f);
        }
    }

    private static void place(float x, float y, float z, Vector3f out) {
        frame.transformPosition(x, y, z, out);
    }

    private static void ring(CosmeticContext ctx, float radius, float alpha) {
        if (alpha < 0.01f || radius <= 0.0f) {
            return;
        }
        int bright = Material.lift(ctx.color, 0.55f);
        float inner = radius * 0.88f;
        double step = Math.PI * 2.0 / RING_STEPS;
        for (int i = 0; i < RING_STEPS; i++) {
            double from = i * step;
            double to = from + step;
            place((float) Math.cos(from) * inner, 0.0f, (float) Math.sin(from) * inner, qa);
            place((float) Math.cos(to) * inner, 0.0f, (float) Math.sin(to) * inner, qb);
            place((float) Math.cos(to) * radius, 0.0f, (float) Math.sin(to) * radius, qc);
            place((float) Math.cos(from) * radius, 0.0f, (float) Math.sin(from) * radius, qd);
            Material.body(ctx, qa, qb, qc, qd, Material.argb(bright, alpha * 0.5f),
                Material.argb(bright, 0.0f));
            Material.light(ctx, qa, qb, qc, qd, Material.argb(bright, alpha * 0.3f),
                Material.argb(bright, 0.0f));
        }
    }

    private static void spark(CosmeticContext ctx, float x, float y, float z, float radius,
                              int centre, int rim) {
        axis.set(x, y, z).sub(ctx.eye);
        if (axis.lengthSquared() < 1.0e-8f) {
            return;
        }
        axis.normalize();
        side.set(0.0f, 1.0f, 0.0f).cross(axis);
        if (side.lengthSquared() < 1.0e-8f) {
            side.set(1.0f, 0.0f, 0.0f);
        }
        side.normalize().mul(radius);
        axis.cross(side, lift);
        lift.normalize().mul(radius);

        qa.set(x - side.x - lift.x, y - side.y - lift.y, z - side.z - lift.z);
        qb.set(x + side.x - lift.x, y + side.y - lift.y, z + side.z - lift.z);
        qc.set(x + side.x + lift.x, y + side.y + lift.y, z + side.z + lift.z);
        qd.set(x - side.x + lift.x, y - side.y + lift.y, z - side.z + lift.z);
        Material.bodyQuad(ctx, qa, qb, qc, qd, rim, rim, rim, rim);
        Material.lightQuad(ctx, qa, qb, qc, qd, centre, centre, centre, centre);
    }

    private static void thread(CosmeticContext ctx, float ax, float ay, float az,
                               float bx, float by, float bz, float width, int colour) {
        axis.set(bx - ax, by - ay, bz - az);
        if (axis.lengthSquared() < 1.0e-8f) {
            return;
        }
        lift.set((ax + bx) * 0.5f, (ay + by) * 0.5f, (az + bz) * 0.5f).sub(ctx.eye);
        if (lift.lengthSquared() < 1.0e-8f) {
            return;
        }
        axis.normalize().cross(lift.normalize(), side);
        if (side.lengthSquared() < 1.0e-8f) {
            return;
        }
        side.normalize().mul(width);

        qa.set(ax - side.x, ay - side.y, az - side.z);
        qb.set(ax + side.x, ay + side.y, az + side.z);
        qc.set(bx + side.x, by + side.y, bz + side.z);
        qd.set(bx - side.x, by - side.y, bz - side.z);
        Material.bodyQuad(ctx, qa, qb, qc, qd, colour, colour, colour, colour);
        Material.lightQuad(ctx, qa, qb, qc, qd, colour, colour, colour, colour);
    }

    public static boolean shaderStyle() {
        String style = ModuleRegistry.killStyle().current();
        return "Portal".equals(style) || "Tornado".equals(style);
    }

    public static int live() {
        float life = ModuleRegistry.killLife().value();
        int count = 0;
        for (int i = 0; i < POOL; i++) {
            float age = (clock - born[i]) / life;
            if (age >= 0.0f && age < 1.0f) {
                count++;
            }
        }
        return count;
    }

    public static float time() {
        return clock;
    }

    public static int fillEvents(ByteBuffer data, Vec3d eye, int max) {
        float life = ModuleRegistry.killLife().value();
        float scale = ModuleRegistry.killSize().value();
        boolean funnel = "Tornado".equals(ModuleRegistry.killStyle().current());
        int count = 0;

        data.position(32);
        for (int i = 0; i < POOL && count < max; i++) {
            float age = (clock - born[i]) / life;
            if (age < 0.0f || age >= 1.0f) {
                continue;
            }
            float height = tall[i] * scale;

            float lifted = funnel ? 0.0f : height * 0.62f;
            data.putFloat((float) (atX[i] - eye.x));
            data.putFloat((float) (atY[i] - eye.y + lifted));
            data.putFloat((float) (atZ[i] - eye.z));
            data.putFloat(funnel ? Math.max(1.1f, wide[i] * 1.8f) * scale
                : Math.max(1.1f, height * 0.85f));
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
            data.putFloat(age).putFloat(funnel ? 1.0f : 0.0f)
                .putFloat((tall[i] * 3.4f + 5.5f) * scale).putFloat(0.0f);
            written++;
        }
        while (data.position() < 32 + max * 16 * 2) {
            data.putFloat(0.0f);
        }
        return count;
    }
}
