package dev.strangevisuals.render;

import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Authentic enhanced Vanilla UI pipeline for StrangeVisuals.
 * Integrates pixel-art icon assets from Icons / Icons-Menus and polished
 * textures from Better Minecraft Menu & Iconic Options.
 */
public final class VanillaStylePipeline {
    public static final Identifier WIDGETS = StrangeVisuals.id("textures/gui/vanilla/widgets.png");
    public static final Identifier SLIDER = StrangeVisuals.id("textures/gui/vanilla/slider.png");
    public static final Identifier OPTIONS_BG = StrangeVisuals.id("textures/gui/vanilla/options_background.png");
    public static final Identifier DIRT_BG = Identifier.ofVanilla("textures/block/dirt.png");
    public static final Identifier LIGHT_DIRT_BG = StrangeVisuals.id("textures/gui/vanilla/light_dirt_background.png");
    public static final Identifier HEADER_SEP = StrangeVisuals.id("textures/gui/vanilla/header_separator.png");
    public static final Identifier FOOTER_SEP = StrangeVisuals.id("textures/gui/vanilla/footer_separator.png");

    public static final Identifier ICONS_MAIN = StrangeVisuals.id("textures/gui/vanilla/main_menu.png");
    public static final Identifier ICONS_OPTIONS = StrangeVisuals.id("textures/gui/vanilla/options.png");
    public static final Identifier ICONS_ESCAPE = StrangeVisuals.id("textures/gui/vanilla/escape_menu.png");
    public static final Identifier ICONS_SINGLEPLAYER = StrangeVisuals.id("textures/gui/vanilla/singleplayer.png");
    public static final Identifier ICONS_MULTIPLAYER = StrangeVisuals.id("textures/gui/vanilla/multiplayer.png");
    public static final Identifier ICONS_VIDEO = StrangeVisuals.id("textures/gui/vanilla/video_settings.png");
    public static final Identifier ICONS_MODS = StrangeVisuals.id("textures/gui/vanilla/modded.png");
    public static final Identifier ICONS_CONTROLS = StrangeVisuals.id("textures/gui/vanilla/controls.png");
    public static final Identifier ICONS_MACROS = StrangeVisuals.id("textures/gui/vanilla/macros.png");
    public static final Identifier ICONS_BACK = StrangeVisuals.id("textures/gui/vanilla/back.png");
    public static final Identifier ICONS_DIRECT = StrangeVisuals.id("textures/gui/vanilla/direct_connect.png");
    public static final Identifier ICONS_RECREATE = StrangeVisuals.id("textures/gui/vanilla/recreate.png");

    public record IconRef(Identifier texture, int u, int v, int width, int height, int sheetW, int sheetH) {
    }

    private VanillaStylePipeline() {
    }

    public static IconRef getIconFor(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String s = label.toLowerCase(java.util.Locale.ROOT).trim();
        String clean = s.replaceAll("§[0-9a-fk-or]", "").replace("...", "").trim();

        // Main Menu
        if (clean.contains("singleplayer") || clean.contains("одиночн") || clean.contains("одиночна") || clean.contains("single")) {
            return new IconRef(ICONS_MAIN, 0, 0, 16, 16, 80, 16);
        }
        if (clean.contains("multiplayer") || clean.contains("сетев") || clean.contains("мережев") || clean.contains("multi")) {
            return new IconRef(ICONS_MAIN, 16, 0, 16, 16, 80, 16);
        }
        if (clean.contains("realms") || clean.contains("alt") || clean.contains("account") || clean.contains("аккаунт") || clean.contains("акаунт")) {
            return new IconRef(ICONS_MAIN, 32, 0, 16, 16, 80, 16);
        }
        if (clean.contains("option") || clean.contains("setting") || clean.contains("настройк") || clean.contains("налаштуван")) {
            return new IconRef(ICONS_MAIN, 48, 0, 16, 16, 80, 16);
        }
        if (clean.contains("quit") || clean.contains("выйти") || clean.contains("вийти") || clean.contains("exit")) {
            return new IconRef(ICONS_MAIN, 64, 0, 16, 16, 80, 16);
        }
        if (clean.contains("mod") || clean.contains("мод")) {
            return new IconRef(ICONS_MODS, 0, 0, 16, 16, 16, 16);
        }

        // Pause Menu
        if (clean.contains("back to game") || clean.contains("вернуться в игру") || clean.contains("повернутися до гри") || clean.equals("return to game") || clean.equals("return")) {
            return new IconRef(ICONS_ESCAPE, 0, 0, 16, 16, 112, 16);
        }
        if (clean.contains("advancement") || clean.contains("достижен") || clean.contains("досягнен")) {
            return new IconRef(ICONS_ESCAPE, 16, 0, 16, 16, 112, 16);
        }
        if (clean.contains("statistic") || clean.contains("статистик")) {
            return new IconRef(ICONS_ESCAPE, 32, 0, 16, 16, 112, 16);
        }
        if (clean.contains("feedback") || clean.contains("отзыв") || clean.contains("відгук")) {
            return new IconRef(ICONS_ESCAPE, 48, 0, 16, 16, 112, 16);
        }
        if (clean.contains("bug") || clean.contains("ошибк") || clean.contains("помилк")) {
            return new IconRef(ICONS_ESCAPE, 64, 0, 16, 16, 112, 16);
        }
        if (clean.contains("lan") || clean.contains("сеть") || clean.contains("мереж")) {
            return new IconRef(ICONS_ESCAPE, 80, 0, 16, 16, 112, 16);
        }
        if (clean.contains("disconnect") || clean.contains("отключ") || clean.contains("від'єдн") || clean.contains("save and quit") || clean.contains("сохранить и выйти")) {
            return new IconRef(ICONS_ESCAPE, 96, 0, 16, 16, 112, 16);
        }
        if (clean.contains("macro") || clean.contains("макрос") || clean.contains("bind") || clean.contains("бинд")) {
            return new IconRef(ICONS_MACROS, 0, 0, 16, 16, 16, 16);
        }

        // Options Categories & Sub-menus
        if (clean.contains("keybind") || clean.contains("клавиш")) {
            return new IconRef(ICONS_CONTROLS, 20, 0, 20, 20, 40, 20);
        }
        if (clean.contains("mouse settings") || clean.contains("настройки мыши") || clean.contains("налаштування миші")) {
            return new IconRef(ICONS_CONTROLS, 0, 0, 20, 20, 40, 20);
        }
        if (clean.contains("fov") || clean.contains("поле зрения") || clean.contains("кут огляду")) {
            return new IconRef(ICONS_OPTIONS, 0, 0, 20, 20, 240, 20);
        }
        if (clean.contains("skin") || clean.contains("скин") || clean.contains("шкір")) {
            return new IconRef(ICONS_OPTIONS, 40, 0, 20, 20, 240, 20);
        }
        if (clean.contains("sound") || clean.contains("music") || clean.contains("звук") || clean.contains("музык") || clean.contains("музик")) {
            return new IconRef(ICONS_OPTIONS, 60, 0, 20, 20, 240, 20);
        }
        if (clean.contains("video") || clean.contains("график") || clean.contains("відео")) {
            return new IconRef(ICONS_OPTIONS, 80, 0, 20, 20, 240, 20);
        }
        if (clean.contains("control") || clean.contains("управлен") || clean.contains("керуван") || clean.contains("mouse") || clean.contains("мышь") || clean.contains("миш")) {
            return new IconRef(ICONS_OPTIONS, 100, 0, 20, 20, 240, 20);
        }
        if (clean.contains("language") || clean.contains("язык") || clean.contains("мова")) {
            return new IconRef(ICONS_OPTIONS, 120, 0, 20, 20, 240, 20);
        }
        if (clean.contains("chat") || clean.contains("чат")) {
            return new IconRef(ICONS_OPTIONS, 140, 0, 20, 20, 240, 20);
        }
        if (clean.contains("resource") || clean.contains("pack") || clean.contains("ресурс") || clean.contains("пакет")) {
            return new IconRef(ICONS_OPTIONS, 160, 0, 20, 20, 240, 20);
        }
        if (clean.contains("accessibility") || clean.contains("доступн")) {
            return new IconRef(ICONS_OPTIONS, 180, 0, 20, 20, 240, 20);
        }
        if (clean.contains("telemetry") || clean.contains("телеметр")) {
            return new IconRef(ICONS_OPTIONS, 200, 0, 20, 20, 240, 20);
        }
        if (clean.contains("credit") || clean.contains("автор") || clean.contains("титр")) {
            return new IconRef(ICONS_OPTIONS, 220, 0, 20, 20, 240, 20);
        }

        // 4. World & Server actions
        if (clean.contains("direct") || clean.contains("по адресу") || clean.contains("по адрессу") || clean.contains("по адресі") || clean.contains("прямое") || clean.contains("пряме")) {
            return new IconRef(ICONS_DIRECT, 0, 0, 16, 16, 16, 16);
        }
        if (clean.contains("play") || clean.contains("играть") || clean.contains("грати")) {
            return new IconRef(ICONS_SINGLEPLAYER, 0, 0, 16, 16, 64, 16);
        }
        if (clean.contains("join") || clean.contains("подключ") || clean.contains("підключ")) {
            return new IconRef(ICONS_SINGLEPLAYER, 0, 0, 16, 16, 64, 16);
        }
        // Re-create: MUST be checked BEFORE "create" / "создать" because "пересоздать" contains "создать"!
        if (clean.contains("re-create") || clean.contains("recreate") || clean.contains("пересоздать") || clean.contains("перестворити")) {
            return null;
        }
        if (clean.contains("add") || clean.contains("добавить") || clean.contains("додати") || clean.contains("create") || clean.contains("создать") || clean.contains("створити") || clean.contains("new world") || clean.contains("новый мир")) {
            return new IconRef(ICONS_MULTIPLAYER, 0, 0, 16, 16, 32, 16);
        }
        if (clean.contains("edit") || clean.contains("настроить") || clean.contains("редакт") || clean.contains("змінити") || clean.contains("налаштувати")) {
            return new IconRef(ICONS_SINGLEPLAYER, 32, 0, 16, 16, 64, 16);
        }
        if (clean.contains("delete") || clean.contains("удал") || clean.contains("видал")) {
            return new IconRef(ICONS_SINGLEPLAYER, 48, 0, 16, 16, 64, 16);
        }
        if (clean.contains("refresh") || clean.contains("обнов") || clean.contains("онов")) {
            return new IconRef(ICONS_MULTIPLAYER, 16, 0, 16, 16, 32, 16);
        }
        if (clean.contains("save") || clean.contains("сохран") || clean.contains("збереж") || clean.contains("done") || clean.contains("готово") || clean.contains("apply") || clean.contains("применить")) {
            return new IconRef(ICONS_VIDEO, 0, 40, 20, 20, 240, 60);
        }
        if (clean.contains("cancel") || clean.contains("отмена") || clean.contains("скасувати") || clean.contains("back") || clean.contains("назад")) {
            return new IconRef(ICONS_BACK, 0, 0, 16, 16, 16, 16);
        }
        if (clean.contains("open") || clean.contains("открыть") || clean.contains("відкрити") || clean.contains("folder") || clean.contains("папк")) {
            return new IconRef(ICONS_OPTIONS, 160, 0, 20, 20, 240, 20);
        }

        return null;
    }

    public static boolean renderButton(DrawContext context, ClickableWidget widget, int mouseX, int mouseY, float delta) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();
        boolean hovered = widget.isSelected() || widget.isHovered() || (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h);
        Text msg = widget.getMessage();
        String originalStr = msg != null ? msg.getString() : "";

        IconRef icon = getIconFor(originalStr);
        if (w <= 24 && icon == null) {
            // Let vanilla render small icon buttons (like vanilla's accessibility button)
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        String clean = originalStr.toLowerCase(java.util.Locale.ROOT).replaceAll("§[0-9a-fk-or]", "").trim();
        boolean isLanguage = clean.contains("language") || clean.contains("язык") || clean.contains("мова");
        String str = originalStr;
        if (w <= 28 || (isLanguage && (w <= 36 || (mc != null && mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen)))) {
            str = "";
        }

        renderButton(context, x, y, w, h, str, icon, widget.active, hovered);
        return true;
    }

    public static void renderButton(DrawContext context, int x, int y, int w, int h, String text, IconRef icon, boolean active, boolean hovered) {
        // vBase in widgets.png (256x256):
        // 46: disabled, 66: normal, 86: hovered
        float vBase = !active ? 46.0f : (hovered ? 86.0f : 66.0f);
        int halfW = w / 2;
        int restW = w - halfW;
        int renderH = Math.min(20, h);
        int renderY = y + (h - renderH) / 2;

        // Draw left side of button (up to halfW)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            x, renderY, 0.0f, vBase,
            halfW, renderH, halfW, renderH,
            256, 256, 0xFFFFFFFF);

        // Draw right side of button
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            x + halfW, renderY, 200.0f - restW, vBase,
            restW, renderH, restW, renderH,
            256, 256, 0xFFFFFFFF);

        // Text & Icon rendering
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int textColor = !active ? 0xFFA0A0A0 : (hovered ? 0xFFFFFFA0 : 0xFFFFFFFF);

        if (icon != null) {
            int isz = Math.min(16, Math.min(w - 4, h - 4));
            int textW = (text != null && !text.isBlank()) ? tr.getWidth(text) : 0;
            int gap = 4;
            int totalContentW = isz + (textW > 0 ? gap + textW : 0);

            if (textW > 0 && (totalContentW > w - 8 || w <= 80)) {
                isz = Math.min(12, h - 4);
                gap = 3;
            }

            int availTextW = Math.max(10, w - 8 - isz - gap);
            float textScale = (textW > availTextW) ? Math.max(0.65f, (float) availTextW / (float) textW) : 1.0f;
            float scaledTextW = textW * textScale;
            float scaledTotalW = isz + (scaledTextW > 0 ? gap + scaledTextW : 0);

            float startX = (scaledTotalW <= w - 8)
                ? (float) x + (w - scaledTotalW) * 0.5f
                : (float) x + 4.0f;

            float ix = startX;
            float iy = (float) y + (h - isz) * 0.5f;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon.texture,
                Math.round(ix), Math.round(iy), (float) icon.u, (float) icon.v,
                isz, isz, icon.width, icon.height,
                icon.sheetW, icon.sheetH, active ? 0xFFFFFFFF : 0x88FFFFFF);

            if (text != null && !text.isBlank()) {
                float tx = ix + isz + gap;
                float ty = (float) y + (h - 8.0f * textScale) * 0.5f;
                if (textScale < 0.999f) {
                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    matrices.translate(tx, ty);
                    matrices.scale(textScale, textScale);
                    context.drawTextWithShadow(tr, text, 0, 0, textColor);
                    matrices.popMatrix();
                } else {
                    context.drawTextWithShadow(tr, text, Math.round(tx), Math.round(ty), textColor);
                }
            }
        } else {
            if (text != null && !text.isBlank()) {
                int textW = tr.getWidth(text);
                int availTextW = Math.max(10, w - 6);
                float textScale = (textW > availTextW) ? Math.max(0.65f, (float) availTextW / (float) textW) : 1.0f;
                float scaledTextW = textW * textScale;
                float tx = (float) x + (w - scaledTextW) * 0.5f;
                float ty = (float) y + (h - 8.0f * textScale) * 0.5f;

                if (textScale < 0.999f) {
                    var matrices = context.getMatrices();
                    matrices.pushMatrix();
                    matrices.translate(tx, ty);
                    matrices.scale(textScale, textScale);
                    context.drawTextWithShadow(tr, text, 0, 0, textColor);
                    matrices.popMatrix();
                } else {
                    context.drawTextWithShadow(tr, text, Math.round(tx), Math.round(ty), textColor);
                }
            }
        }
    }

    public static void renderSlider(DrawContext context, int x, int y, int w, int h, double value, Text message, boolean active, boolean hovered) {
        int halfW = w / 2;
        int restW = w - halfW;
        int renderH = Math.min(20, h);
        int renderY = y + (h - renderH) / 2;

        // 1. Draw slider track (v=46 disabled button look)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            x, renderY, 0.0f, 46.0f,
            halfW, renderH, halfW, renderH,
            256, 256, 0xFFFFFFFF);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            x + halfW, renderY, 200.0f - restW, 46.0f,
            restW, renderH, restW, renderH,
            256, 256, 0xFFFFFFFF);

        // 2. Draw slider thumb (width = 8px)
        int thumbW = 8;
        int thumbX = x + (int) (Math.max(0.0, Math.min(1.0, value)) * (w - thumbW));
        float thumbV = hovered ? 86.0f : 66.0f;

        // Thumb left 4px
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            thumbX, renderY, 0.0f, thumbV,
            4, renderH, 4, renderH,
            256, 256, 0xFFFFFFFF);
        // Thumb right 4px
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS,
            thumbX + 4, renderY, 196.0f, thumbV,
            4, renderH, 4, renderH,
            256, 256, 0xFFFFFFFF);

        // 3. Draw text label
        if (message != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = client.textRenderer;
            String text = message.getString();
            int textW = tr.getWidth(text);
            int tx = x + (w - textW) / 2;
            int ty = y + (h - 8) / 2;
            int textColor = !active ? 0xFFA0A0A0 : (hovered ? 0xFFFFFFA0 : 0xFFFFFFFF);
            context.drawTextWithShadow(tr, text, tx, ty, textColor);
        }
    }

    public static void renderOptionsBackground(DrawContext context, int width, int height) {
        PanoramaManager.render(context, PanoramaManager.PanoramaType.SETTINGS, width, height, 0.0f);
        context.fillGradient(0, 0, width, height, 0x1A000000, 0x30000000);

        // Header and footer separators (seamlessly repeated across screen width)
        int sepTiles = (width + 31) / 32;
        for (int i = 0; i < sepTiles; i++) {
            int w = Math.min(32, width - i * 32);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, HEADER_SEP,
                i * 32, 32, 0.0f, 0.0f,
                w, 2, w, 2,
                32, 2, 0xFFFFFFFF);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, FOOTER_SEP,
                i * 32, height - 36, 0.0f, 0.0f,
                w, 2, w, 2,
                32, 2, 0xFFFFFFFF);
        }

        // Top and bottom header/footer shadow darkening
        context.fillGradient(0, 0, width, 32, 0xAA000000, 0x00000000);
        context.fillGradient(0, height - 36, width, height, 0x00000000, 0xAA000000);
    }

    public static void renderDirtBackground(DrawContext context, int width, int height) {
        PanoramaManager.render(context, PanoramaManager.getProceduralLoadingType(), width, height, 0.0f);
        context.fillGradient(0, 0, width, height, 0x1A000000, 0x30000000);
    }

    public static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y + 1, x + 1, y + h - 1, color);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
