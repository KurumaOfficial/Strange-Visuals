package dev.strangevisuals.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Unified, AAA-grade button and widget rendering engine for StrangeVisuals.
 * Guarantees 100% visual parity with the flagship Main Menu buttons across all
 * options screens, dialogs, sliders, and sub-menus.
 * Features 60fps spring-lerp hover animations, deep obsidian glass cards,
 * ambient neon outlines, and razor-sharp typography.
 */
public final class WidgetStylePipeline {
    private static final Map<ClickableWidget, Float> HOVER_PROGRESS = new WeakHashMap<>();
    private static final Map<ClickableWidget, Long> LAST_UPDATE = new WeakHashMap<>();

    private WidgetStylePipeline() {
    }

    public static float updateHover(ClickableWidget widget, boolean isHovered, float delta) {
        long now = System.nanoTime();
        Long prev = LAST_UPDATE.get(widget);
        LAST_UPDATE.put(widget, now);
        float dt = prev == null ? 0.016f : (float) Math.min(0.05, Math.max(0.001, (now - prev) / 1_000_000_000.0));

        float target = (isHovered && widget.active) ? 1.0f : 0.0f;
        Float current = HOVER_PROGRESS.get(widget);
        if (current == null) {
            current = target;
        } else {
            float speed = 14.0f;
            current += (target - current) * Math.min(1.0f, dt * speed);
        }
        HOVER_PROGRESS.put(widget, current);
        return current;
    }

    public static void renderButton(DrawContext context, ClickableWidget widget, int mouseX, int mouseY, float delta) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();
        boolean isHovered = widget.isSelected() || widget.isHovered() || (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h);
        float hover = updateHover(widget, isHovered, delta);

        Text msg = widget.getMessage();
        int customTextColor = 0;
        if (msg != null) {
            if (msg.getStyle() != null && msg.getStyle().getColor() != null) {
                customTextColor = msg.getStyle().getColor().getRgb();
            } else {
                String raw = msg.getString();
                if (raw.contains("§a") || raw.contains("§2")) {
                    customTextColor = 0x55FF55;
                }
            }
        }

        int customBorderColor = customTextColor != 0 ? (0x60 << 24) | customTextColor : 0;
        drawStyledButton(context, x, y, w, h, msg != null ? msg.getString() : "", 0, null, widget.active, hover, 1.0f, customBorderColor, customTextColor);
    }

    public static void drawStyledButton(DrawContext context, float x, float y, float w, float h,
                                        String label, int iconGlyph, GlyphAtlas iconFont,
                                        boolean enabled, float hover, float visible, int customBorderColor) {
        drawStyledButton(context, x, y, w, h, label, iconGlyph, iconFont, enabled, hover, visible, customBorderColor, 0);
    }

    public static void drawStyledButton(DrawContext context, float x, float y, float w, float h,
                                        String label, int iconGlyph, GlyphAtlas iconFont,
                                        boolean enabled, float hover, float visible, int customBorderColor, int customTextColor) {
        if (!enabled) {
            RoundedRectPipeline.draw(context, x, y, w, h, (Math.round(100.0f * visible) << 24) | 0x080A0E);
            RoundedRectPipeline.drawOutline(context, x, y, w, h, (Math.round(40.0f * visible) << 24) | 0x22242D);
            drawButtonText(context, label, iconGlyph, iconFont, null, x, y, w, h, (Math.round(140.0f * visible) << 24) | 0x657185);
            return;
        }

        // 1. Deep obsidian glass fill (alpha 150 -> 210 on hover)
        int fillAlpha = Math.round((150.0f + hover * 60.0f) * visible);
        RoundedRectPipeline.draw(context, x, y, w, h, (fillAlpha << 24) | 0x0E0F14);

        // 2. Ambient neon outline (soft white 20 -> NeonEdge.cool() 95)
        int defaultBorder = mix(0xFFFFFF, NeonEdge.cool(), hover);
        int borderRgb = customBorderColor != 0 ? (customBorderColor & 0xFFFFFF) : defaultBorder;
        int borderAlpha = Math.round((20.0f + hover * 75.0f) * visible);
        RoundedRectPipeline.drawOutline(context, x, y, w, h, (borderAlpha << 24) | (borderRgb & 0xFFFFFF));

        // 3. Left vertical neon accent strip on hover (identical to Main Menu)
        if (hover > 0.01f) {
            int accentAlpha = Math.round(235.0f * hover * visible);
            int stripRgb = customBorderColor != 0 ? (customBorderColor & 0xFFFFFF) : NeonEdge.cool();
            PanelPipeline.draw(context, x + 2.0f, y + 4.0f, 1.6f, h - 8.0f,
                (accentAlpha << 24) | (stripRgb & 0xFFFFFF));
        }

        // 4. Crisp high-contrast label & icon
        int textRgb;
        if (customTextColor != 0) {
            textRgb = mix(customTextColor, 0x88FF88, hover * 0.35f);
        } else {
            textRgb = mix(0xD7DCE6, 0xFFFFFF, hover);
        }
        int textColor = (Math.round((215.0f + hover * 40.0f) * visible) << 24) | (textRgb & 0xFFFFFF);
        drawButtonText(context, label, iconGlyph, iconFont, null, x, y, w, h, textColor);
    }

    public static void drawStyledButton(DrawContext context, float x, float y, float w, float h,
                                        String label, Identifier iconTexture,
                                        boolean enabled, float hover, float visible, int customBorderColor) {
        if (!enabled) {
            RoundedRectPipeline.draw(context, x, y, w, h, (Math.round(100.0f * visible) << 24) | 0x080A0E);
            RoundedRectPipeline.drawOutline(context, x, y, w, h, (Math.round(40.0f * visible) << 24) | 0x22242D);
            drawButtonText(context, label, 0, null, iconTexture, x, y, w, h, (Math.round(140.0f * visible) << 24) | 0x657185);
            return;
        }

        // 1. Deep obsidian glass fill (alpha 150 -> 210 on hover)
        int fillAlpha = Math.round((150.0f + hover * 60.0f) * visible);
        RoundedRectPipeline.draw(context, x, y, w, h, (fillAlpha << 24) | 0x0E0F14);

        // 2. Ambient neon outline (soft white 20 -> NeonEdge.cool() 95)
        int borderRgb = customBorderColor != 0 ? customBorderColor : mix(0xFFFFFF, NeonEdge.cool(), hover);
        int borderAlpha = Math.round((20.0f + hover * 75.0f) * visible);
        RoundedRectPipeline.drawOutline(context, x, y, w, h, (borderAlpha << 24) | (borderRgb & 0xFFFFFF));

        // 3. Left vertical neon accent strip on hover (identical to Main Menu)
        if (hover > 0.01f) {
            int accentAlpha = Math.round(235.0f * hover * visible);
            PanelPipeline.draw(context, x + 2.0f, y + 4.0f, 1.6f, h - 8.0f,
                (accentAlpha << 24) | (NeonEdge.cool() & 0xFFFFFF));
        }

        // 4. Crisp high-contrast label & icon
        int textRgb = mix(0xD7DCE6, 0xFFFFFF, hover);
        int textColor = (Math.round((215.0f + hover * 40.0f) * visible) << 24) | (textRgb & 0xFFFFFF);
        drawButtonText(context, label, 0, null, iconTexture, x, y, w, h, textColor);
    }

    public static void drawButtonText(DrawContext context, String label, int iconGlyph, GlyphAtlas iconFont,
                                      Identifier iconTexture,
                                      float x, float y, float w, float h, int color) {
        if (label == null) label = "";
        String cleanLabel = label.replaceAll("§[0-9a-fk-or]", "");
        GlyphAtlas font = getTitleFont();
        MinecraftClient client = MinecraftClient.getInstance();

        if (iconTexture != null) {
            float iconSize = Math.min(13.0f, h * 0.58f);
            if (cleanLabel.isEmpty()) {
                int ix = Math.round(x + (w - iconSize) * 0.5f);
                int iy = Math.round(y + (h - iconSize) * 0.5f);
                int isz = Math.round(iconSize);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture, ix, iy, 0.0f, 0.0f, isz, isz, 64, 64, 64, 64, color);
            } else {
                int ix = Math.round(x + 10.0f);
                int iy = Math.round(y + (h - iconSize) * 0.5f);
                int isz = Math.round(iconSize);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture, ix, iy, 0.0f, 0.0f, isz, isz, 64, 64, 64, 64, color);
                float textX = x + iconSize + 15.0f;
                if (font != null && font.canRender(cleanLabel)) {
                    MsdfText.drawOn(context, font, cleanLabel, textX, y + h * 0.5f, 7.8f, color);
                } else {
                    int textY = Math.round(y + (h - 8.0f) * 0.5f);
                    context.drawTextWithShadow(client.textRenderer, label, Math.round(textX), textY, color);
                }
            }
        } else if (iconFont != null && iconGlyph > 0) {
            float iconSize = Math.min(6.5f, h * 0.28f);
            IconPipeline.draw(context, iconFont, iconGlyph, x + 14.0f, y + h * 0.5f, iconSize, color);
            float textX = x + 26.0f;
            if (font != null && font.canRender(cleanLabel)) {
                MsdfText.drawOn(context, font, cleanLabel, textX, y + h * 0.5f, 7.8f, color);
            } else {
                int textY = Math.round(y + (h - 8.0f) * 0.5f);
                context.drawTextWithShadow(client.textRenderer, label, Math.round(textX), textY, color);
            }
        } else {
            if (font != null && font.canRender(cleanLabel)) {
                float size = Math.min(8.0f, Math.max(5.8f, (w - 14.0f) / Math.max(1.0f, cleanLabel.length() * 0.72f)));
                MsdfText.drawCenteredOn(context, font, cleanLabel, x + w * 0.5f, y + h * 0.5f, size, color);
            } else {
                int textY = Math.round(y + (h - 8.0f) * 0.5f);
                context.drawCenteredTextWithShadow(client.textRenderer, label, Math.round(x + w * 0.5f), textY, color);
            }
        }
    }

    public static void drawButtonText(DrawContext context, String label, int iconGlyph, GlyphAtlas iconFont,
                                      float x, float y, float w, float h, int color) {
        drawButtonText(context, label, iconGlyph, iconFont, null, x, y, w, h, color);
    }

    private static final net.minecraft.util.Identifier TITLE_FONT_DATA = dev.strangevisuals.StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final net.minecraft.util.Identifier TITLE_FONT_ATLAS = dev.strangevisuals.StrangeVisuals.id("fonts/msdf/sf_bold.png");
    private static GlyphAtlas titleFont;

    public static GlyphAtlas getTitleFont() {
        if (titleFont == null) {
            try {
                Pipelines.warmUp();
                titleFont = GlyphAtlas.load(TITLE_FONT_DATA, TITLE_FONT_ATLAS);
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to load title font: {}", e.getMessage());
            }
        }
        return titleFont;
    }

    public static void drawCenteredLabel(DrawContext context, Text message, int x, int y, int w, int h, int color) {
        String str = message.getString();
        String cleanStr = str.replaceAll("§[0-9a-fk-or]", "");
        GlyphAtlas font = getTitleFont();
        int customColor = color;
        if (message.getStyle() != null && message.getStyle().getColor() != null) {
            customColor = (color & 0xFF000000) | (message.getStyle().getColor().getRgb() & 0x00FFFFFF);
        } else if (str.contains("§a") || str.contains("§2")) {
            customColor = (color & 0xFF000000) | 0x55FF55;
        }
        if (font != null && font.canRender(cleanStr)) {
            float size = Math.min(8.2f, Math.max(5.8f, (w - 16.0f) / Math.max(1.0f, cleanStr.length() * 0.72f)));
            MsdfText.drawCenteredOn(context, font, cleanStr, x + w * 0.5f, y + h * 0.5f, size, customColor);
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            int textY = y + (h - 8) / 2;
            context.drawCenteredTextWithShadow(client.textRenderer, message, x + w / 2, textY, customColor);
        }
    }

    public static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
