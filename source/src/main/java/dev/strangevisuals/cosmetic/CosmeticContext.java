package dev.strangevisuals.cosmetic;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CosmeticContext {
    public final VertexConsumer target;

    public static final int SOLID = 0;

    public static final int SHEET = 1;

    public static final int LIGHT = 2;

    public final int pass;
    public final boolean lit;
    public final Matrix4f matrix;

    public final Vector3f eye = new Vector3f();
    public final AbstractClientPlayerEntity player;

    public final float time;
    public final float size;
    public final int color;

    public final float speed;

    public final float hurt;
    public final float height;

    public CosmeticContext(VertexConsumer target, int pass, Matrix4f matrix,
                           AbstractClientPlayerEntity player, float time, float size, int color,
                           float speed, float hurt, float height) {
        this.target = target;
        this.pass = pass;
        this.lit = pass == LIGHT;
        this.matrix = matrix;
        this.player = player;
        this.time = time;
        this.size = size;
        this.color = color;
        this.speed = speed;
        this.hurt = hurt;
        this.height = height;

        SCRATCH.set(matrix).invert().transformPosition(eye.set(0.0f, 0.0f, 0.0f));
    }

    private static final Matrix4f SCRATCH = new Matrix4f();

    public float drive() {
        return Math.min(1.0f, speed / 5.6f);
    }
}
