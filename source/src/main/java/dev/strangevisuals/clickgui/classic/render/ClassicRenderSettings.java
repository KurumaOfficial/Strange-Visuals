package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import dev.strangevisuals.clickgui.classic.util.ColorUtil;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.classic.util.RenderUtil;
import dev.strangevisuals.clickgui.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ClassicRenderSettings {
    private static final Identifier ICON_BIND = StrangeVisuals.id("textures/gui/b_s.png");
    private static final Identifier ICON_MODE_ARROW = StrangeVisuals.id("textures/gui/m_d.png");
    private static final Identifier ICON_COLOR_BG = StrangeVisuals.id("textures/gui/c_bg.png");
    private static final Identifier ICON_HUE = StrangeVisuals.id("textures/gui/hue.png");

    private ClassicRenderSettings() {
    }

    public static void renderSettings(DrawContext ctx, List<Setting> settings, float x, float y, double mouseX, double mouseY) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        ClassicTheme theme = ClassicThemeManager.getTheme();
        boolean isTranslucent = theme == ClassicTheme.TRANSPARENT_WHITE
            || theme == ClassicTheme.TRANSPARENT_BLACK
            || theme == ClassicTheme.PURPLE
            || theme == ClassicTheme.PINK;

        int mainCol = isTranslucent
            ? ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 125)
            : ColorUtil.getMainColor(1, 1);
        int textCol = ColorUtil.getTextColor(1, 1);

        float up = 0.0F;

        for (Setting setting : settings) {
            float xSettings = x;
            float ySettings = y + up;

            if (setting instanceof BooleanSetting bool) {
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, bool.name(), xSettings + 6, ySettings + 11, 5, textCol);
                boolean hovered = isHovered(mouseX, mouseY, xSettings + 88, ySettings + 4, 10, 10);
                RenderUtil.Round.draw(ctx, xSettings + 88, ySettings + 4, 10, 10, 2.0F, mainCol);
                if (hovered) {
                    RenderUtil.Border.draw(ctx, xSettings + 88, ySettings + 4, 10, 10, 2.0F, 0.4F, ColorUtil.replAlpha(textCol, 120));
                }
                if (bool.value()) {
                    RenderUtil.Round.draw(ctx, xSettings + 90, ySettings + 6, 6, 6, 1.5F, ColorUtil.getMainColor(1, 1));
                }
                up += 20.0F;
            } else if (setting instanceof SliderSetting slider) {
                String label = truncateToWidth(slider.name(), 5, 55.0F);
                String valStr = slider.display();
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, label, xSettings + 5, ySettings + 8, 5, textCol);
                float valW = FontDraw.getWidth(FontDraw.FontType.MEDIUM, valStr, 5);
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, valStr, xSettings + 98 - valW, ySettings + 8, 5, textCol);

                int size = 94;
                float sliderWidth = slider.fraction() * size;

                RenderUtil.Round.draw(ctx, xSettings + 4, ySettings + 12, size, 5, 2.0F, mainCol);
                if (sliderWidth > 0.0F) {
                    RenderUtil.Round.draw(ctx, xSettings + 4, ySettings + 12, Math.min(size, sliderWidth), 5, 2.0F, ColorUtil.getMainColor(1, 1));
                }
                RenderUtil.Round.draw(ctx, xSettings + 3 + Math.min(size - 3, sliderWidth), ySettings + 11, 3, 7, 1.5F, textCol);
                up += 20.0F;
            } else if (setting instanceof ModeSetting mode) {
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, mode.name(), xSettings + 6, ySettings + 11, 5, textCol);

                float btnX = xSettings + 52.0F;
                float btnY = ySettings + 3.0F;
                float btnW = 44.0F;
                float btnH = 12.0F;

                boolean btnHover = isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
                RenderUtil.Round.draw(ctx, btnX, btnY, btnW, btnH, 2.0F, mainCol);
                if (btnHover) {
                    RenderUtil.Border.draw(ctx, btnX, btnY, btnW, btnH, 2.0F, 0.4F, ColorUtil.replAlpha(textCol, 120));
                }

                RenderUtil.Image.draw(ctx, ICON_MODE_ARROW, btnX + btnW - 10.0F, btnY + 2.0F, 8, 8, textCol);

                String currentMode = mode.current();
                if (currentMode.length() > 7) {
                    currentMode = currentMode.substring(0, 7) + "..";
                }
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, currentMode, btnX + 4.0F, btnY + 8.5F, 5, textCol);

                if (mode.open()) {
                    List<String> options = mode.options();
                    float itemH = 11.0F;
                    float dropY = btnY + btnH + 1.0F;
                    float totalDropH = options.size() * itemH + 2.0F;

                    int dropBg = isTranslucent
                        ? ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), 235)
                        : ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 245);

                    RenderUtil.Round.draw(ctx, btnX, dropY, btnW, totalDropH, 2.0F, dropBg);
                    RenderUtil.Border.draw(ctx, btnX, dropY, btnW, totalDropH, 2.0F, 0.4F, ColorUtil.replAlpha(textCol, 60));

                    for (int i = 0; i < options.size(); i++) {
                        String opt = options.get(i);
                        if (opt.length() > 9) {
                            opt = opt.substring(0, 9) + "..";
                        }
                        float rowY = dropY + 1.0F + i * itemH;
                        boolean itemHover = isHovered(mouseX, mouseY, btnX, rowY, btnW, itemH);

                        if (itemHover) {
                            RenderUtil.Round.draw(ctx, btnX + 1.0F, rowY, btnW - 2.0F, itemH, 1.5F, ColorUtil.replAlpha(textCol, 40));
                        }
                        if (i == mode.index()) {
                            RenderUtil.Round.draw(ctx, btnX + 2.0F, rowY + 2.0F, 2.0F, itemH - 4.0F, 1.0F, ColorUtil.getMainColor(1, 1));
                        }

                        int optCol = (i == mode.index() || itemHover) ? textCol : ColorUtil.replAlpha(textCol, 140);
                        FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, opt, btnX + (i == mode.index() ? 7.0F : 5.0F), rowY + 8.0F, 5, optCol);
                    }
                    up += options.size() * itemH + 4.0F;
                }
                up += 20.0F;
            } else if (setting instanceof ColorSetting color) {
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, color.name(), xSettings + 6, ySettings + 11, 5, textCol);
                RenderUtil.Round.draw(ctx, xSettings + 88, ySettings + 4, 10, 10, 1.5F, mainCol);
                RenderUtil.Round.draw(ctx, xSettings + 90, ySettings + 6, 6, 6, 1.0F, color.rgb() | 0xFF000000);

                if (color.open()) {
                    float xColor = xSettings + 5;
                    float yColor = ySettings + 16;
                    int size = 82;

                    // Container panel
                    RenderUtil.Round.draw(ctx, xColor, yColor, 92, 78, 2.5F, mainCol);

                    // 2D Saturation / Value gradient area
                    RenderUtil.Round.draw(ctx, xColor + 5, yColor + 5, size, 60, 1.5F, color.hueColor() | 0xFF000000);
                    RenderUtil.Image.draw(ctx, ICON_COLOR_BG, xColor + 5, yColor + 5, size, 60, 0xFFFFFFFF);

                    float cursorX = xColor + 5 + (color.saturation() * size) - 3;
                    float cursorY = yColor + 5 + ((1.0F - color.value()) * 60.0F) - 3;
                    RenderUtil.Border.draw(ctx, cursorX, cursorY, 6, 6, 2.0F, 0.3F, 0xFFFFFFFF);

                    // 1D Hue bar
                    RenderUtil.Image.draw(ctx, ICON_HUE, xColor + 5, yColor + 69, size, 4, 0xFFFFFFFF);
                    float hueCursorX = xColor + 5 - 3 + (color.hue() * size);
                    RenderUtil.Border.draw(ctx, hueCursorX, yColor + 68, 6, 6, 2.0F, 0.3F, 0xFFFFFFFF);

                    up += 80.0F;
                }
                up += 20.0F;
            } else if (setting instanceof KeybindSetting bind) {
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, bind.name(), xSettings + 6, ySettings + 11, 5, textCol);
                RenderUtil.Round.draw(ctx, xSettings + 58, ySettings + 4, 40, 10, 1.5F, mainCol);
                RenderUtil.Image.draw(ctx, ICON_BIND, xSettings + 89, ySettings + 5, 8, 8, textCol);

                String bindText = bind.label();
                if (bindText.length() > 8) {
                    bindText = bindText.substring(0, 8) + "...";
                }
                FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, bindText, xSettings + 61, ySettings + 11, 5, textCol);
                up += 20.0F;
            } else {
                up += 20.0F;
            }
        }
    }

    private static String truncateToWidth(String text, int fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (FontDraw.getWidth(FontDraw.FontType.MEDIUM, text, fontSize) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + ellipsis;
            if (FontDraw.getWidth(FontDraw.FontType.MEDIUM, candidate, fontSize) <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    private static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
