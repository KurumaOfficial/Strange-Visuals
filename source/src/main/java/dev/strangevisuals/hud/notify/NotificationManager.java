package dev.strangevisuals.hud.notify;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.hud.HudSurface;
import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.ui.Spring;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class NotificationManager {
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");
    private static final Identifier ICON = StrangeVisuals.id("textures/gui/toast_icon.png");

    private static final int COLOR_ENABLED = 0x4ADE80;
    private static final int COLOR_DISABLED = 0xFF6B6B;

    private static final float HEIGHT = 17.0f;
    private static final float PADDING = 8.0f;
    private static final float ICON_SIZE = 9.0f;
    private static final float GAP_ICON = 7.0f;
    private static final float DIVIDER_GAP = 7.0f;
    private static final float TEXT_SIZE = 6.8f;
    private static final float MARGIN = 14.0f;
    private static final float STACK_GAP = 5.0f;
    private static final int MAX_VISIBLE = 5;

    private static final Deque<ModuleToast> active = new ConcurrentLinkedDeque<>();

    private static GlyphAtlas bold;
    private static long lastFrame = System.nanoTime();

    private NotificationManager() {
    }

    public static void push(String moduleName, boolean enabled) {
        long now = System.nanoTime() / 1_000_000L;
        active.addLast(new ModuleToast(moduleName, enabled, now));
        while (active.size() > MAX_VISIBLE) {
            active.pollFirst();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(
                enabled ? StrangeVisuals.TOGGLE_ON_SOUND : StrangeVisuals.TOGGLE_OFF_SOUND, 0.5f, 1.0f));
        }
    }

    public static void render(DrawContext context) {
        if (active.isEmpty()) {
            return;
        }
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }

        long now = System.nanoTime() / 1_000_000L;
        long nowNanos = System.nanoTime();
        float deltaSeconds = (float) Math.min((nowNanos - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = nowNanos;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int index = 0;
        Iterator<ModuleToast> iterator = active.iterator();
        while (iterator.hasNext()) {
            ModuleToast toast = iterator.next();
            long age = now - toast.createdAt;
            if (!toast.closing && age > ModuleToast.HOLD_MS) {
                toast.closing = true;
            }

            toast.appear.step(toast.closing ? 0.0 : 1.0, deltaSeconds);
            float appear = (float) toast.appear.get();
            if (toast.closing && appear <= 0.006f) {
                iterator.remove();
                continue;
            }

            toast.width = measureWidth(toast);
            float targetY = screenHeight - MARGIN - HEIGHT - index * (HEIGHT + STACK_GAP);
            if (toast.stackY == null) {
                toast.stackY = new Spring(targetY, 1100.0, 52.0);
            } else {
                toast.stackY.step(targetY, deltaSeconds);
            }
            float y = (float) toast.stackY.get();

            float ease = appear * appear * (3.0f - 2.0f * appear);
            float slide = (1.0f - ease) * 22.0f;
            float x = screenWidth - MARGIN - toast.width + slide;

            renderToast(context, toast, x, y, appear);
            index++;
        }
    }

    private static void renderToast(DrawContext context, ModuleToast toast, float x, float y, float appear) {
        int accent = toast.enabled ? COLOR_ENABLED : COLOR_DISABLED;
        HudSurface.panel(context, "notify", x, y, toast.width, HEIGHT, appear);

        float centerY = y + HEIGHT * 0.5f;
        float iconX = x + PADDING;
        float iconY = centerY - ICON_SIZE * 0.5f;
        GlowPipeline.radial(context, iconX + ICON_SIZE * 0.5f, centerY, ICON_SIZE * 1.5f, argb(accent, Math.round(75 * appear)));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON, Math.round(iconX), Math.round(iconY), 0.0f, 0.0f,
            Math.round(ICON_SIZE), Math.round(ICON_SIZE), 128, 128, 128, 128, argb(accent, Math.round(255 * appear)));

        float dividerX = x + PADDING + ICON_SIZE + GAP_ICON * 0.5f;
        context.fill(Math.round(dividerX), Math.round(y + 4.0f), Math.round(dividerX) + 1, Math.round(y + HEIGHT - 4.0f),
            argb(0xFFFFFF, Math.round(32 * appear)));

        float textX = x + PADDING + ICON_SIZE + GAP_ICON + DIVIDER_GAP;
        float textY = y + (HEIGHT - TEXT_SIZE) * 0.5f - 0.5f;
        String prefix = "Module ";
        String state = toast.enabled ? " enabled" : " disabled";

        MsdfText.draw(context, bold, prefix, textX, textY, TEXT_SIZE, argb(0xFFFFFF, Math.round(250 * appear)));
        float cursor = textX + MsdfText.width(bold, prefix, TEXT_SIZE);
        MsdfText.draw(context, bold, toast.name, cursor, textY, TEXT_SIZE, argb(0x8B8D93, Math.round(240 * appear)));
        cursor += MsdfText.width(bold, toast.name, TEXT_SIZE);
        MsdfText.draw(context, bold, state, cursor, textY, TEXT_SIZE, argb(accent, Math.round(255 * appear)));
    }

    private static float measureWidth(ModuleToast toast) {
        float textWidth = MsdfText.width(bold, "Module ", TEXT_SIZE)
            + MsdfText.width(bold, toast.name, TEXT_SIZE)
            + MsdfText.width(bold, toast.enabled ? " enabled" : " disabled", TEXT_SIZE);
        return PADDING + ICON_SIZE + GAP_ICON + DIVIDER_GAP + textWidth + PADDING;
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
