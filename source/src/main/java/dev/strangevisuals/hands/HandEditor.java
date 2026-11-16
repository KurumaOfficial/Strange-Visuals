package dev.strangevisuals.hands;

import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.Arm;

import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

public final class HandEditor {
    public static final float PIVOT_X = 0.56f;
    public static final float PIVOT_Y = -0.52f;
    public static final float PIVOT_Z = -0.72f;

    private static final float CENTRE_Y = -0.26f;
    private static final float CENTRE_OUT = 0.05f;

    private static final float HANDLE_RADIUS = 11.0f;
    private static final float GRAB_RADIUS = 28.0f;

    private static final Anchor right = new Anchor();
    private static final Anchor left = new Anchor();

    private static float fov;
    private static Arm grabbed;
    private static Arm hovering;
    private static double lastX;
    private static double lastY;
    private static boolean dirty;
    private static long lastFrame = System.nanoTime();

    private HandEditor() {
    }

    public static boolean active() {
        MinecraftClient client = MinecraftClient.getInstance();
        return dev.strangevisuals.clickgui.ModuleRegistry.betterHands().enabled()
            && client.currentScreen instanceof ChatScreen
            && client.player != null;
    }

    public static boolean holding() {
        return grabbed != null;
    }

    public static void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null || fov <= 0.0f) {
            right.valid = false;
            left.valid = false;
            return;
        }
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        place(right, Arm.RIGHT, 1.0f, width, height);
        place(left, Arm.LEFT, -1.0f, width, height);
    }

    private static void place(Anchor anchor, Arm arm, float side, float width, float height) {
        float depth = Math.abs(PIVOT_Z + HandPose.z(arm));

        float perPixel = 2.0f * depth * (float) Math.tan(Math.toRadians(fov) * 0.5) / height;
        float scale = HandPose.scale(arm);

        float centreX = side * (PIVOT_X + scale * CENTRE_OUT) + HandPose.x(arm);
        float centreY = PIVOT_Y + scale * (CENTRE_Y - PIVOT_Y) + HandPose.y(arm);

        anchor.perPixelX = perPixel;
        anchor.perPixelY = perPixel;

        float margin = GRAB_RADIUS + 8.0f;
        float wantX = width * 0.5f + centreX / perPixel;
        float wantY = height * 0.5f - centreY / perPixel;
        anchor.screenX = Math.max(margin, Math.min(width - margin, wantX));
        anchor.screenY = Math.max(margin, Math.min(height - margin, wantY));
        anchor.parked = Math.abs(wantX - anchor.screenX) > 0.5f
            || Math.abs(wantY - anchor.screenY) > 0.5f;
        anchor.pixelX = anchor.screenX / width;
        anchor.pixelY = 1.0f - anchor.screenY / height;
        anchor.valid = true;
    }

    public static void setFov(float degrees) {
        fov = degrees;
    }

    public static Arm pick(double mouseX, double mouseY) {
        if (HandPanel.inside(mouseX, mouseY)) {
            return null;
        }
        boolean swapped = right.valid && left.valid && right.screenX < left.screenX;
        Anchor rightBox = swapped ? left : right;
        Anchor leftBox = swapped ? right : left;

        float toRight = rightBox.valid ? distanceTo(rightBox, mouseX, mouseY) : Float.MAX_VALUE;
        float toLeft = leftBox.valid ? distanceTo(leftBox, mouseX, mouseY) : Float.MAX_VALUE;
        float reach = GRAB_RADIUS * GRAB_RADIUS;
        if (toRight > reach && toLeft > reach) {
            return null;
        }
        return toRight <= toLeft ? Arm.RIGHT : Arm.LEFT;
    }

    private static float distanceTo(Anchor anchor, double mouseX, double mouseY) {
        float dx = (float) (mouseX - anchor.screenX);
        float dy = (float) (mouseY - anchor.screenY);
        return dx * dx + dy * dy;
    }

    public static void putSelection(ByteBuffer data) {
        boolean swapped = right.valid && left.valid && right.screenX < left.screenX;
        Anchor rightBox = swapped ? left : right;
        Anchor leftBox = swapped ? right : left;
        data.putFloat(rightBox.valid ? rightBox.pixelX : -10.0f);
        data.putFloat(rightBox.valid ? rightBox.pixelY : -10.0f);
        data.putFloat(leftBox.valid ? leftBox.pixelX : -10.0f);
        data.putFloat(leftBox.valid ? leftBox.pixelY : -10.0f);
        data.putFloat(code(grabbed));
        data.putFloat(code(hovering));
        data.putFloat(0.0f).putFloat(0.0f);
    }

    private static float code(Arm arm) {
        if (arm == Arm.RIGHT) {
            return 1.0f;
        }
        return arm == Arm.LEFT ? 2.0f : 0.0f;
    }

    public static boolean press(double mouseX, double mouseY, int button) {
        if (!active()) {
            return false;
        }
        Arm picked = pick(mouseX, mouseY);
        if (picked == null) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            HandPose.reset(picked);
            HandPose.save();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }
        grabbed = picked;
        lastX = mouseX;
        lastY = mouseY;
        return true;
    }

    public static boolean scroll(double mouseX, double mouseY, double amount) {
        if (!active() || amount == 0.0) {
            return false;
        }
        Arm picked = grabbed != null ? grabbed : pick(mouseX, mouseY);
        if (picked == null) {
            return false;
        }
        if (shiftHeld()) {
            HandPose.move(picked, 0.0f, 0.0f, (float) amount * 0.015f);
        } else {
            HandPose.resize(picked, (float) Math.pow(1.035, amount));
        }
        dirty = true;
        return true;
    }

    private static boolean shiftHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return false;
        }
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active() || client.getWindow() == null) {
            release();
            return;
        }

        if (client.options != null && client.options.getPerspective() != Perspective.FIRST_PERSON) {
            right.valid = false;
            left.valid = false;
            release();
            return;
        }

        long handle = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!down || grabbed == null) {
            release();
            return;
        }

        double scale = client.getWindow().getScaleFactor();
        double mouseX = client.mouse.getX() / scale;
        double mouseY = client.mouse.getY() / scale;
        Anchor anchor = grabbed == Arm.RIGHT ? right : left;
        if (anchor.valid) {
            float dx = (float) ((mouseX - lastX) * anchor.perPixelX);
            float dy = (float) ((mouseY - lastY) * anchor.perPixelY);

            HandPose.move(grabbed, dx, -dy, 0.0f);
            dirty = true;
        }
        lastX = mouseX;
        lastY = mouseY;
    }

    private static void release() {
        if (grabbed != null && dirty) {
            HandPose.save();
            dirty = false;
        }
        grabbed = null;
    }

    public static void renderOverlay(DrawContext context, double mouseX, double mouseY) {
        if (!active()) {
            hovering = null;
            return;
        }
        hovering = grabbed != null ? grabbed : pick(mouseX, mouseY);
        if (hovering == null) {
            return;
        }
        String hint = grabbed != null
            ? "drag to place    wheel to size    shift + wheel for depth"
            : "hold to move    wheel to size    middle click to reset";
        HandPanel.renderHint(context, hint, grabbed != null);
    }

    public static void renderHandles(DrawContext context, double mouseX, double mouseY) {
        if (!active()) {
            hovering = null;
            return;
        }
        long now = System.nanoTime();
        double delta = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        hovering = grabbed != null ? grabbed : pick(mouseX, mouseY);
        boolean swapped = right.valid && left.valid && right.screenX < left.screenX;
        handle(context, swapped ? left : right, hovering == Arm.RIGHT, grabbed == Arm.RIGHT, delta);
        handle(context, swapped ? right : left, hovering == Arm.LEFT, grabbed == Arm.LEFT, delta);
    }

    private static void handle(DrawContext context, Anchor anchor, boolean hovered,
                               boolean held, double delta) {
        if (!anchor.valid) {
            return;
        }
        anchor.attention.step(held ? 1.0 : hovered ? 0.6 : 0.0, delta);
        float show = (float) anchor.attention.get();
        int accent = Theme.accent();
        int bright = Theme.lighten(accent, 0.45f);
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000.0f;

        float radius = HANDLE_RADIUS * (1.0f + show * 0.45f) - (held ? 2.6f : 0.0f);
        if (anchor.parked) {
            radius *= 0.78f;
        }
        GlowPipeline.radial(context, anchor.screenX, anchor.screenY, radius * 2.6f,
            Math.round(54 + show * 70) << 24 | accent & 0xFFFFFF);

        int marks = 16;
        for (int i = 0; i < marks; i++) {
            double angle = Math.PI * 2.0 * i / marks + time * (held ? 1.5f : 0.6f);
            float lit = 0.35f + 0.65f * runner(i / (float) marks, time);
            float alpha = (0.55f + show * 0.45f) * lit;
            GlowPipeline.radial(context,
                anchor.screenX + (float) Math.cos(angle) * radius,
                anchor.screenY + (float) Math.sin(angle) * radius,
                2.1f + show * 0.9f, Math.round(245 * alpha) << 24 | bright & 0xFFFFFF);
        }

        CirclePipeline.draw(context, anchor.screenX, anchor.screenY, 2.4f + show * 1.4f,
            Math.round(200 + show * 55) << 24 | 0xFFFFFF);
    }

    private static float runner(float position, float time) {
        float head = (time * 0.35f) % 1.0f;
        float behind = position - head;
        if (behind < 0.0f) {
            behind += 1.0f;
        }
        if (behind > 0.34f) {
            return 0.0f;
        }
        float fade = 1.0f - behind / 0.34f;
        return fade * fade;
    }

    private static final class Anchor {
        final Spring attention = new Spring(0.0, 900.0, 46.0);
        boolean valid;

        boolean parked;
        float minX;
        float minY;
        float maxX;
        float maxY;
        float screenX;
        float screenY;

        float pixelX;
        float pixelY;
        float perPixelX;
        float perPixelY;

        boolean covers(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }
}
