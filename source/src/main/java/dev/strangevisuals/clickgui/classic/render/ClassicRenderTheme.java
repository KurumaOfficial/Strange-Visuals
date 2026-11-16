package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.classic.ClassicCategory;
import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.localization.GuiLanguage;
import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import dev.strangevisuals.clickgui.classic.util.ColorUtil;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.classic.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class ClassicRenderTheme {
    private static final Map<ClassicTheme, Float> hoverAnimations = new EnumMap<>(ClassicTheme.class);
    private static final Map<ClassicTheme, Float> selectAnimations = new EnumMap<>(ClassicTheme.class);
    private static final float ANIMATION_SPEED = 0.18F;

    private static final float START_X_OFFSET = 7.0F;
    private static final float START_Y_OFFSET = 50.0F;
    private static final float CARD_WIDTH = 101.0F;
    private static final float CARD_HEIGHT = 28.0F;
    private static final float CARD_SPACING = 8.0F;
    private static final float COLUMN_SPACING = 9.0F;

    private static final float LANG_SWITCH_WIDTH = 56.0F;
    private static final float LANG_SWITCH_HEIGHT = 16.0F;
    private static final float LANG_SWITCH_OFFSET_RIGHT = 6.0F;
    private static final float LANG_SWITCH_OFFSET_TOP = -22.0F;
    private static final float LANG_BUTTON_GAP = 2.0F;

    static {
        for (ClassicTheme t : ClassicTheme.values()) {
            hoverAnimations.put(t, 0.0F);
            selectAnimations.put(t, 0.0F);
        }
    }

    private ClassicRenderTheme() {
    }

    public static void renderTheme(DrawContext ctx, double mouseX, double mouseY) {
        if (ClassicGuiState.selectedCategory != ClassicCategory.THEME) {
            return;
        }

        float startX = ClassicGuiState.x + START_X_OFFSET;
        float startY = ClassicGuiState.y + START_Y_OFFSET;

        ClassicTheme current = ClassicThemeManager.getTheme();
        boolean isTranslucent = current == ClassicTheme.TRANSPARENT_WHITE
            || current == ClassicTheme.TRANSPARENT_BLACK
            || current == ClassicTheme.PURPLE
            || current == ClassicTheme.PINK;
        boolean isBlack = current == ClassicTheme.BLACK;

        ClassicTheme[] themes = ClassicTheme.values();
        for (int i = 0; i < themes.length; i++) {
            ClassicTheme t = themes[i];
            int row = i / 2;
            int col = i % 2;

            float drawX = startX + col * (CARD_WIDTH + COLUMN_SPACING);
            float drawY = startY + row * (CARD_HEIGHT + CARD_SPACING);

            boolean hovered = ClassicGuiState.isHovered(mouseX, mouseY, drawX, drawY, CARD_WIDTH, CARD_HEIGHT);
            float targetHover = hovered ? 1.0F : 0.0F;
            float currentHover = hoverAnimations.get(t) + (targetHover - hoverAnimations.get(t)) * ANIMATION_SPEED;
            hoverAnimations.put(t, currentHover);

            float targetSelect = (t == current) ? 1.0F : 0.0F;
            float currentSelect = selectAnimations.get(t) + (targetSelect - selectAnimations.get(t)) * ANIMATION_SPEED;
            selectAnimations.put(t, currentSelect);

            float finalDrawY = drawY - currentHover * 2.0F;

            // Border color
            int borderAlpha = (int) (80 + currentSelect * 100 + currentHover * 50);
            int borderColor = isBlack
                ? ColorUtil.replAlpha(0xFFFFFFFF, Math.min(255, borderAlpha))
                : isTranslucent
                ? ColorUtil.replAlpha(0x00000000, Math.min(255, borderAlpha / 2))
                : ColorUtil.replAlpha(0x00000000, Math.min(255, borderAlpha));

            RenderUtil.Border.draw(ctx, drawX, finalDrawY, CARD_WIDTH, CARD_HEIGHT, 2.5F,
                0.1F + currentSelect * 0.5F, borderColor);

            // Background
            int bgAlpha = (int) (57 + currentHover * 30);
            int bgColor = isTranslucent
                ? ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), Math.min(255, bgAlpha))
                : ColorUtil.getBackGroundColor(1, 1);
            RenderUtil.Round.draw(ctx, drawX, finalDrawY, CARD_WIDTH, CARD_HEIGHT, 2.5F, bgColor);

            // Preview Thumbnail
            String themeTextureName = t == ClassicTheme.NEON ? "purple" : t.name().toLowerCase();
            Identifier thumb = StrangeVisuals.id("textures/theme/" + themeTextureName + ".png");
            RenderUtil.Image.draw(ctx, thumb, drawX - 0.5F, finalDrawY, CARD_WIDTH + 1.0F, 15.0F, 0xFFFFFFFF);

            // Name
            int textAlpha = (int) (200 + currentHover * 55);
            int textCol = ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), textAlpha);
            FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, t.getName(), drawX + 5.0F, finalDrawY + 22.0F, 6, textCol);

            // Radio indicator
            if (currentSelect > 0.01F) {
                int radioCol = ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), (int) (255 * currentSelect));
                RenderUtil.Round.draw(ctx, drawX + CARD_WIDTH - 10.0F, finalDrawY + 4.0F, 6.0F, 6.0F, 3.0F, radioCol);
            }
        }
    }

    public static void renderLanguageSwitch(DrawContext ctx, double mouseX, double mouseY) {
        float switchX = ClassicGuiState.x + ClassicGuiState.width - LANG_SWITCH_WIDTH - LANG_SWITCH_OFFSET_RIGHT;
        float switchY = ClassicGuiState.y + LANG_SWITCH_OFFSET_TOP;

        boolean dark = ClassicThemeManager.getTheme() == ClassicTheme.BLACK || ClassicThemeManager.getTheme() == ClassicTheme.TRANSPARENT_BLACK;
        int bgColor = dark ? 0xE6121212 : 0xE6F5F5F5;
        int borderColor = dark ? 0xB4505050 : 0xB4B4B4B4;

        RenderUtil.Round.draw(ctx, switchX, switchY, LANG_SWITCH_WIDTH, LANG_SWITCH_HEIGHT, 4.0F, bgColor);
        RenderUtil.Border.draw(ctx, switchX, switchY, LANG_SWITCH_WIDTH, LANG_SWITCH_HEIGHT, 4.0F, 0.5F, borderColor);

        float segW = (LANG_SWITCH_WIDTH - LANG_BUTTON_GAP) / 2.0F;
        float ruX = switchX;
        float enX = switchX + segW + LANG_BUTTON_GAP;

        boolean ruHover = ClassicGuiState.isHovered(mouseX, mouseY, ruX, switchY, segW, LANG_SWITCH_HEIGHT);
        boolean enHover = ClassicGuiState.isHovered(mouseX, mouseY, enX, switchY, segW, LANG_SWITCH_HEIGHT);
        boolean ruActive = GuiLocalization.currentLanguage() == GuiLanguage.RU;
        boolean enActive = GuiLocalization.currentLanguage() == GuiLanguage.EN;

        drawLangSegment(ctx, ruX, switchY, segW, LANG_SWITCH_HEIGHT, GuiLocalization.tr("gui.lang.ru"), ruActive, ruHover, dark);
        drawLangSegment(ctx, enX, switchY, segW, LANG_SWITCH_HEIGHT, GuiLocalization.tr("gui.lang.en"), enActive, enHover, dark);
    }

    private static void drawLangSegment(DrawContext ctx, float x, float y, float w, float h, String label, boolean active, boolean hovered, boolean dark) {
        int bg = active
            ? ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 220)
            : dark
            ? ColorUtil.replAlpha(0xFF1E1E1E, hovered ? 200 : 160)
            : ColorUtil.replAlpha(0xFFE6E6E6, hovered ? 220 : 180);

        int text = active
            ? ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), 255)
            : dark
            ? (hovered ? 0xFFFFFFFF : 0xFFC8C8C8)
            : (hovered ? 0xFF101010 : 0xFF303030);

        RenderUtil.Round.draw(ctx, x, y, w, h, 4.0F, bg);
        FontDraw.drawCenter(FontDraw.FontType.MEDIUM, ctx, label, x + w / 2.0F, y + h / 2.0F + 1.0F, 5, text, false);
    }

    public static boolean isLanguageSwitchHovered(double mouseX, double mouseY) {
        float switchX = ClassicGuiState.x + ClassicGuiState.width - LANG_SWITCH_WIDTH - LANG_SWITCH_OFFSET_RIGHT;
        float switchY = ClassicGuiState.y + LANG_SWITCH_OFFSET_TOP;
        return ClassicGuiState.isHovered(mouseX, mouseY, switchX, switchY, LANG_SWITCH_WIDTH, LANG_SWITCH_HEIGHT);
    }

    public static void handleLanguageSwitchClick(double mouseX, double mouseY) {
        float switchX = ClassicGuiState.x + ClassicGuiState.width - LANG_SWITCH_WIDTH - LANG_SWITCH_OFFSET_RIGHT;
        float switchY = ClassicGuiState.y + LANG_SWITCH_OFFSET_TOP;
        float segW = (LANG_SWITCH_WIDTH - LANG_BUTTON_GAP) / 2.0F;
        float ruX = switchX;
        float enX = switchX + segW + LANG_BUTTON_GAP;

        if (ClassicGuiState.isHovered(mouseX, mouseY, ruX, switchY, segW, LANG_SWITCH_HEIGHT)) {
            GuiLocalization.setLanguage(GuiLanguage.RU);
        } else if (ClassicGuiState.isHovered(mouseX, mouseY, enX, switchY, segW, LANG_SWITCH_HEIGHT)) {
            GuiLocalization.setLanguage(GuiLanguage.EN);
        }
    }

    public static boolean handleThemeClick(double mouseX, double mouseY) {
        if (ClassicGuiState.selectedCategory != ClassicCategory.THEME) {
            return false;
        }

        float startX = ClassicGuiState.x + START_X_OFFSET;
        float startY = ClassicGuiState.y + START_Y_OFFSET;

        ClassicTheme[] themes = ClassicTheme.values();
        for (int i = 0; i < themes.length; i++) {
            ClassicTheme t = themes[i];
            int row = i / 2;
            int col = i % 2;

            float drawX = startX + col * (CARD_WIDTH + COLUMN_SPACING);
            float drawY = startY + row * (CARD_HEIGHT + CARD_SPACING);

            if (ClassicGuiState.isHovered(mouseX, mouseY, drawX, drawY, CARD_WIDTH, CARD_HEIGHT)) {
                ClassicThemeManager.setTheme(t);
                return true;
            }
        }
        return false;
    }
}
