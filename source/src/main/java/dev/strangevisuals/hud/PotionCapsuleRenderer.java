package dev.strangevisuals.hud;

import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.PanelPipeline;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.Locale;

final class PotionCapsuleRenderer {
    static final float CAPSULE_W = 22.0f;
    static final float CAPSULE_H = 58.0f;
    private static final float ICON_SIZE = 13.0f;
    private static final float ICON_TOP_PAD = 5.0f;
    private static final float TRACK_W = 2.0f;
    private static final float NUMBER_SIZE = 6.5f;
    private static final float NUMBER_BOTTOM_PAD = 5.0f;

    private static final int COLOR_NORMAL = 0xFFFFFF;
    private static final int COLOR_HARMFUL = 0xFF5C5C;

    private static final Identifier EXAMPLE_POSITIVE = Identifier.of("minecraft", "textures/mob_effect/regeneration.png");
    private static final Identifier EXAMPLE_NEGATIVE = Identifier.of("minecraft", "textures/mob_effect/poison.png");

    private PotionCapsuleRenderer() {
    }

    static void draw(DrawContext context, GlyphAtlas bold, float x, float y,
                      RegistryEntry<StatusEffect> type, PotionEffectTracker.EffectState state) {
        float appear = (float) state.appear.get();
        if (appear <= 0.01f) {
            return;
        }
        float ease = appear * appear * (3.0f - 2.0f * appear);
        int accent = state.harmful ? COLOR_HARMFUL : COLOR_NORMAL;

        Matrix3x2fStack matrices = beginScale(context, x, y, ease);

        HudSurface.panel(context, "potions", x, y, CAPSULE_W, CAPSULE_H, ease);

        Identifier effectId = Registries.STATUS_EFFECT.getId(type.value());
        Identifier icon = Identifier.of(effectId.getNamespace(), "textures/mob_effect/" + effectId.getPath() + ".png");
        float iconX = x + CAPSULE_W * 0.5f - ICON_SIZE * 0.5f;
        float iconY = y + ICON_TOP_PAD;
        GlowPipeline.radial(context, x + CAPSULE_W * 0.5f, iconY + ICON_SIZE * 0.5f, ICON_SIZE * 0.9f,
            argb(accent, Math.round(35 * ease)));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, Math.round(iconX), Math.round(iconY), 0.0f, 0.0f,
            Math.round(ICON_SIZE), Math.round(ICON_SIZE), 18, 18, 18, 18, argb(0xFFFFFF, Math.round(255 * ease)));

        float trackTop = iconY + ICON_SIZE + 3.0f;
        float trackBottom = y + CAPSULE_H - NUMBER_BOTTOM_PAD - NUMBER_SIZE - 3.0f;
        float trackHeight = Math.max(0.0f, trackBottom - trackTop);
        float trackX = x + CAPSULE_W * 0.5f - TRACK_W * 0.5f;

        float fraction = state.maxDuration <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, state.duration / (float) state.maxDuration));
        PanelPipeline.draw(context, trackX, trackTop, TRACK_W, trackHeight, argb(accent, Math.round(30 * ease)));
        PanelPipeline.draw(context, trackX, trackTop, TRACK_W, trackHeight * fraction, argb(accent, Math.round(210 * ease)));

        float numberBaseline = y + CAPSULE_H - NUMBER_BOTTOM_PAD - NUMBER_SIZE * 0.72f;
        float secondsValue = state.duration / 20.0f;
        String seconds = secondsValue >= 100.0f
            ? String.format(Locale.ROOT, "%.0f", secondsValue)
            : String.format(Locale.ROOT, "%.1f", secondsValue);
        float numberWidth = MsdfText.width(bold, seconds, NUMBER_SIZE);
        float numberSize = numberWidth > CAPSULE_W - 4.0f ? NUMBER_SIZE * (CAPSULE_W - 4.0f) / numberWidth : NUMBER_SIZE;
        MsdfText.drawCentered(context, bold, seconds, x + CAPSULE_W * 0.5f, numberBaseline, numberSize,
            argb(accent, Math.round(245 * ease)));

        if (state.amplifier > 0) {
            float badgeSize = 7.0f;
            float badgeX = x + CAPSULE_W * 0.5f + ICON_SIZE * 0.5f - badgeSize * 0.6f;
            float badgeY = iconY - badgeSize * 0.4f;
            PanelPipeline.draw(context, badgeX, badgeY, badgeSize, badgeSize, argb(0x1A1B20, Math.round(230 * ease)));
            MsdfText.drawCentered(context, bold, String.valueOf(state.amplifier + 1), badgeX + badgeSize * 0.5f,
                badgeY + 1.1f, 4.5f, argb(0xFFFFFF, Math.round(240 * ease)));
        }

        matrices.popMatrix();
    }

    static void drawGhost(DrawContext context, GlyphAtlas bold, float x, float y, boolean harmful) {
        int accent = harmful ? COLOR_HARMFUL : COLOR_NORMAL;
        float dim = 0.55f;

        HudSurface.plate(context, "potions", x, y, CAPSULE_W, CAPSULE_H, 0.45f);

        Identifier icon = harmful ? EXAMPLE_NEGATIVE : EXAMPLE_POSITIVE;
        float iconX = x + CAPSULE_W * 0.5f - ICON_SIZE * 0.5f;
        float iconY = y + ICON_TOP_PAD;
        GlowPipeline.radial(context, x + CAPSULE_W * 0.5f, iconY + ICON_SIZE * 0.5f, ICON_SIZE * 0.9f,
            argb(accent, Math.round(35 * dim)));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, Math.round(iconX), Math.round(iconY), 0.0f, 0.0f,
            Math.round(ICON_SIZE), Math.round(ICON_SIZE), 18, 18, 18, 18, argb(0xFFFFFF, Math.round(255 * dim)));

        float trackTop = iconY + ICON_SIZE + 3.0f;
        float trackBottom = y + CAPSULE_H - NUMBER_BOTTOM_PAD - NUMBER_SIZE - 3.0f;
        float trackHeight = Math.max(0.0f, trackBottom - trackTop);
        float trackX = x + CAPSULE_W * 0.5f - TRACK_W * 0.5f;
        PanelPipeline.draw(context, trackX, trackTop, TRACK_W, trackHeight, argb(accent, Math.round(30 * dim)));
        PanelPipeline.draw(context, trackX, trackTop, TRACK_W, trackHeight * 0.62f, argb(accent, Math.round(210 * dim)));

        float numberBaseline = y + CAPSULE_H - NUMBER_BOTTOM_PAD - NUMBER_SIZE * 0.72f;
        String seconds = harmful ? "3.5" : "12.0";
        MsdfText.drawCentered(context, bold, seconds, x + CAPSULE_W * 0.5f, numberBaseline, NUMBER_SIZE,
            argb(accent, Math.round(245 * dim)));
    }

    private static Matrix3x2fStack beginScale(DrawContext context, float x, float y, float ease) {
        float scale = 0.82f + 0.18f * ease;
        float centerX = x + CAPSULE_W * 0.5f;
        float centerY = y + CAPSULE_H * 0.5f;
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, -centerY);
        return matrices;
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
