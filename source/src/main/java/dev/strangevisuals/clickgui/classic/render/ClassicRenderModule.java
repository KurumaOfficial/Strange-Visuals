package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.clickgui.ModuleInfo;
import dev.strangevisuals.clickgui.classic.ClassicCategory;
import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import dev.strangevisuals.clickgui.classic.util.ColorUtil;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.classic.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class ClassicRenderModule {
    private static float descriptionAlpha = 0.0F;
    private static ModuleInfo lastHoveredModule = null;

    private ClassicRenderModule() {
    }

    public static void renderModule(DrawContext ctx, double mouseX, double mouseY) {
        if (ClassicGuiState.selectedCategory == ClassicCategory.THEME) {
            return;
        }

        ClassicTheme theme = ClassicThemeManager.getTheme();
        boolean isTranslucent = theme == ClassicTheme.TRANSPARENT_WHITE
            || theme == ClassicTheme.TRANSPARENT_BLACK
            || theme == ClassicTheme.PURPLE
            || theme == ClassicTheme.PINK;
        boolean isBlack = theme == ClassicTheme.BLACK;

        ClassicGuiState.ModulePanel panel = ClassicGuiState.modulePanelBounds();
        float modulesX = panel.x();
        float modulesY = panel.y();
        float modulesWidth = panel.width();
        float modulesHeight = panel.height();

        // Category title
        int textCol = ColorUtil.getTextColor(1, 1);
        FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, ClassicGuiState.selectedCategory.getName(),
            modulesX + 10, modulesY + 3, 7, textCol);

        ClassicGuiState.scroll.update();

        float listStartY = ClassicGuiState.listStartY();
        float listHeight = ClassicGuiState.listHeight();
        float clipTop = listStartY;
        float clipBottom = listStartY + listHeight;

        List<ModuleInfo> visibleModules = ClassicGuiState.visibleModules();

        ctx.enableScissor((int) modulesX, (int) listStartY, (int) (modulesX + modulesWidth), (int) (listStartY + listHeight));

        float yDown = 0.0F;
        float scrollY = ClassicGuiState.scroll.getScroll();

        for (ModuleInfo module : visibleModules) {
            float up = ClassicGuiState.calcUP(module);
            float drawY = listStartY + yDown + scrollY;

            if (drawY + 26.0F + up < clipTop || drawY > clipBottom) {
                yDown += ClassicGuiState.MODULE_VERTICAL_SPACING + up;
                continue;
            }

            boolean headerHovered = mouseX >= modulesX && mouseX <= modulesX + modulesWidth
                && mouseY >= drawY && mouseY <= drawY + ClassicGuiState.MODULE_HEADER_HEIGHT;

            int cardBorder = headerHovered
                ? ColorUtil.replAlpha(textCol, 100)
                : isBlack
                ? ColorUtil.replAlpha(0xFFFFFFFF, 60)
                : isTranslucent
                ? ColorUtil.replAlpha(0x00000000, 30)
                : ColorUtil.replAlpha(0x00000000, 60);

            int cardBg = isTranslucent
                ? ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), 180)
                : ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), 200);

            // Card body
            RenderUtil.Round.draw(ctx, modulesX, drawY, modulesWidth, 26.0F + up, 5.0F, cardBg);
            RenderUtil.Border.draw(ctx, modulesX, drawY, modulesWidth, 26.0F + up, 5.0F, 0.1F, cardBorder);

            // Icon
            RenderUtil.Image.draw(ctx, ClassicGuiState.selectedCategory.icon(), modulesX + 7, drawY + 7, 12, 12, textCol);

            // Display Name & Keybind
            String nameText = GuiLocalization.moduleName(module.name());
            if (module.binding()) {
                nameText += " [...]";
            } else if (module.hasKeybind()) {
                nameText += " [" + module.keybind() + "]";
            }
            FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, nameText, modulesX + 25, drawY + 12.0F, 6, textCol);

            // Status Badge
            boolean enabled = module.enabled();
            String statusLabel = enabled
                ? GuiLocalization.tr("gui.status.enabled")
                : GuiLocalization.tr("gui.status.disabled");
            float badgeW = FontDraw.getWidth(FontDraw.FontType.MEDIUM, statusLabel, 4);

            int badgeBg = enabled ? 0x3300FF3A : 0x33FF0010;
            int badgeTextCol = enabled ? 0xFF266E2C : 0xFF920009;

            RenderUtil.Round.draw(ctx, modulesX + 23, drawY + 14.5F, badgeW + 5.5F, 7.0F, 3.0F, badgeBg);
            FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, statusLabel, modulesX + 26.0F, drawY + 19.5F, 4, badgeTextCol);

            // 3-dots indicator
            if (module.hasSettings()) {
                float dotsX = modulesX + 185;
                int dotsColor = module.expanded() ? ColorUtil.getMainColor(1, 1) : ColorUtil.replAlpha(textCol, 180);
                RenderUtil.Round.draw(ctx, dotsX, drawY + 8.0F, 3, 3, 1.5F, dotsColor);
                RenderUtil.Round.draw(ctx, dotsX, drawY + 11.5F, 3, 3, 1.5F, dotsColor);
                RenderUtil.Round.draw(ctx, dotsX, drawY + 15.0F, 3, 3, 1.5F, dotsColor);
            }

            // Expanded settings columns
            if (module.hasSettings() && module.expanded()) {
                float headerH = ClassicGuiState.MODULE_HEADER_HEIGHT;
                RenderUtil.Rect.draw(ctx, modulesX + 6.0F, drawY + headerH, modulesWidth - 12.0F, 0.5F, ColorUtil.replAlpha(textCol, 30));
                ClassicRenderSettings.renderSettings(ctx, module.leftSettings(), modulesX + 7, drawY + headerH + 2.0F, mouseX, mouseY);
                ClassicRenderSettings.renderSettings(ctx, module.rightSettings(), modulesX + modulesWidth / 2.0F + 2.0F, drawY + headerH + 2.0F, mouseX, mouseY);
            }

            yDown += ClassicGuiState.MODULE_VERTICAL_SPACING + up;
        }

        ctx.disableScissor();

        float contentHeight = 0.0F;
        for (ModuleInfo m : visibleModules) {
            contentHeight += ClassicGuiState.MODULE_VERTICAL_SPACING + ClassicGuiState.calcUP(m);
        }
        ClassicGuiState.scroll.setMax(contentHeight, listHeight);

        // Hover tooltip
        renderModuleDescription(ctx, mouseX, mouseY, scrollY, modulesX, modulesY, modulesHeight, listStartY, visibleModules);
    }

    private static void renderModuleDescription(DrawContext ctx, double mouseX, double mouseY, float scrollY,
                                                float modulesX, float modulesY, float modulesHeight,
                                                float listStartY, List<ModuleInfo> visibleModules) {
        ModuleInfo hovered = null;
        float yDown = 0.0F;

        for (ModuleInfo m : visibleModules) {
            float up = ClassicGuiState.calcUP(m);
            float drawY = listStartY + yDown + scrollY;

            boolean isVisible = drawY + ClassicGuiState.MODULE_HEADER_HEIGHT > listStartY
                && drawY < modulesY + modulesHeight;

            if (isVisible && ClassicGuiState.isHovered(mouseX, mouseY, modulesX, drawY, ClassicGuiState.MODULE_PANEL_WIDTH, ClassicGuiState.MODULE_HEADER_HEIGHT)) {
                hovered = m;
                break;
            }

            yDown += ClassicGuiState.MODULE_VERTICAL_SPACING + up;
        }

        if (hovered != null && hovered != lastHoveredModule) {
            descriptionAlpha = 0.0F;
        }
        if (hovered != null) {
            descriptionAlpha = Math.min(1.0F, descriptionAlpha + 0.15F);
        } else {
            descriptionAlpha = Math.max(0.0F, descriptionAlpha - 0.20F);
        }
        lastHoveredModule = hovered;

        if (descriptionAlpha > 0.01F && hovered != null && hovered.description() != null && !hovered.description().isBlank()) {
            String desc = GuiLocalization.moduleDescription(hovered.name(), hovered.description());
            float descW = FontDraw.getWidth(FontDraw.FontType.MEDIUM, desc, 6) + 12.0F;
            float descH = 18.0F;

            float descX = (float) mouseX + 10.0F;
            float descY = (float) mouseY - descH - 5.0F;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getWindow() != null) {
                if (descX + descW > mc.getWindow().getScaledWidth()) {
                    descX = (float) mouseX - descW - 10.0F;
                }
                if (descY < 0) {
                    descY = (float) mouseY + 10.0F;
                }
            }

            int bgAlpha = (int) (200 * descriptionAlpha);
            int borderAlpha = (int) (140 * descriptionAlpha);
            int textAlpha = (int) (255 * descriptionAlpha);

            int bgCol = ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), bgAlpha);
            int borderCol = ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), borderAlpha);
            int textCol = ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), textAlpha);

            RenderUtil.Round.draw(ctx, descX, descY, descW, descH, 4.0F, bgCol);
            RenderUtil.Border.draw(ctx, descX, descY, descW, descH, 4.0F, 0.5F, borderCol);
            FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, desc, descX + 6.0F, descY + 12.0F, 6, textCol);
        }
    }
}
