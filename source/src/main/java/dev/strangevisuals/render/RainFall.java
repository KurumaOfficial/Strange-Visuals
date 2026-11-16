package dev.strangevisuals.render;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.cosmetic.CosmeticContext;
import dev.strangevisuals.cosmetic.Material;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;

public final class RainFall {
    private static final int FAST_COUNT = 220;
    private static final int QUALITY_COUNT = 620;

    private static final float SPREAD = 13.0f;
    private static final float TOP = 9.0f;
    private static final float LENGTH = 0.85f;
    private static final float WIDTH = 0.012f;

    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();
    private static final BlockPos.Mutable probe = new BlockPos.Mutable();
    private static float clock;
    private static long lastFrame = System.nanoTime();

    private RainFall() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        clock += (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        if (!ModuleRegistry.shaders().enabled() || client.world == null || client.player == null) {
            return;
        }
        String mode = ModuleRegistry.rainMode().current();
        if ("Off".equals(mode)) {
            return;
        }
        float fall = client.world.getRainGradient(1.0f);
        if (fall < 0.02f) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        if (consumers == null || matrices == null || camera == null) {
            return;
        }

        int count = Math.round(("Quality".equals(mode) ? QUALITY_COUNT : FAST_COUNT) * fall);
        Vec3d eye = camera.getCameraPos();
        int tint = ModuleRegistry.rainColor().rgb();
        float strength = ModuleRegistry.rainStrength().value();
        for (int pass = 0; pass < 2; pass++) {
            boolean lit = pass == 1;
            VertexConsumer target = consumers.getBuffer(lit ? Material.LIGHT : Material.BODY);
            CosmeticContext ctx = new CosmeticContext(target, lit ? CosmeticContext.LIGHT : CosmeticContext.SHEET,
                matrices.peek().getPositionMatrix(), client.player,
                clock, 1.0f, tint, 0.0f, 0.0f, 1.0f);
            for (int i = 0; i < count; i++) {
                drop(ctx, client, eye, i, fall * strength);
            }
        }
    }

    private static void drop(CosmeticContext ctx, MinecraftClient client, Vec3d eye,
                             int index, float amount) {
        float spread = Material.noise(index * 3 + 1);
        float angle = Material.noise(index * 3 + 2) * (float) (Math.PI * 2.0);

        float radius = (float) Math.sqrt(spread) * SPREAD;
        double x = Math.floor(eye.x + Math.cos(angle) * radius) + 0.5;
        double z = Math.floor(eye.z + Math.sin(angle) * radius) + 0.5;

        int ground = client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x),
            (int) Math.floor(z));
        if (eye.y + TOP < ground) {
            return;
        }
        probe.set((int) Math.floor(x), ground, (int) Math.floor(z));
        if (!client.world.isSkyVisible(probe)) {
            return;
        }

        float seed = Material.noise(index * 3 + 3);
        float pace = 14.0f + seed * 7.0f;
        float top = (float) eye.y + TOP;
        float span = Math.max(1.0f, top - ground);

        float fallen = (ctx.time * pace + seed * 97.0f) % span;
        float y = top - fallen;
        if (y < ground) {
            return;
        }

        float head = (float) (y - eye.y);
        float across = WIDTH * (1.0f + seed);
        float length = LENGTH * (0.7f + seed * 0.6f);
        float ox = (float) (x - eye.x);
        float oz = (float) (z - eye.z);

        float sideX = -oz;
        float sideZ = ox;
        float len = (float) Math.sqrt(sideX * sideX + sideZ * sideZ);
        if (len < 0.0001f) {
            return;
        }
        sideX = sideX / len * across;
        sideZ = sideZ / len * across;

        a.set(ox - sideX, head + length, oz - sideZ);
        b.set(ox + sideX, head + length, oz + sideZ);
        c.set(ox + sideX * 0.35f, head, oz + sideZ * 0.35f);
        d.set(ox - sideX * 0.35f, head, oz - sideZ * 0.35f);

        int deep = Material.deepen(ctx.color, 0.25f);
        Material.body(ctx, a, b, c, d,
            Material.argb(deep, 0.0f), Material.argb(deep, 0.32f * amount));

        int bright = Material.lift(ctx.color, 0.45f);
        Material.light(ctx, a, b, c, d,
            Material.argb(bright, 0.0f), Material.argb(bright, 0.22f * amount));
    }
}
