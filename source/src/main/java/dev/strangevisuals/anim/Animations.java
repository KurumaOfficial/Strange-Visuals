package dev.strangevisuals.anim;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.ui.Spring;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.Perspective;
import org.joml.Matrix3x2fStack;

public final class Animations {
    private static final Spring chat = new Spring(0.0, 780.0, 44.0);
    private static final Spring tab = new Spring(0.0, 900.0, 48.0);
    private static final Spring container = new Spring(0.0, 850.0, 46.0);
    private static final Spring perspective = new Spring(0.0, 620.0, 42.0);

    private static final Spring flip = new Spring(0.0, 540.0, 40.0);
    private static final Spring slotHover = new Spring(0.0, 1500.0, 42.0);

    private static final Spring carried = new Spring(0.0, 1250.0, 40.0);

    private static final float GRAB_DECAY = 0.5f;
    private static final float TYPE_DECAY = 0.22f;

    private static long lastFrame = System.nanoTime();
    private static boolean containerClosing;
    private static long closeStartedAt;
    private static int focusedSlot = -1;
    private static int grabbedSlot = -1;
    private static long grabbedAt = Long.MIN_VALUE / 2;
    private static long typedAt = Long.MIN_VALUE / 2;
    private static String typedLetter;

    private Animations() {
    }

    public static boolean enabled() {
        return ModuleRegistry.betterAnimations() != null && ModuleRegistry.betterAnimations().enabled();
    }

    private static boolean on(String element) {
        return enabled() && ModuleRegistry.animate(element);
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        double delta = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        chat.step(client.currentScreen instanceof ChatScreen ? 1.0 : 0.0, delta);

        boolean containerUp = client.currentScreen instanceof HandledScreen<?>;
        if (!containerUp) {
            containerClosing = false;
            focusedSlot = -1;
            grabbedSlot = -1;
        }
        container.step(containerUp && !containerClosing ? 1.0 : 0.0, delta);
        slotHover.step(focusedSlot >= 0 ? 1.0 : 0.0, delta);

        boolean holding = containerUp && client.player != null
            && !client.player.currentScreenHandler.getCursorStack().isEmpty();
        carried.step(holding ? 1.0 : 0.0, delta);

        boolean tabHeld = client.options != null && client.options.playerListKey.isPressed()
            && client.currentScreen == null;
        tab.step(tabHeld ? 1.0 : 0.0, delta);

        Perspective view = client.options != null ? client.options.getPerspective() : Perspective.FIRST_PERSON;
        perspective.step(view != Perspective.FIRST_PERSON ? 1.0 : 0.0, delta);
        flip.step(view == Perspective.THIRD_PERSON_FRONT ? 1.0 : 0.0, delta);
    }

    public static float chat() {
        return on("chat") ? (float) chat.get() : 1.0f;
    }

    public static float tab() {
        return on("tab") ? (float) tab.get() : 1.0f;
    }

    public static float container() {
        return on("inventory") ? (float) container.get() : 1.0f;
    }

    public static float perspective() {
        if (!on("perspective")) {
            return -1.0f;
        }
        return (float) perspective.get();
    }

    public static float flip() {
        if (!on("perspective")) {
            return -1.0f;
        }
        return (float) flip.get();
    }

    public static boolean beginContainerClose() {
        if (!on("inventory") || containerClosing) {
            return false;
        }
        containerClosing = true;
        closeStartedAt = System.nanoTime();
        return true;
    }

    public static boolean containerClosed() {
        return containerClosing
            && (container.get() < 0.02 || System.nanoTime() - closeStartedAt > 600_000_000L);
    }

    public static void clearContainerClose() {
        containerClosing = false;
    }

    public static void focusSlot(int id) {
        if (id != focusedSlot) {
            focusedSlot = id;
            slotHover.reset(0.0);
        }
    }

    public static void slotGrabbed(int id) {
        grabbedSlot = id;
        grabbedAt = System.nanoTime();
    }

    public static float cursorScale() {
        if (!on("inventory")) {
            return 1.0f;
        }
        float held = (float) carried.get();
        if (held < 0.001f) {
            return 1.0f;
        }
        float breathe = (float) Math.sin(System.currentTimeMillis() / 190.0) * 0.035f;
        return 1.0f + held * (0.22f + breathe) + pop(grabbedAt, GRAB_DECAY) * 0.16f;
    }

    public static float slotScale(int id) {
        if (!on("inventory")) {
            return 1.0f;
        }
        float scale = 1.0f;
        if (id == focusedSlot) {
            scale += (float) slotHover.get() * 0.07f;
        }
        if (id == grabbedSlot) {
            scale += pop(grabbedAt, GRAB_DECAY) * 0.34f;
        }
        return scale;
    }

    public static float slotBurst(int id) {
        if (!on("inventory") || id != grabbedSlot) {
            return -1.0f;
        }
        float age = seconds(grabbedAt) / GRAB_DECAY;
        return age >= 1.0f ? -1.0f : age;
    }

    public static void typed(String letter) {
        typedAt = System.nanoTime();
        typedLetter = letter;
    }

    public static String typedLetter() {
        return typedLetter;
    }

    public static float typeAge() {
        if (!on("chat")) {
            return -1.0f;
        }
        float age = seconds(typedAt) / TYPE_DECAY;
        return age >= 1.0f ? -1.0f : age;
    }

    public static float typePulse() {
        return on("chat") ? decay(typedAt, TYPE_DECAY) : 0.0f;
    }

    private static float seconds(long since) {
        return (System.nanoTime() - since) / 1_000_000_000.0f;
    }

    private static float decay(long since, float window) {
        float age = seconds(since);
        if (age < 0.0f || age >= window) {
            return 0.0f;
        }
        float t = age / window;
        return (float) Math.sin(t * Math.PI) * (1.0f - t);
    }

    private static float pop(long since, float window) {
        float age = seconds(since);
        if (age < 0.0f || age >= window) {
            return 0.0f;
        }
        float t = age / window;
        float attack = 0.16f;
        if (t < attack) {
            float rise = t / attack;
            return rise * rise * (3.0f - 2.0f * rise);
        }
        float fall = (t - attack) / (1.0f - attack);
        float settle = (1.0f - fall) * (1.0f - fall);
        return settle * (1.0f + 0.22f * (float) Math.sin(fall * Math.PI * 2.0));
    }

    public static void pushGrow(Matrix3x2fStack matrices, float centerX, float centerY, float amount, float from) {
        pushScale(matrices, centerX, centerY, from + (1.0f - from) * amount);
    }

    public static void pushScale(Matrix3x2fStack matrices, float centerX, float centerY, float scale) {
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, -centerY);
    }

    public static float ease(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        float inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }
}
