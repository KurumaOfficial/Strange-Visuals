package dev.strangevisuals.clickgui;

import dev.strangevisuals.clickgui.setting.ModeSetting;
import dev.strangevisuals.render.ScreenCapture;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

public final class MenuCloseEffect {
    public enum Mode {
        SHATTER,
        ZOOM_OUT
    }

    private static final int COLUMNS = 8;
    private static final int ROWS = 5;
    private static final int SHARDS = COLUMNS * ROWS;

    private static final float SHATTER_DURATION = 2.6f;
    private static final float ZOOM_DURATION = 3.23f;

    private static final float DISTANCE = 1.6f;
    private static final float GRAVITY = 1.15f;

    private static final float ZOOM_END_DISTANCE = 3.4f;

    private static final float ZOOM_START_SCALE = 1.14f;

    private static final Shard[] SHARD_POOL = new Shard[SHARDS];
    private static final Random RANDOM = new Random();

    private static final Vector3f scratch = new Vector3f();
    private static final Vector3f corner = new Vector3f();
    private static final Quaternionf spin = new Quaternionf();

    private static final Vector3f planeRight = new Vector3f();
    private static final Vector3f planeUp = new Vector3f();
    private static final Vector3f planeNormal = new Vector3f();

    private static Mode mode = Mode.SHATTER;
    private static boolean active;
    private static float elapsed;
    private static Vec3d origin = Vec3d.ZERO;

    private static float windowHalfW;
    private static float windowHalfH;
    private static float windowMinU;
    private static float windowMaxU;
    private static float windowMinV;
    private static float windowMaxV;

    static {
        for (int i = 0; i < SHARDS; i++) {
            SHARD_POOL[i] = new Shard();
        }
    }

    private MenuCloseEffect() {
    }

    public static boolean active() {
        return active;
    }

    private static Mode selectedMode() {
        ModeSetting setting = ModuleRegistry.closeAnimation();
        if (setting != null && "Zoom Out".equals(setting.current())) {
            return Mode.ZOOM_OUT;
        }
        return Mode.SHATTER;
    }

    public static boolean start(float windowX, float windowY, float windowW, float windowH) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.getWindow() == null) {
            return false;
        }
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null || !ScreenCapture.grab()) {
            return false;
        }

        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        float cosPitch = (float) Math.cos(pitch);
        planeNormal.set((float) -(Math.sin(yaw) * cosPitch), (float) -Math.sin(pitch),
            (float) (Math.cos(yaw) * cosPitch)).normalize();

        planeRight.set((float) -Math.cos(yaw), 0.0f, (float) -Math.sin(yaw)).normalize();
        planeRight.cross(planeNormal, planeUp).normalize();

        Vec3d cameraPos = camera.getCameraPos();
        origin = cameraPos.add(planeNormal.x * DISTANCE, planeNormal.y * DISTANCE, planeNormal.z * DISTANCE);

        float scale = (float) client.getWindow().getScaleFactor();
        float fbWidth = client.getWindow().getFramebufferWidth();
        float fbHeight = client.getWindow().getFramebufferHeight();
        float fov = (float) Math.toRadians(client.options.getFov().getValue());
        float worldPerPixel = 2.0f * DISTANCE * (float) Math.tan(fov * 0.5f) / fbHeight;

        float rectX = windowX * scale;
        float rectY = windowY * scale;
        float rectW = windowW * scale;
        float rectH = windowH * scale;
        windowHalfW = rectW * worldPerPixel * 0.5f;
        windowHalfH = rectH * worldPerPixel * 0.5f;
        windowMinU = rectX / fbWidth;
        windowMaxU = (rectX + rectW) / fbWidth;

        windowMinV = 1.0f - rectY / fbHeight;
        windowMaxV = 1.0f - (rectY + rectH) / fbHeight;

        mode = selectedMode();
        if (mode == Mode.SHATTER) {
            buildShards(rectX, rectY, rectW, rectH, fbWidth, fbHeight);
        }

        elapsed = 0.0f;
        active = true;
        return true;
    }

    private static void buildShards(float rectX, float rectY, float rectW, float rectH, float fbWidth, float fbHeight) {
        float[] columnEdges = edges(COLUMNS);
        float[] rowEdges = edges(ROWS);

        int index = 0;
        for (int cx = 0; cx < COLUMNS; cx++) {
            for (int cy = 0; cy < ROWS; cy++) {
                Shard shard = SHARD_POOL[index++];
                float u0 = columnEdges[cx];
                float u1 = columnEdges[cx + 1];
                float v0 = rowEdges[cy];
                float v1 = rowEdges[cy + 1];

                shard.minU = (rectX + u0 * rectW) / fbWidth;
                shard.maxU = (rectX + u1 * rectW) / fbWidth;
                shard.minV = 1.0f - (rectY + v0 * rectH) / fbHeight;
                shard.maxV = 1.0f - (rectY + v1 * rectH) / fbHeight;

                float centerU = (u0 + u1) * 0.5f - 0.5f;
                float centerV = (v0 + v1) * 0.5f - 0.5f;
                shard.centerX = centerU * 2.0f * windowHalfW;
                shard.centerY = -centerV * 2.0f * windowHalfH;
                shard.halfW = (u1 - u0) * windowHalfW;
                shard.halfH = (v1 - v0) * windowHalfH;

                float outward = 0.42f + RANDOM.nextFloat() * 0.46f;
                shard.velX = centerU * outward + (RANDOM.nextFloat() - 0.5f) * 0.14f;
                shard.velY = -centerV * outward + RANDOM.nextFloat() * 0.16f;
                shard.velZ = (RANDOM.nextFloat() - 0.25f) * 0.3f;

                float ax = RANDOM.nextFloat() - 0.5f;
                float ay = RANDOM.nextFloat() - 0.5f;
                float az = RANDOM.nextFloat() - 0.5f;
                float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
                if (len < 0.0001f) {
                    ax = 1.0f;
                    ay = 0.0f;
                    az = 0.0f;
                    len = 1.0f;
                }
                shard.axisX = ax / len;
                shard.axisY = ay / len;
                shard.axisZ = az / len;
                shard.spinRate = (RANDOM.nextFloat() - 0.5f) * 2.1f;
                shard.delay = RANDOM.nextFloat() * 0.22f;
            }
        }
    }

    private static float[] edges(int cells) {
        float[] result = new float[cells + 1];
        result[0] = 0.0f;
        result[cells] = 1.0f;
        float step = 1.0f / cells;
        for (int i = 1; i < cells; i++) {
            result[i] = i * step + (RANDOM.nextFloat() - 0.5f) * step * 0.55f;
        }
        return result;
    }

    public static void render(WorldRenderContext context) {
        if (!active) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float duration = mode == Mode.SHATTER ? SHATTER_DURATION : ZOOM_DURATION;
        elapsed += Math.min(client.getRenderTickCounter().getDynamicDeltaTicks() / 20.0f, 0.1f);
        if (elapsed >= duration) {
            active = false;
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        if (consumers == null || matrices == null || camera == null) {
            return;
        }

        VertexConsumer buffer = consumers.getBuffer(
            RenderLayers.entityTranslucentEmissive(ScreenCapture.textureId()));

        Vec3d cameraPos = camera.getCameraPos();
        matrices.push();
        matrices.translate(origin.x - cameraPos.x, origin.y - cameraPos.y, origin.z - cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (mode == Mode.SHATTER) {
            renderShatter(buffer, matrix);
        } else {
            renderZoomOut(buffer, matrix);
        }

        matrices.pop();
    }

    private static void renderShatter(VertexConsumer buffer, Matrix4f matrix) {
        for (Shard shard : SHARD_POOL) {
            float t = Math.max(0.0f, elapsed - shard.delay);
            float life = Math.min(1.0f, t / SHATTER_DURATION);

            int alpha = Math.round(255.0f * (1.0f - smoothstep(0.62f, 1.0f, life)));
            if (alpha <= 2) {
                continue;
            }

            float offX = shard.centerX + shard.velX * t;
            float offY = shard.centerY + shard.velY * t - 0.5f * GRAVITY * t * t;
            float offZ = shard.velZ * t;
            spin.identity().rotateAxis(shard.spinRate * t, shard.axisX, shard.axisY, shard.axisZ);

            quad(buffer, matrix, shard.halfW, shard.halfH, offX, offY, offZ,
                shard.minU, shard.maxU, shard.minV, shard.maxV, alpha, true);
        }
    }

    private static void renderZoomOut(VertexConsumer buffer, Matrix4f matrix) {
        float life = Math.min(1.0f, elapsed / ZOOM_DURATION);

        float eased = 1.0f - (float) Math.pow(1.0 - life, 2.2);
        float travel = (ZOOM_END_DISTANCE - DISTANCE) * eased;

        int alpha = Math.round(255.0f * (1.0f - smoothstep(0.2f, 1.0f, life)));
        if (alpha <= 2) {
            return;
        }
        quad(buffer, matrix, windowHalfW * ZOOM_START_SCALE, windowHalfH * ZOOM_START_SCALE,
            0.0f, 0.0f, travel, windowMinU, windowMaxU, windowMinV, windowMaxV, alpha, false);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, float halfW, float halfH,
                             float offX, float offY, float offZ,
                             float minU, float maxU, float minV, float maxV, int alpha, boolean rotated) {
        emit(buffer, matrix, halfW, halfH, offX, offY, offZ, -1.0f, -1.0f, minU, minV, alpha, rotated);
        emit(buffer, matrix, halfW, halfH, offX, offY, offZ, -1.0f, 1.0f, minU, maxV, alpha, rotated);
        emit(buffer, matrix, halfW, halfH, offX, offY, offZ, 1.0f, 1.0f, maxU, maxV, alpha, rotated);
        emit(buffer, matrix, halfW, halfH, offX, offY, offZ, 1.0f, -1.0f, maxU, minV, alpha, rotated);
    }

    private static void emit(VertexConsumer buffer, Matrix4f matrix, float halfW, float halfH,
                             float offX, float offY, float offZ, float signX, float signY,
                             float u, float v, int alpha, boolean rotated) {
        corner.set(signX * halfW, -signY * halfH, 0.0f);
        if (rotated) {
            spin.transform(corner);
        }
        corner.add(offX, offY, offZ);

        scratch.set(planeRight.x * corner.x, planeRight.y * corner.x, planeRight.z * corner.x);
        scratch.add(planeUp.x * corner.y, planeUp.y * corner.y, planeUp.z * corner.y);
        scratch.add(planeNormal.x * corner.z, planeNormal.y * corner.z, planeNormal.z * corner.z);

        buffer.vertex(matrix, scratch.x, scratch.y, scratch.z)
            .color(255, 255, 255, alpha)
            .texture(u, v)
            .overlay(0, 10)
            .light(0xF0, 0xF0)
            .normal(-planeNormal.x, -planeNormal.y, -planeNormal.z);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    private static final class Shard {
        float minU;
        float maxU;
        float minV;
        float maxV;
        float centerX;
        float centerY;
        float halfW;
        float halfH;
        float velX;
        float velY;
        float velZ;
        float axisX;
        float axisY;
        float axisZ;
        float spinRate;
        float delay;
    }
}
